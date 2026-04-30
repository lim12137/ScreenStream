package info.dvkr.screenstream.mjpeg.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

public class WebViewForegroundSessionTest {

    @Test
    public fun `start is rejected when host activity is not visible`() {
        val session = WebViewForegroundSession()

        val result = session.start()

        val rejected = assertIs<WebViewForegroundSession.StartResult.Rejected>(result)
        assertIs<VisibleActivityRequiredException>(rejected.cause)
        assertFalse(session.active)
        assertFalse(session.canAcceptFrames())
    }

    @Test
    public fun `visible host allows webview streaming to start`() {
        val session = WebViewForegroundSession()
        session.updateHostVisibility(isVisible = true)

        val result = session.start()

        assertIs<WebViewForegroundSession.StartResult.Started>(result)
        assertTrue(session.hostVisible)
        assertTrue(session.active)
        assertTrue(session.canAcceptFrames())
    }

    @Test
    public fun `losing visible host stops active webview session`() {
        val session = WebViewForegroundSession()
        session.updateHostVisibility(isVisible = true)
        session.start()

        val result = session.updateHostVisibility(isVisible = false)

        val stopRequired = assertIs<WebViewForegroundSession.VisibilityResult.StopRequired>(result)
        assertIs<VisibleActivityRequiredException>(stopRequired.cause)
        assertFalse(session.hostVisible)
        assertFalse(session.active)
        assertFalse(session.canAcceptFrames())
    }
}
