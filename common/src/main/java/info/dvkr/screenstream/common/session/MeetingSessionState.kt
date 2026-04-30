package info.dvkr.screenstream.common.session

public data class MeetingSessionTarget(
    public val targetId: String,
    public val entryUrl: String,
)

public enum class HostVisibility {
    FOREGROUND,
    BACKGROUND,
}

public enum class MeetingSessionEndReason {
    CONTROLLER_EXPLICIT,
}

public sealed interface MeetingSessionStartRejectionReason {
    public data class ForegroundStartFailed(public val details: String) : MeetingSessionStartRejectionReason
}

public sealed interface MeetingSessionState {

    public data object Idle : MeetingSessionState

    public data class Active(
        public val roomId: String,
        public val currentTarget: MeetingSessionTarget,
        public val hostVisibility: HostVisibility,
    ) : MeetingSessionState

    public data class Ending(
        public val roomId: String,
        public val currentTarget: MeetingSessionTarget,
        public val hostVisibility: HostVisibility,
        public val reason: MeetingSessionEndReason,
    ) : MeetingSessionState

    public data class StartRejected(
        public val roomId: String?,
        public val lastTarget: MeetingSessionTarget?,
        public val reason: MeetingSessionStartRejectionReason,
    ) : MeetingSessionState
}

public sealed interface MeetingSessionEvent {

    public data class StartRoom(
        public val roomId: String,
        public val targetId: String,
        public val entryUrl: String,
    ) : MeetingSessionEvent

    public data class SwitchTarget(
        public val roomId: String,
        public val nextTargetId: String,
        public val nextEntryUrl: String,
    ) : MeetingSessionEvent

    public data class EndRoom(
        public val roomId: String,
        public val reason: MeetingSessionEndReason = MeetingSessionEndReason.CONTROLLER_EXPLICIT,
    ) : MeetingSessionEvent

    public data class ForegroundStartFailed(
        public val roomId: String?,
        public val reason: String,
    ) : MeetingSessionEvent

    public data object HostBackgrounded : MeetingSessionEvent

    public data object HostForegrounded : MeetingSessionEvent
}
