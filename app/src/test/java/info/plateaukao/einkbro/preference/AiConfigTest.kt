package info.plateaukao.einkbro.preference

import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var config: AiConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        config = AiConfig(sp)
    }

    @Test
    fun `gptActionList falls back to legacy prompts when nothing stored`() {
        // default system prompt is non-blank, so a single migrated action is synthesized
        val actions = config.gptActionList
        assertEquals(1, actions.size)
        assertEquals("You are a good interpreter.", actions[0].systemMessage)
        assertEquals("Translate following content to English:", actions[0].userMessage)
    }

    @Test
    fun `gptActionList is empty when prompts are blank and nothing stored`() {
        config.gptSystemPrompt = ""
        config.gptUserPromptPrefix = ""
        assertEquals(emptyList<ChatGPTActionInfo>(), config.gptActionList)
    }

    @Test
    fun `gptActionList round trips`() {
        val actions = listOf(
            ChatGPTActionInfo(
                name = "Summarize",
                systemMessage = "You summarize.",
                userMessage = "Summarize:",
                actionType = GptActionType.Gemini,
                model = "gemini-2.5-flash",
                display = GptActionDisplay.NewTab,
                scope = GptActionScope.WholePage,
                id = "fixed-id-1",
            ),
            ChatGPTActionInfo(name = "Translate", id = "fixed-id-2"),
        )
        config.gptActionList = actions
        assertEquals(actions, config.gptActionList)
    }

    @Test
    fun `gptActionList decoding ignores unknown keys`() {
        sp.store[AiConfig.K_GPT_ACTION_ITEMS] =
            """[{"name":"Old","unknownFutureField":42,"id":"x"}]"""
        val actions = config.gptActionList
        assertEquals(1, actions.size)
        assertEquals("Old", actions[0].name)
        // unspecified fields fall back to defaults
        assertEquals(GptActionType.Default, actions[0].actionType)
        assertEquals(GptActionDisplay.Popup, actions[0].display)
    }

    @Test
    fun `addGptAction and deleteGptAction modify persisted list`() {
        config.gptActionList = emptyList()
        val action = ChatGPTActionInfo(name = "A", id = "id-a")
        config.addGptAction(action)
        assertEquals(listOf(action), config.gptActionList)

        config.deleteGptAction(action)
        assertEquals(emptyList<ChatGPTActionInfo>(), config.gptActionList)
    }

    @Test
    fun `deleteAllGptActions clears the list`() {
        config.gptActionList = listOf(ChatGPTActionInfo(name = "A", id = "1"))
        config.deleteAllGptActions()
        assertEquals(emptyList<ChatGPTActionInfo>(), config.gptActionList)
    }

    @Test
    fun `gptActionForExternalSearch defaults to null and round trips`() {
        assertNull(config.gptActionForExternalSearch)
        val action = ChatGPTActionInfo(name = "Lookup", id = "ext-1")
        config.gptActionForExternalSearch = action
        assertEquals(action, config.gptActionForExternalSearch)
    }

    @Test
    fun `getDefaultActionType depends on api flags`() {
        assertEquals(GptActionType.OpenAi, config.getDefaultActionType())

        config.useCustomGptUrl = true
        assertEquals(GptActionType.SelfHosted, config.getDefaultActionType())

        config.useGeminiApi = true // gemini wins over custom url
        assertEquals(GptActionType.Gemini, config.getDefaultActionType())
    }

    @Test
    fun `getDefaultActionModel depends on api flags`() {
        config.gptModel = "gpt-x"
        config.alternativeModel = "alt-x"
        config.geminiModel = "gem-x"

        assertEquals("gpt-x", config.getDefaultActionModel())
        config.useCustomGptUrl = true
        assertEquals("alt-x", config.getDefaultActionModel())
        config.useGeminiApi = true
        assertEquals("gem-x", config.getDefaultActionModel())
    }

    @Test
    fun `getGptTypeModelMap contains all action types`() {
        val map = config.getGptTypeModelMap()
        assertTrue(GptActionType.entries.all { it in map })
    }

    @Test
    fun `externalSearchMethod round trips`() {
        assertEquals(TRANSLATE_API.GOOGLE, config.externalSearchMethod)
        config.externalSearchMethod = TRANSLATE_API.DEEPL
        assertEquals(TRANSLATE_API.DEEPL, config.externalSearchMethod)
    }

    @Test
    fun `gptForChatWeb and gptForSummary round trip`() {
        assertEquals(GptActionType.Default, config.gptForChatWeb)
        config.gptForChatWeb = GptActionType.SelfHosted
        assertEquals(GptActionType.SelfHosted, config.gptForChatWeb)

        config.gptForSummary = GptActionType.Gemini
        assertEquals(GptActionType.Gemini, config.gptForSummary)
    }
}
