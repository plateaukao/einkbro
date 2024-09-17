package info.plateaukao.einkbro.tts.entity

import kotlinx.serialization.Serializable

@Serializable
data class VoiceTag(
    val ContentCategories: List<String>,
    val VoicePersonalities: List<String>
)