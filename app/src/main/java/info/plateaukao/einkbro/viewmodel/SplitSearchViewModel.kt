package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import java.net.URLEncoder

class SplitSearchViewModel: ViewModel() {
    var state: ActionModeMenuState.SplitSearch? = null

    fun reset() {
        state = null
    }
    fun getUrl(text: String): String =
        state?.stringFormat?.format(URLEncoder.encode(text , "UTF-8")) ?: ""
}