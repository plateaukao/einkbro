package info.plateaukao.einkbro.unit

import android.content.Context
import android.content.SharedPreferences
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.preference.FakeSharedPreferences
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.lang.reflect.Method
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tests for BackupUnit's JSON (de)serialization: the bookmarks backup format
 * and the GPT settings export/import. The bookmark converters are private
 * top-level functions, so they are invoked through reflection rather than by
 * spinning up the full KoinComponent dependency graph.
 */
class BackupUnitJsonTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeSp: FakeSharedPreferences
    private lateinit var backupUnit: BackupUnit

    @Before
    fun setUp() {
        fakeSp = FakeSharedPreferences()
        startKoin {
            modules(module { single<SharedPreferences> { fakeSp } })
        }
        backupUnit = BackupUnit(mockk<Context>(relaxed = true))
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ── reflection helpers for the private top-level converters ─────────────

    private val fileFacade: Class<*> = Class.forName("info.plateaukao.einkbro.unit.BackupUnitKt")

    private fun bookmarksToJsonString(bookmarks: List<Bookmark>): String {
        val method = fileFacade.getDeclaredMethod("toJsonString", List::class.java)
        method.isAccessible = true
        return method.invoke(null, bookmarks) as String
    }

    private fun jsonToBookmark(json: JSONObject): Bookmark {
        val method = fileFacade.getDeclaredMethod("toBookmark", JSONObject::class.java)
        method.isAccessible = true
        return method.invoke(null, json) as Bookmark
    }

    // ── bookmarks JSON ───────────────────────────────────────────────────────

    @Test
    fun `bookmarks survive a JSON export-import round trip`() {
        val folder = Bookmark("Tech", "", isDirectory = true, parent = 0, order = 0)
            .apply { id = 1 }
        val child = Bookmark("EinkBro", "https://github.com/plateaukao/einkbro", false, 1, 2)
            .apply { id = 5 }
        val original = listOf(folder, child)

        val jsonString = bookmarksToJsonString(original)
        val jsonArray = JSONArray(jsonString)
        val restored = (0 until jsonArray.length()).map { jsonToBookmark(jsonArray.getJSONObject(it)) }

        // data-class equality covers title/url/isDirectory/parent/order...
        assertEquals(original, restored)
        // ...but id lives outside the constructor, so check it explicitly
        assertEquals(original.map { it.id }, restored.map { it.id })
    }

    @Test
    fun `bookmark JSON uses the stable backup field names`() {
        val bookmark = Bookmark("title", "https://x.com", false, 3, 4).apply { id = 9 }
        val obj = JSONArray(bookmarksToJsonString(listOf(bookmark))).getJSONObject(0)

        // These keys are the on-disk backup format; renaming any of them breaks
        // restoring backups created by older versions.
        assertEquals(9, obj.getInt("id"))
        assertEquals("title", obj.getString("title"))
        assertEquals("https://x.com", obj.getString("url"))
        assertEquals(false, obj.getBoolean("isDirectory"))
        assertEquals(3, obj.getInt("parent"))
        assertEquals(4, obj.getInt("order"))
    }

    @Test
    fun `bookmark restore tolerates missing fields with defaults`() {
        val restored = jsonToBookmark(JSONObject("""{"title":"only title"}"""))
        assertEquals("only title", restored.title)
        assertEquals("", restored.url)
        assertFalse(restored.isDirectory)
        assertEquals(0, restored.parent)
        assertEquals(0, restored.order)
        assertEquals(0, restored.id)
    }

    @Test
    fun `empty bookmark list exports as empty JSON array`() {
        assertEquals(0, JSONArray(bookmarksToJsonString(emptyList())).length())
    }

    // ── backup zip manifest ──────────────────────────────────────────────────

    private fun createBackupZip(manifest: JSONObject?): File {
        val file = tempFolder.newFile("backup.zip")
        ZipOutputStream(file.outputStream()).use { zos ->
            if (manifest != null) {
                zos.putNextEntry(ZipEntry("_manifest.json"))
                zos.write(manifest.toString().toByteArray())
                zos.closeEntry()
            }
            zos.putNextEntry(ZipEntry("bookmarks.json"))
            zos.write("[]".toByteArray())
            zos.closeEntry()
        }
        return file
    }

    @Test
    fun `getAvailableCategories reads categories from manifest`() {
        val manifest = JSONObject().apply {
            put("version", 2)
            put("categories", JSONArray(listOf("BOOKMARKS", "HISTORY")))
        }
        val categories = backupUnit.getAvailableCategories(createBackupZip(manifest))
        assertEquals(setOf(BackupCategory.BOOKMARKS, BackupCategory.HISTORY), categories)
    }

    @Test
    fun `getAvailableCategories ignores unknown category names`() {
        val manifest = JSONObject().apply {
            put("version", 99)
            put("categories", JSONArray(listOf("BOOKMARKS", "FROM_THE_FUTURE")))
        }
        val categories = backupUnit.getAvailableCategories(createBackupZip(manifest))
        assertEquals(setOf(BackupCategory.BOOKMARKS), categories)
    }

    @Test
    fun `getAvailableCategories returns null for legacy zip without manifest`() {
        assertNull(backupUnit.getAvailableCategories(createBackupZip(manifest = null)))
    }

    // ── GPT settings export/import ───────────────────────────────────────────

    private fun invokePrivate(name: String, vararg args: Any?): Any? {
        val method: Method = BackupUnit::class.java.declaredMethods.first { it.name == name }
        method.isAccessible = true
        return method.invoke(backupUnit, *args)
    }

    @Test
    fun `gpt settings export only includes gpt keys`() {
        fakeSp.store["sp_gpt_api_key"] = "sk-secret"
        fakeSp.store["sp_use_openai_tts"] = true
        fakeSp.store["K_GPT_VOICE_OPTION"] = 3
        fakeSp.store["sp_fontSize"] = "120" // unrelated preference

        val json = invokePrivate("exportGptSettings") as JSONObject

        assertEquals("sk-secret", json.getString("sp_gpt_api_key"))
        assertTrue(json.getBoolean("sp_use_openai_tts"))
        assertEquals(3, json.getInt("K_GPT_VOICE_OPTION"))
        assertFalse(json.has("sp_fontSize"))
    }

    @Test
    fun `gpt settings survive an export-import round trip`() {
        fakeSp.store["sp_gpt_api_key"] = "sk-secret"
        fakeSp.store["sp_gemini_api_key"] = "gm-key"
        fakeSp.store["sp_gp_model"] = "gpt-4.1"
        fakeSp.store["sp_use_openai_tts"] = false
        fakeSp.store["sp_enable_open_ai_stream"] = true
        fakeSp.store["K_GPT_VOICE_OPTION"] = 2
        fakeSp.store["sp_gpt_action_items"] = """[{"name":"A","id":"1"}]"""

        val exported = invokePrivate("exportGptSettings") as JSONObject

        // import into a clean preferences store
        val originalValues = fakeSp.store.toMap()
        fakeSp.store.clear()
        invokePrivate("importGptSettings", exported)

        assertEquals(originalValues, fakeSp.store.toMap())
    }

    @Test
    fun `gpt settings export skips keys that are not set`() {
        fakeSp.store["sp_gpt_api_key"] = "only-this"
        val json = invokePrivate("exportGptSettings") as JSONObject
        assertEquals(1, json.length())
    }
}
