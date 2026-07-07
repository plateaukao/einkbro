package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.runtime.Composable
import info.plateaukao.einkbro.activity.GptActionDialog
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.GptActionType

class ShowEditGptActionDialogFragment(
    private val editActionIndex: Int = -1,
) : ComposeDialogFragment() {
    @Composable
    override fun Content() {
        var actionList = config.ai.gptActionList
        GptActionDialog(
            editActionIndex,
            if (editActionIndex >= 0) actionList[editActionIndex] else createDefaultGptAction(),
            config.ai.getGptTypeModelMap(),
            okAction = { modifiedAction ->
                actionList = actionList.toMutableList().apply {
                    if (editActionIndex >= 0) set(editActionIndex, modifiedAction)
                    else add(modifiedAction)
                }
                config.ai.gptActionList = actionList
                dismiss()
            },
            dismissAction = { dismiss() }
        )
    }

    private fun createDefaultGptAction(): ChatGPTActionInfo {
        return ChatGPTActionInfo(
            "",
            "",
            "",
            GptActionType.Default,
            config.ai.getDefaultActionModel()
        )
    }
}