package icu.xmc.edgettslib.entity

import info.plateaukao.einkbro.tts.entity.VoiceTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceItem(
    @SerialName("FriendlyName")
    val friendlyName: String,
    @SerialName("Gender")
    val gender: String,
    @SerialName("Locale")
    val locale: String,
    @SerialName("Name")
    val name: String,
    @SerialName("ShortName")
    val shortName: String,
    @SerialName("Status")
    val status: String,
    @SerialName("SuggestedCodec")
    val suggestedCodec: String,
    @SerialName("VoiceTag")
    val voiceTag: VoiceTag
) {
    private fun description(): String {
        return "${shortName.replace("Neural", "")}  " +
                if (specialPersonalities().isNotEmpty()) {
                    "(${specialPersonalities().joinToString(", ")})"
                } else {
                    ""
                }
    }

    fun getLanguageCode(): String {
        return shortName.split("-")[0]
    }

    fun getCountryCode(): String {
        return shortName.split("-")[1]
    }

    fun getVoiceRole(): String = description().split("-").last()

    private fun specialPersonalities(): List<String> =
        voiceTag.voicePersonalities.filter { it != "Friendly" && it != "Positive" }
}

// create a dummy VoiceItem
val dummyVoiceItem = VoiceItem(
    friendlyName = "dummy",
    gender = "dummy",
    locale = "en-US",
    name = "dummy",
    shortName = "dummy",
    status = "dummy",
    suggestedCodec = "dummy",
    voiceTag = VoiceTag(listOf("dummy"), listOf("dummy")),
)
