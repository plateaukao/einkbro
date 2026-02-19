package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.view.compose.MyTheme

class PageAiActionDialogFragment(
    private val actions: List<ChatGPTActionInfo>,
    private val onActionClicked: (ChatGPTActionInfo) -> Unit,
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.page_ai_action_title),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    actions.forEachIndexed { index, action ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onActionClicked(action)
                                    dismiss()
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = actionIconRes(action)),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onBackground
                                )
                                Column {
                                    Text(
                                        text = action.name,
                                        style = MaterialTheme.typography.subtitle1,
                                        color = MaterialTheme.colors.onBackground
                                    )
                                }
                            }
                            if (index < actions.lastIndex) {
                                HorizontalSeparator()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun actionIconRes(action: ChatGPTActionInfo): Int {
        val actionType = action.actionType.takeIf { it != GptActionType.Default }
            ?: GptActionType.OpenAi
        return when (actionType) {
            GptActionType.OpenAi -> R.drawable.ic_chat_gpt
            GptActionType.SelfHosted -> R.drawable.ic_ollama
            GptActionType.Gemini -> R.drawable.ic_gemini
            else -> R.drawable.ic_chat_gpt
        }
    }
}
