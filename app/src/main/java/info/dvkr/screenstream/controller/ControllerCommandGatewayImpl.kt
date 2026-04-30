package info.dvkr.screenstream.controller

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import info.dvkr.screenstream.SingleActivity
import info.dvkr.screenstream.common.controller.ControllerAccessScope
import info.dvkr.screenstream.common.controller.ControllerCommand
import info.dvkr.screenstream.common.controller.ControllerCommandError
import info.dvkr.screenstream.common.controller.ControllerCommandGateway
import info.dvkr.screenstream.common.controller.ControllerCommandResult
import info.dvkr.screenstream.common.controller.ControllerCommandResultType
import info.dvkr.screenstream.common.controller.ControllerCommandSource
import info.dvkr.screenstream.common.controller.ControllerEndRequest
import info.dvkr.screenstream.common.controller.ControllerSessionSnapshot
import info.dvkr.screenstream.common.controller.ControllerSessionSnapshotProjector
import info.dvkr.screenstream.common.controller.ControllerSnapshotMetadata
import info.dvkr.screenstream.common.controller.ControllerSnapshotRequest
import info.dvkr.screenstream.common.controller.ControllerStartRequest
import info.dvkr.screenstream.common.controller.ControllerStreamStatus
import info.dvkr.screenstream.common.controller.ControllerSwitchRequest
import info.dvkr.screenstream.common.controller.ControllerTokenExchangeRequest
import info.dvkr.screenstream.common.controller.ControllerTokenExchangeResult
import info.dvkr.screenstream.common.controller.ControllerSessionStatus
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.common.module.StreamingModuleManager
import info.dvkr.screenstream.common.session.MeetingSessionCoordinator
import info.dvkr.screenstream.common.session.MeetingSessionEndReason
import info.dvkr.screenstream.common.session.MeetingSessionEvent
import info.dvkr.screenstream.common.session.MeetingSessionState
import info.dvkr.screenstream.mjpeg.MjpegStreamingModule
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal fun interface ControllerHostLauncher {
    fun launch(command: ControllerCommand)
}

internal class SingleActivityControllerHostLauncher(
    private val appContext: Context,
) : ControllerHostLauncher {

    override fun launch(command: ControllerCommand) {
        val intent: Intent = SingleActivity.getIntent(appContext, command).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Handler(Looper.getMainLooper()).post {
            appContext.startActivity(intent)
        }
    }
}

internal data class ControllerStreamingProjectionState(
    val activeModuleId: StreamingModule.Id? = null,
    val isRunning: Boolean = false,
    val isStreaming: Boolean = false,
    val hasActiveConsumer: Boolean = false,
)

internal object AppControllerSessionSnapshotProjector {

    fun project(
        meetingState: MeetingSessionState,
        metadata: ControllerSnapshotMetadata,
        streamingState: ControllerStreamingProjectionState,
    ): ControllerSessionSnapshot {
        val baseSnapshot = ControllerSessionSnapshotProjector.project(
            state = meetingState,
            metadata = metadata,
        )

        return when (meetingState) {
            MeetingSessionState.Idle -> baseSnapshot.copy(
                sessionStatus = ControllerSessionStatus.IDLE,
                streamStatus = ControllerStreamStatus.STOPPED,
            )

            is MeetingSessionState.Active -> {
                val isLive = streamingState.activeModuleId == MjpegStreamingModule.Id &&
                    streamingState.isRunning &&
                    streamingState.isStreaming
                baseSnapshot.copy(
                    sessionStatus = if (isLive) ControllerSessionStatus.ACTIVE else ControllerSessionStatus.STARTING,
                    streamStatus = if (isLive) ControllerStreamStatus.LIVE else ControllerStreamStatus.STARTING,
                )
            }

            is MeetingSessionState.Ending -> baseSnapshot.copy(
                sessionStatus = ControllerSessionStatus.ENDING,
                streamStatus = ControllerStreamStatus.STOPPING,
            )

            is MeetingSessionState.StartRejected -> baseSnapshot.copy(
                sessionStatus = ControllerSessionStatus.START_REJECTED,
                streamStatus = ControllerStreamStatus.FAILED,
            )
        }
    }
}

