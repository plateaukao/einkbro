package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.unit.ShareUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemoteConnViewModel : ViewModel() {
    var isSendingTextSearch: Boolean = false
    private var isReceivingLink: Boolean = false

    private val _remoteConnected = MutableStateFlow(false)
    val remoteConnected: StateFlow<Boolean> = _remoteConnected.asStateFlow()

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