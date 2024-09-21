package info.plateaukao.einkbro.tts.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

    fun getShortNameWithoutNeural(): String = shortName.replace("Neural", "").split("-").last()

    private fun specialPersonalities(): List<String> =
        voiceTag.voicePersonalities.filter { it != "Friendly" && it != "Positive" }
}

/*
use this to generate the default voice item
 */
val defaultVoiceItem: VoiceItem = Json.decodeFromString("""
  {
    "Name": "Microsoft Server Speech Text to Speech Voice (en-US, AvaMultilingualNeural)",
    "ShortName": "en-US-AvaMultilingualNeural",
    "Gender": "Female",
    "Locale": "en-US",
    "SuggestedCodec": "audio-24khz-48kbitrate-mono-mp3",
    "FriendlyName": "Microsoft AvaMultilingual Online (Natural) - English (United States)",
    "Status": "GA",
    "VoiceTag": {
      "ContentCategories": [
        "Conversation",
        "Copilot"
      ],
      "VoicePersonalities": [
        "Expressive",
        "Caring",
        "Pleasant",
        "Friendly"
      ]
    }
  }
""".trimIndent()
)
