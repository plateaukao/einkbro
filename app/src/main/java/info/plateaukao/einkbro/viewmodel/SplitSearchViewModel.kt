package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import java.net.URLEncoder

class SplitSearchViewModel : ViewModel() {
    var state: ActionModeMenuState.SplitSearch? = null

    fun reset() {
        state = null
    }

    fun getUrl(text: String): String {
        val stringFormat = state?.stringFormat ?: return ""
        if (stringFormat.contains("=")) {
            val url = stringFormat.format(text, "UTF-8")
            return url.split("=")[0] + "=" +
                    URLEncoder.encode(url.split("=")[1], "UTF-8")
        } else {
            return state?.stringFormat?.format(URLEncoder.encode(text, "UTF-8")).orEmpty()
        }
    }
}