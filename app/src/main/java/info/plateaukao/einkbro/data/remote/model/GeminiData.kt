package info.plateaukao.einkbro.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ContentPart(
    val text: String = "",
    // Gemini 3+ marks reasoning-summary parts with thought=true and may attach a
    // thoughtSignature to answer parts; text must default so signature-only parts decode.
    val thought: Boolean = false,
)

@Serializable
data class Content(val parts: List<ContentPart> = emptyList())

@Serializable
data class SafetySetting(val category: String, val threshold: String)

@Serializable
data class ThinkingConfig(val thinkingBudget: Int, val includeThoughts: Boolean = false)

@Serializable
data class GenerationConfig(val thinkingConfig: ThinkingConfig)

@Serializable
data class RequestData(
    val contents: List<Content>,
    val safety_settings: List<SafetySetting>,
    // null = omit thinkingConfig entirely and let the model use its default.
    val generationConfig: GenerationConfig? = null,
)

@Serializable
data class ResponseData(val candidates: List<Candidate> = emptyList())

@Serializable
data class Candidate(
    val content: Content = Content(),
    val finishReason: String? = null,
)
