package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import info.plateaukao.einkbro.tts.entity.VoiceItem
import info.plateaukao.einkbro.tts.entity.defaultVoiceItem
import info.plateaukao.einkbro.viewmodel.TtsType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

class TtsConfig(private val sp: SharedPreferences) {

    private val K_TTS_LOCALE = "sp_tts_locale"
    var ttsLocale: Locale
        get() = Locale(
            sp.getString(K_TTS_LOCALE, Locale.getDefault().language) ?: Locale.getDefault().language
        )
        set(value) {
            sp.edit { putString(K_TTS_LOCALE, value.language) }
        }

    var ttsSpeedValue: Int
        get() = sp.getString(K_TTS_SPEED_VALUE, "100")?.toInt() ?: 100
        set(value) {
            sp.edit { putString(K_TTS_SPEED_VALUE, value.toString()) }
        }

    var ttsType: TtsType
        get() = TtsType.entries[sp.getInt("K_TTS_TYPE", 0)]
        set(value) {
            sp.edit { putInt("K_TTS_TYPE", value.ordinal) }
            useOpenAiTts = value == TtsType.GPT
        }

    var ttsShowCurrentText by BooleanPreference(sp, "K_TTS_SHOW_CURRENT_TEXT", false)

    var ttsShowTextTranslation by BooleanPreference(sp, "K_TTS_SHOW_TEXT_TRANSLATION", false)

    var useOpenAiTts by BooleanPreference(sp, K_USE_OPENAI_TTS, true)

    private val K_RECENT_USED_TTS_VOICES = "sp_recent_used_tts_voices"
    var recentUsedTtsVoices: MutableList<VoiceItem>
        get() {
            val string = sp.getString(K_RECENT_USED_TTS_VOICES, "").orEmpty()
            if (string.isBlank()) return mutableListOf()

            return try {
                string.split("###")
                    .mapNotNull { Json.decodeFromString<VoiceItem>(it) }
                    .toMutableList()
            } catch (exception: Exception) {
                sp.edit { remove(K_RECENT_USED_TTS_VOICES) }
                mutableListOf()
            }
        }
        set(value) {
            val processedValue = if (value.distinct().size > 5) {
                value.distinct().subList(0, 5)
            } else {
                value.distinct()
            }

            sp.edit {
                if (processedValue.isEmpty()) {
                    remove(K_RECENT_USED_TTS_VOICES)
                } else {
                    // check if the new value the same as the old one
                    putString(
                        K_RECENT_USED_TTS_VOICES,
                        processedValue.joinToString("###") { Json.encodeToString(it) }
                    )
                }
            }
        }

    var ettsVoice: VoiceItem
        get() = Json.decodeFromString(
            sp.getString(
                "K_ETTS_VOICE", Json.encodeToString(defaultVoiceItem)
            ) ?: Json.encodeToString(defaultVoiceItem)
        )
        set(value) {
            sp.edit { putString("K_ETTS_VOICE", Json.encodeToString(value)) }
            recentUsedTtsVoices = recentUsedTtsVoices.apply { add(0, value) }
        }

    var dualCaptionLocale by StringPreference(sp, K_DUAL_CAPTION_LOCALE, "")

    companion object {
        const val K_TTS_SPEED_VALUE = "sp_tts_speed"
        const val K_USE_OPENAI_TTS = "sp_use_openai_tts"
        const val K_DUAL_CAPTION_LOCALE = "sp_dual_caption_locale"
    }
}
