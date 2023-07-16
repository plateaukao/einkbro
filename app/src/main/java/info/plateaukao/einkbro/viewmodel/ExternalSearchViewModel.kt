package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ExternalSearchViewModel: ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()

    private val _showButton = MutableStateFlow(false)
    val showButton: StateFlow<Boolean> = _showButton.asStateFlow()

    fun toggleButtonVisibility(isVisible: Boolean) {
        _showButton.value = isVisible
    }
}