package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URLEncoder

class ExternalSearchViewModel: ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()
    private val otherSearchItems = config.splitSearchItemInfoList
    private var currentStringPattern = config.customProcessTextUrl
    private var currentSearchText = ""

    private val _showButton = MutableStateFlow(false)
    val showButton: StateFlow<Boolean> = _showButton.asStateFlow()

    fun setButtonVisibility(isVisible: Boolean) {
        _showButton.value = isVisible
    }

    fun switchSearchItem() {
        if (otherSearchItems.isEmpty()) return

        val currentSelected = otherSearchItems.indexOfFirst { it.stringPattern == currentStringPattern }
        currentStringPattern = if (currentSelected == -1) otherSearchItems[0].stringPattern
        else {
            val nextIndex = (currentSelected + 1) % otherSearchItems.size
            if (nextIndex == 0) {
                config.customProcessTextUrl
            } else {
                otherSearchItems[nextIndex].stringPattern
            }
        }
    }

    fun generateSearchUrl(searchText: String = currentSearchText): String {
        currentSearchText = searchText
        val correctPattern =
            if (currentStringPattern.contains("%s")) currentStringPattern
            else "$currentStringPattern%s"
        return correctPattern.format(URLEncoder.encode(searchText, "UTF-8"))
    }

    fun hasMoreSearchItems(): Boolean = otherSearchItems.isNotEmpty()
}