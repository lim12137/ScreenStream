package info.dvkr.screenstream.common.controller

import info.dvkr.screenstream.common.session.HostVisibility
import info.dvkr.screenstream.common.session.MeetingSessionStartRejectionReason
import info.dvkr.screenstream.common.session.MeetingSessionState
import info.dvkr.screenstream.common.session.MeetingSessionTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

public class ControllerContractsTest {

    @Test
    public fun writeCommandsDefaultToControllerCommandSource() {
        val startRequest = ControllerStartRequest(
            commandId = "cmd-start",
            controllerSessionId = "controller-session-1",
            stateVersion = 3L,
            roomId = "room-1",
            targetId = "tablet-a",
            entryUrl = "https://host/room-1/tablet-a",
        )
        val switchRequest = ControllerSwitchRequest(
            commandId = "cmd-switch",
            controllerSessionId = "controller-session-1",
            stateVersion = 4L,
            roomId = "room-1",
            nextTargetId = "tablet-b",
            nextEntryUrl = "https://host/room-1/tablet-b",
        )
        val endRequest = ControllerEndRequest(
            commandId = "cmd-end",
            controllerSessionId = "controller-session-1",
            stateVersion = 5L,
            roomId = "room-1",
        )

        assertEquals(ControllerCommandSource.CONTROLLER_COMMAND, startRequest.source)
        assertEquals(ControllerCommandSource.CONTROLLER_COMMAND, switchRequest.source)
        assertEquals(ControllerCommandSource.CONTROLLER_COMMAND, endRequest.source)
        assertEquals(ControllerEndReason.CONTROLLER_EXPLICIT, endRequest.reason)
    }

    @Test
    public fun idleStateProjectsToStoppedSnapshot() {
        val snapshot = ControllerSessionSnapshotProjector.project(
            state = MeetingSessionState.Idle,
            metadata = ControllerSnapshotMetadata(
                controllerSessionId = "controller-session-1",
                stateVersion = 0L,
                lastAppliedCommandId = null,
                updatedAt = 1_746_000_000_000L,
                ownerControllerId = null,
            ),
        )

        assertEquals(
            ControllerSessionSnapshot(
                controllerSessionId = "controller-session-1",
                stateVersion = 0L,
                lastAppliedCommandId = null,
                updatedAt = 1_746_000_000_000L,
                ownerControllerId = null,
                sessionStatus = ControllerSessionStatus.IDLE,
                streamStatus = ControllerStreamStatus.STOPPED,
                hostVisibility = null,
                target = null,
                lastError = null,
            ),
            snapshot,
        )
    }

    @Test
    public fun activeStateProjectsTargetAndVisibility() {
        val snapshot = ControllerSessionSnapshotProjector.project(
            state = MeetingSessionState.Active(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                hostVisibility = HostVisibility.BACKGROUND,
            ),
            metadata = ControllerSnapshotMetadata(
                controllerSessionId = "controller-session-1",
                stateVersion = 8L,
                lastAppliedCommandId = "cmd-start",
                updatedAt = 1_746_000_000_111L,
                ownerControllerId = "win-main-1",
            ),
        )

        assertEquals(ControllerSessionStatus.ACTIVE, snapshot.sessionStatus)
        assertEquals(ControllerStreamStatus.LIVE, snapshot.streamStatus)
        assertEquals(ControllerHostVisibility.BACKGROUND, snapshot.hostVisibility)
        assertEquals(
            ControllerSessionTarget(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
            snapshot.target,
        )
        assertNull(snapshot.lastError)
    }

    @Test
    public fun startRejectedProjectsFailureErrorWithMetadata() {
        val snapshot = ControllerSessionSnapshotProjector.project(
            state = MeetingSessionState.StartRejected(
                roomId = "room-1",
                lastTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                reason = MeetingSessionStartRejectionReason.ForegroundStartFailed(
                    details = "foreground service missing",
                ),
            ),
            metadata = ControllerSnapshotMetadata(
                controllerSessionId = "controller-session-1",
                stateVersion = 9L,
                lastAppliedCommandId = "cmd-start",
                updatedAt = 1_746_000_000_222L,
                ownerControllerId = "win-main-1",
                lastCommandSource = ControllerCommandSource.CONTROLLER_COMMAND,
            ),
        )

        assertEquals(ControllerSessionStatus.START_REJECTED, snapshot.sessionStatus)
        assertEquals(ControllerStreamStatus.FAILED, snapshot.streamStatus)
        assertEquals(
            ControllerSessionTarget(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
            snapshot.target,
        )
        assertEquals(
            ControllerCommandError(
                commandId = "cmd-start",
                controllerSessionId = "controller-session-1",
                source = ControllerCommandSource.CONTROLLER_COMMAND,
                roomId = "room-1",
                targetId = "tablet-a",
                code = "foreground-start-failed",
                message = "foreground service missing",
                occurredAt = 1_746_000_000_222L,
            ),
            snapshot.lastError,
        )
    }

    @Test
    public fun startRejectedWithoutRoomIdDropsTargetProjection() {
        val snapshot = ControllerSessionSnapshotProjector.project(
            state = MeetingSessionState.StartRejected(
                roomId = null,
                lastTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                reason = MeetingSessionStartRejectionReason.ForegroundStartFailed(
                    details = "permission denied",
                ),
            ),
            metadata = ControllerSnapshotMetadata(
                controllerSessionId = "controller-session-1",
                stateVersion = 10L,
                lastAppliedCommandId = "cmd-start",
                updatedAt = 1_746_000_000_333L,
                ownerControllerId = null,
                lastCommandSource = ControllerCommandSource.LOCAL_MANUAL,
            ),
        )

        assertNull(snapshot.target)
        assertEquals(ControllerCommandSource.LOCAL_MANUAL, snapshot.lastError?.source)
        assertEquals("permission denied", snapshot.lastError?.message)
    }
}
