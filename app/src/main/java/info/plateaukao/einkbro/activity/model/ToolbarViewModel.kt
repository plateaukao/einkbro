package info.plateaukao.einkbro.activity.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.view.toolbaricons.ToolbarActionInfo
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToolbarViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()

    val titleLiveData: LiveData<String>
        get() = _titleLiveData
    private val _titleLiveData = MutableLiveData<String>()

    fun setTitle(title: String) {
        _titleLiveData.value = title
    }

    val toolbarActionInfoListLiveData: LiveData<List<ToolbarActionInfo>>
        get() = _toolbarActionInfoListLiveData
    private val _toolbarActionInfoListLiveData =
        MutableLiveData<List<ToolbarActionInfo>>(config.toolbarActions.toToolbarActionInfoList())

    private fun List<ToolbarAction>.toToolbarActionInfoList(): List<ToolbarActionInfo> {
        return this.map { toolbarAction ->
            when (toolbarAction) {
                ToolbarAction.BoldFont -> ToolbarActionInfo(toolbarAction, config.boldFontStyle)
                ToolbarAction.Refresh -> ToolbarActionInfo(toolbarAction, false)
                ToolbarAction.Desktop -> ToolbarActionInfo(toolbarAction, config.desktop)
                ToolbarAction.Touch -> ToolbarActionInfo(toolbarAction, config.enableTouchTurn)
                else -> ToolbarActionInfo(toolbarAction, false)
            }
        }
    }
}


