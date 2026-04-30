package info.dvkr.screenstream.mjpeg.internal

import info.dvkr.screenstream.common.controller.ControllerRouteRegistrar
import info.dvkr.screenstream.common.controller.ControllerRoutes
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

internal val NoOpControllerRouteRegistrar: ControllerRouteRegistrar = ControllerRouteRegistrar {}

internal fun Routing.registerHttpServerRoutes(
    controllerRouteRegistrar: ControllerRouteRegistrar,
    registerViewerRoutes: Route.() -> Unit
) {
    registerViewerRoutes()
    route(ControllerRoutes.ROOT_PATH) {
        controllerRouteRegistrar.register(this)
    }
}
