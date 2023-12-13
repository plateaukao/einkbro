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

    fun deleteArticle(articleId: Int) {
        viewModelScope.launch {
            bookmarkManager.deleteArticle(articleId)
        }
    }
    fun getHighlightsForArticle(articleId: Int) =
        bookmarkManager.getHighlightsForArticle(articleId)

    fun launchUrl(activity: Activity, url: String) {
        IntentUnit.launchUrl(activity, url)
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch {
            bookmarkManager.deleteHighlight(highlight)
        }
    }
}
