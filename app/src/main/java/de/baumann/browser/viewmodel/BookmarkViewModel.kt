package de.baumann.browser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkDao
import kotlinx.coroutines.flow.Flow

class BookmarkViewModel(private val bookmarkDao: BookmarkDao): ViewModel() {
    fun bookmarksByParent(parentId: Int): Flow<List<Bookmark>> = bookmarkDao.getBookmarksByParentFlow(parentId)
}

class BookmarkViewModelFactory(private val bookmarkDao: BookmarkDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookmarkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookmarkViewModel(bookmarkDao) as T
        }
        throw IllegalAccessException("Unknown ViewModel class")
    }
}