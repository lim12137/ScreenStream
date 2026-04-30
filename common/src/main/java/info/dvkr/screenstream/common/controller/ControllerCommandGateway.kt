package info.dvkr.screenstream.common.controller

public interface ControllerCommandGateway {

    public suspend fun getSnapshot(request: ControllerSnapshotRequest): ControllerSessionSnapshot

    public suspend fun handle(command: ControllerCommand): ControllerCommandResult

    public suspend fun exchangeToken(request: ControllerTokenExchangeRequest): ControllerTokenExchangeResult
}
