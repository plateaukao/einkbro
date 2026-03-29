package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.SavedPage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SavedPageViewModel : ViewModel(), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    fun getAllSavedPages() = bookmarkManager.getAllSavedPages()

    fun deleteSavedPage(savedPage: SavedPage) {
        viewModelScope.launch {
            File(savedPage.filePath).delete()
            bookmarkManager.deleteSavedPage(savedPage)
        }
    }
}
