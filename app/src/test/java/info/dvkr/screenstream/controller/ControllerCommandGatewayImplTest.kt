package info.dvkr.screenstream.controller

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import info.dvkr.screenstream.common.controller.ControllerCommand
import info.dvkr.screenstream.common.controller.ControllerCommandResultType
import info.dvkr.screenstream.common.controller.ControllerCommandSource
import info.dvkr.screenstream.common.controller.ControllerSessionStatus
import info.dvkr.screenstream.common.controller.ControllerSnapshotMetadata
import info.dvkr.screenstream.common.controller.ControllerStartRequest
import info.dvkr.screenstream.common.controller.ControllerStreamStatus
import info.dvkr.screenstream.common.controller.ControllerTokenExchangeRequest
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.common.module.StreamingModuleManager
import info.dvkr.screenstream.common.session.HostVisibility
import info.dvkr.screenstream.common.session.MeetingSessionCoordinator
import info.dvkr.screenstream.common.session.MeetingSessionState
import info.dvkr.screenstream.common.session.MeetingSessionTarget
import info.dvkr.screenstream.common.settings.AppSettings
import info.dvkr.screenstream.mjpeg.MjpegStreamingModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

public class ControllerCommandGatewayImplTest {

    @Test
    public fun projectorMarksActiveSessionAsStartingUntilMjpegIsLive() {
        val snapshot = AppControllerSessionSnapshotProjector.project(
            meetingState = MeetingSessionState.Active(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                hostVisibility = HostVisibility.FOREGROUND,
            ),
            metadata = ControllerSnapshotMetadata(
                controllerSessionId = "controller-session-1",
                stateVersion = 4L,
                lastAppliedCommandId = "cmd-start",
                updatedAt = 1_746_000_000_100L,
                ownerControllerId = "win-main-1",
                lastCommandSource = ControllerCommandSource.CONTROLLER_COMMAND,
            ),
            streamingState = ControllerStreamingProjectionState(
                activeModuleId = MjpegStreamingModule.Id,
                isRunning = true,
                isStreaming = false,
            ),
        )

        assertEquals(ControllerSessionStatus.STARTING, snapshot.sessionStatus)
        assertEquals(ControllerStreamStatus.STARTING, snapshot.streamStatus)
    }

    @Test
    public fun projectorMarksActiveSessionAsLiveAfterMjpegStartsStreaming() {
        val snapshot = AppControllerSessionSnapshotProjector.project(
            meetingState = MeetingSessionState.Active(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                hostVisibility = HostVisibility.BACKGROUND,
            ),
            metadata = ControllerSnapshotMetadata(
                controllerSessionId = "controller-session-1",
                stateVersion = 5L,
                lastAppliedCommandId = "cmd-start",
                updatedAt = 1_746_000_000_200L,
                ownerControllerId = "win-main-1",
                lastCommandSource = ControllerCommandSource.CONTROLLER_COMMAND,
            ),
            streamingState = ControllerStreamingProjectionState(
                activeModuleId = MjpegStreamingModule.Id,
                isRunning = true,
                isStreaming = true,
                hasActiveConsumer = true,
            ),
        )

        assertEquals(ControllerSessionStatus.ACTIVE, snapshot.sessionStatus)
        assertEquals(ControllerStreamStatus.LIVE, snapshot.streamStatus)
    }

    @Test
    public fun localManualStartIsRejectedAfterControllerTokenExchange() = runTest {
        val gateway = createGateway()

        gateway.exchangeToken(
            ControllerTokenExchangeRequest(
                pairingToken = "pairing-token",
                controllerSessionId = "win-session-1",
                deviceName = "win-main-1",
            ),
        )

        val result = gateway.handleCommandNow(
            gateway.newLocalStartCommand(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
        )

        assertEquals(ControllerCommandResultType.REJECTED_CONFLICT, result.result)
        assertEquals(ControllerSessionStatus.IDLE, result.snapshot.sessionStatus)
        assertEquals("win-main-1", result.snapshot.ownerControllerId)
        assertEquals("controller-owner-conflict", result.error?.code)
    }

    @Test
    public fun foregroundStartFailureBridgesToStartRejectedLastError() = runTest {
        val gateway = createGateway()
        val startResult = gateway.handleCommandNow(
            ControllerStartRequest(
                commandId = "cmd-start",
                controllerSessionId = "win-session-1",
                stateVersion = 0L,
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
        )

        assertEquals(ControllerCommandResultType.APPLIED, startResult.result)
        assertTrue(startResult.snapshot.sessionStatus == ControllerSessionStatus.STARTING || startResult.snapshot.sessionStatus == ControllerSessionStatus.ACTIVE)

        val failedSnapshot = gateway.onForegroundStartFailed("permission denied")

        assertEquals(ControllerSessionStatus.START_REJECTED, failedSnapshot.sessionStatus)
        assertEquals(ControllerStreamStatus.FAILED, failedSnapshot.streamStatus)
        assertEquals("cmd-start", failedSnapshot.lastError?.commandId)
        assertEquals(ControllerCommandSource.CONTROLLER_COMMAND, failedSnapshot.lastError?.source)
        assertEquals("permission denied", failedSnapshot.lastError?.message)
    }

    private fun createGateway(): ControllerCommandGatewayImpl {
        val fakeModule = FakeStreamingModule()
        val moduleManager = StreamingModuleManager(
            modules = listOf(fakeModule),
            appSettings = FakeAppSettings(streamingModuleId = fakeModule.id),
        )

        return ControllerCommandGatewayImpl(
            hostLauncher = RecordingHostLauncher(),
            meetingSessionCoordinator = MeetingSessionCoordinator(),
            streamingModuleManager = moduleManager,
            coroutineScope = CoroutineScope(StandardTestDispatcher()),
            nowMillis = { 1_746_000_000_000L },
            newId = { "generated-id" },
        )
    }
}

private class RecordingHostLauncher : ControllerHostLauncher {

    val launchedCommands: MutableList<ControllerCommand> = mutableListOf()

    override fun launch(command: ControllerCommand) {
        launchedCommands += command
    }
}

private class FakeAppSettings(
    streamingModuleId: StreamingModule.Id,
) : AppSettings {

    private val state = MutableStateFlow(AppSettings.Data(streamingModule = streamingModuleId))

    override val data: StateFlow<AppSettings.Data> = state

    override suspend fun updateData(transform: AppSettings.Data.() -> AppSettings.Data) {
        state.value = transform(state.value)
    }
}

private class FakeStreamingModule : StreamingModule {

    private val runningState = MutableStateFlow(false)
    private val streamingState = MutableStateFlow(false)
    private val activeConsumerState = MutableStateFlow(false)

    override val id: StreamingModule.Id = MjpegStreamingModule.Id
    override val priority: Int = 30
    override val isRunning: Flow<Boolean> = runningState
    override val isStreaming: Flow<Boolean> = streamingState
    override val hasActiveConsumer: Flow<Boolean> = activeConsumerState
    override val nameResource: Int = 0
    override val descriptionResource: Int = 0
    override val detailsResource: Int = 0

    @Composable
    override fun StreamUIContent(windowWidthSizeClass: StreamingModule.WindowWidthSizeClass, modifier: Modifier) {
        Unit
    }

    override fun startModule(context: Context) {
        runningState.value = true
    }

    override suspend fun stopModule() {
        runningState.value = false
        streamingState.value = false
        activeConsumerState.value = false
    }

    override fun stopStream(reason: String) {
        streamingState.value = false
        activeConsumerState.value = false
    }
}
