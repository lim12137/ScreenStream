package info.dvkr.screenstream.common.session

import kotlin.test.Test
import kotlin.test.assertEquals

public class MeetingSessionCoordinatorTest {

    @Test
    public fun `start room enters active state with foreground visibility`() {
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
    public fun `switch target updates active room only`() {
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
    public fun `host background and foreground only update visibility`() {
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
    public fun `end room enters ending and can complete to idle`() {
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
    public fun `switch target while idle is ignored`() {
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
    public fun `foreground start failed moves active room to start rejected`() {
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
