@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package de.baumann.browser.view.compose

import android.graphics.Bitmap
import androidx.appcompat.widget.ButtonBarLayout
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.Record
import de.baumann.browser.view.dialog.compose.ActionIcon

@Composable
fun HistoryAndTabs(
    isPreviewOpen: Boolean = false,
    isHistoryOpen: Boolean = false,
    shouldShowTwoColumns: Boolean = false,
    shouldReverse: Boolean = false,

    tabInfoList: List<TabInfo>,
    onTabClick: (TabInfo) -> Unit,
    onTabLongClick: (TabInfo) -> Unit,

    records: List<Record>,
    onHistoryClick: (Record) -> Unit,
    onHistoryLongClick: (Record) -> Unit,

    addIncognitoTab: () -> Unit,
    addTab: () -> Unit,
    closePanel: () -> Unit,
) {
    var isHistoryOpen = remember { mutableStateOf(isHistoryOpen) }
    var isPreviewOpen = remember { mutableStateOf(isPreviewOpen) }

    Column {
        if (isPreviewOpen.value) {
            PreviewTabs(
                shouldShowTwoColumns = shouldShowTwoColumns,
                shouldReverse = shouldReverse,
                tabInfoList = tabInfoList,
                onClick = onTabClick,
                onLongClick = onTabLongClick,
            )
        }
        if (isHistoryOpen.value) {
            BrowseHistoryList(
                records = records,
                shouldReverse = shouldReverse,
                shouleShowTwoColumns = shouldShowTwoColumns,
                onClick = onHistoryClick,
                onLongClick = onHistoryLongClick,
            )
        }

        ButtonBarLayout(
            addIncognitoTab = addIncognitoTab,
            addTab = addTab,
            closePanel = closePanel,
            toggleHistory = { isHistoryOpen.value = true ; isPreviewOpen.value = false },
            togglePreview = { isPreviewOpen.value = true ; isHistoryOpen.value = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewTabs(
    shouldShowTwoColumns: Boolean = false,
    shouldReverse: Boolean = false,
    tabInfoList: List<TabInfo>,
    onClick: (TabInfo) -> Unit,
    onLongClick: (TabInfo) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (shouldShowTwoColumns) 2 else 1),
        reverseLayout = shouldReverse
    ){
        itemsIndexed(tabInfoList) { index, tabInfo ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            TabItem(
                modifier = Modifier.combinedClickable (
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onClick(tabInfo) },
                    onLongClick = { onLongClick(tabInfo) }
                ),
                isPressed = isPressed,
                tabInfo = tabInfo,
            )
        }
    }
}

@Composable
private fun TabItem(
    modifier: Modifier,
    isPressed: Boolean = false,
    tabInfo: TabInfo,
) {
    val borderWidth = if (isPressed || tabInfo.focused) 1.dp else -1.dp

    Row(
        modifier = modifier
            .height(54.dp)
            .padding(8.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp)),
        horizontalArrangement = Arrangement.Center
    ) {
        if (tabInfo.favicon != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(36.dp)
                    .padding(end = 5.dp),
                bitmap = tabInfo.favicon.asImageBitmap(),
                contentDescription = null,
            )
        } else {
            ActionIcon(
                modifier = Modifier.align(Alignment.CenterVertically),
                iconResId = R.drawable.icon_earth,
            )
        }

        Text(
            modifier = Modifier
                .weight(1F)
                .align(Alignment.CenterVertically),
            text = tabInfo.title,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

data class TabInfo(val focused: Boolean, val url: String, val title: String, val favicon: Bitmap? = null)

@Composable
fun ButtonBarLayout(
    addIncognitoTab: () -> Unit,
    toggleHistory: () -> Unit,
    togglePreview: () -> Unit,
    addTab: () -> Unit,
    closePanel: () -> Unit,
) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colors.background)
            .horizontalScroll(rememberScrollState(), reverseScrolling = true) // default on right side
            .clickable(enabled = false) {}, // these two lines prevent row having click action
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
        ){
        ButtonIcon(iconResId = R.drawable.ic_incognito, onClick = addIncognitoTab)
        ButtonIcon(iconResId = R.drawable.ic_history, onClick = toggleHistory)
        ButtonIcon(iconResId = R.drawable.icon_tab_plus, onClick = togglePreview )
        ButtonIcon(iconResId = R.drawable.icon_plus, onClick = addTab )
        ButtonIcon(iconResId = R.drawable.icon_arrow_down_gest, onClick = closePanel)

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ButtonIcon(
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
        tint = MaterialTheme.colors.onBackground
    )
}

@Preview
@Composable
fun PreviewHistoryAndTabs() {
    val recordList = listOf(
        Record(title = "Hello aaa aaa aaa aa aa aaa aa a aa a a a aa a a a a a a a a a aa a a ", url = "123", time = System.currentTimeMillis()),
        Record(title = "Hello 2", url = "123", time = System.currentTimeMillis()),
        Record(title = "Hello 3", url = "123", time = System.currentTimeMillis()),
    )

    HistoryAndTabs(
        isPreviewOpen = true,
        isHistoryOpen = false,
        shouldShowTwoColumns = false,
        shouldReverse = false,
        tabInfoList = listOf(
            TabInfo(
                focused = true,
                url = "https://www.google.com",
                title = "Google",
                favicon = null
            ),
            TabInfo(
                focused = false,
                url = "https://www.google.com",
                title = "Google",
                favicon = null
            ),
        ),
        onTabClick = {},
        onTabLongClick = {},
        records = recordList,
        onHistoryClick = {},
        onHistoryLongClick = {},

        addIncognitoTab = {},
        addTab = {},
        closePanel = {},
    )
}