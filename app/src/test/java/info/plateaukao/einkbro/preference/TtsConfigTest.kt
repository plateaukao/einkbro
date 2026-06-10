package info.plateaukao.einkbro.preference

import info.plateaukao.einkbro.tts.entity.VoiceItem
import info.plateaukao.einkbro.tts.entity.VoiceTag
import info.plateaukao.einkbro.tts.entity.defaultVoiceItem
import info.plateaukao.einkbro.viewmodel.TtsType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TtsConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var config: TtsConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        config = TtsConfig(sp)
    }

    private fun voiceItem(shortName: String) = VoiceItem(
        friendlyName = "Friendly $shortName",
        gender = "Female",
        locale = shortName.substringBeforeLast("-"),
        name = "Microsoft Server Speech Text to Speech Voice ($shortName)",
        shortName = shortName,
        status = "GA",
        suggestedCodec = "audio-24khz-48kbitrate-mono-mp3",
        voiceTag = VoiceTag(
            contentCategories = listOf("General"),
            voicePersonalities = listOf("Friendly"),
        ),
    )

    @Test
    fun `ttsSpeedValue defaults to 100 and round trips as string`() {
        assertEquals(100, config.ttsSpeedValue)
        config.ttsSpeedValue = 85
        assertEquals(85, config.ttsSpeedValue)
        assertEquals("85", sp.store[TtsConfig.K_TTS_SPEED_VALUE])
    }

    @Test
    fun `ttsType round trips and drives useOpenAiTts`() {
        assertEquals(TtsType.SYSTEM, config.ttsType)

        config.ttsType = TtsType.GPT
        assertEquals(TtsType.GPT, config.ttsType)
        assertTrue(config.useOpenAiTts)

        config.ttsType = TtsType.ETTS
        assertEquals(TtsType.ETTS, config.ttsType)
        assertFalse(config.useOpenAiTts)
    }

    @Test
    fun `recentUsedTtsVoices defaults to empty list`() {
        assertEquals(emptyList<VoiceItem>(), config.recentUsedTtsVoices)
    }

    @Test
    fun `recentUsedTtsVoices round trips`() {
        val voices = mutableListOf(voiceItem("en-US-Test1Neural"), voiceItem("ja-JP-Test2Neural"))
        config.recentUsedTtsVoices = voices
        assertEquals(voices, config.recentUsedTtsVoices)
    }

    @Test
    fun `recentUsedTtsVoices deduplicates and caps at five entries`() {
        val voices = (1..7).map { voiceItem("en-US-Test${it}Neural") }
        val withDuplicates = (voices + voices).toMutableList()
        config.recentUsedTtsVoices = withDuplicates
        assertEquals(voices.take(5), config.recentUsedTtsVoices)
    }

    @Test
    fun `recentUsedTtsVoices clears corrupted stored value`() {
        sp.store["sp_recent_used_tts_voices"] = "not-a-json###also-not-json"
        assertEquals(emptyList<VoiceItem>(), config.recentUsedTtsVoices)
        assertFalse(sp.contains("sp_recent_used_tts_voices"))
    }

    @Test
    fun `ettsVoice defaults to the bundled default voice`() {
        assertEquals(defaultVoiceItem, config.ettsVoice)
    }

    @Test
    fun `ettsVoice round trips and is added to recent voices`() {
        val voice = voiceItem("de-DE-Test9Neural")
        config.ettsVoice = voice
        assertEquals(voice, config.ettsVoice)
        assertEquals(voice, config.recentUsedTtsVoices.first())
    }
}
