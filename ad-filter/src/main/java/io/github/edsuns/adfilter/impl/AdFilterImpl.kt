package io.github.edsuns.adfilter.impl

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import io.github.edsuns.adblockclient.ResourceType
import io.github.edsuns.adfilter.*
import io.github.edsuns.adfilter.impl.Constants.FILE_STORE_DIR
import io.github.edsuns.adfilter.script.ElementHiding
import io.github.edsuns.adfilter.script.JsAssets
import io.github.edsuns.adfilter.script.ScriptInjection
import io.github.edsuns.adfilter.script.Scriptlet
import java.io.File
import java.util.Collections
import java.util.WeakHashMap

/**
 * Created by Edsuns@qq.com on 2021/7/29.
 */
internal class AdFilterImpl(appContext: Context) : AdFilter {

    init {
        // Injected-JS sources live in module assets; wire up the loader before
        // anything can touch ElementHiding/Scriptlet/ScriptInjection.
        JsAssets.init(appContext)
    }

    private val detector: Detector = DetectorImpl()
    internal val binaryDataStore: BinaryDataStore =
        BinaryDataStore(File(appContext.filesDir, FILE_STORE_DIR))
    private val filterDataLoader: FilterDataLoader = FilterDataLoader(detector, binaryDataStore)

    private val elementHiding: ElementHiding = ElementHiding(detector)
    private val scriptlet: Scriptlet = Scriptlet(detector)

    override val customFilter = filterDataLoader.getCustomFilter()

    override val viewModel = FilterViewModelImpl(appContext, filterDataLoader, binaryDataStore)

    // Cached current main-frame URL per WebView. Written on the main-frame
    // shouldInterceptRequest and in performScript (onPageStarted); read on
    // WebView IO threads when filtering subresources. Avoids hopping to the
    // main thread for every subresource, which used to serialize requests
    // behind UI work.
    private val mainFrameUrls: MutableMap<WebView, String> =
        Collections.synchronizedMap(WeakHashMap())

    override val hasInstallation: Boolean
        get() = viewModel.sharedPreferences.hasInstallation

    override fun setEnabled(enable: Boolean) {
        if (enable) {
            viewModel.filters.value.values.forEach {
                if (it.isEnabled && it.hasDownloaded()) {
                    viewModel.enableFilter(it)
                }
            }
            filterDataLoader.load(FilterDataLoader.ID_CUSTOM)
        } else {
            filterDataLoader.unloadAll()
            filterDataLoader.unloadCustomFilter()
        }
        viewModel.updateEnabledFilterCount()
    }

    /**
     * Notify the application of a resource request and allow the application to return the data.
     *
     * If the return value is null, the WebView will continue to load the resource as usual.
     * Otherwise, the return response and data will be used.
     *
     * NOTE: This method is called on a thread other than the UI thread so clients should exercise
     * caution when accessing private data or the view system.
     */
    override fun shouldIntercept(
        webView: WebView,
        request: WebResourceRequest,
    ): FilterResult {
        val url = request.url.toString()
        if (request.isForMainFrame) {
            mainFrameUrls[webView] = url
            return FilterResult(null, url, null)
        }

        val documentUrl = mainFrameUrls[webView]
            ?: return FilterResult(null, url, null)

        val resourceType = ResourceType.from(request)

        val result = shouldIntercept(url, documentUrl, resourceType)
        if (result.shouldBlock && resourceType.isVisibleResource()) {
            elementHiding.elemhideBlockedResource(webView, url)
        }

        return result
    }

    override fun shouldIntercept(
        url: String,
        documentUrl: String,
        resourceType: ResourceType?,
    ): FilterResult {
        val type = resourceType ?: ResourceType.from(Uri.parse(url)) ?: ResourceType.UNKNOWN
        val rule = detector.shouldBlock(url, documentUrl, type)

        return if (rule != null) {
            FilterResult(rule, url, WebResourceResponse(null, null, null))
        } else {
            FilterResult(null, url, null)
        }
    }

    private fun ResourceType.isVisibleResource(): Boolean =
        this === ResourceType.IMAGE || this === ResourceType.MEDIA || this === ResourceType.SUBDOCUMENT

    override fun setupWebView(webView: WebView) {
        webView.addJavascriptInterface(elementHiding, ScriptInjection.bridgeNameFor(elementHiding))
        webView.addJavascriptInterface(scriptlet, ScriptInjection.bridgeNameFor(scriptlet))
    }

    override fun performScript(webView: WebView?, url: String?) {
        if (webView != null && !url.isNullOrEmpty()) {
            mainFrameUrls[webView] = url
        }
        elementHiding.perform(webView, url)
        scriptlet.perform(webView, url)
    }
}