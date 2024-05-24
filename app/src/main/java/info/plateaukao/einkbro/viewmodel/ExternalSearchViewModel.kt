package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.SplitSearchItemInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URLEncoder

class ExternalSearchViewModel: ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()
    val searchActions = config.splitSearchItemInfoList + SplitSearchItemInfo("external", config.customProcessTextUrl, true)
    var currentSearchAction = searchActions.last()
    private var currentSearchText = ""

    private val _showButton = MutableStateFlow(false)
    val showButton: StateFlow<Boolean> = _showButton.asStateFlow()

    fun setButtonVisibility(isVisible: Boolean) {
        _showButton.value = isVisible
    }

    fun generateSearchUrl(
        searchText: String = currentSearchText,
        splitSearchItemInfo: SplitSearchItemInfo = currentSearchAction,
    ): String {
        currentSearchText = searchText
        return if (splitSearchItemInfo.stringPattern.contains("%s"))
            splitSearchItemInfo.stringPattern.format(URLEncoder.encode(searchText, "UTF-8"))
        else "${splitSearchItemInfo.stringPattern}$searchText"
    }
}