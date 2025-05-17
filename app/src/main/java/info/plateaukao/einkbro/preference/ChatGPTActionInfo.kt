package info.plateaukao.einkbro.preference

import kotlinx.serialization.Serializable

@Serializable
data class ChatGPTActionInfo (
    val name: String = "ChatGPT",
    val systemMessage: String = "",
    val userMessage: String = "",
    val actionType: GptActionType = GptActionType.Default,
    val model: String = "",
    val display: GptActionDisplay = GptActionDisplay.Popup,
)

enum class GptActionType {
    Default,
    OpenAi,
    SelfHosted,
    Gemini
}

enum class GptActionDisplay {
    Popup,
    NewTab,
    SplitScreen,
}