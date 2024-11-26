@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package info.plateaukao.einkbro.view.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.AttributeSet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.view.Album
import info.plateaukao.einkbro.view.dialog.compose.ActionIcon
import info.plateaukao.einkbro.view.dialog.compose.HorizontalSeparator
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.max

class HistoryAndTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    var albumList = mutableStateOf(listOf<Album>())
    var albumFocusIndex = mutableStateOf(0)
    var isHistoryOpen by mutableStateOf(false)
    var shouldReverse by mutableStateOf(true)
    var shouldShowTwoColumns by mutableStateOf(false)

    var onTabIconClick by mutableStateOf({})
    var onTabClick by mutableStateOf<(Album) -> Unit>({})
    var onTabLongClick by mutableStateOf<(Album) -> Unit>({})

    var recordList: List<Record> by mutableStateOf(emptyList())
    var onHistoryIconClick by mutableStateOf({})
    var onHistoryItemClick by mutableStateOf<(Record) -> Unit>({})
    var onHistoryItemLongClick by mutableStateOf<(Record, Point) -> Unit>({ _, _ -> })

    var addIncognitoTab by mutableStateOf({})
    var addTab by mutableStateOf({})
    var closePanel by mutableStateOf({})
    var onDeleteAllHistoryAction by mutableStateOf({})
    var onCloseAllTabs by mutableStateOf({})
    var launchNewBrowserAction by mutableStateOf({})

    @Composable
    override fun Content() {
        MyTheme {
            HistoryAndTabs(
                bookmarkManager = bookmarkManager,
                isHistoryOpen = isHistoryOpen,
                shouldReverseHistory = shouldReverse,
                shouldShowTwoColumns = shouldShowTwoColumns,
                albumList = albumList,
                albumFocusIndex = albumFocusIndex,
                onTabIconClick = onTabIconClick,
                onTabClick = onTabClick,
                onTabLongClick = onTabLongClick,

                records = recordList,
                onHistoryIconClick = onHistoryIconClick,
                onHistoryItemClick = onHistoryItemClick,
                onHistoryItemLongClick = onHistoryItemLongClick,
                addIncognitoTab = addIncognitoTab,
                addTab = addTab,
                closePanel = closePanel,
                onDeleteAction = onDeleteAllHistoryAction,
                onCloseAllTabs = onCloseAllTabs,
                launchNewBrowserAction = launchNewBrowserAction,
            )
        }
    }
}

