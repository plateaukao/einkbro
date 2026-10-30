package info.plateaukao.einkbro.activity

import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible

class WebAppActivity : BrowserActivity() {
    override var shouldRunClearService: Boolean = false

    init {
        browserState.isWebApp = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val uri = intent.data
        val valid = uri?.scheme in listOf("http", "https") && !uri?.host.isNullOrBlank()
        val startUri = if (valid) uri!! else Uri.parse("about:blank")
        intent.data = startUri
        super.onCreate(savedInstanceState)
        if (!valid) {
            finish()
            return
        }
        savedInstanceState?.getBundle("webAppState")?.let { ebWebView.restoreState(it) }
        if (savedInstanceState?.getBoolean("webAppControls", true) == false && browserState.binding.appBar.isVisible) {
            toggleFullscreen()
        }
        @Suppress("DEPRECATION")
        setTaskDescription(TaskDescription(intent.getStringExtra(Intent.EXTRA_TITLE) ?: startUri.host, null, 0))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("webAppState", Bundle().also { ebWebView.saveState(it) })
        outState.putBoolean("webAppControls", browserState.binding.appBar.isVisible)
        super.onSaveInstanceState(outState)
    }

    override fun handleBackKey() {
        if (browserState.binding.layoutOverview.isVisible) {
            hideOverview()
            return
        }
        if (onHideCustomView()) return
        if (ebWebView.canGoBack()) ebWebView.goBack() else removeAlbum()
    }

    override fun addNewTab(url: String) = ebWebView.loadUrl(url)

    override fun updateProgress(progress: Int) {
        super.updateProgress(progress)
        tabManager.updateTitle()
    }
}

internal fun Uri.isSameOriginAs(other: Uri): Boolean {
    fun Uri.effectivePort() = if (port != -1) port else if (scheme == "https") 443 else 80
    return scheme == other.scheme && host.equals(other.host, ignoreCase = true) &&
        effectivePort() == other.effectivePort()
}
