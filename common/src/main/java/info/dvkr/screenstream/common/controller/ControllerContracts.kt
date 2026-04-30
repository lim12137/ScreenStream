package info.dvkr.screenstream.common.controller

public enum class ControllerCommandSource {
    CONTROLLER_COMMAND,
    LOCAL_MANUAL,
}

public enum class ControllerSessionStatus {
    IDLE,
    STARTING,
    ACTIVE,
    SWITCHING,
    ENDING,
    START_REJECTED,
}

public enum class ControllerStreamStatus {
    STOPPED,
    STARTING,
    LIVE,
    STOPPING,
    FAILED,
}

public enum class ControllerHostVisibility {
    FOREGROUND,
    BACKGROUND,
}

public enum class ControllerEndReason {
    CONTROLLER_EXPLICIT,
}

public enum class ControllerCommandResultType {
    APPLIED,
    ALREADY_APPLIED,
    NOOP,
    REJECTED_UNAUTHORIZED,
    REJECTED_STALE,
    REJECTED_CONFLICT,
    REJECTED_UNAVAILABLE,
}

public enum class ControllerAccessScope {
    READ,
    WRITE,
}

public data class ControllerSnapshotRequest(
    public val controllerSessionId: String,
)

public sealed interface ControllerCommand {
    public val commandId: String
    public val controllerSessionId: String
    public val stateVersion: Long
    public val source: ControllerCommandSource
}

public data class ControllerStartRequest(
    override val commandId: String,
    override val controllerSessionId: String,
    override val stateVersion: Long,
    public val roomId: String,
    public val targetId: String,
    public val entryUrl: String,
    override val source: ControllerCommandSource = ControllerCommandSource.CONTROLLER_COMMAND,
) : ControllerCommand

public data class ControllerSwitchRequest(
    override val commandId: String,
    override val controllerSessionId: String,
    override val stateVersion: Long,
    public val roomId: String,
    public val nextTargetId: String,
    public val nextEntryUrl: String,
    override val source: ControllerCommandSource = ControllerCommandSource.CONTROLLER_COMMAND,
) : ControllerCommand

public data class ControllerEndRequest(
    override val commandId: String,
    override val controllerSessionId: String,
    override val stateVersion: Long,
    public val roomId: String,
    public val reason: ControllerEndReason = ControllerEndReason.CONTROLLER_EXPLICIT,
    override val source: ControllerCommandSource = ControllerCommandSource.CONTROLLER_COMMAND,
) : ControllerCommand

public data class ControllerSessionTarget(
    public val roomId: String,
    public val targetId: String,
    public val entryUrl: String,
)

public data class ControllerCommandError(
    public val commandId: String?,
    public val controllerSessionId: String,
    public val source: ControllerCommandSource,
    public val roomId: String?,
    public val targetId: String?,
    public val code: String,
    public val message: String,
    public val occurredAt: Long,
)

public data class ControllerSessionSnapshot(
    public val controllerSessionId: String,
    public val stateVersion: Long,
    public val lastAppliedCommandId: String?,
    public val updatedAt: Long,
    public val ownerControllerId: String?,
    public val sessionStatus: ControllerSessionStatus,
    public val streamStatus: ControllerStreamStatus,
    public val hostVisibility: ControllerHostVisibility?,
    public val target: ControllerSessionTarget?,
    public val lastError: ControllerCommandError?,
)

public data class ControllerCommandResult(
    public val commandId: String,
    public val controllerSessionId: String,
    public val stateVersion: Long,
    public val result: ControllerCommandResultType,
    public val snapshot: ControllerSessionSnapshot,
    public val error: ControllerCommandError? = null,
)

public data class ControllerTokenExchangeRequest(
    public val pairingToken: String,
    public val controllerSessionId: String,
    public val deviceName: String? = null,
    public val clientNonce: String? = null,
)

public data class ControllerTokenExchangeResult(
    public val controllerSessionId: String,
    public val ownerControllerId: String,
    public val bearerToken: String,
    public val expiresAt: Long,
    public val stateVersion: Long? = null,
    public val scope: Set<ControllerAccessScope>? = null,
)
