package info.plateaukao.einkbro.view.dialog.compose

import info.plateaukao.einkbro.activity.GptActionDialog
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.view.compose.MyTheme

class ShowEditGptActionDialogFragment(
    private val editActionIndex:Int = -1,
): ComposeDialogFragment() {
    override fun setupComposeView() {
        var actionList = config.gptActionList
        composeView.setContent {
            MyTheme {
                GptActionDialog(
                    true,
                    editActionIndex,
                    if (editActionIndex >= 0) actionList[editActionIndex] else null,
                    okAction = { name, systemMessage, userMessage, type ->
                        actionList = actionList.toMutableList().apply {
                            if (editActionIndex >= 0)
                                set(
                                    editActionIndex,
                                    ChatGPTActionInfo(name, systemMessage, userMessage, type)
                                )
                            else
                                add(ChatGPTActionInfo(name, systemMessage, userMessage, type))
                        }
                        config.gptActionList = actionList
                        dismiss()
                    },
                    dismissAction = { dismiss() }
                )
            }
        }
    }
}