package info.plateaukao.einkbro.view.dialog.compose

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.activity.MenuInfo
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import kotlinx.coroutines.launch

class ActionModeDialogFragment(
    private val actionModeMenuViewModel: ActionModeMenuViewModel,
    private val menuInfos: List<MenuInfo>,
    private val onDismiss: () -> Unit = {},
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        val text by actionModeMenuViewModel.selectedText.collectAsState()
        MyTheme {
            ActionModeMenu(text, menuInfos) { intent ->
                if (intent != null) {
                    startActivity(intent.apply {
                        putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    })
                }

                actionModeMenuViewModel.finishActionMode()
                this@ActionModeDialogFragment.dismiss()
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
    }

    private fun Point.isValid() = x != 0 && y != 0
}

@Composable
private fun ActionModeMenu(
    text: String,
    menuInfos: List<MenuInfo>,
    onClicked: (Intent?) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .wrapContentWidth()
    ) {
        items(menuInfos.size) { index ->
            val info = menuInfos[index]
            ActionModeMenuItem(info.title) {
                info.action?.invoke()
                onClicked(info.intent)
            }
        }
    }
}

@Composable
fun ActionModeMenuItem(
    title: String,
    onClicked: () -> Unit = {},
) = Text(
    text = title,
    Modifier
        .height(80.dp)
        .width(80.dp)
        .clickable { onClicked() },
)
