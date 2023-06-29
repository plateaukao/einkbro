package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ShareUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteConnViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()

    var isSendingTextSearch: Boolean = false
    var isReceivingLink: Boolean = false

    private val _remoteConnected = MutableStateFlow(false)
    val remoteConnected: StateFlow<Boolean> = _remoteConnected.asStateFlow()

    fun sendTextSearch(text: String) {
        val url = config.splitSearchItemInfoList.first().stringPattern.format(text)
        ShareUtil.startBroadcastingUrl(viewModelScope, url, 10)
    }

    fun toggleTextSearch() {
        isSendingTextSearch = !isSendingTextSearch
        if (!isSendingTextSearch) {
            ShareUtil.stopBroadcast()
        }
        _remoteConnected.value = isSendingTextSearch
    }

    fun toggleReceiveLink(receivedAction: (String) -> Unit) {
        isReceivingLink = !isReceivingLink
        if (isReceivingLink) {
            ShareUtil.startReceiving(viewModelScope) {
                receivedAction(it)
            }
        } else {
            ShareUtil.stopBroadcast()
        }
        _remoteConnected.value = isReceivingLink
    }

    fun reset() {
        if (isSendingTextSearch) {
            toggleTextSearch()
        }
        if (isReceivingLink) {
            toggleReceiveLink {}
        }
        _remoteConnected.value = false
    }
}