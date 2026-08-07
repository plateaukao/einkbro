package info.plateaukao.einkbro.browser

import android.webkit.JavascriptInterface
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordType
import info.plateaukao.einkbro.search.suggestion.SearchSuggestionViewModel
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.EBWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Javascript bridge backing the search box on the built-in start page
 * (assets/start_page.html): serves the same suggestion list as the native
 * input bar and loads the submitted text or url.
 *
 * The interface is attached to every page, so each entry point re-checks that
 * the current page really is the start page before doing anything — arbitrary
 * sites must not read history/bookmark-based suggestions or steer the tab.
 */
class StartPageBridge(private val webView: EBWebView) : KoinComponent {
    private val coroutineScope: CoroutineScope by inject()
    private val suggestionViewModel = SearchSuggestionViewModel()

    private var initialized = false
    private var searchJob: Job? = null

    @JavascriptInterface
    fun querySuggestions(query: String, token: Int) {
        webView.post {
            if (webView.url != Constants.START_PAGE_URL) return@post
            searchJob?.cancel()
            searchJob = coroutineScope.launch(Dispatchers.IO) {
                if (query.isEmpty() || !initialized) {
                    suggestionViewModel.initSuggestions()
                    initialized = true
                }
                if (query.isNotEmpty()) {
                    suggestionViewModel.updateSuggestions(query)
                }
                val json = toJson(suggestionViewModel.suggestions.value)
                webView.post {
                    if (webView.url != Constants.START_PAGE_URL) return@post
                    webView.evaluateJavascript(
                        "window.__einkbroSuggestions && window.__einkbroSuggestions($token, $json)",
                        null
                    )
                }
            }
        }
    }

    @JavascriptInterface
    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        webView.post {
            if (webView.url != Constants.START_PAGE_URL) return@post
            // loadUrl wraps non-url text into a search engine query
            webView.loadUrl(trimmed)
        }
    }

    private fun toJson(records: List<Record>): String {
        val array = JSONArray()
        records.take(MAX_SUGGESTIONS).forEach {
            array.put(
                JSONObject()
                    .put("title", it.title ?: "")
                    .put("url", it.url)
                    .put("s", it.type == RecordType.Suggestion)
            )
        }
        return array.toString()
    }

    companion object {
        private const val MAX_SUGGESTIONS = 8
    }
}
