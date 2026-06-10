package info.plateaukao.einkbro.preference

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round-trip tests for the data classes persisted as JSON / delimited strings
 * in SharedPreferences. If any of these encodings break, users lose settings
 * on upgrade, so the wire format is asserted explicitly where it matters.
 */
class SerializableDataTest {

    private val lenientJson = Json { ignoreUnknownKeys = true }

    // ── ChatGPTActionInfo (kotlinx.serialization) ────────────────────────────

    @Test
    fun `ChatGPTActionInfo round trips through Json`() {
        val action = ChatGPTActionInfo(
            name = "Summarize",
            systemMessage = "system",
            userMessage = "user",
            actionType = GptActionType.SelfHosted,
            model = "llama3",
            display = GptActionDisplay.SplitScreen,
            scope = GptActionScope.WholePage,
            id = "stable-id",
        )
        val decoded = Json.decodeFromString<ChatGPTActionInfo>(Json.encodeToString(action))
        assertEquals(action, decoded)
    }

    @Test
    fun `ChatGPTActionInfo list round trips through Json`() {
        val actions = listOf(
            ChatGPTActionInfo(name = "A", id = "1"),
            ChatGPTActionInfo(name = "B", actionType = GptActionType.Gemini, id = "2"),
        )
        val decoded = Json.decodeFromString<List<ChatGPTActionInfo>>(Json.encodeToString(actions))
        assertEquals(actions, decoded)
    }

    @Test
    fun `ChatGPTActionInfo decodes legacy JSON missing newer fields`() {
        // Simulates data written by an older app version before new fields existed.
        val decoded = lenientJson.decodeFromString<ChatGPTActionInfo>(
            """{"name":"Old","systemMessage":"s","userMessage":"u"}"""
        )
        assertEquals("Old", decoded.name)
        assertEquals(GptActionType.Default, decoded.actionType)
        assertEquals(GptActionDisplay.Popup, decoded.display)
        assertEquals(GptActionScope.TextSelection, decoded.scope)
    }

    @Test
    fun `ChatGPTActionInfo decoding ignores unknown future fields`() {
        val decoded = lenientJson.decodeFromString<ChatGPTActionInfo>(
            """{"name":"New","someFutureField":true}"""
        )
        assertEquals("New", decoded.name)
    }

    @Test
    fun `Gpt enums keep stable serial names`() {
        // These names are persisted in SharedPreferences; renaming them breaks restore.
        assertEquals("\"Default\"", Json.encodeToString(GptActionType.Default))
        assertEquals("\"OpenAi\"", Json.encodeToString(GptActionType.OpenAi))
        assertEquals("\"SelfHosted\"", Json.encodeToString(GptActionType.SelfHosted))
        assertEquals("\"Gemini\"", Json.encodeToString(GptActionType.Gemini))
        assertEquals("\"Popup\"", Json.encodeToString(GptActionDisplay.Popup))
        assertEquals("\"NewTab\"", Json.encodeToString(GptActionDisplay.NewTab))
        assertEquals("\"SplitScreen\"", Json.encodeToString(GptActionDisplay.SplitScreen))
        assertEquals("\"TextSelection\"", Json.encodeToString(GptActionScope.TextSelection))
        assertEquals("\"WholePage\"", Json.encodeToString(GptActionScope.WholePage))
    }

    // ── SplitSearchItemInfo (kotlinx.serialization) ──────────────────────────

    @Test
    fun `SplitSearchItemInfo round trips through Json`() {
        val item = SplitSearchItemInfo("Wiki", "https://en.wikipedia.org/wiki/%s", true)
        assertEquals(item, Json.decodeFromString<SplitSearchItemInfo>(Json.encodeToString(item)))
    }

    // ── CustomFontInfo (delimited string) ────────────────────────────────────

    @Test
    fun `CustomFontInfo round trips through serialized string`() {
        val info = CustomFontInfo("My Font", "content://com.android.providers/font.ttf")
        assertEquals(info, info.toSerializedString().toCustomFontInfo())
    }

    @Test
    fun `CustomFontInfo url containing separator survives round trip`() {
        val info = CustomFontInfo("Font", "https://host/path::with::colons")
        assertEquals(info, info.toSerializedString().toCustomFontInfo())
    }

    @Test
    fun `CustomFontInfo parsing returns null for malformed string`() {
        assertNull("no-separator-here".toCustomFontInfo())
    }

    // ── AlbumInfo (delimited string) ─────────────────────────────────────────

    @Test
    fun `AlbumInfo round trips through serialized string`() {
        val info = AlbumInfo("EinkBro - E-ink Browser", "https://github.com/plateaukao/einkbro")
        assertEquals(info, info.toSerializedString().toAlbumInfo())
    }

    @Test
    fun `AlbumInfo parsing returns null for malformed string`() {
        assertNull("just a title".toAlbumInfo())
    }

    // ── RecentBookmark (delimited string) ────────────────────────────────────

    @Test
    fun `RecentBookmark round trips through serialized string`() {
        val bookmark = RecentBookmark("News", "https://news.example.com/page?a=1", 7)
        assertEquals(bookmark, bookmark.toSerializedString().toRecentBookmark())
    }

    @Test
    fun `RecentBookmark parsing returns null for malformed string`() {
        assertNull("name::url-only".toRecentBookmark())
    }
}
