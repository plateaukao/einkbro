package info.plateaukao.einkbro.browser

import android.app.Activity
import android.content.Context
import android.net.http.SslError
import android.security.KeyChain
import android.util.Log
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.compose.AuthenticationDialogFragment

class WebViewSslHandler(
    private val context: Context,
    private val config: ConfigManager,
) {
    private val dialogManager: DialogManager by lazy { DialogManager(context as Activity) }

    fun onReceivedHttpAuthRequest(handler: HttpAuthHandler?) {
        AuthenticationDialogFragment { username, password ->
            handler?.proceed(username, password)
        }.show((context as FragmentActivity).supportFragmentManager, "AuthenticationDialog")
    }

    // return true means it's processed
    private fun handlePrivateKeyAlias(request: ClientCertRequest, alias: String?): Boolean {
        val keyAlias = alias ?: return false
        val holder = context as? Activity ?: return false
        try {
            val certChain = KeyChain.getCertificateChain(holder, keyAlias) ?: return false
            val privateKey = KeyChain.getPrivateKey(holder, keyAlias) ?: return false
            request.proceed(privateKey, certChain)
            return true
        } catch (e: Exception) {
            Log.e(
                "ebWebViewClient",
                "Error when getting CertificateChain or PrivateKey for alias '${alias}'",
                e
            )
        }
        return false
    }

    fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest, fallback: () -> Unit) {
        val holder = view.context as? Activity ?: return
        KeyChain.choosePrivateKeyAlias(
            holder,
            { alias ->
                if (!handlePrivateKeyAlias(request, alias)) {
                    fallback()
                }
            },
            request.keyTypes, request.principals, request.host, request.port, null
        )
    }

    fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val title = "An Error Occurred!!!"
        var message =
            "The page you are trying to view cannot be shown because the connection isn't private or the authenticity of the received data could not be verified. \n\nIf you want to take the risk and continue viewing the page, please press OK.\n\n\nReason: "
        when (error.primaryError) {
            SslError.SSL_UNTRUSTED -> message += """"Certificate authority is not trusted.""""
            SslError.SSL_EXPIRED -> message += """"Certificate has expired.""""
            SslError.SSL_IDMISMATCH -> message += """"Certificate Hostname mismatch.""""
            SslError.SSL_NOTYETVALID -> message += """"Certificate is not yet valid.""""
            SslError.SSL_DATE_INVALID -> message += """"Certificate date is invalid.""""
            SslError.SSL_INVALID -> message += """"Certificate is invalid.""""
        }

        Log.e(TAG, "onReceivedSslError: $message")
        if (config.browser.enableCertificateErrorDialog) {
            dialogManager.showOkCancelDialog(
                title = title,
                message = message,
                showInCenter = true,
                okAction = { handler.proceed() },
                cancelAction = { handler.cancel() }
            )
        } else {
            handler.proceed()
        }
    }

    companion object {
        private const val TAG = "ebWebViewClient"
    }
}
