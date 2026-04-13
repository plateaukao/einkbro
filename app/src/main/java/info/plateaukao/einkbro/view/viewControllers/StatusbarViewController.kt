package info.plateaukao.einkbro.view.viewControllers

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
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

    init {
        composeView.setContent {
            MyTheme {
                Statusbar(items = items, pageInfo = pageInfo)
            }
        }
        applyConstraints(config.ui.statusbarPosition)
    }

    fun show() {
        if (!config.ui.statusbarEnabled) {
            hide()
            return
        }
        items = config.ui.statusbarItems
        applyConstraints(config.ui.statusbarPosition)
        composeView.visibility = View.VISIBLE
    }

    fun hide() {
        composeView.visibility = View.GONE
    }

    fun updatePageInfo(text: String) {
        pageInfo = text
    }

    fun refresh() {
        items = config.ui.statusbarItems
        applyConstraints(config.ui.statusbarPosition)
    }
}
