@file:OptIn(ExperimentalFoundationApi::class)

package info.plateaukao.einkbro.view.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.Album
import info.plateaukao.einkbro.view.dialog.compose.HorizontalSeparator
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Bookmark
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.NewTab
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.PageInfo
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Spacer1
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Spacer2
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.TabCount
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Time
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Title
import info.plateaukao.einkbro.view.toolbaricons.ToolbarActionInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val toolbarIconWidth = 46.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposedToolbar(
    showTabs: Boolean,
    toolbarActionInfos: List<ToolbarActionInfo>,
    title: String,
    tabCount: String,
    pageInfo: String,
    isIncognito: Boolean,
    onIconClick: (ToolbarAction) -> Unit,
    onIconLongClick: ((ToolbarAction) -> Unit)? = null,
    albumList: MutableState<List<Album>>,
    albumFocusIndex: MutableState<Int>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
) {
    val height = if (showTabs) 100.dp else 50.dp
    Column(
        modifier = Modifier
            .height(height)
            .background(MaterialTheme.colors.background)
            .semantics { testTagsAsResourceId = true },
        horizontalAlignment = Alignment.End
    ) {
        if (showTabs) {
            Row(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth()
            ) {
                PreviewTabs(
                    Modifier.weight(1f),
                    albumList = albumList.value,
                    albumFocusIndex = albumFocusIndex,
                    onClick = onAlbumClick,
                    closeAction = onAlbumLongClick,
                    showHorizontal = true
                )
                ButtonIcon(
                    iconResId = R.drawable.icon_plus,
                    onClick = { onIconClick(NewTab) },
                    onLongClick = { onIconLongClick?.invoke(NewTab) },
                )
            }
            HorizontalSeparator()
        }
        ComposedIconBar(
            toolbarActionInfos = toolbarActionInfos,
            title = title,
            tabCount = tabCount,
            pageInfo = pageInfo,
            isIncognito = isIncognito,
            onClick = onIconClick,
            onLongClick = onIconLongClick,
        )
    }
}

@Composable
fun ComposedIconBar(
    toolbarActionInfos: List<ToolbarActionInfo>,
    title: String,
    tabCount: String,
    pageInfo: String,
    isIncognito: Boolean,
    onClick: (ToolbarAction) -> Unit,
    onLongClick: ((ToolbarAction) -> Unit)? = null,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val shouldTitleWidthFixed = toolbarActionInfos.map { it.toolbarAction }.contains(Title) &&
            (toolbarActionInfos.filter { it.toolbarAction != Title }.size + 1) * 46 > screenWidth
    Row(
        modifier = Modifier
            .height(50.dp)
            .background(MaterialTheme.colors.background)
            .conditional(shouldTitleWidthFixed) {
                horizontalScroll(
                    rememberScrollState(),
                    reverseScrolling = true
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        toolbarActionInfos.forEach { toolbarActionInfo ->
            when (val toolbarAction = toolbarActionInfo.toolbarAction) {
                Title -> {
                    val titleModifier = Modifier
                        .padding(start = 2.dp, top = 6.dp, bottom = 6.dp)
                        .fillMaxHeight()
                        .border(
                            0.5.dp,
                            MaterialTheme.colors.onBackground,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(start = 3.dp, end = 1.dp)
                        .then(
                            if (shouldTitleWidthFixed) Modifier.widthIn(max = 300.dp) else Modifier.weight(
                                1F
                            )
                        )
                        .clickable { onClick(toolbarAction) }
                    Row(
                        modifier = titleModifier,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            color = MaterialTheme.colors.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // show a current time (hour:minute) in the toolbar
                Time -> CurrentTimeText()

                TabCount -> TabCountIcon(isIncognito, tabCount, onClick, onLongClick)

                PageInfo -> PageInfoIcon(pageInfo, onClick, onLongClick)

                Spacer1, Spacer2 -> Spacer(modifier = Modifier.weight(1F))

                else -> ToolbarIcon(
                    toolbarAction,
                    toolbarActionInfo.getCurrentResId(),
                    onClick,
                    onLongClick
                )
            }
        }
    }
}

// pageInfo
@Composable
fun PageInfoIcon(
    pageInfo: String,
    onClick: (ToolbarAction) -> Unit,
    onLongClick: ((ToolbarAction) -> Unit)? = null,
) {
    Text(
        text = pageInfo,
        color = MaterialTheme.colors.onBackground,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(2.dp)
            .defaultMinSize(minWidth = 46.dp)
            .wrapContentWidth()
            .combinedClickable(
                onClick = { onClick(PageInfo) },
                onLongClick = { onLongClick?.invoke(PageInfo) }
            )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarIcon(
    toolbarAction: ToolbarAction,
    iconResId: Int,
    onClick: (ToolbarAction) -> Unit,
    onLongClick: ((ToolbarAction) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 0.5.dp else (-1).dp

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
            .padding(6.dp)
            .testTag(toolbarAction.name.lowercase()),
        painter = painterResource(id = iconResId),
        contentDescription = stringResource(id = toolbarAction.titleResId),
        tint = MaterialTheme.colors.onBackground
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabCountIcon(
    isIncognito: Boolean,
    count: String,
    onClick: (ToolbarAction) -> Unit,
    onLongClick: ((ToolbarAction) -> Unit)? = null,
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
                onClick = { onClick(TabCount) },
                onLongClick = { onLongClick?.invoke(TabCount) }
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

@Composable
fun CurrentTimeText(
    modifier: Modifier = Modifier,
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000L * 60) // Update every second
        }
    }

    Text(
        text = currentTime,
        color = MaterialTheme.colors.onBackground,
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 6.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

private inline fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier,
): Modifier {
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
        ComposedIconBar(
            toolbarActionInfos = ToolbarAction.values().map { ToolbarActionInfo(it, false) },
            "hihi",
            tabCount = "1",
            pageInfo = "1/1",
            isIncognito = true,
            { },
        )
    }
}

@Preview
@Composable
fun PreviewToolbarLongTitle() {
    MyTheme {
        ComposedIconBar(
            toolbarActionInfos = listOf(
                ToolbarActionInfo(Bookmark, false),
                ToolbarActionInfo(Spacer1, false),
                ToolbarActionInfo(TabCount, false),
                ToolbarActionInfo(ToolbarAction.InputUrl, false),
                ToolbarActionInfo(ToolbarAction.IconSetting, false),
                ToolbarActionInfo(PageInfo, false),
                ToolbarActionInfo(Spacer2, false),
                ToolbarActionInfo(Time, false),
            ),
            "hi 1 2 3 456789",
            tabCount = "1",
            pageInfo = "1/1",
            isIncognito = true,
            { },
        )
    }
}
