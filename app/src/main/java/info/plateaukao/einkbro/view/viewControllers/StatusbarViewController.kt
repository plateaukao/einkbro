package info.plateaukao.einkbro.view.viewControllers

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.statusbar.Statusbar
import info.plateaukao.einkbro.view.statusbar.StatusbarItem
import info.plateaukao.einkbro.view.statusbar.StatusbarPosition
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StatusbarViewController(
    private val composeView: ComposeView,
    private val applyConstraints: (StatusbarPosition) -> Unit,
) : KoinComponent {

    private val config: ConfigManager by inject()

    private var items by mutableStateOf(config.ui.statusbarItems)
    private var pageInfo by mutableStateOf("")
    private var webAppOrigin by mutableStateOf<String?>(null)

    init {
        composeView.setContent {
            MyTheme {
                val origin = webAppOrigin
                if (origin != null) {
                    Text(
                        origin,
                        Modifier.fillMaxWidth().background(MaterialTheme.colors.background).padding(8.dp),
                        color = MaterialTheme.colors.onBackground,
                    )
                } else Statusbar(items = items, pageInfo = pageInfo)
            }
        }
        applyConstraints(config.ui.statusbarPosition)
    }

    fun show() {
        if (webAppOrigin != null) {
            hide()
            return
        }
        if (!config.ui.statusbarEnabled) {
            hide()
            return
        }
        items = config.ui.statusbarItems
        applyConstraints(config.ui.statusbarPosition)
        composeView.visibility = View.VISIBLE
    }

    fun hide() {
        composeView.visibility = if (webAppOrigin == null) View.GONE else View.VISIBLE
    }

    fun showWebAppOrigin(origin: String?) {
        webAppOrigin = origin
        hide()
    }

    fun updatePageInfo(text: String) {
        // Called per scroll event; a GONE ComposeView still recomposes, so
        // skip when the bar is hidden or the item isn't configured.
        if (composeView.visibility != View.VISIBLE) return
        if (!items.contains(StatusbarItem.PageInfo)) return
        pageInfo = text
    }

    fun refresh() {
        items = config.ui.statusbarItems
        applyConstraints(config.ui.statusbarPosition)
    }
}
