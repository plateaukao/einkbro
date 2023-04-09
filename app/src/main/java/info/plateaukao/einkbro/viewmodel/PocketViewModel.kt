package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.plateaukao.einkbro.pocket.PocketNetwork
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalCoroutinesApi::class)
class PocketViewModel : KoinComponent, ViewModel() {
    private val pocketNetwork: PocketNetwork by inject()
    private val configManager: ConfigManager by inject()

    private var requestToken: String = ""

    fun isPocketLoggedIn(): Boolean {
        return configManager.pocketAccessToken.isNotBlank()
    }
    suspend fun getAuthUrl(): String {
        return suspendCancellableCoroutine { continuation ->
            pocketNetwork.getRequestToken { requestToken ->
                this.requestToken = requestToken
                val authUrl =
                    "https://getpocket.com/auth/authorize?request_token=$requestToken&redirect_uri=einkbropocket://pocket-auth"
                continuation.resume(authUrl) {}
            }
        }
    }

    suspend fun getAndSaveAccessToken(): String {
        return suspendCancellableCoroutine { continuation ->
            pocketNetwork.getAccessToken(this.requestToken) { accessToken ->
                configManager.pocketAccessToken = accessToken
                continuation.resume(accessToken) { }
            }
        }
    }

    suspend fun addUrlToPocket(url: String, title: String? = null, tags: String? = null): String? {
        return pocketNetwork.addUrlToPocket(configManager.pocketAccessToken, url, title, tags)
    }
}

class PocketViewModelFactory() : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PocketViewModel() as T
}
