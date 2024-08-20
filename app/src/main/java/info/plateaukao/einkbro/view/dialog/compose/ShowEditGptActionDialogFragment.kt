package info.plateaukao.einkbro.view.dialog.compose

import info.plateaukao.einkbro.activity.GptActionDialog
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.view.compose.MyTheme

class ShowEditGptActionDialogFragment(
    private val editActionIndex: Int = -1,
) : ComposeDialogFragment() {
    override fun setupComposeView() {
        var actionList = config.gptActionList
        composeView.setContent {
            MyTheme {
                GptActionDialog(
                    editActionIndex,
                    if (editActionIndex >= 0) actionList[editActionIndex] else createDefaultGptAction(),
                    config.getGptTypeModelMap(),
                    okAction = { modifiedAction ->
                        actionList = actionList.toMutableList().apply {
                            if (editActionIndex >= 0) set(editActionIndex, modifiedAction)
                            else add(modifiedAction)
                        }
                        config.gptActionList = actionList
                        dismiss()
                    },
                    dismissAction = { dismiss() }
                )
            }
        }
    }

    private fun createDefaultGptAction(): ChatGPTActionInfo {
        return ChatGPTActionInfo(
            "",
            "",
            "",
            GptActionType.Default,
            config.getDefaultActionModel()
        )
    }
}