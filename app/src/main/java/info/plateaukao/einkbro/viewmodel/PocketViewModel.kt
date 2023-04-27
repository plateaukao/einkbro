package info.plateaukao.einkbro.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.plateaukao.einkbro.pocket.PocketNetwork
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.IntentUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@OptIn(ExperimentalCoroutinesApi::class)
class PocketViewModel : KoinComponent, ViewModel() {
    private val pocketNetwork: PocketNetwork by inject()
    private val configManager: ConfigManager by inject()

    private var requestToken: String = ""

    var urlToBeAdded: String = ""

    suspend fun shareToPocketWithLoginCheck(
        context: Context,
        url: String,
    ): PocketShareState {
        if (shareToPocketApp(context, url)) return PocketShareState.SharedByPocketApp

        if (!isPocketLoggedIn()) {
            urlToBeAdded = url
            val authUrl = getAuthUrl()
            return PocketShareState.NeedLogin(authUrl)
        }

        val resolvedUrl = addUrlToPocket(url) ?: return PocketShareState.Failed
        return PocketShareState.SharedByEinkBro(resolvedUrl)
    }

    private fun shareToPocketApp(context: Context, url: String): Boolean {
        if (IntentUnit.isPocketInstalled(context)) {
            return try {
                val intent = Intent().apply {
                    setClassName(
                        "com.ideashower.readitlater.pro",
                        "com.ideashower.readitlater.activity.AddActivity"
                    )
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            return false
        }
    }

    private fun isPocketLoggedIn(): Boolean {
        return configManager.pocketAccessToken.isNotBlank()
    }

    private suspend fun getAuthUrl(): String {
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

    private suspend fun addUrlToPocket(url: String, title: String? = null, tags: String? = null): String? {
        return pocketNetwork.addUrlToPocket(configManager.pocketAccessToken, url, title, tags)
    }
}

sealed class PocketShareState {
    object SharedByPocketApp: PocketShareState()
    object Failed: PocketShareState()
    data class SharedByEinkBro(val pocketUrl: String): PocketShareState()
    data class NeedLogin(val authUrl: String): PocketShareState()
}

class PocketViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PocketViewModel() as T
}
