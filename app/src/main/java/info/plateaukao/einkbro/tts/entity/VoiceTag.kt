package info.plateaukao.einkbro.tts.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceTag(
    @SerialName("ContentCategories")
    val contentCategories: List<String>,
    @SerialName("VoicePersonalities")
    val voicePersonalities: List<String>,
)