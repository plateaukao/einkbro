package icu.xmc.edgettslib.entity

import info.plateaukao.einkbro.tts.entity.VoiceTag
import kotlinx.serialization.Serializable

@Serializable
data class VoiceItem(
    val FriendlyName: String,
    val Gender: String,
    val Locale: String,
    val Name: String,
    val ShortName: String,
    val Status: String,
    val SuggestedCodec: String,
    val VoiceTag: VoiceTag,
) {
    private fun description(): String {
        return "${ShortName.replace("Neural", "")}  " +
                if (specialPersonalities().isNotEmpty()) {
                    "(${specialPersonalities().joinToString(", ")})"
                } else {
                    ""
                }
    }

    fun getLanguageCode(): String {
        return ShortName.split("-")[0]
    }

    fun getCountryCode(): String {
        return ShortName.split("-")[1]
    }

    fun getVoiceRole(): String = description().split("-").last()

    private fun specialPersonalities(): List<String> =
        VoiceTag.VoicePersonalities.filter { it != "Friendly" && it != "Positive" }
}

// create a dummy VoiceItem
val dummyVoiceItem = VoiceItem(
    FriendlyName = "dummy",
    Gender = "dummy",
    Locale = "en-US",
    Name = "dummy",
    ShortName = "dummy",
    Status = "dummy",
    SuggestedCodec = "dummy",
    VoiceTag = VoiceTag(listOf("dummy"), listOf("dummy")),
)
