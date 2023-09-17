package info.plateaukao.einkbro.view.dialog.compose

import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.GptViewModel


class GPTDialogFragment(
    private val gptActionInfo: ChatGPTActionInfo,
    private val gptViewModel: GptViewModel,
    private val anchorPoint: Point,
    private val hasBackgroundColor: Boolean = false,
    private val onTranslateClick: () -> Unit = {},
    private val onDismissed: () -> Unit = {}
) : DraggableComposeDialogFragment() {

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            GptResponse(gptViewModel, hasBackgroundColor, onTranslateClick) {
                dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setupDialogPosition(anchorPoint)

        gptViewModel.gptActionInfo = gptActionInfo
        gptViewModel.query()

        if (hasBackgroundColor) {
            dialog?.window?.setBackgroundDrawableResource(R.drawable.white_bgd_with_border_margin)
        }

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        onDismissed()
    }
}

@Composable
private fun GptResponse(
    gptViewModel: GptViewModel,
    hasBackgroundColor: Boolean,
    onTranslateClick: () -> Unit = {},
    closeClick: () -> Unit = {}
) {
    val showControls by gptViewModel.showControls.collectAsState()
    val requestMessage by gptViewModel.inputMessage.collectAsState()
    val responseMessage by gptViewModel.responseMessage.collectAsState()
    val showRequest = remember { mutableStateOf(false) }

    val clipboardManager =
        LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Box {
        Column(
            modifier = Modifier
                .defaultMinSize(minWidth = 200.dp)
                .wrapContentHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showRequest.value) {
                Text(
                    text = requestMessage,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(10.dp),
                )
                Divider()
            }
            Text(
                text = responseMessage,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .padding(
                        top = if (!showRequest.value) 5.dp else 10.dp,
                        bottom = 10.dp,
                        start = 10.dp,
                        end = 10.dp
                    )
                    .align(Alignment.Start)
            )
            if (showControls) {
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_refresh),
                        contentDescription = "Retry Icon",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(5.dp)
                            .clickable {
                                gptViewModel.updateInputMessage(gptViewModel.inputMessage.value)
                                gptViewModel.query()
                            }
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = "Copy text",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(5.dp)
                            .clickable { clipboardManager.text = responseMessage }
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_translate),
                        contentDescription = "Translate Icon",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(5.dp)
                            .clickable { onTranslateClick(); closeClick() }
                    )
                    Icon(
                        painter = painterResource(
                            id = if (showRequest.value) R.drawable.icon_arrow_up_gest else R.drawable.icon_info
                        ),
                        contentDescription = "Info Icon",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(5.dp)
                            .clickable { showRequest.value = !showRequest.value }
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.icon_close),
                        contentDescription = "Close Icon",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(5.dp)
                            .clickable { closeClick() }
                    )
                }
            }
        }
    }
}

