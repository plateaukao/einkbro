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
    val VoiceTag: VoiceTag
)

// create a dummy VoiceItem
val dummyVoiceItem = VoiceItem(
    FriendlyName = "dummy",
    Gender = "dummy",
    Locale = "dummy",
    Name = "dummy",
    ShortName = "dummy",
    Status = "dummy",
    SuggestedCodec = "dummy",
    VoiceTag = VoiceTag(listOf("dummy"), listOf("dummy")),
)
