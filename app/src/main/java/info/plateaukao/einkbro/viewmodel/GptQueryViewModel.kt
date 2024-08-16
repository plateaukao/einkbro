package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.ChatGptQuery
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GptQueryViewModel : ViewModel(), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    fun getGptQueries() = bookmarkManager.getAllChatGptQueries()

    suspend fun getGptQuery(gptQueryId: Int) = bookmarkManager.getChatGptQueryById(gptQueryId)

//    suspend fun dumpArticlesHighlights(): String {
//        val articles = getAllArticlesAsync()
//        var data = ""
//        articles.sortedByDescending { it.date }.forEach {
//            data += dumpSingleArticleHighlights(it.id) + "<br/><br/>"
//        }
//        return data
//    }

//    suspend fun dumpSingleArticleHighlights(articleId: Int): String {
//        val article = getArticle(articleId)
//        val articleTitle = article?.title.orEmpty()
//        val highlights = getHighlightsForArticleAsync(articleId)
//        var data = "<h2>$articleTitle</h2><hr/>"
//        data += highlights.joinToString("<br/><br/>") { it.content }
//        data += "<br/><br/>"
//        return data
//    }

    fun deleteGptQuery(query: ChatGptQuery) {
        viewModelScope.launch {
            bookmarkManager.deleteChatGptQuery(query)
        }
    }
}
