package info.dvkr.screenstream.mjpeg.internal

import android.app.ServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Parcelable
import info.dvkr.screenstream.mjpeg.MjpegModuleService
import info.dvkr.screenstream.mjpeg.R
import info.dvkr.screenstream.mjpeg.ui.MjpegError
import kotlinx.parcelize.Parcelize

internal open class MjpegEvent(val priority: Int) {

    internal object Priority {
        internal const val NONE: Int = -1
        internal const val RESTART_IGNORE: Int = 10
        internal const val RECOVER_IGNORE: Int = 20
        internal const val START_PROJECTION: Int = 21
        internal const val START_WEBVIEW: Int = 22
        internal const val DESTROY_IGNORE: Int = 30
    }

    internal sealed class Intentable(priority: Int) : MjpegEvent(priority), Parcelable {
        internal companion object {
            private const val EXTRA_PARCELABLE = "EXTRA_PARCELABLE"

            @Suppress("DEPRECATION")
            internal fun fromIntent(intent: Intent): Intentable? =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(EXTRA_PARCELABLE)
                else intent.getParcelableExtra(EXTRA_PARCELABLE, Intentable::class.java)
        }

        @Parcelize internal data class StartService(val token: String) : Intentable(Priority.NONE)
        @Parcelize internal data class StartProjection(val intent: Intent) : Intentable(Priority.START_PROJECTION)
        @Parcelize internal data class StopStream(val reason: String) : Intentable(Priority.RESTART_IGNORE)
        @Parcelize internal data object RecoverError : Intentable(Priority.RECOVER_IGNORE)

        internal fun toIntent(context: Context): Intent = MjpegModuleService.getIntent(context).putExtra(EXTRA_PARCELABLE, this)
    }

    internal data object CastPermissionsDenied : MjpegEvent(Priority.RECOVER_IGNORE)
    internal data class StartProjection(
        val intent: Intent, val foregroundStartProcessed: Boolean = false, val foregroundStartError: Throwable? = null
    ) : MjpegEvent(Priority.START_PROJECTION)
    internal data object StartWebViewStream : MjpegEvent(Priority.START_WEBVIEW)
    internal data class WebViewFrame(val bitmap: Bitmap) : MjpegEvent(Priority.RESTART_IGNORE)
    internal data object CreateNewPin : MjpegEvent(Priority.DESTROY_IGNORE)
}

internal class VisibleActivityRequiredException :
    IllegalStateException("WebView-only streaming requires a visible host activity")

internal class WebViewForegroundSession(initialHostVisible: Boolean = false) {

    internal sealed interface StartResult {
        data object Started : StartResult
        data object AlreadyActive : StartResult
        data class Rejected(val cause: Throwable) : StartResult
    }

    internal sealed interface VisibilityResult {
        data object Noop : VisibilityResult
        data object BecameVisible : VisibilityResult
        data class StopRequired(val stopReason: String, val cause: Throwable) : VisibilityResult
    }

    internal var hostVisible: Boolean = initialHostVisible
        private set

    internal var active: Boolean = false
        private set

    internal fun reset(hostVisible: Boolean) {
        this.hostVisible = hostVisible
        active = false
    }

    internal fun start(): StartResult = when {
        active -> StartResult.AlreadyActive
        hostVisible.not() -> StartResult.Rejected(VisibleActivityRequiredException())
        else -> {
            active = true
            StartResult.Started
        }
    }

    internal fun stop(): Boolean = active.also { active = false }

    internal fun canAcceptFrames(): Boolean = active && hostVisible

    internal fun updateHostVisibility(isVisible: Boolean): VisibilityResult {
        if (hostVisible == isVisible) return VisibilityResult.Noop

        hostVisible = isVisible
        return if (isVisible) {
            VisibilityResult.BecameVisible
        } else if (active) {
            active = false
            VisibilityResult.StopRequired(
                stopReason = "WebViewHostNotVisible",
                cause = VisibleActivityRequiredException()
            )
        } else {
            VisibilityResult.Noop
        }
    }
}

internal fun Throwable.toForegroundAvailabilityError(context: Context): MjpegError {
    val messageId =
        when (this) {
            is VisibleActivityRequiredException -> R.string.mjpeg_error_webview_visible_activity_required
            is ServiceStartNotAllowedException -> R.string.mjpeg_error_webview_foreground_unavailable
            else -> R.string.mjpeg_error_webview_foreground_unavailable
        }

    return MjpegError.UnknownError(IllegalStateException(context.getString(messageId), this))
}
