package info.plateaukao.einkbro.preference

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatGPTActionInfo (
    val name: String = "ChatGPT",
    val systemMessage: String = "",
    val userMessage: String = "",
    val actionType: GptActionType = GptActionType.Default,
    val model: String = "",
    val display: GptActionDisplay = GptActionDisplay.Popup,
    val scope: GptActionScope = GptActionScope.TextSelection,
    val id: String = UUID.randomUUID().toString(),
)

@Serializable
enum class GptActionType {
    Default,
    OpenAi,
    SelfHosted,
    Gemini
}

@Serializable
enum class GptActionDisplay {
    Popup,
    NewTab,
    SplitScreen,
}

@Serializable
enum class GptActionScope {
    TextSelection,
    WholePage,
}
