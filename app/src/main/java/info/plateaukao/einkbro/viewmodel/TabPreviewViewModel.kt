package info.plateaukao.einkbro.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class TabPreviewViewModel : ViewModel() {
    private val _tabs = mutableStateListOf<TabInfo>()
    val tabs: List<TabInfo>
        get() = _tabs

    fun addTab(tab: TabInfo): Boolean {
        return _tabs.add(tab)
    }

    fun removeTab(tab: TabInfo) = _tabs.remove(tab)

    fun updateTabs(tabs: List<TabInfo>) {
        _tabs.clear()
        _tabs.addAll(tabs)
    }
}

class TabInfo(val title: String, val url: String, val favicon: Bitmap? = null, val isActivated: Boolean = false)