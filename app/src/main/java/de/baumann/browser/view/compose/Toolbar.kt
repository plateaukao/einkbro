@file:OptIn(ExperimentalFoundationApi::class)

package de.baumann.browser.view.compose

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.baumann.browser.view.toolbaricons.ToolbarAction
import de.baumann.browser.view.toolbaricons.ToolbarAction.*
import de.baumann.browser.view.toolbaricons.ToolbarActionInfo


private val toolbarIconWidth = 46.dp

@Composable
fun ComposedToolbar(
    toolbarActionInfos: List<ToolbarActionInfo>,
    title: String,
    tabCount: String,
    isIncognito: Boolean,
    onClick: (ToolbarAction) -> Unit,
    onLongClick: ((ToolbarAction) -> Unit)? = null,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val shouldTitleWidthFixed =  toolbarActionInfos.size * 46 > screenWidth
    Row(
        modifier = Modifier
            .height(50.dp)
            .background(MaterialTheme.colors.background)
            .conditional(shouldTitleWidthFixed) { horizontalScroll(rememberScrollState(), reverseScrolling = true) }
            .clickable { onClick(Title) }, // these two lines prevent row having click action
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        toolbarActionInfos.forEach { toolbarActionInfo ->
            when (val toolbarAction = toolbarActionInfo.toolbarAction) {
                Title -> {
                    val titleModifier = Modifier
                        .padding(horizontal = 1.dp)
                        .then(if (shouldTitleWidthFixed) Modifier.widthIn(max = 300.dp) else Modifier.weight(1F))
                        .clickable { onClick(toolbarAction) }
                    Text(
                        modifier = titleModifier,
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                }
                TabCount ->
                    TabCountIcon(
                        isIncognito = isIncognito,
                        count = tabCount,
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction) }
                    )
                else -> ToolbarIcon(toolbarActionInfo, onClick, onLongClick)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarIcon(
    toolbarActionInfo: ToolbarActionInfo,
    onClick: (ToolbarAction) -> Unit,
    onLongClick: ((ToolbarAction) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 0.5.dp else -1.dp

    val toolbarAction = toolbarActionInfo.toolbarAction
    Icon(
        modifier = Modifier
            .fillMaxHeight()
            .width(toolbarIconWidth)
            .padding(6.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .combinedClickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = { onClick(toolbarAction) },
                onLongClick = { onLongClick?.invoke(toolbarAction) }
            )
            .padding(6.dp),
        painter = painterResource(id = toolbarActionInfo.getCurrentResId()),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabCountIcon(
    isIncognito: Boolean,
    count: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val border = if (isIncognito)
        Modifier.dashedBorder(1.dp, 7.dp, color = MaterialTheme.colors.onBackground)
    else
        Modifier.border(1.dp, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))

    Box(
        modifier = Modifier
            .height(toolbarIconWidth)
            .width(toolbarIconWidth)
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
                .then(border)
                .padding(top = 2.dp),
            text = count,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

private inline fun Modifier.conditional(condition : Boolean, modifier : Modifier.() -> Modifier) : Modifier {
    return if (condition) {
        modifier.invoke(this)
    } else {
        this
    }
}

@Preview
@Composable
fun PreviewTabCount() {
    MyTheme {
        TabCountIcon(false, "3", {}, {})
    }
}

@Preview
@Composable
fun PreviewTabCountIncognito() {
    MyTheme {
        TabCountIcon(true, "3", {}, {})
    }
}

@Preview
@Composable
fun PreviewToolbar() {
    MyTheme {
        ComposedToolbar(
            toolbarActionInfos = ToolbarAction.values().map { ToolbarActionInfo(it, false) },
            "hihi",
            tabCount = "1",
            isIncognito = true,
            { },
        )
    }
}

@Preview
@Composable
fun PreviewToolbarLongTitle() {
    MyTheme {
        ComposedToolbar(
            toolbarActionInfos = listOf(
                ToolbarActionInfo(Desktop, false),
                ToolbarActionInfo(TabCount, false),
                ToolbarActionInfo(Title, false),
                ToolbarActionInfo(Desktop, false),
                ToolbarActionInfo(Desktop, false),
            ),
            "hi hihi hihi hihi hihi hihi hihihihih ihih 1 2 3 456789",
            tabCount = "1",
            isIncognito = true,
            { },
        )
    }
}
