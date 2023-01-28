package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Stack

class BookmarkViewModel(private val bookmarkDao: BookmarkDao) : ViewModel() {

    private val _uiState = MutableStateFlow<List<Bookmark>>(emptyList())
    val uiState: StateFlow<List<Bookmark>> = _uiState

    private val folderStack: Stack<Bookmark> = Stack<Bookmark>().apply { push(Bookmark("", "")) }

    init {
        updateUiState()
    }

    private fun updateUiState() {
        viewModelScope.launch {
            bookmarkDao.getBookmarksByParentFlow(folderStack.peek().id).collect {
                _uiState.value = it
            }
        }
    }

    val currentFolder: Bookmark
        get() = folderStack.peek()

    fun toRootFolder() {
        while (folderStack.size > 1) {
            folderStack.pop()
        }
        updateUiState()
    }

    fun outOfFolder() {
        if (folderStack.size > 1) {
            folderStack.pop()
            updateUiState()
        }
    }

    fun intoFolder(bookmark: Bookmark) {
        folderStack.push(bookmark)
        updateUiState()
    }
}

class BookmarkViewModelFactory(private val bookmarkDao: BookmarkDao) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BookmarkViewModel(bookmarkDao) as T
}