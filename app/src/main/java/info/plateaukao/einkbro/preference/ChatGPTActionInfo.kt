package info.plateaukao.einkbro.preference

import kotlinx.serialization.Serializable

@Serializable
data class ChatGPTActionInfo (
    val name: String = "ChatGPT",
    val systemMessage: String = "",
    val userMessage: String = "",
    val actionType: GptActionType = GptActionType.Default,
    val model: String = "",
)

enum class GptActionType {
    Default,
    OpenAi,
    SelfHosted,
    Gemini
}