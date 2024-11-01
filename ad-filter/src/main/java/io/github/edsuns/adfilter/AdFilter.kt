package io.github.edsuns.adfilter

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.edsuns.adblockclient.ResourceType
import io.github.edsuns.adfilter.impl.AdFilterImpl

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
interface AdFilter {

    /**
     * View model of AdFilter.
     */
    val viewModel: FilterViewModel

    /**
     * Whether any filters have been added since the app was installed.
     */
    val hasInstallation: Boolean

    /**
     * Custom filter.
     */
    val customFilter: CustomFilter

    fun setEnabled(enable: Boolean)
    /**
     * Call this function when [WebViewClient.shouldInterceptRequest],
     * and use [FilterResult.resourceResponse] as return value.
     */
    fun shouldIntercept(webView: WebView, request: WebResourceRequest): FilterResult

    /**
     * @param url url of the request
     * @param documentUrl the origin of the request
     * @param resourceType [ResourceType]
     */
    fun shouldIntercept(
        url: String,
        documentUrl: String = url,
        resourceType: ResourceType? = null
    ): FilterResult

    /**
     * Call this function when [WebViewClient.onPageStarted], it will run the filter script.
     */
    fun performScript(webView: WebView?, url: String?)

    /**
     * Call this function when [webView] is created to setup filter on it.
     */
    fun setupWebView(webView: WebView)

    companion object {
        @Volatile
        private var instance: AdFilter? = null

        /**
         * @return [AdFilter] singleton (if it is not instantiated, an exception is thrown)
         */
        fun get(): AdFilter =
            instance ?: throw RuntimeException("Should call create() before get()")

        /**
         * @return [AdFilter] singleton (if it is not instantiated, singleton will be created)
         */
        fun get(context: Context): AdFilter = instance ?: synchronized(this) {
            // keep application context rather than any other context to avoid memory leak
            instance = instance ?: AdFilterImpl(context.applicationContext)
            instance!!
        }

        /**
         * Instantiate [AdFilter] singleton.
         */
        fun create(context: Context): AdFilter = get(context)
    }
}