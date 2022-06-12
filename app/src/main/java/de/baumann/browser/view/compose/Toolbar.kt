@file:OptIn(ExperimentalFoundationApi::class)

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
import de.baumann.browser.view.toolbaricons.ToolbarActionInfo


@Composable
fun ComposedToolbar(
    toolbarActionInfos: List<ToolbarActionInfo>,
    title: String,
    tabCount: String,
    isIncognito: Boolean,
    isReader: Boolean,
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
        toolbarActionInfos.forEach { toolbarActionInfo ->
            when(val toolbarAction = toolbarActionInfo.toolbarAction) {
                Title ->
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .weight(1F)
                            .clickable { onClick(toolbarAction) },
                        text = title
                    )
                TabCount ->
                    if (!isReader) TabCountIcon(
                        isIncognito = isIncognito,
                        count = tabCount,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    ) else ToolbarIcon(toolbarActionInfo, onClick, onLongClick)
                else -> ToolbarIcon(toolbarActionInfo, onClick, onLongClick)
            }
        }
    }
}

@Composable
fun ToolbarIcon(
    toolbarActionInfo: ToolbarActionInfo,
    onClick: (ToolbarAction)->Unit,
    onLongClick:((ToolbarAction)->Unit)? = null,
) {
    val toolbarAction = toolbarActionInfo.toolbarAction
    Icon(
        modifier = Modifier
            .fillMaxHeight()
            .width(46.dp)
            .combinedClickable(
                onClick = { onClick(toolbarAction) },
                onLongClick = { onLongClick?.invoke(toolbarAction) }
            )
            .padding(12.dp),
        painter = painterResource(id = toolbarActionInfo.getCurrentResId()),
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
                .height(28.dp)
                .width(28.dp)
                .border(
                    width = thickness,
                    color = MaterialTheme.colors.onBackground,
                    RoundedCornerShape(7.dp)
                )
                .padding(top = 2.dp),
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
            toolbarActionInfos = ToolbarAction.values().map {ToolbarActionInfo(it, false)},
            "hihi",
            tabCount = "1",
            isIncognito = true,
            isReader = true,
            { },
        )
    }
}