package info.dvkr.screenstream.common.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class MeetingSessionCoordinator {

    private val lock: Any = Any()

    private val _stateFlow: MutableStateFlow<MeetingSessionState> = MutableStateFlow(MeetingSessionState.Idle)
    public val stateFlow: StateFlow<MeetingSessionState>
        get() = _stateFlow.asStateFlow()

    public fun currentState(): MeetingSessionState = _stateFlow.value

    public fun handleEvent(event: MeetingSessionEvent): MeetingSessionState = synchronized(lock) {
        reduce(current = _stateFlow.value, event = event).also { nextState ->
            _stateFlow.value = nextState
        }
    }

    public fun completeEnding(roomId: String): MeetingSessionState = synchronized(lock) {
        val currentState: MeetingSessionState = _stateFlow.value
        val nextState: MeetingSessionState = when {
            currentState is MeetingSessionState.Ending && currentState.roomId == roomId -> MeetingSessionState.Idle
            else -> currentState
        }
        _stateFlow.value = nextState
        nextState
    }

    private fun reduce(current: MeetingSessionState, event: MeetingSessionEvent): MeetingSessionState = when (event) {
        is MeetingSessionEvent.StartRoom -> reduceStartRoom(current = current, event = event)
        is MeetingSessionEvent.SwitchTarget -> reduceSwitchTarget(current = current, event = event)
        is MeetingSessionEvent.EndRoom -> reduceEndRoom(current = current, event = event)
        is MeetingSessionEvent.ForegroundStartFailed -> reduceForegroundStartFailed(current = current, event = event)
        MeetingSessionEvent.HostBackgrounded -> reduceHostVisibility(current = current, hostVisibility = HostVisibility.BACKGROUND)
        MeetingSessionEvent.HostForegrounded -> reduceHostVisibility(current = current, hostVisibility = HostVisibility.FOREGROUND)
    }

    private fun reduceStartRoom(
        current: MeetingSessionState,
        event: MeetingSessionEvent.StartRoom,
    ): MeetingSessionState = when (current) {
        MeetingSessionState.Idle,
        is MeetingSessionState.StartRejected -> MeetingSessionState.Active(
            roomId = event.roomId,
            currentTarget = MeetingSessionTarget(targetId = event.targetId, entryUrl = event.entryUrl),
            hostVisibility = HostVisibility.FOREGROUND,
        )

        is MeetingSessionState.Active,
        is MeetingSessionState.Ending -> current
    }

    private fun reduceSwitchTarget(
        current: MeetingSessionState,
        event: MeetingSessionEvent.SwitchTarget,
    ): MeetingSessionState = when {
        current is MeetingSessionState.Active && current.roomId == event.roomId -> current.copy(
            currentTarget = MeetingSessionTarget(targetId = event.nextTargetId, entryUrl = event.nextEntryUrl),
        )

        else -> current
    }

    private fun reduceEndRoom(
        current: MeetingSessionState,
        event: MeetingSessionEvent.EndRoom,
    ): MeetingSessionState = when {
        current is MeetingSessionState.Active && current.roomId == event.roomId -> MeetingSessionState.Ending(
            roomId = current.roomId,
            currentTarget = current.currentTarget,
            hostVisibility = current.hostVisibility,
            reason = event.reason,
        )

        else -> current
    }

    private fun reduceForegroundStartFailed(
        current: MeetingSessionState,
        event: MeetingSessionEvent.ForegroundStartFailed,
    ): MeetingSessionState = when {
        current is MeetingSessionState.Ending -> current
        current is MeetingSessionState.Active && event.roomId != null && event.roomId != current.roomId -> current
        current is MeetingSessionState.Active -> MeetingSessionState.StartRejected(
            roomId = current.roomId,
            lastTarget = current.currentTarget,
            reason = MeetingSessionStartRejectionReason.ForegroundStartFailed(details = event.reason),
        )

        else -> MeetingSessionState.StartRejected(
            roomId = event.roomId,
            lastTarget = null,
            reason = MeetingSessionStartRejectionReason.ForegroundStartFailed(details = event.reason),
        )
    }

    private fun reduceHostVisibility(
        current: MeetingSessionState,
        hostVisibility: HostVisibility,
    ): MeetingSessionState = when (current) {
        MeetingSessionState.Idle,
        is MeetingSessionState.StartRejected -> current

        is MeetingSessionState.Active -> current.copy(hostVisibility = hostVisibility)
        is MeetingSessionState.Ending -> current.copy(hostVisibility = hostVisibility)
    }
}
