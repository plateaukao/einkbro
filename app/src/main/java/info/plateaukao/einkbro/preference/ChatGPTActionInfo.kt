package info.plateaukao.einkbro.preference

import kotlinx.serialization.Serializable

@Serializable
data class ChatGPTActionInfo (
    val name: String = "ChatGPT",
    val systemMessage: String = "",
    val userMessage: String = "",
)