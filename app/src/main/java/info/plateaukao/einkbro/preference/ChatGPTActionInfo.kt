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
    val reasoning: ReasoningEffort = ReasoningEffort.Default,
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

/**
 * Reasoning/thinking effort for AI requests. On an action, [Default] means
 * "follow the global Gen AI setting"; on the global setting itself it means
 * "model default" — no reasoning parameter is sent at all, which matches the
 * app's behavior before this setting existed.
 */
@Serializable
enum class ReasoningEffort {
    Default,
    Off,
    Low,
    Medium,
    High,
}
