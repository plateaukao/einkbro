package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.SavedPage
import kotlinx.coroutines.launch
import java.io.File

class SavedPageViewModel(
    private val bookmarkManager: BookmarkManager,
) : ViewModel() {

    fun getAllSavedPages() = bookmarkManager.getAllSavedPages()

    fun deleteSavedPage(savedPage: SavedPage) {
        viewModelScope.launch {
            File(savedPage.filePath).delete()
            bookmarkManager.deleteSavedPage(savedPage)
        }
    }
}
