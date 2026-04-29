package info.dvkr.screenstream

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import info.dvkr.screenstream.common.settings.AppSettings
import info.dvkr.screenstream.ui.enableEdgeToEdge
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

public class SingleActivity : AppUpdateActivity() {

    internal companion object {
        internal fun getIntent(context: Context): Intent = Intent(context, SingleActivity::class.java)
    }

    private lateinit var webView: WebView
    private val appSettings: AppSettings by lazy { get() }
    private var currentEntryUrl: String = BuildConfig.LAUNCH_URL
    private var pendingWebPermissionRequest: PermissionRequest? = null
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

        if (savedInstanceState == null) {
            webView.loadUrl(currentEntryUrl)
        } else {
            val restoredState = webView.restoreState(savedInstanceState)
            if (restoredState == null) webView.loadUrl(currentEntryUrl)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        XLog.d(getLog("onNewIntent"))
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        hideSystemBars()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        pendingWebPermissionRequest?.deny()
        pendingWebPermissionRequest = null
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
                    currentEntryUrl = resolvedUrl
                    webView.loadUrl(currentEntryUrl)
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

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
