package info.plateaukao.einkbro.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Highlight
import info.plateaukao.einkbro.unit.IntentUnit
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HighlightViewModel : ViewModel(), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    fun getAllArticles() = bookmarkManager.getAllArticles()

    private suspend fun getAllArticlesAsync() = bookmarkManager.getAllArticlesAsync()

    suspend fun getArticle(articleId: Int) = bookmarkManager.getArticle(articleId)

    suspend fun dumpArticlesHighlightsAsHtml(): String {
        val articles = getAllArticlesAsync()
        var data = ""
        articles.sortedByDescending { it.date }.forEach {
            data += dumpSingleArticleHighlights(it.id) + "<br/><br/>"
        }
        return data
    }

    suspend fun dumpSingleArticleHighlights(articleId: Int): String {
        val article = getArticle(articleId)
        val articleTitle = article?.title.orEmpty()
        val highlights = getHighlightsForArticleAsync(articleId)
        var data = "<h2>$articleTitle</h2><hr/>"
        data += highlights.joinToString("<br/><br/>") { it.content }
        data += "<br/><br/>"
        return data
    }

    fun deleteArticle(articleId: Int) {
        viewModelScope.launch {
            bookmarkManager.deleteArticle(articleId)
        }
    }

    fun getHighlightsForArticle(articleId: Int) =
        bookmarkManager.getHighlightsForArticle(articleId)

    private suspend fun getHighlightsForArticleAsync(articleId: Int) =
        bookmarkManager.getHighlightsForArticleAsync(articleId)

    fun launchUrl(activity: Activity, url: String) {
        IntentUnit.launchUrl(activity, url)
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch {
            bookmarkManager.deleteHighlight(highlight)
        }
    }
}
