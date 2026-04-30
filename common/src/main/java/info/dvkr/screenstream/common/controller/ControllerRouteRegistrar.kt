package info.dvkr.screenstream.common.controller

import io.ktor.server.routing.Route

public object ControllerRoutes {
    public const val ROOT_PATH: String = "/controller/v1"
    public const val SNAPSHOT_PATH: String = "snapshot"
    public const val START_PATH: String = "start"
    public const val SWITCH_PATH: String = "switch"
    public const val END_PATH: String = "end"
    public const val TOKEN_EXCHANGE_PATH: String = "token/exchange"
}

public fun interface ControllerRouteRegistrar {
    public fun register(rootRoute: Route)
}