@Composable
fun HistoryAndTabs(
    bookmarkManager: BookmarkManager? = null,
    isHistoryOpen: Boolean = false,
    shouldShowTwoColumns: Boolean = false,
    shouldReverseHistory: Boolean = false,

    albumList: MutableState<List<Album>>,
    albumFocusIndex: MutableState<Int>,
    onTabIconClick: () -> Unit,
    onTabClick: (Album) -> Unit,
    onTabLongClick: (Album) -> Unit,

    records: List<Record>,
    onHistoryIconClick: () -> Unit,
    onHistoryItemClick: (Record) -> Unit,
    onHistoryItemLongClick: (Record, Point) -> Unit,

    addIncognitoTab: () -> Unit,
    addTab: () -> Unit,
    closePanel: () -> Unit,
    onDeleteAction: () -> Unit,
    onCloseAllTabs: () -> Unit,
    launchNewBrowserAction: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isBarOnTop = !shouldReverseHistory
    Column(
        Modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { closePanel() },
        verticalArrangement = if (isBarOnTop) Arrangement.Top else Arrangement.Bottom,
    ) {
        if (isBarOnTop) {
            ButtonBarLayout(
                isHistoryOpen = isHistoryOpen,
                addIncognitoTab = addIncognitoTab,
                addTab = addTab,
                closePanel = closePanel,
                toggleHistory = { onHistoryIconClick() },
                togglePreview = { onTabIconClick() },
                onDeleteAction = onDeleteAction,
                onCloseAllTabs = onCloseAllTabs,
                launchNewBrowserAction = launchNewBrowserAction,
            )
            HorizontalSeparator()
        }
        MainContent(
            modifier = Modifier.Companion
                .weight(1f, false)
                .background(MaterialTheme.colors.background)
                .horizontalBorder(
                    drawTop = !isBarOnTop,
                    drawBottom = isBarOnTop,
                    MaterialTheme.colors.onBackground
                ),
            isHistoryOpen,
            shouldShowTwoColumns,
            shouldReverseHistory,
            albumList,
            albumFocusIndex,
            onTabClick,
            onTabLongClick,
            bookmarkManager,
            records,
            onHistoryItemClick,
            onHistoryItemLongClick
        )
        if (!isBarOnTop) {
            HorizontalSeparator()
            ButtonBarLayout(
                isHistoryOpen = isHistoryOpen,
                addIncognitoTab = addIncognitoTab,
                addTab = addTab,
                closePanel = closePanel,
                toggleHistory = { onHistoryIconClick() },
                togglePreview = { onTabIconClick() },
                onDeleteAction = onDeleteAction,
                onCloseAllTabs = onCloseAllTabs,
                launchNewBrowserAction = launchNewBrowserAction,
            )
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    isHistoryOpen: Boolean,
    shouldShowTwoColumns: Boolean,
    shouldReverse: Boolean,
    albumList: MutableState<List<Album>>,
    albumFocusIndex: MutableState<Int>,
    onTabClick: (Album) -> Unit,
    onTabLongClick: (Album) -> Unit,
    bookmarkManager: BookmarkManager?,
    records: List<Record>,
    onHistoryItemClick: (Record) -> Unit,
    onHistoryItemLongClick: (Record, Point) -> Unit,
) {
    if (!isHistoryOpen) {
        PreviewTabs(
            modifier = modifier,
            shouldShowTwoColumns = shouldShowTwoColumns,
            albumList = albumList.value,
            albumFocusIndex = albumFocusIndex,
            onClick = onTabClick,
            closeAction = {
                onTabLongClick.invoke(it)
            }
        )
    }
    if (isHistoryOpen) {
        BrowseHistoryList(
            modifier = modifier,
            bookmarkManager = bookmarkManager,
            records = records,
            shouldReverse = shouldReverse,
            shouldShowTwoColumns = shouldShowTwoColumns,
            onClick = onHistoryItemClick,
            onLongClick = onHistoryItemLongClick,
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewTabs(
    modifier: Modifier = Modifier,
    shouldShowTwoColumns: Boolean = false,
    albumList: List<Album>,
    albumFocusIndex: MutableState<Int>,
    onClick: (Album) -> Unit,
    closeAction: (Album) -> Unit,
    showHorizontal: Boolean = false,
) {
    if (showHorizontal) {
        val maxItemWidth = 200
        val barWidth = LocalConfiguration.current.screenWidthDp - 50 // 50 is the plus button width
        val itemWidth =
            if (albumList.size * 200 > barWidth) max(
                barWidth / albumList.size,
                80
            )
            else maxItemWidth

        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        LazyRow(modifier = modifier, state = listState) {
            items(albumList.size) { index ->
                val album = albumList[index]
                val interactionSource = remember { MutableInteractionSource() }
                TabItem(
                    modifier = Modifier
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onClick(album) },
                            onLongClick = { closeAction(album) }
                        )
                        .width(itemWidth.dp),
                    showCloseButton = false,
                    album = album
                ) { closeAction(album) }
            }
        }
        LaunchedEffect(albumFocusIndex.value) {
            if (
                !listState.layoutInfo.visibleItemsInfo.map { it.index }
                    .contains(albumFocusIndex.value)
            ) {
                coroutineScope.launch { listState.scrollToItem(albumFocusIndex.value) }
            }
        }
    } else {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Fixed(if (shouldShowTwoColumns) 2 else 1),
        ) {
            items(albumList.size) { index ->
                val album = albumList[index]
                val interactionSource = remember { MutableInteractionSource() }
                TabItem(
                    modifier = Modifier
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onClick(album) },
                            onLongClick = {
                                closeAction(album)
                            }
                        ),
                    album = album
                ) {
                    closeAction(album)
                }
            }
        }
    }
}

@Composable
private fun scrollToFocusedItem(
    listState: LazyListState,
    albumFocusIndex: Int,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(true) {
        scope.launch {
            listState.scrollToItem(albumFocusIndex)
        }
    }
}

