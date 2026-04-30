package info.dvkr.screenstream

import android.test.AndroidTestCase
import info.dvkr.screenstream.common.session.HostVisibility
import info.dvkr.screenstream.common.session.MeetingSessionCoordinator
import info.dvkr.screenstream.common.session.MeetingSessionEndReason
import info.dvkr.screenstream.common.session.MeetingSessionState

@Suppress("DEPRECATION")
public class SingleActivityMeetingSessionTest : AndroidTestCase() {

    public fun testActiveMeetingPauseAndStopOnlyChangeVisibility() {
        val host = SingleActivityMeetingHost(MeetingSessionCoordinator())

        val activeState = host.attach("https://host/room-1/tablet-a").state as MeetingSessionState.Active
        assertFalse(host.shouldPauseWebViewOnHostPause(finalReleaseStarted = false))
        assertFalse(host.shouldFinalizeOnDestroy(finalReleaseStarted = false))

        val backgroundState = host.onHostBackgrounded()
        assertEquals(
            MeetingSessionState.Active(
                roomId = activeState.roomId,
                currentTarget = activeState.currentTarget,
                hostVisibility = HostVisibility.BACKGROUND,
            ),
            backgroundState,
        )

        val foregroundState = host.onHostForegrounded()
        assertEquals(
            MeetingSessionState.Active(
                roomId = activeState.roomId,
                currentTarget = activeState.currentTarget,
                hostVisibility = HostVisibility.FOREGROUND,
            ),
            foregroundState,
        )
    }

    public fun testSwitchTargetKeepsRoomAndDoesNotRequestFinalRelease() {
        val host = SingleActivityMeetingHost(MeetingSessionCoordinator())

        val initialState = host.attach("https://host/room-1/tablet-a").state as MeetingSessionState.Active
        val update = host.switchTarget(
            roomId = initialState.roomId,
            targetId = "tablet-b",
            entryUrl = "https://host/room-1/tablet-b",
        )

        assertFalse(update.shouldFinalize)
        assertEquals("https://host/room-1/tablet-b", update.entryUrlToLoad)
        assertEquals(
            MeetingSessionState.Active(
                roomId = initialState.roomId,
                currentTarget = initialState.currentTarget.copy(
                    targetId = "tablet-b",
                    entryUrl = "https://host/room-1/tablet-b",
                ),
                hostVisibility = HostVisibility.FOREGROUND,
            ),
            update.state,
        )
    }

    public fun testExplicitEndAndFinishingPathRequireFinalRelease() {
        val host = SingleActivityMeetingHost(MeetingSessionCoordinator())

        val activeState = host.attach("https://host/room-1/tablet-a").state as MeetingSessionState.Active
        val endUpdate = host.endRoom(activeState.roomId)

        assertTrue(endUpdate.shouldFinalize)
        assertEquals(
            MeetingSessionState.Ending(
                roomId = activeState.roomId,
                currentTarget = activeState.currentTarget,
                hostVisibility = HostVisibility.FOREGROUND,
                reason = MeetingSessionEndReason.CONTROLLER_EXPLICIT,
            ),
            endUpdate.state,
        )
        assertTrue(host.shouldFinalizeOnDestroy(finalReleaseStarted = false))
    }

    public fun testFinishingWithoutExplicitEndDoesNotRequestFinalRelease() {
        val host = SingleActivityMeetingHost(MeetingSessionCoordinator())
        host.attach("https://host/room-1/tablet-a")

        assertFalse(host.shouldFinalizeOnDestroy(finalReleaseStarted = false))
    }

    public fun testBackWithoutHistoryBackgroundsActiveMeeting() {
        val activeHost = SingleActivityMeetingHost(MeetingSessionCoordinator())
        activeHost.attach("https://host/room-1/tablet-a")

        assertTrue(activeHost.shouldMoveTaskToBackOnBackPress(canGoBack = false))
        assertFalse(activeHost.shouldMoveTaskToBackOnBackPress(canGoBack = true))

        val idleHost = SingleActivityMeetingHost(MeetingSessionCoordinator())
        assertFalse(idleHost.shouldMoveTaskToBackOnBackPress(canGoBack = false))
    }
}