internal class ControllerCommandGatewayImpl(
    private val hostLauncher: ControllerHostLauncher,
    private val meetingSessionCoordinator: MeetingSessionCoordinator,
    private val streamingModuleManager: StreamingModuleManager,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ControllerCommandGateway {

    private val lock: Any = Any()
    private val streamingProjectionState = MutableStateFlow(ControllerStreamingProjectionState())

    private var controllerSessionId: String = LOCAL_MANUAL_CONTROLLER_SESSION_ID
    private var ownerControllerId: String? = null
    private var stateVersion: Long = 0L
    private var lastAppliedCommandId: String? = null
    private var lastCommandSource: ControllerCommandSource = ControllerCommandSource.LOCAL_MANUAL
    private var updatedAt: Long = nowMillis()
    private var lastObservedMeetingState: MeetingSessionState = meetingSessionCoordinator.currentState()
    private val processedCommandResults: LinkedHashMap<String, ControllerCommandResult> = LinkedHashMap()

    init {
        coroutineScope.launch {
            streamingModuleManager.activeModuleStateFlow.collectLatest { activeModule ->
                if (activeModule == null) {
                    streamingProjectionState.value = ControllerStreamingProjectionState()
                    return@collectLatest
                }

                combine(activeModule.isRunning, activeModule.isStreaming, activeModule.hasActiveConsumer) { isRunning, isStreaming, hasActiveConsumer ->
                    ControllerStreamingProjectionState(
                        activeModuleId = activeModule.id,
                        isRunning = isRunning,
                        isStreaming = isStreaming,
                        hasActiveConsumer = hasActiveConsumer,
                    )
                }.collect { projection ->
                    streamingProjectionState.value = projection
                }
            }
        }
    }

    override suspend fun getSnapshot(request: ControllerSnapshotRequest): ControllerSessionSnapshot =
        synchronized(lock) {
            buildSnapshotLocked()
        }

    override suspend fun handle(command: ControllerCommand): ControllerCommandResult = handleCommandNow(command)

    override suspend fun exchangeToken(request: ControllerTokenExchangeRequest): ControllerTokenExchangeResult =
        synchronized(lock) {
            require(request.controllerSessionId.isNotBlank()) { "controllerSessionId is required" }
            require(request.pairingToken.isNotBlank()) { "pairingToken is required" }

            controllerSessionId = request.controllerSessionId
            ownerControllerId = request.deviceName?.trim().orEmpty().ifBlank { request.controllerSessionId }
            updatedAt = nowMillis()

            ControllerTokenExchangeResult(
                controllerSessionId = controllerSessionId,
                ownerControllerId = ownerControllerId.orEmpty(),
                bearerToken = newToken(),
                expiresAt = updatedAt + TOKEN_TTL_MS,
                stateVersion = stateVersion,
                scope = setOf(ControllerAccessScope.READ, ControllerAccessScope.WRITE),
            )
        }

    internal fun currentSnapshot(): ControllerSessionSnapshot = synchronized(lock) {
        buildSnapshotLocked()
    }

    internal fun newLocalStartCommand(
        roomId: String,
        targetId: String,
        entryUrl: String,
    ): ControllerStartRequest = synchronized(lock) {
        ControllerStartRequest(
            commandId = "local-start-${newId()}",
            controllerSessionId = LOCAL_MANUAL_CONTROLLER_SESSION_ID,
            stateVersion = stateVersion,
            roomId = roomId,
            targetId = targetId,
            entryUrl = entryUrl,
            source = ControllerCommandSource.LOCAL_MANUAL,
        )
    }

    internal fun newLocalSwitchCommand(
        roomId: String,
        targetId: String,
        entryUrl: String,
    ): ControllerSwitchRequest = synchronized(lock) {
        ControllerSwitchRequest(
            commandId = "local-switch-${newId()}",
            controllerSessionId = LOCAL_MANUAL_CONTROLLER_SESSION_ID,
            stateVersion = stateVersion,
            roomId = roomId,
            nextTargetId = targetId,
            nextEntryUrl = entryUrl,
            source = ControllerCommandSource.LOCAL_MANUAL,
        )
    }

    internal fun newLocalEndCommand(roomId: String): ControllerEndRequest = synchronized(lock) {
        ControllerEndRequest(
            commandId = "local-end-${newId()}",
            controllerSessionId = LOCAL_MANUAL_CONTROLLER_SESSION_ID,
            stateVersion = stateVersion,
            roomId = roomId,
            source = ControllerCommandSource.LOCAL_MANUAL,
        )
    }

    internal fun localManualControllerSessionId(): String = LOCAL_MANUAL_CONTROLLER_SESSION_ID

    internal fun newCommandId(prefix: String): String = "$prefix-${newId()}"

    internal fun handleCommandNow(command: ControllerCommand): ControllerCommandResult {
        val (result, launchCommand) = synchronized(lock) {
            processedCommandResults[command.commandId]?.let { storedResult ->
                return@synchronized storedResult.copy(result = ControllerCommandResultType.ALREADY_APPLIED) to null
            }

            val currentState = meetingSessionCoordinator.currentState()

            validateCommandLocked(command = command, currentState = currentState)?.let { rejection ->
                return@synchronized rejection to null
            }

            val nextState = applyCommandLocked(command = command, currentState = currentState)
            val stateChanged = nextState != currentState
            if (stateChanged) {
                applyCommandMetadataLocked(command = command, nextState = nextState)
                val snapshot = buildSnapshotLocked()
                val result = ControllerCommandResult(
                    commandId = command.commandId,
                    controllerSessionId = controllerSessionId,
                    stateVersion = stateVersion,
                    result = ControllerCommandResultType.APPLIED,
                    snapshot = snapshot,
                    error = snapshot.lastError,
                )
                storeCommandResultLocked(result)
                result to if (command.source == ControllerCommandSource.CONTROLLER_COMMAND) command else null
            } else {
                ControllerCommandResult(
                    commandId = command.commandId,
                    controllerSessionId = controllerSessionId,
                    stateVersion = stateVersion,
                    result = ControllerCommandResultType.NOOP,
                    snapshot = buildSnapshotLocked(),
                    error = null,
                ) to null
            }
        }

        launchCommand?.let(hostLauncher::launch)
        return result
    }

    internal fun onMeetingStateObserved() {
        synchronized(lock) {
            updateObservedMeetingStateLocked()
        }
    }

    internal fun onForegroundStartFailed(reason: String): ControllerSessionSnapshot = synchronized(lock) {
        val currentState = meetingSessionCoordinator.currentState()
        if (currentState is MeetingSessionState.Idle && lastAppliedCommandId == null) {
            return@synchronized buildSnapshotLocked()
        }

        val roomId = when (currentState) {
            is MeetingSessionState.Active -> currentState.roomId
            is MeetingSessionState.Ending -> currentState.roomId
            is MeetingSessionState.StartRejected -> currentState.roomId
            MeetingSessionState.Idle -> null
        }

        meetingSessionCoordinator.handleEvent(
            MeetingSessionEvent.ForegroundStartFailed(
                roomId = roomId,
                reason = reason,
            ),
        )
        updateObservedMeetingStateLocked()

        val snapshot = buildSnapshotLocked()
        lastAppliedCommandId?.let { commandId ->
            storeCommandResultLocked(
                ControllerCommandResult(
                    commandId = commandId,
                    controllerSessionId = snapshot.controllerSessionId,
                    stateVersion = snapshot.stateVersion,
                    result = ControllerCommandResultType.REJECTED_UNAVAILABLE,
                    snapshot = snapshot,
                    error = snapshot.lastError,
                ),
            )
        }
        snapshot
    }

    internal fun completeEnding(roomId: String): ControllerSessionSnapshot = synchronized(lock) {
        meetingSessionCoordinator.completeEnding(roomId)
        updateObservedMeetingStateLocked()
        buildSnapshotLocked()
    }

    private fun validateCommandLocked(
        command: ControllerCommand,
        currentState: MeetingSessionState,
    ): ControllerCommandResult? {
        if (command.stateVersion != stateVersion) {
            return rejectionResultLocked(
                command = command,
                resultType = ControllerCommandResultType.REJECTED_STALE,
                code = "stale-state-version",
                message = "Expected stateVersion=$stateVersion but was ${command.stateVersion}",
            )
        }

        val activeControllerOwnsSession = ownerControllerId != null && controllerSessionId != LOCAL_MANUAL_CONTROLLER_SESSION_ID
        if (command.source == ControllerCommandSource.LOCAL_MANUAL && activeControllerOwnsSession) {
            return rejectionResultLocked(
                command = command,
                resultType = ControllerCommandResultType.REJECTED_CONFLICT,
                code = "controller-owner-conflict",
                message = "Controller session $controllerSessionId currently owns the meeting",
            )
        }

        if (command.source == ControllerCommandSource.CONTROLLER_COMMAND &&
            activeControllerOwnsSession &&
            command.controllerSessionId != controllerSessionId
        ) {
            return rejectionResultLocked(
                command = command,
                resultType = ControllerCommandResultType.REJECTED_CONFLICT,
                code = "controller-session-mismatch",
                message = "Controller session ${command.controllerSessionId} does not own the meeting",
            )
        }

        return when (command) {
            is ControllerStartRequest -> validateStartLocked(command = command, currentState = currentState)
            is ControllerSwitchRequest -> validateSwitchLocked(command = command, currentState = currentState)
            is ControllerEndRequest -> validateEndLocked(command = command, currentState = currentState)
        }
    }

    private fun validateStartLocked(
        command: ControllerStartRequest,
        currentState: MeetingSessionState,
    ): ControllerCommandResult? = when (currentState) {
        MeetingSessionState.Idle,
        is MeetingSessionState.StartRejected -> null

        is MeetingSessionState.Active -> when {
            currentState.roomId == command.roomId &&
                currentState.currentTarget.targetId == command.targetId &&
                currentState.currentTarget.entryUrl == command.entryUrl -> null

            currentState.roomId == command.roomId -> null

            else -> rejectionResultLocked(
                command = command,
                resultType = ControllerCommandResultType.REJECTED_CONFLICT,
                code = "room-conflict",
                message = "Meeting room ${currentState.roomId} is already active",
            )
        }

        is MeetingSessionState.Ending -> rejectionResultLocked(
            command = command,
            resultType = ControllerCommandResultType.REJECTED_CONFLICT,
            code = "meeting-ending",
            message = "Meeting room ${currentState.roomId} is ending",
        )
    }

    private fun validateSwitchLocked(
        command: ControllerSwitchRequest,
        currentState: MeetingSessionState,
    ): ControllerCommandResult? = when (currentState) {
        is MeetingSessionState.Active -> when {
            currentState.roomId != command.roomId -> rejectionResultLocked(
                command = command,
                resultType = ControllerCommandResultType.REJECTED_CONFLICT,
                code = "room-mismatch",
                message = "Meeting room ${currentState.roomId} does not match ${command.roomId}",
            )

            else -> null
        }

        is MeetingSessionState.Ending -> rejectionResultLocked(
            command = command,
            resultType = ControllerCommandResultType.REJECTED_CONFLICT,
            code = "meeting-ending",
            message = "Meeting room ${currentState.roomId} is ending",
        )

        MeetingSessionState.Idle,
        is MeetingSessionState.StartRejected -> rejectionResultLocked(
            command = command,
            resultType = ControllerCommandResultType.REJECTED_CONFLICT,
            code = "meeting-not-active",
            message = "No active meeting available for switch",
        )
    }

    private fun validateEndLocked(
        command: ControllerEndRequest,
        currentState: MeetingSessionState,
    ): ControllerCommandResult? = when (currentState) {
        is MeetingSessionState.Active -> if (currentState.roomId == command.roomId) null else rejectionResultLocked(
            command = command,
            resultType = ControllerCommandResultType.REJECTED_CONFLICT,
            code = "room-mismatch",
            message = "Meeting room ${currentState.roomId} does not match ${command.roomId}",
        )

        is MeetingSessionState.Ending -> if (currentState.roomId == command.roomId) null else rejectionResultLocked(
            command = command,
            resultType = ControllerCommandResultType.REJECTED_CONFLICT,
            code = "room-mismatch",
            message = "Meeting room ${currentState.roomId} does not match ${command.roomId}",
        )

        MeetingSessionState.Idle,
        is MeetingSessionState.StartRejected -> null
    }

    private fun applyCommandLocked(
        command: ControllerCommand,
        currentState: MeetingSessionState,
    ): MeetingSessionState = when (command) {
        is ControllerStartRequest -> when (currentState) {
            is MeetingSessionState.Active -> {
                if (currentState.roomId == command.roomId &&
                    currentState.currentTarget.targetId == command.targetId &&
                    currentState.currentTarget.entryUrl == command.entryUrl
                ) {
                    currentState
                } else if (currentState.roomId == command.roomId) {
                    meetingSessionCoordinator.handleEvent(
                        MeetingSessionEvent.SwitchTarget(
                            roomId = command.roomId,
                            nextTargetId = command.targetId,
                            nextEntryUrl = command.entryUrl,
                        ),
                    )
                } else {
                    currentState
                }
            }

            else -> meetingSessionCoordinator.handleEvent(
                MeetingSessionEvent.StartRoom(
                    roomId = command.roomId,
                    targetId = command.targetId,
                    entryUrl = command.entryUrl,
                ),
            )
        }

        is ControllerSwitchRequest -> when (currentState) {
            is MeetingSessionState.Active -> {
                if (currentState.roomId == command.roomId &&
                    currentState.currentTarget.targetId == command.nextTargetId &&
                    currentState.currentTarget.entryUrl == command.nextEntryUrl
                ) {
                    currentState
                } else {
                    meetingSessionCoordinator.handleEvent(
                        MeetingSessionEvent.SwitchTarget(
                            roomId = command.roomId,
                            nextTargetId = command.nextTargetId,
                            nextEntryUrl = command.nextEntryUrl,
                        ),
                    )
                }
            }

            else -> currentState
        }

        is ControllerEndRequest -> when (currentState) {
            is MeetingSessionState.Active -> meetingSessionCoordinator.handleEvent(
                MeetingSessionEvent.EndRoom(
                    roomId = command.roomId,
                    reason = command.reason.toMeetingEndReason(),
                ),
            )

            else -> currentState
        }
    }

    private fun applyCommandMetadataLocked(
        command: ControllerCommand,
        nextState: MeetingSessionState,
    ) {
        if (command.source == ControllerCommandSource.CONTROLLER_COMMAND) {
            controllerSessionId = command.controllerSessionId
            ownerControllerId = ownerControllerId ?: command.controllerSessionId
        } else if (ownerControllerId == null) {
            controllerSessionId = LOCAL_MANUAL_CONTROLLER_SESSION_ID
        }

        lastAppliedCommandId = command.commandId
        lastCommandSource = command.source
        lastObservedMeetingState = nextState
        stateVersion += 1
        updatedAt = nowMillis()
    }

    private fun updateObservedMeetingStateLocked() {
        val currentState = meetingSessionCoordinator.currentState()
        if (currentState == lastObservedMeetingState) return

        lastObservedMeetingState = currentState
        stateVersion += 1
        updatedAt = nowMillis()
    }

    private fun buildSnapshotLocked(): ControllerSessionSnapshot = AppControllerSessionSnapshotProjector.project(
        meetingState = meetingSessionCoordinator.currentState(),
        metadata = ControllerSnapshotMetadata(
            controllerSessionId = controllerSessionId,
            stateVersion = stateVersion,
            lastAppliedCommandId = lastAppliedCommandId,
            updatedAt = updatedAt,
            ownerControllerId = ownerControllerId,
            lastCommandSource = lastCommandSource,
        ),
        streamingState = streamingProjectionState.value,
    )

    private fun rejectionResultLocked(
        command: ControllerCommand,
        resultType: ControllerCommandResultType,
        code: String,
        message: String,
    ): ControllerCommandResult {
        val snapshot = buildSnapshotLocked()
        val error = ControllerCommandError(
            commandId = command.commandId,
            controllerSessionId = snapshot.controllerSessionId,
            source = command.source,
            roomId = snapshot.target?.roomId,
            targetId = snapshot.target?.targetId,
            code = code,
            message = message,
            occurredAt = nowMillis(),
        )
        return ControllerCommandResult(
            commandId = command.commandId,
            controllerSessionId = snapshot.controllerSessionId,
            stateVersion = snapshot.stateVersion,
            result = resultType,
            snapshot = snapshot,
            error = error,
        )
    }

    private fun storeCommandResultLocked(result: ControllerCommandResult) {
        processedCommandResults[result.commandId] = result
        while (processedCommandResults.size > MAX_PROCESSED_COMMANDS) {
            val eldestKey = processedCommandResults.entries.first().key
            processedCommandResults.remove(eldestKey)
        }
    }

    private fun newToken(): String = "ctrl-${newId()}"

    private fun info.dvkr.screenstream.common.controller.ControllerEndReason.toMeetingEndReason(): MeetingSessionEndReason = when (this) {
        info.dvkr.screenstream.common.controller.ControllerEndReason.CONTROLLER_EXPLICIT -> MeetingSessionEndReason.CONTROLLER_EXPLICIT
    }

    private companion object {
        private const val LOCAL_MANUAL_CONTROLLER_SESSION_ID: String = "local-manual-session"
        private const val MAX_PROCESSED_COMMANDS: Int = 64
        private const val TOKEN_TTL_MS: Long = 24L * 60L * 60L * 1000L
    }
}
