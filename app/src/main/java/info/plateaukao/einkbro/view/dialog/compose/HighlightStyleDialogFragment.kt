package info.plateaukao.einkbro.view.dialog.compose

import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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
    val scrollState = rememberScrollState()
    Row(modifier = Modifier.horizontalScroll(scrollState)) {
        HighlightStyle.values().map { highlightStyle ->
            IconButton(
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .border(
                        1.dp,
                        if (style == highlightStyle) MaterialTheme.colors.onBackground else Color.Transparent
                    )
                    .background(MaterialTheme.colors.background),
                onClick = { onOk(highlightStyle) }
            ) {
                Icon(
                    modifier = Modifier.size(30.dp),
                    imageVector = ImageVector.vectorResource(id = highlightStyle.iconResId),
                    contentDescription = null,
                    tint = highlightStyle.color ?: MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewHighlightStyleContent() {
    MyTheme {
        HighlightStyleContent(HighlightStyle.BACKGROUND_GREEN) {}
    }
}
