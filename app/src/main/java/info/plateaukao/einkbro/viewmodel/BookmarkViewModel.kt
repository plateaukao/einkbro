package info.plateaukao.einkbro.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Stack

class BookmarkViewModel(private val bookmarkManager: BookmarkManager) : ViewModel() {

    private val _uiState = MutableStateFlow<List<Bookmark>>(emptyList())
    val uiState: StateFlow<List<Bookmark>> = _uiState

    private val folderStack: Stack<Bookmark> = Stack<Bookmark>().apply { push(Bookmark("", "")) }

    var currentFolder: MutableState<Bookmark> = mutableStateOf(folderStack.peek())

    init {
        updateUiState()
    }

    private var sortMode = BookmarkManager.SortMode.BY_ORDER

    private fun updateUiState() {
        viewModelScope.launch {
            val bookmarks = bookmarkManager.getBookmarksByParent(folderStack.peek().id)
            currentFolder.value = folderStack.peek()
            if (sortMode == BookmarkManager.SortMode.BY_ORDER) {
                _uiState.value = bookmarks.sortedBy { bookmark -> bookmark.order }
            } else {
                _uiState.value = bookmarks.sortedBy { bookmark -> bookmark.title }
            }
        }
    }

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

    fun insertBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkManager.insert(bookmark)
            updateUiState()
        }
    }

    fun updateBookmarksOrder(bookmarks: List<Bookmark>) {
        viewModelScope.launch {
            bookmarkManager.updateBookmarksOrder(bookmarks)
            sortMode = BookmarkManager.SortMode.BY_ORDER
            updateUiState()
        }
    }

    suspend fun insertDirectory(title: String, parentId: Int = 0) {
        bookmarkManager.insert(
            Bookmark(
                title = title,
                url = "",
                isDirectory = true,
                parent = parentId,
            )
        )
        updateUiState()
    }

    suspend fun getBookmarkFolders(): List<Bookmark> {
        return bookmarkManager.getBookmarkFolders()
    }
}

class BookmarkViewModelFactory(private val bookmarkManager: BookmarkManager) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BookmarkViewModel(bookmarkManager) as T
}