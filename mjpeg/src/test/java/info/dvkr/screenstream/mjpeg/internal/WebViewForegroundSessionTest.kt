package info.dvkr.screenstream.mjpeg.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class WebViewForegroundSessionTest {

    @Test
    public fun startIsRejectedWhenHostActivityIsNotVisible() {
        val session = WebViewForegroundSession()

        val result = session.start()

        assertTrue(result is WebViewForegroundSession.StartResult.Rejected)
        val rejected = result as WebViewForegroundSession.StartResult.Rejected
        assertTrue(rejected.cause is VisibleActivityRequiredException)
        assertFalse(session.active)
        assertFalse(session.canAcceptFrames())
    }

    @Test
    public fun visibleHostAllowsWebViewStreamingToStart() {
        val session = WebViewForegroundSession()
        session.updateHostVisibility(isVisible = true)

        val result = session.start()

        assertTrue(result is WebViewForegroundSession.StartResult.Started)
        assertTrue(session.hostVisible)
        assertTrue(session.active)
        assertTrue(session.canAcceptFrames())
    }

    @Test
    public fun losingVisibleHostStopsActiveWebViewSession() {
        val session = WebViewForegroundSession()
        session.updateHostVisibility(isVisible = true)
        session.start()

        val result = session.updateHostVisibility(isVisible = false)

        assertTrue(result is WebViewForegroundSession.VisibilityResult.StopRequired)
        val stopRequired = result as WebViewForegroundSession.VisibilityResult.StopRequired
        assertTrue(stopRequired.cause is VisibleActivityRequiredException)
        assertFalse(session.hostVisible)
        assertFalse(session.active)
        assertFalse(session.canAcceptFrames())
    }
}
