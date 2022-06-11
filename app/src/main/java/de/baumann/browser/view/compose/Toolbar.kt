@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package de.baumann.browser.view.compose

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.view.toolbaricons.ToolbarAction
import de.baumann.browser.view.toolbaricons.ToolbarAction.*
import de.baumann.browser.Ninja.R


@Composable
fun ComposedToolbar(
    toolbarActions: List<ToolbarAction>,
    title: String,
    tabCount: String,
    enableTouch: Boolean,
    isIncognito: Boolean,
    isDesktopMode: Boolean,
    isBoldFont: Boolean,
    isLoading: Boolean,
    onClick: (ToolbarAction)->Unit,
    onLongClick:((ToolbarAction)->Unit)? = null,
) {
    Row(
        modifier = Modifier
            .height(50.dp)
            .background(MaterialTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        toolbarActions.forEach { toolbarAction ->
            when(toolbarAction) {
                Title ->
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .weight(1F),
                        text = title
                    )
                BoldFont ->
                    ActivableIcon(
                        isActivated = isBoldFont,
                        onResId = R.drawable.ic_bold_font_active,
                        offResId = R.drawable.ic_bold_font,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    )
                Refresh ->
                    ActivableIcon(
                        isActivated = isLoading,
                        onResId = R.drawable.ic_stop,
                        offResId = toolbarAction.iconResId,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    )
                Desktop ->
                    ActivableIcon(
                        isActivated = isDesktopMode,
                        onResId = R.drawable.icon_desktop_activate,
                        offResId = toolbarAction.iconResId,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    )
                Touch ->
                    ActivableIcon(
                        isActivated = enableTouch,
                        onResId = R.drawable.ic_touch_enabled,
                        offResId = R.drawable.ic_touch_disabled,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    )
                TabCount ->
                    TabCountIcon(
                        isIncognito = isIncognito,
                        count = tabCount,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    )
                else ->
                    ToolbarIcon(
                        iconResId = toolbarAction.iconResId,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    )
            }
        }
    }
}

@Composable
fun ActivableIcon(
    isActivated: Boolean,
    onResId: Int,
    offResId: Int,
    onClick: ()->Unit,
    onLongClick:(()->Unit)? = null,
) {
    val resId = if (isActivated) onResId else offResId
    ToolbarIcon(iconResId = resId, onClick = onClick, onLongClick = onLongClick)
}

@Composable
fun ToolbarIcon(
    iconResId: Int,
    onClick: ()->Unit,
    onLongClick:(()->Unit)? = null,
) {
    Icon(
        modifier = Modifier
            .fillMaxHeight()
            .width(46.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        painter = painterResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colors.onSurface
    )
}

@Composable
private fun TabCountIcon(
    isIncognito: Boolean,
    count: String,
    onClick: ()->Unit,
    onLongClick:(()->Unit)? = null,
) {
    val thickness = if (isIncognito) 3.dp else 1.dp
    Box(
        modifier = Modifier.height(46.dp).width(46.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .height(30.dp)
                .width(30.dp)
                .border(
                    width = thickness,
                    color = MaterialTheme.colors.onBackground,
                    RoundedCornerShape(7.dp)
                )
                .padding(top = 3.dp),
            text = count,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@Preview
@Composable
fun previewTabCount() {
    AppCompatTheme {
        TabCountIcon(false, "3", {}, {})
    }
}

@Preview
@Composable
fun previewToolbar() {
    AppCompatTheme {
        ComposedToolbar(
            toolbarActions = values().toList(),
            "hihi",
            tabCount = "1",
            enableTouch =  true,
            isIncognito = true,
            isDesktopMode = true,
            isBoldFont = true,
            isLoading = false,
            {_-> },
        )
    }
}