package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkDao
import kotlinx.coroutines.flow.Flow

class BookmarkViewModel(private val bookmarkDao: BookmarkDao): ViewModel() {
    fun bookmarksByParent(parentId: Int): Flow<List<Bookmark>> = bookmarkDao.getBookmarksByParentFlow(parentId)
}

class BookmarkViewModelFactory(private val bookmarkDao: BookmarkDao): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = BookmarkViewModel(bookmarkDao) as T
}