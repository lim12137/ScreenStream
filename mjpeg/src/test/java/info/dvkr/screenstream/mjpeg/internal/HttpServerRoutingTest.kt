package info.dvkr.screenstream.mjpeg.internal

import info.dvkr.screenstream.common.controller.ControllerRouteRegistrar
import info.dvkr.screenstream.common.controller.ControllerRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Test

public class HttpServerRoutingTest {

    @Test
    public fun controllerRoutesAreMountedWithoutBreakingViewerRoot() = testApplication {
        application {
            routing {
                registerHttpServerRoutes(
                    controllerRouteRegistrar = ControllerRouteRegistrar { rootRoute ->
                        rootRoute.get(ControllerRoutes.SNAPSHOT_PATH) {
                            call.respondText("controller-snapshot")
                        }
                    }
                ) {
                    get("/") {
                        call.respondText("viewer-root")
                    }
                }
            }
        }

        val viewerResponse = client.get("/")
        assertEquals(HttpStatusCode.OK, viewerResponse.status)
        assertEquals("viewer-root", viewerResponse.bodyAsText())

        val controllerResponse = client.get("${ControllerRoutes.ROOT_PATH}/${ControllerRoutes.SNAPSHOT_PATH}")
        assertEquals(HttpStatusCode.OK, controllerResponse.status)
        assertEquals("controller-snapshot", controllerResponse.bodyAsText())
    }
}
