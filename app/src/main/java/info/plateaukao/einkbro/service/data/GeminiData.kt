package info.plateaukao.einkbro.service.data

import kotlinx.serialization.Serializable

@Serializable
data class ContentPart(val text: String)

@Serializable
data class Content(val parts: List<ContentPart>)

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
    val generationConfig: GenerationConfig = GenerationConfig(ThinkingConfig(thinkingBudget = 0))
)

@Serializable
data class ResponseData(val candidates: List<Candidate>)

@Serializable
data class Candidate(val content: Content)
