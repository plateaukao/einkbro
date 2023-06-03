package info.plateaukao.einkbro.view.dialog.compose

import android.content.Intent
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.data.MenuInfo
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import kotlinx.coroutines.launch

class ActionModeDialogFragment(
    private val actionModeMenuViewModel: ActionModeMenuViewModel,
    private val menuInfos: List<MenuInfo>,
    private val onDismiss: () -> Unit = {},
) : ComposeDialogFragment() {

//    init {
//        shouldShowInCenter = true
//    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun setupComposeView() = composeView.setContent {
        val text by actionModeMenuViewModel.selectedText.collectAsState()
        MyTheme {
            ActionModeMenu(menuInfos) { intent ->
                if (intent != null) {
                    startActivity(intent.apply {
                        putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    })
                }

                actionModeMenuViewModel.finishActionMode()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        lifecycleScope.launch {
            actionModeMenuViewModel.clickedPoint.collect { anchorPoint ->
                updateDialogPosition(anchorPoint)
            }
        }
        lifecycleScope.launch {
            actionModeMenuViewModel.showMenu.collect { show ->
                if (show) {
                    dialog?.show()
                } else {
                    dialog?.hide()
                }
            }
        }

        dialog?.setOnDismissListener { onDismiss() }

        return view
    }

    private fun updateDialogPosition(position: Point) {
        val window = dialog?.window ?: return
        window.setGravity(Gravity.TOP or Gravity.LEFT)

        if (position.isValid()) {
            val params = window.attributes.apply {
                x = position.x
                y = position.y
            }
            window.attributes = params
        }
        dialog?.hide()
    }

    private fun Point.isValid() = x != 0 && y != 0
}

@Composable
private fun ActionModeMenu(
    menuInfos: List<MenuInfo>,
    onClicked: (Intent?) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .wrapContentHeight()
            .width(280.dp)
    ) {
        items(menuInfos.size) { index ->
            val info = menuInfos[index]
            ActionMenuItem(info.title, info.icon) {
                info.action?.invoke()
                if (info.intent != null || info.closeMenu) onClicked(info.intent)
            }
        }
    }
}

@Composable
fun ActionMenuItem(
    title: String,
    iconDrawable: Drawable?,
    onClicked: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 0.5.dp else (-1).dp

    val configuration = LocalConfiguration.current
    val width = when {
        configuration.screenWidthDp > 500 -> 55.dp
        else -> 45.dp
    }

    val fontSize = if (configuration.screenWidthDp > 500) 10.sp else 8.sp
    Column(
        modifier = Modifier
            .width(width)
            .wrapContentHeight()
            .padding(8.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { onClicked() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = iconDrawable),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .padding(horizontal = 6.dp),
        )
        Text(
            modifier = Modifier
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
