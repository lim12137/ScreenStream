package info.dvkr.screenstream

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog
import info.dvkr.screenstream.common.module.StreamingModuleManager
import info.dvkr.screenstream.common.session.MeetingSessionCoordinator
import info.dvkr.screenstream.common.session.MeetingSessionEvent
import info.dvkr.screenstream.common.session.MeetingSessionState
import info.dvkr.screenstream.common.settings.AppSettings
import info.dvkr.screenstream.mjpeg.MjpegStreamingModule
import info.dvkr.screenstream.ui.enableEdgeToEdge
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

public class SingleActivity : AppUpdateActivity() {

    internal companion object {
        private const val ACTION_START_ROOM: String = "info.dvkr.screenstream.action.START_ROOM"
        private const val ACTION_SWITCH_TARGET: String = "info.dvkr.screenstream.action.SWITCH_TARGET"
        private const val ACTION_END_ROOM: String = "info.dvkr.screenstream.action.END_ROOM"
        private const val EXTRA_ROOM_ID: String = "info.dvkr.screenstream.extra.ROOM_ID"
        private const val EXTRA_TARGET_ID: String = "info.dvkr.screenstream.extra.TARGET_ID"
        private const val EXTRA_ENTRY_URL: String = "info.dvkr.screenstream.extra.ENTRY_URL"
        private const val DEFAULT_ROOM_ID: String = "single-activity-room"

        internal fun getIntent(context: Context): Intent = Intent(context, SingleActivity::class.java)

        internal fun getStartRoomIntent(
            context: Context,
            roomId: String,
            targetId: String,
            entryUrl: String,
        ): Intent = getIntent(context).apply {
            action = ACTION_START_ROOM
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_ENTRY_URL, entryUrl)
        }

