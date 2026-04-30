package info.dvkr.screenstream.common.session

import org.junit.Assert.assertEquals
import org.junit.Test

public class MeetingSessionCoordinatorTest {

    @Test
    public fun startRoomEntersActiveStateWithForegroundVisibility() {
        val coordinator = MeetingSessionCoordinator()

        val state = coordinator.handleEvent(
            MeetingSessionEvent.StartRoom(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
        )

        assertEquals(
            MeetingSessionState.Active(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                hostVisibility = HostVisibility.FOREGROUND,
            ),
            state,
        )
        assertEquals(state, coordinator.stateFlow.value)
    }

    @Test
    public fun switchTargetUpdatesActiveRoomOnly() {
        val coordinator = MeetingSessionCoordinator()
        coordinator.handleEvent(
            MeetingSessionEvent.StartRoom(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
        )

        val state = coordinator.handleEvent(
            MeetingSessionEvent.SwitchTarget(
                roomId = "room-1",
                nextTargetId = "tablet-b",
                nextEntryUrl = "https://host/room-1/tablet-b",
            ),
        )

        assertEquals(
            MeetingSessionState.Active(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-b",
                    entryUrl = "https://host/room-1/tablet-b",
                ),
                hostVisibility = HostVisibility.FOREGROUND,
            ),
            state,
        )
    }

    @Test
    public fun hostBackgroundAndForegroundOnlyUpdateVisibility() {
        val coordinator = MeetingSessionCoordinator()
        coordinator.handleEvent(
            MeetingSessionEvent.StartRoom(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
        )

        val backgroundState = coordinator.handleEvent(MeetingSessionEvent.HostBackgrounded)
        assertEquals(
            MeetingSessionState.Active(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                hostVisibility = HostVisibility.BACKGROUND,
            ),
            backgroundState,
        )

        val foregroundState = coordinator.handleEvent(MeetingSessionEvent.HostForegrounded)
        assertEquals(
            MeetingSessionState.Active(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                hostVisibility = HostVisibility.FOREGROUND,
            ),
            foregroundState,
        )
    }

    @Test
    public fun endRoomEntersEndingAndCanCompleteToIdle() {
        val coordinator = MeetingSessionCoordinator()
        coordinator.handleEvent(
            MeetingSessionEvent.StartRoom(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
        )

        val endingState = coordinator.handleEvent(MeetingSessionEvent.EndRoom(roomId = "room-1"))
        assertEquals(
            MeetingSessionState.Ending(
                roomId = "room-1",
                currentTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                hostVisibility = HostVisibility.FOREGROUND,
                reason = MeetingSessionEndReason.CONTROLLER_EXPLICIT,
            ),
            endingState,
        )

        assertEquals(MeetingSessionState.Idle, coordinator.completeEnding(roomId = "room-1"))
    }

    @Test
    public fun switchTargetWhileIdleIsIgnored() {
        val coordinator = MeetingSessionCoordinator()

        val state = coordinator.handleEvent(
            MeetingSessionEvent.SwitchTarget(
                roomId = "room-1",
                nextTargetId = "tablet-b",
                nextEntryUrl = "https://host/room-1/tablet-b",
            ),
        )

        assertEquals(MeetingSessionState.Idle, state)
    }

    @Test
    public fun foregroundStartFailedMovesActiveRoomToStartRejected() {
        val coordinator = MeetingSessionCoordinator()
        coordinator.handleEvent(
            MeetingSessionEvent.StartRoom(
                roomId = "room-1",
                targetId = "tablet-a",
                entryUrl = "https://host/room-1/tablet-a",
            ),
        )

        val state = coordinator.handleEvent(
            MeetingSessionEvent.ForegroundStartFailed(
                roomId = "room-1",
                reason = "notification permission denied",
            ),
        )

        assertEquals(
            MeetingSessionState.StartRejected(
                roomId = "room-1",
                lastTarget = MeetingSessionTarget(
                    targetId = "tablet-a",
                    entryUrl = "https://host/room-1/tablet-a",
                ),
                reason = MeetingSessionStartRejectionReason.ForegroundStartFailed(
                    details = "notification permission denied",
                ),
            ),
            state,
        )
    }
}
