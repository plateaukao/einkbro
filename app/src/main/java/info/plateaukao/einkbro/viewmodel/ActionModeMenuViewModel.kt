package info.plateaukao.einkbro.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.view.ActionMode
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserActivity
import info.plateaukao.einkbro.activity.MenuInfo
import info.plateaukao.einkbro.activity.toMenuInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.util.Constants.Companion.ACTION_GPT
import info.plateaukao.einkbro.view.dialog.compose.ActionModeDialogFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ActionModeMenuViewModel : ViewModel(), KoinComponent {
    private val configManager: ConfigManager by inject()

    private var actionMode: ActionMode? = null
    private val _clickedPoint = MutableStateFlow(Point(0, 0))
    val clickedPoint: StateFlow<Point> = _clickedPoint.asStateFlow()

    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    fun isInActionMode(): Boolean = actionMode != null

    fun updateActionMode(actionMode: ActionMode?) {
        this.actionMode = actionMode
    }

    private var fragment: DialogFragment? = null
    fun showActionModeDialogFragment(
        context: Context,
        supportFragmentManager: FragmentManager,
        packageManager: PackageManager
    ) {
        if (fragment != null && fragment?.isAdded == true) {
            return
        }
        fragment = ActionModeDialogFragment(
            this,
            getAllProcessTextMenuInfos(context, packageManager)
        ) {
            finishActionMode()
            fragment = null
        }.apply {
            show(supportFragmentManager, "action_mode_dialog")
        }
    }

    fun finishActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    fun updateSelectedText(text: String) {
        _selectedText.value = text
    }

    fun updateClickedPoint(point: Point) {
        _clickedPoint.value = point
    }

    private fun getAllProcessTextMenuInfos(
        context: Context,
        packageManager: PackageManager
    ): List<MenuInfo> {
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        val menuInfos = resolveInfos.map { it.toMenuInfo(packageManager) }.toMutableList()

        menuInfos.add(
            0,
            MenuInfo(
                "Copy", //R.string.menu_browser,
                icon = context.getDrawable(R.drawable.ic_copy),
                action = {
                    ShareUtil.copyToClipboard(context, selectedText.value)
                }
            )
        )
        if (configManager.gptApiKey.isNotEmpty()) {
            menuInfos.add(
                0,
                MenuInfo(

                    "GPT", //R.string.menu_gpt,
                    icon = context.getDrawable(R.drawable.ic_chat_gpt),
                    intent = Intent(context, BrowserActivity::class.java).apply {
                        action = ACTION_GPT
                    }
                )
            )
        }

        return menuInfos
    }

}