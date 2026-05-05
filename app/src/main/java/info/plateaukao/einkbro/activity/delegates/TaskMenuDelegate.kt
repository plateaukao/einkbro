package info.plateaukao.einkbro.activity.delegates

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.task.BrowserTools
import info.plateaukao.einkbro.task.InitialPageSnapshot
import info.plateaukao.einkbro.task.TaskCatalog
import info.plateaukao.einkbro.task.TaskRunner
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.compose.CustomTaskInputDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TaskMenuDialogFragment
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class TaskMenuDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val config: ConfigManager,
    private val taskRunner: TaskRunner,
    private val translationViewModel: TranslationViewModel,
    private val dispatch: (BrowserAction) -> Unit,
    private val showTranslationDialog: (Boolean) -> Unit,
    private val chatWithWebAgent: suspend (String, InitialPageSnapshot) -> Unit,
) {
    fun showTaskMenu() {
        TaskMenuDialogFragment(
            descriptors = TaskCatalog.builtIns,
            onTemplateClicked = { descriptor -> dispatch(BrowserAction.RunTask(descriptor.id)) },
            onCustomClicked = {
                CustomTaskInputDialogFragment(
                    onSubmit = { prompt -> dispatch(BrowserAction.RunCustomTask(prompt)) },
                ).showNow(activity.supportFragmentManager, "customTaskInput")
            },
        ).showNow(activity.supportFragmentManager, "taskMenu")
    }

    fun runTaskById(taskId: String) {
        val descriptor = TaskCatalog.byId(taskId)
        if (descriptor == null) {
            EBToast.show(activity, R.string.task_unknown)
            return
        }
        if (!translationViewModel.hasOpenAiApiKey() && !config.ai.useGeminiApi) {
            EBToast.show(activity, R.string.gpt_api_key_not_set)
            return
        }
        // Start the task FIRST so the progress StateFlow holds the fresh Running state
        // before we attach the collector — otherwise the collector's first emission is
        // the previous task's leftover Done value and the dialog briefly shows that
        // stale content before being overwritten.
        taskRunner.run(descriptor.factory())
        translationViewModel.setupTaskStream(taskRunner.progress)
        showTranslationDialog(true)
    }

    fun runCustomTask(prompt: String) {
        if (prompt.isBlank()) return
        if (config.ai.useGeminiApi) {
            EBToast.show(activity, R.string.task_requires_openai)
            return
        }
        if (!translationViewModel.hasOpenAiApiKey()) {
            EBToast.show(activity, R.string.gpt_api_key_not_set)
            return
        }
        // Snapshot the page the user is currently viewing, then hand off to a new
        // agent chat tab. The agent will see this snapshot via initialPage* tools so
        // it can act on the originating page without extra fetches.
        activity.lifecycleScope.launch {
            val current = state.ebWebView
            val rawText = current.getRawText()
            val rawLinks = try {
                current.jsBridge.getPageLinks()
            } catch (e: Exception) {
                "[]"
            }
            val links = parseSnapshotLinks(rawLinks)
            val rawHtml = captureRawBodyHtml(current)
            val snapshot = InitialPageSnapshot(
                url = current.url.orEmpty(),
                title = current.title.orEmpty(),
                text = rawText,
                links = links,
                rawHtml = rawHtml,
            )
            chatWithWebAgent(prompt, snapshot)
        }
    }

    /** Snapshot the user's current page as raw HTML. Used by agent tasks that need to
     *  identify a DOM element the user described (banners, popups, ads). Returns "" on
     *  failure — the agent will fall back to reader-mode text. */
    private suspend fun captureRawBodyHtml(webView: EBWebView): String =
        suspendCoroutine { cont ->
            try {
                webView.evaluateJavascript(
                    "(function(){try{return document.body.innerHTML;}catch(e){return '';}})();"
                ) { raw ->
                    val unquoted = if (raw.startsWith("\"") && raw.endsWith("\""))
                        HelperUnit.unescapeJava(raw).let { it.substring(1, it.length - 1) }
                    else raw
                    cont.resume(unquoted)
                }
            } catch (e: Exception) {
                cont.resume("")
            }
        }

    private fun parseSnapshotLinks(raw: String): List<BrowserTools.Link> {
        if (raw.isBlank() || raw == "null") return emptyList()
        return try {
            val array = Json { ignoreUnknownKeys = true }
                .parseToJsonElement(raw) as? JsonArray ?: return emptyList()
            array.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val text = (obj["text"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                val href = (obj["href"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                BrowserTools.Link(text, href)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