@Composable
private fun TabItem(
    modifier: Modifier,
    album: Album,
    showCloseButton: Boolean = true,
    closeAction: () -> Unit,
) {
    val tabInfo = album.toTabInfo()
    val borderWidth = if (album.isActivated) 1.dp else -1.dp

    Row(
        modifier = modifier
            .height(54.dp)
            .padding(4.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        if (tabInfo.isTranslatePage) {
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 5.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_translate),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground
            )
        } else if (tabInfo.favicon != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(36.dp)
                    .padding(start = 2.dp, end = 5.dp),
                bitmap = tabInfo.favicon.asImageBitmap(),
                contentDescription = null,
            )
        } else {
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 5.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_earth),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground
            )
        }

        Text(
            modifier = Modifier
                .weight(1F)
                .align(Alignment.CenterVertically),
            text = tabInfo.title,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = MaterialTheme.colors.onBackground,
        )

        if (showCloseButton || album.isActivated) {
            ActionIcon(
                modifier = Modifier.align(Alignment.CenterVertically),
                iconResId = R.drawable.icon_close,
                action = closeAction,
            )
        }
    }
}

data class TabInfo(
    val url: String,
    val title: String,
    val favicon: Bitmap? = null,
    val isTranslatePage: Boolean,
)

@Composable
fun ButtonBarLayout(
    isHistoryOpen: Boolean = true,
    addIncognitoTab: () -> Unit,
    toggleHistory: () -> Unit,
    togglePreview: () -> Unit,
    addTab: () -> Unit,
    closePanel: () -> Unit,
    onDeleteAction: () -> Unit,
    onCloseAllTabs: () -> Unit,
    launchNewBrowserAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colors.background)
            .horizontalScroll(
                rememberScrollState(),
                reverseScrolling = true
            ) // default on right side
            .clickable(enabled = false) {}, // these two lines prevent row having click action
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        val historyResId =
            if (isHistoryOpen) R.drawable.ic_history_activated else R.drawable.ic_history
        val tabResId =
            if (!isHistoryOpen) R.drawable.ic_tab_plus_activated else R.drawable.icon_tab_plus

        if (isHistoryOpen) {
            ButtonIcon(iconResId = R.drawable.icon_delete, onClick = onDeleteAction)
        } else {
            ButtonIcon(iconResId = R.drawable.ic_remove_all_tabs, onClick = onCloseAllTabs)
        }
        ButtonIcon(iconResId = R.drawable.ic_incognito, onClick = addIncognitoTab)
        ButtonIcon(iconResId = historyResId, onClick = toggleHistory)
        ButtonIcon(iconResId = tabResId, onClick = togglePreview)
        ButtonIcon(
            iconResId = R.drawable.icon_plus,
            onClick = addTab,
            onLongClick = launchNewBrowserAction
        )
        ButtonIcon(iconResId = R.drawable.icon_arrow_down_gest, onClick = closePanel)

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ButtonIcon(
    iconResId: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
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
        imageVector = ImageVector.vectorResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground
    )
}

@SuppressLint("UnrememberedMutableState")
@Preview
@Composable
fun PreviewHistoryAndTabs() {
    val recordList = listOf(
        Record(
            title = "Hello aaa aaa aaa aa aa aaa aa a aa a a a aa a a a a a a a a a aa a a ",
            url = "123",
            time = System.currentTimeMillis()
        ),
        Record(title = "Hello 2", url = "123", time = System.currentTimeMillis()),
        Record(title = "Hello 3", url = "123", time = System.currentTimeMillis()),
    )

    val albumList = mutableStateOf(listOf<Album>())

    HistoryAndTabs(
        isHistoryOpen = true,
        shouldShowTwoColumns = false,
        shouldReverseHistory = false,
        albumList = albumList,
        albumFocusIndex = mutableStateOf(0),
        onTabIconClick = {},
        onTabClick = {},
        onTabLongClick = {},
        records = recordList,
        onHistoryIconClick = {},
        onHistoryItemClick = {},
        onHistoryItemLongClick = { _, _ -> },

        addIncognitoTab = {},
        addTab = {},
        closePanel = {},
        onDeleteAction = {},
        launchNewBrowserAction = {},
        onCloseAllTabs = {},
    )
}

private fun Album.toTabInfo(): TabInfo =
    TabInfo(
        title = this.albumTitle,
        url = this.getUrl(),
        favicon = this.bitmap,
        isTranslatePage = this.isTranslatePage
    )

