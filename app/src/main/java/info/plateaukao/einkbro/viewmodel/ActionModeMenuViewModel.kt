package info.plateaukao.einkbro.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.view.ActionMode
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.data.MenuInfo
import info.plateaukao.einkbro.view.data.toMenuInfo
import info.plateaukao.einkbro.view.dialog.compose.ActionModeView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ActionModeMenuViewModel : ViewModel(), KoinComponent {
    private val configManager: ConfigManager by inject()

    private var actionMode: ActionMode? = null
    private val _clickedPoint = MutableStateFlow(Point(100, 100))
    val clickedPoint: StateFlow<Point> = _clickedPoint.asStateFlow()

    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    private val _actionModeMenuState =
        MutableStateFlow(ActionModeMenuState.Idle as ActionModeMenuState)
    val actionModeMenuState: StateFlow<ActionModeMenuState> = _actionModeMenuState.asStateFlow()

    fun isInActionMode(): Boolean = actionMode != null

    fun updateActionMode(actionMode: ActionMode?) {
        this.actionMode = actionMode
        if (actionMode == null) {
            finish()
        }
    }

    private var actionModeView: View? = null
    fun showActionModeView(
        context: Context,
        viewGroup: ViewGroup,
    ) {
        actionModeView = ActionModeView(context = context).apply {
            init(
                actionModeMenuViewModel = this@ActionModeMenuViewModel,
                menuInfos = getAllProcessTextMenuInfos(context, context.packageManager),
            )
        }

        actionModeView?.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        actionModeView?.x = _clickedPoint.value.x.toFloat()
        actionModeView?.y = _clickedPoint.value.y + ViewUnit.dpToPixel(context, 5)

        viewGroup.addView(actionModeView)

        actionModeView?.post { checkBoundary() }

        viewModelScope.launch {
            clickedPoint.collect { anchorPoint ->
                actionModeView?.x = anchorPoint.x.toFloat()
                actionModeView?.y = (anchorPoint.y + ViewUnit.dpToPixel(context, 5))
                actionModeView?.post { checkBoundary() }
            }
        }
    }

    private fun checkBoundary() {
        val view = actionModeView ?: return
        val parentWidth = (view.parent as View).width
        val parentHeight = (view.parent as View).height

        // Calculate the new position to ensure the view is within bounds
        val padding = ViewUnit.dpToPixel(view.context, 10)
        view.x =
            if (view.x + view.width + padding > parentWidth) parentWidth - view.width - padding else view.x
        view.y =
            if (view.y + view.height + padding > parentHeight) parentHeight - view.height - padding else view.y
    }

    fun finish() {
        actionMode?.finish()
        actionMode = null
        if (actionModeView?.isAttachedToWindow == true) {
            (actionModeView?.parent as? ViewGroup)?.removeView(actionModeView)
            actionModeView = null
        }
    }

    fun updateSelectedText(text: String) {
        _selectedText.value = text
    }

    fun updateClickedPoint(point: Point) {
        _clickedPoint.value = point
    }

    fun hide() {
        actionModeView?.visibility = INVISIBLE
    }

    fun show() {
        actionModeView?.visibility = VISIBLE
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

        if (configManager.papagoApiSecret.isNotEmpty()) {
            menuInfos.add(
                0,
                MenuInfo(
                    context.getString(R.string.papago),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_papago),
                    action = { _actionModeMenuState.value = ActionModeMenuState.Papago }
                )
            )
            menuInfos.add(
                0,
                MenuInfo(
                    context.getString(R.string.naver_translate),
                    icon = ContextCompat.getDrawable(context, R.drawable.icon_search),
                    action = { _actionModeMenuState.value = ActionModeMenuState.Naver }
                )
            )

        }
        menuInfos.add(
            0,
            MenuInfo(
                context.getString(R.string.google_translate),
                icon = ContextCompat.getDrawable(context, R.drawable.ic_translate),
                action = { _actionModeMenuState.value = ActionModeMenuState.GoogleTranslate }
            )
        )
        if (configManager.gptApiKey.isNotEmpty() && configManager.gptActionList.isNotEmpty()) {
            configManager.gptActionList.forEach { actionInfo ->
                menuInfos.add(
                    0,
                    MenuInfo(
                        actionInfo.name,
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_chat_gpt),
                        action = {
                            _actionModeMenuState.value =
                                ActionModeMenuState.Gpt(actionInfo)
                        }
                    )
                )
            }
        }

        menuInfos.add(
            0,
            MenuInfo(
                context.getString(android.R.string.copy),
                icon = ContextCompat.getDrawable(context, R.drawable.ic_copy),
                action = { ShareUtil.copyToClipboard(context, selectedText.value) }
            )
        )
        if (configManager.splitSearchItemInfoList.isNotEmpty()) {
            configManager.splitSearchItemInfoList.forEach { itemInfo ->
                menuInfos.add(
                    MenuInfo(
                        itemInfo.title,
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_split_screen),
                        action = {
                            _actionModeMenuState.value =
                                ActionModeMenuState.SplitSearch(itemInfo.stringPattern)
                        }
                    )
                )
            }
        }
        menuInfos.add(
            MenuInfo(
                context.getString(R.string.re_select),
                icon = ContextCompat.getDrawable(context, R.drawable.ic_reselect),
                closeMenu = false,
                action = { actionModeView?.visibility = INVISIBLE }
            )
        )

        return menuInfos
    }
}

sealed class ActionModeMenuState {
    object Idle : ActionModeMenuState()
    class Gpt(val gptAction: ChatGPTActionInfo) : ActionModeMenuState()
    object GoogleTranslate : ActionModeMenuState()
    object Papago : ActionModeMenuState()
    object Naver : ActionModeMenuState()
    class SplitSearch(val stringFormat: String) : ActionModeMenuState()
}