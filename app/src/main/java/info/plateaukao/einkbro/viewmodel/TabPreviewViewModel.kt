package info.plateaukao.einkbro.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkDao
import kotlinx.coroutines.flow.Flow

class TabPreviewViewModel() : ViewModel() {
    private val _tabs = mutableStateListOf<TabInfo>()
    val tabs: List<TabInfo>
        get() = _tabs

    fun addTab(tab: TabInfo) = _tabs.add(tab)

    fun removeTab(tab: TabInfo) = _tabs.remove(tab)

    fun updateTabs(tabs: List<TabInfo>) {
        _tabs.clear()
        _tabs.addAll(tabs)
    }
}

@Suppress("UNCHECKED_CAST")
class TabPreviewViewModelFactory() : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TabPreviewViewModel() as T
}

class TabInfo(val title: String, val url: String, val favicon: Bitmap? = null, val isActivated: Boolean = false)