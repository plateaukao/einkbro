package info.plateaukao.einkbro.view.dialog.compose

import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent

class HighlightStyleDialogFragment(
    private val anchorPoint: Point? = null,
    val okAction: (HighlightStyle) -> Unit,
    val onDismissAction: () -> Unit = {},
) : DraggableComposeDialogFragment(), KoinComponent {
    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            HighlightStyleContent(
                config.highlightStyle
            ) { highlightStyle ->
                okAction(highlightStyle)
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismissAction()
        super.onDismiss(dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        anchorPoint?.let {
            setupDialogPosition(it)
        }
        return view
    }
}

@Composable
private fun HighlightStyleContent(
    style: HighlightStyle,
    onOk: (HighlightStyle) -> Unit,
) {
    Row {
        TextButton(
            modifier = Modifier
                .padding(6.dp)
                .size(40.dp)
                .border(
                    if (style == HighlightStyle.UNDERLINE) 1.dp else 0.dp,
                    MaterialTheme.colors.onBackground
                )
                .background(MaterialTheme.colors.background),
            onClick = { onOk(HighlightStyle.UNDERLINE) }
        ) {
            Text("__")
        }
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(40.dp)
                .border(
                    if (style == HighlightStyle.BACKGROUND_YELLOW) 2.dp else 0.dp,
                    MaterialTheme.colors.onBackground
                )
                .background(Color.Yellow)
                .clickable {
                    onOk(HighlightStyle.BACKGROUND_YELLOW)
                }
        )
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(40.dp)
                .border(
                    if (style == HighlightStyle.BACKGROUND_GREEN) 2.dp else 0.dp,
                    MaterialTheme.colors.onBackground
                )
                .background(Color.Green)
                .clickable {
                    onOk(HighlightStyle.BACKGROUND_GREEN)
                }
        )
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(40.dp)
                .border(
                    if (style == HighlightStyle.BACKGROUND_BLUE) 2.dp else 0.dp,
                    MaterialTheme.colors.onBackground
                )
                .background(Color.Blue)
                .clickable {
                    onOk(HighlightStyle.BACKGROUND_BLUE)
                }
        )
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(40.dp)
                .border(
                    if (style == HighlightStyle.BACKGROUND_RED) 2.dp else 0.dp,
                    MaterialTheme.colors.onBackground
                )
                .background(Color.Red)
                .clickable {
                    onOk(HighlightStyle.BACKGROUND_RED)
                }
        )
    }
}

@Preview
@Composable
fun PreviewHighlightStyleContent() {
    MyTheme {
        HighlightStyleContent(HighlightStyle.BACKGROUND_GREEN) {}
    }
}
