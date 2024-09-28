package info.plateaukao.einkbro.view.dialog.compose

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.data.MenuInfo
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel

class ActionModeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    private lateinit var actionModeMenuViewModel: ActionModeMenuViewModel
    private lateinit var clearSelectionAction: () -> Unit

    @Composable
    override fun Content() {
        val text by actionModeMenuViewModel.selectedText.collectAsState()
        MyTheme {
            ActionModeMenu(
                actionModeMenuViewModel.menuInfos,
                actionModeMenuViewModel.showIcons,
            ) { intent ->
                if (intent != null) {
                    context.startActivity(intent.apply {
                        putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    })
                }
                clearSelectionAction()
                actionModeMenuViewModel.updateActionMode(null)
            }
        }
    }

    fun init(
        actionModeMenuViewModel: ActionModeMenuViewModel,
        clearSelectionAction: () -> Unit,
    ) {
        this.actionModeMenuViewModel = actionModeMenuViewModel
        this.clearSelectionAction = clearSelectionAction
    }
}

@Composable
private fun ActionModeMenu(
    menus: MutableState<List<MenuInfo>>,
    showIcons: Boolean = true,
    onClicked: (Intent?) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .wrapContentHeight()
            .width(280.dp)
            .border(1.dp, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
    ) {
        val menuInfos = menus.value
        items(menuInfos.size) { index ->
            val info = menuInfos[index]
            ActionMenuItem(
                info.title,
                if (showIcons) info.drawable else null,
                onClicked = {
                    info.action?.invoke()
                    if (info.closeMenu) onClicked(info.intent)
                },
                onLongClicked = {
                    info.longClickAction?.invoke()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionMenuItem(
    title: String,
    iconDrawable: Drawable?,
    onClicked: () -> Unit = {},
    onLongClicked: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 0.5.dp else (-1).dp

    val configuration = LocalConfiguration.current
    val width = when {
        configuration.screenWidthDp > 500 -> 55.dp
        else -> 45.dp
    }

    val fontSize = if (iconDrawable == null) 12.sp else
        if (configuration.screenWidthDp > 500) 10.sp else 8.sp
    Column(
        modifier = Modifier
            .width(width)
            .wrapContentHeight()
            .padding(8.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .combinedClickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClicked,
                onLongClick = onLongClicked,
            ),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (iconDrawable != null) {
            Image(
                painter = rememberDrawablePainter(drawable = iconDrawable),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .padding(horizontal = 6.dp),
            )
        }
        if (title.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .padding(2.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = title,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = fontSize,
                fontSize = fontSize,
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}
