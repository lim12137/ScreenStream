package info.dvkr.screenstream.common.controller

import info.dvkr.screenstream.common.session.HostVisibility
import info.dvkr.screenstream.common.session.MeetingSessionStartRejectionReason
import info.dvkr.screenstream.common.session.MeetingSessionState
import info.dvkr.screenstream.common.session.MeetingSessionTarget

public data class ControllerSnapshotMetadata(
    public val controllerSessionId: String,
    public val stateVersion: Long,
    public val lastAppliedCommandId: String?,
    public val updatedAt: Long,
    public val ownerControllerId: String?,
    public val lastCommandSource: ControllerCommandSource = ControllerCommandSource.CONTROLLER_COMMAND,
)

public object ControllerSessionSnapshotProjector {

    public fun project(
        state: MeetingSessionState,
        metadata: ControllerSnapshotMetadata,
    ): ControllerSessionSnapshot = when (state) {
        MeetingSessionState.Idle -> ControllerSessionSnapshot(
            controllerSessionId = metadata.controllerSessionId,
            stateVersion = metadata.stateVersion,
            lastAppliedCommandId = metadata.lastAppliedCommandId,
            updatedAt = metadata.updatedAt,
            ownerControllerId = metadata.ownerControllerId,
            sessionStatus = ControllerSessionStatus.IDLE,
            streamStatus = ControllerStreamStatus.STOPPED,
            hostVisibility = null,
            target = null,
            lastError = null,
        )

        is MeetingSessionState.Active -> ControllerSessionSnapshot(
            controllerSessionId = metadata.controllerSessionId,
            stateVersion = metadata.stateVersion,
            lastAppliedCommandId = metadata.lastAppliedCommandId,
            updatedAt = metadata.updatedAt,
            ownerControllerId = metadata.ownerControllerId,
            sessionStatus = ControllerSessionStatus.ACTIVE,
            streamStatus = ControllerStreamStatus.LIVE,
            hostVisibility = state.hostVisibility.toControllerHostVisibility(),
            target = state.currentTarget.toControllerTarget(roomId = state.roomId),
            lastError = null,
        )

        is MeetingSessionState.Ending -> ControllerSessionSnapshot(
            controllerSessionId = metadata.controllerSessionId,
            stateVersion = metadata.stateVersion,
            lastAppliedCommandId = metadata.lastAppliedCommandId,
            updatedAt = metadata.updatedAt,
            ownerControllerId = metadata.ownerControllerId,
            sessionStatus = ControllerSessionStatus.ENDING,
            streamStatus = ControllerStreamStatus.STOPPING,
            hostVisibility = state.hostVisibility.toControllerHostVisibility(),
            target = state.currentTarget.toControllerTarget(roomId = state.roomId),
            lastError = null,
        )

        is MeetingSessionState.StartRejected -> ControllerSessionSnapshot(
            controllerSessionId = metadata.controllerSessionId,
            stateVersion = metadata.stateVersion,
            lastAppliedCommandId = metadata.lastAppliedCommandId,
            updatedAt = metadata.updatedAt,
            ownerControllerId = metadata.ownerControllerId,
            sessionStatus = ControllerSessionStatus.START_REJECTED,
            streamStatus = ControllerStreamStatus.FAILED,
            hostVisibility = null,
            target = state.lastTarget?.let { target ->
                state.roomId?.let { roomId -> target.toControllerTarget(roomId = roomId) }
            },
            lastError = state.toControllerCommandError(metadata = metadata),
        )
    }

    private fun HostVisibility.toControllerHostVisibility(): ControllerHostVisibility = when (this) {
        HostVisibility.FOREGROUND -> ControllerHostVisibility.FOREGROUND
        HostVisibility.BACKGROUND -> ControllerHostVisibility.BACKGROUND
    }

    private fun MeetingSessionTarget.toControllerTarget(roomId: String): ControllerSessionTarget = ControllerSessionTarget(
        roomId = roomId,
        targetId = targetId,
        entryUrl = entryUrl,
    )

    private fun MeetingSessionState.StartRejected.toControllerCommandError(
        metadata: ControllerSnapshotMetadata,
    ): ControllerCommandError {
        val (code, message) = reason.toControllerErrorCodeAndMessage()
        return ControllerCommandError(
            commandId = metadata.lastAppliedCommandId,
            controllerSessionId = metadata.controllerSessionId,
            source = metadata.lastCommandSource,
            roomId = roomId,
            targetId = lastTarget?.targetId,
            code = code,
            message = message,
            occurredAt = metadata.updatedAt,
        )
    }

    private fun MeetingSessionStartRejectionReason.toControllerErrorCodeAndMessage(): Pair<String, String> = when (this) {
        is MeetingSessionStartRejectionReason.ForegroundStartFailed -> FOREGROUND_START_FAILED_CODE to details
    }

    private const val FOREGROUND_START_FAILED_CODE: String = "foreground-start-failed"
}