        internal fun getSwitchTargetIntent(
            context: Context,
            roomId: String,
            targetId: String,
            entryUrl: String,
        ): Intent = getIntent(context).apply {
            action = ACTION_SWITCH_TARGET
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_ENTRY_URL, entryUrl)
        }

        internal fun getEndRoomIntent(context: Context, roomId: String): Intent = getIntent(context).apply {
            action = ACTION_END_ROOM
            putExtra(EXTRA_ROOM_ID, roomId)
        }
    }

    private lateinit var webView: WebView
    private val appSettings: AppSettings by lazy { get() }
    private val streamingModuleManager: StreamingModuleManager by lazy { get() }
    private val meetingSessionCoordinator: MeetingSessionCoordinator by lazy { get() }
    private val meetingHost: SingleActivityMeetingHost by lazy { SingleActivityMeetingHost(meetingSessionCoordinator) }
    private var currentEntryUrl: String = BuildConfig.LAUNCH_URL
    private var webViewFrameLoopStarted: Boolean = false
    private val webViewFrameHandler = Handler(Looper.getMainLooper())
    private val webViewFrameIntervalMs: Long = 100L
    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var streamingModule: MjpegStreamingModule? = null
    private var finalReleaseStarted: Boolean = false
    private val recordAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        val request = pendingWebPermissionRequest ?: return@registerForActivityResult
        if (isGranted) request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) else request.deny()
        pendingWebPermissionRequest = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        XLog.d(this@SingleActivity.getLog("onCreate", "Bug workaround: ${window.decorView}"))
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge(
            statusBarColor = androidx.compose.ui.graphics.Color.Black,
            navigationBarColor = androidx.compose.ui.graphics.Color.Black
        )

        currentEntryUrl = resolveLaunchUrl(appSettings.data.value.webEntryUrl)
        applyMeetingUpdate(
            update = resolveLaunchMeetingUpdate(intent = intent, fallbackEntryUrl = currentEntryUrl),
            loadCurrentTarget = false,
            finishWhenFinalized = false,
        )

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = false
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    handleWebPermissionRequest(request)
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
            }
            setOnLongClickListener {
                showWebEntryUrlDialog()
                true
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets -> insets }

        setContentView(webView)
        hideSystemBars()
        startWebViewMjpegStreaming()

        if (savedInstanceState == null) {
            webView.loadUrl(currentEntryUrl)
        } else {
            val restoredState = webView.restoreState(savedInstanceState)
            if (restoredState == null) webView.loadUrl(currentEntryUrl)
        }

        if (finalReleaseStarted && isFinishing.not()) finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        XLog.d(getLog("onNewIntent"))
        applyMeetingUpdate(
            update = resolveMeetingIntentUpdate(intent = intent, fallbackEntryUrl = currentEntryUrl),
            loadCurrentTarget = true,
        )
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        meetingHost.onHostForegrounded()
        webView.onResume()
        hideSystemBars()
    }

    override fun onPause() {
        if (meetingHost.shouldPauseWebViewOnHostPause(finalReleaseStarted = finalReleaseStarted)) {
            webView.onPause()
        }
        super.onPause()
    }

    override fun onStop() {
        meetingHost.onHostBackgrounded()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else if (meetingHost.shouldMoveTaskToBackOnBackPress(canGoBack = false)) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (meetingHost.shouldFinalizeOnDestroy(finalReleaseStarted = finalReleaseStarted)) {
            finalizeMeetingHost(reason = "SingleActivity.onDestroy")
        } else {
            stopWebViewFrameLoop()
            pendingWebPermissionRequest?.deny()
            pendingWebPermissionRequest = null
            streamingModule = null
        }
        if (::webView.isInitialized) {
            webView.apply {
                stopLoading()
                onPause()
                webChromeClient = null
                webViewClient = WebViewClient()
                destroy()
            }
        }
        super.onDestroy()
    }

    private fun startWebViewMjpegStreaming() {
        lifecycleScope.launch {
            runCatching {
                streamingModuleManager.startModule(MjpegStreamingModule.Id, this@SingleActivity)
                val module = streamingModuleManager.modules.firstOrNull { it.id == MjpegStreamingModule.Id } as? MjpegStreamingModule
                streamingModule = module
                module?.startWebViewStreaming()
                startWebViewFrameLoop()
            }.onFailure {
                XLog.e(getLog("startWebViewMjpegStreaming"), it)
            }
        }
    }

    private fun startWebViewFrameLoop() {
        if (webViewFrameLoopStarted) return
        webViewFrameLoopStarted = true
        webViewFrameHandler.post(object : Runnable {
            override fun run() {
                if (!webViewFrameLoopStarted) return
                pushWebViewFrame()
                webViewFrameHandler.postDelayed(this, webViewFrameIntervalMs)
            }
        })
    }

    private fun stopWebViewFrameLoop() {
        webViewFrameLoopStarted = false
        webViewFrameHandler.removeCallbacksAndMessages(null)
    }

    private fun pushWebViewFrame() {
        val module = streamingModule ?: return
        if (::webView.isInitialized.not()) return
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return

        val bitmap = runCatching { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) }.getOrNull() ?: return
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        module.submitWebViewFrame(bitmap)
    }

    private fun showWebEntryUrlDialog() {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(currentEntryUrl)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Web Entry URL")
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val inputUrl = editText.text?.toString().orEmpty().trim()
                val resolvedUrl = resolveLaunchUrl(inputUrl)
                lifecycleScope.launch {
                    appSettings.updateData {
                        copy(webEntryUrl = inputUrl)
                    }
                }
                if (resolvedUrl != currentEntryUrl) {
                    applyMeetingUpdate(
                        update = meetingHost.switchCurrentTarget(entryUrl = resolvedUrl),
                        loadCurrentTarget = true,
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun handleWebPermissionRequest(request: PermissionRequest) {
        val audioRequested = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        if (audioRequested.not()) {
            request.deny()
            return
        }
        val trustedOrigin = isTrustedAudioPermissionOrigin(request.origin, currentEntryUrl)
        if (trustedOrigin.not()) {
            request.deny()
            return
        }

        runOnUiThread {
            val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            } else {
                pendingWebPermissionRequest?.deny()
                pendingWebPermissionRequest = request
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun isTrustedAudioPermissionOrigin(requestOrigin: Uri?, entryUrl: String): Boolean {
        val entryUri = runCatching { Uri.parse(entryUrl) }.getOrNull() ?: return false
        val requestScheme = requestOrigin?.scheme?.lowercase().orEmpty()
        val entryScheme = entryUri.scheme?.lowercase().orEmpty()
        val requestHost = requestOrigin?.host?.lowercase().orEmpty()
        val entryHost = entryUri.host?.lowercase().orEmpty()
        if (requestScheme.isBlank() || entryScheme.isBlank() || requestHost.isBlank() || entryHost.isBlank()) return false

        val isSameOrigin = requestScheme == entryScheme &&
            requestHost == entryHost &&
            requestOrigin?.port == entryUri.port
        val isSameSchemeHost = requestScheme == entryScheme && requestHost == entryHost
        return isSameOrigin || isSameSchemeHost
    }

    private fun resolveLaunchUrl(savedUrl: String): String {
        val candidate = savedUrl.trim()
        val uri = runCatching { Uri.parse(candidate) }.getOrNull()
        val isValidHttpUrl = uri?.scheme?.let { it == "http" || it == "https" } == true && uri.host.isNullOrBlank().not()
        return if (isValidHttpUrl) candidate else BuildConfig.LAUNCH_URL
    }

    private fun resolveLaunchMeetingUpdate(intent: Intent?, fallbackEntryUrl: String): SingleActivityMeetingUpdate {
        val explicitIntentUpdate = resolveMeetingIntentUpdate(intent = intent, fallbackEntryUrl = fallbackEntryUrl)
        return if (explicitIntentUpdate != null) explicitIntentUpdate else meetingHost.attach(entryUrl = fallbackEntryUrl)
    }

    private fun resolveMeetingIntentUpdate(intent: Intent?, fallbackEntryUrl: String): SingleActivityMeetingUpdate? {
        val command = parseMeetingCommand(intent = intent, fallbackEntryUrl = fallbackEntryUrl)
        return when (command) {
            MeetingCommand.None -> null
            is MeetingCommand.StartRoom -> meetingHost.startRoom(
                roomId = command.roomId,
                targetId = command.targetId,
                entryUrl = command.entryUrl,
            )
            is MeetingCommand.SwitchTarget -> meetingHost.switchTarget(
                roomId = command.roomId,
                targetId = command.targetId,
                entryUrl = command.entryUrl,
            )
            is MeetingCommand.EndRoom -> meetingHost.endRoom(roomId = command.roomId)
        }
    }

    private fun parseMeetingCommand(intent: Intent?, fallbackEntryUrl: String): MeetingCommand {
        if (intent == null) return MeetingCommand.None

        val roomId = intent.getStringExtra(EXTRA_ROOM_ID).orEmpty().ifBlank { DEFAULT_ROOM_ID }
        val entryUrl = resolveLaunchUrl(intent.getStringExtra(EXTRA_ENTRY_URL).orEmpty().ifBlank { fallbackEntryUrl })
        val targetId = intent.getStringExtra(EXTRA_TARGET_ID).orEmpty().ifBlank { entryUrl }

        return when (intent.action) {
            ACTION_START_ROOM -> MeetingCommand.StartRoom(
                roomId = roomId,
                targetId = targetId,
                entryUrl = entryUrl,
            )
            ACTION_SWITCH_TARGET -> MeetingCommand.SwitchTarget(
                roomId = roomId,
                targetId = targetId,
                entryUrl = entryUrl,
            )
            ACTION_END_ROOM -> MeetingCommand.EndRoom(roomId = roomId)
            else -> MeetingCommand.None
        }
    }

    private fun applyMeetingUpdate(
        update: SingleActivityMeetingUpdate?,
        loadCurrentTarget: Boolean,
        finishWhenFinalized: Boolean = true,
    ) {
        if (update == null) return

        update.entryUrlToLoad?.let { nextEntryUrl ->
            val resolvedEntryUrl = resolveLaunchUrl(nextEntryUrl)
            val shouldReload = resolvedEntryUrl != currentEntryUrl
            currentEntryUrl = resolvedEntryUrl
            if (loadCurrentTarget && shouldReload && ::webView.isInitialized) {
                webView.loadUrl(currentEntryUrl)
            }
        }

        if (update.shouldFinalize) {
            finalizeMeetingHost(reason = "SingleActivity.explicitEnd")
            if (finishWhenFinalized && isFinishing.not()) finish()
        }
    }

    private fun finalizeMeetingHost(reason: String) {
        if (finalReleaseStarted) return

        finalReleaseStarted = true
        stopWebViewFrameLoop()
        pendingWebPermissionRequest?.deny()
        pendingWebPermissionRequest = null
        if (::webView.isInitialized) {
            webView.onPause()
        }
        streamingModule?.stopWebViewStreaming(reason)
        streamingModule = null
        clearMeetingSessionAfterFinalRelease()
    }

    private fun clearMeetingSessionAfterFinalRelease() {
        when (val state = meetingHost.currentState()) {
            is MeetingSessionState.Ending -> meetingSessionCoordinator.completeEnding(roomId = state.roomId)
            MeetingSessionState.Idle,
            is MeetingSessionState.Active,
            is MeetingSessionState.StartRejected -> Unit
        }
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

}

internal data class SingleActivityMeetingUpdate(
    val state: MeetingSessionState,
    val entryUrlToLoad: String? = null,
    val shouldFinalize: Boolean = false,
)

internal sealed interface MeetingCommand {
    data object None : MeetingCommand

    data class StartRoom(
        val roomId: String,
        val targetId: String,
        val entryUrl: String,
    ) : MeetingCommand

    data class SwitchTarget(
        val roomId: String,
        val targetId: String,
        val entryUrl: String,
    ) : MeetingCommand

    data class EndRoom(val roomId: String) : MeetingCommand
}

internal class SingleActivityMeetingHost(
    private val coordinator: MeetingSessionCoordinator,
) {

    private companion object {
        private const val DEFAULT_ROOM_ID: String = "single-activity-room"
    }

    fun attach(entryUrl: String): SingleActivityMeetingUpdate = when (val state = coordinator.currentState()) {
        MeetingSessionState.Idle,
        is MeetingSessionState.StartRejected -> startRoom(
            roomId = DEFAULT_ROOM_ID,
            targetId = entryUrl,
            entryUrl = entryUrl,
        )
        is MeetingSessionState.Active -> SingleActivityMeetingUpdate(
            state = state,
            entryUrlToLoad = state.currentTarget.entryUrl,
        )
        is MeetingSessionState.Ending -> SingleActivityMeetingUpdate(
            state = state,
            entryUrlToLoad = state.currentTarget.entryUrl,
            shouldFinalize = true,
        )
    }

    fun startRoom(roomId: String, targetId: String, entryUrl: String): SingleActivityMeetingUpdate {
        val previousState = coordinator.currentState()
        val nextState = coordinator.handleEvent(
            MeetingSessionEvent.StartRoom(
                roomId = roomId,
                targetId = targetId,
                entryUrl = entryUrl,
            ),
        )
        return when {
            nextState is MeetingSessionState.Active -> SingleActivityMeetingUpdate(
                state = nextState,
                entryUrlToLoad = nextState.currentTarget.entryUrl,
            )
            previousState is MeetingSessionState.Active && previousState.roomId == roomId -> switchTarget(
                roomId = roomId,
                targetId = targetId,
                entryUrl = entryUrl,
            )
            else -> SingleActivityMeetingUpdate(state = nextState)
        }
    }

    fun switchCurrentTarget(entryUrl: String): SingleActivityMeetingUpdate {
        val currentState = coordinator.currentState()
        return if (currentState is MeetingSessionState.Active) {
            switchTarget(
                roomId = currentState.roomId,
                targetId = entryUrl,
                entryUrl = entryUrl,
            )
        } else {
            attach(entryUrl = entryUrl)
        }
    }

    fun switchTarget(roomId: String, targetId: String, entryUrl: String): SingleActivityMeetingUpdate {
        val nextState = coordinator.handleEvent(
            MeetingSessionEvent.SwitchTarget(
                roomId = roomId,
                nextTargetId = targetId,
                nextEntryUrl = entryUrl,
            ),
        )
        return when (nextState) {
            is MeetingSessionState.Active -> SingleActivityMeetingUpdate(
                state = nextState,
                entryUrlToLoad = nextState.currentTarget.entryUrl,
            )
            else -> SingleActivityMeetingUpdate(state = nextState)
        }
    }

    fun endRoom(roomId: String): SingleActivityMeetingUpdate {
        val nextState = coordinator.handleEvent(MeetingSessionEvent.EndRoom(roomId = roomId))
        val shouldFinalize = nextState is MeetingSessionState.Ending && nextState.roomId == roomId
        return SingleActivityMeetingUpdate(
            state = nextState,
            shouldFinalize = shouldFinalize,
        )
    }

    fun onHostForegrounded(): MeetingSessionState = coordinator.handleEvent(MeetingSessionEvent.HostForegrounded)

    fun onHostBackgrounded(): MeetingSessionState = coordinator.handleEvent(MeetingSessionEvent.HostBackgrounded)

    fun currentState(): MeetingSessionState = coordinator.currentState()

    fun shouldPauseWebViewOnHostPause(finalReleaseStarted: Boolean): Boolean = when {
        finalReleaseStarted -> false
        coordinator.currentState() is MeetingSessionState.Active -> false
        coordinator.currentState() is MeetingSessionState.Ending -> false
        else -> true
    }

    fun shouldFinalizeOnDestroy(finalReleaseStarted: Boolean): Boolean =
        finalReleaseStarted.not() && coordinator.currentState() is MeetingSessionState.Ending

    fun shouldMoveTaskToBackOnBackPress(canGoBack: Boolean): Boolean =
        canGoBack.not() && coordinator.currentState() is MeetingSessionState.Active
}
