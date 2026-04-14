package info.plateaukao.einkbro.task

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.WebViewCallback
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates execution of a [BrowserTask]. Owns the lifecycle of the off-screen
 * [BrowserTools], exposes a [progress] StateFlow for the UI to render, and supports
 * cancellation.
 *
 * Scoped per-[FragmentActivity] — hold a lazy instance in [BrowserActivity], like
 * `aiChatDelegate`.
 */
class TaskRunner(
    private val activity: FragmentActivity,
    private val context: Context,
    private val webViewCallback: WebViewCallback,
    private val browserState: BrowserState,
    private val config: ConfigManager,
    private val ttsViewModel: TtsViewModel,
) {
    private val _progress = MutableStateFlow<TaskProgress?>(null)
    val progress: StateFlow<TaskProgress?> = _progress.asStateFlow()

    private var currentJob: Job? = null
    private var currentTools: BrowserTools? = null

    fun run(task: BrowserTask): Job {
        cancel()
        val openAi = OpenAiRepository()

        val tools = BrowserToolsImpl(
            context = context,
            webViewCallback = webViewCallback,
            browserState = browserState,
            config = config,
            openAiRepository = openAi,
            ttsViewModel = ttsViewModel,
            progressSink = { line -> appendStep(line) },
            finishSink = { markdown -> markFinished(markdown) },
        )
        currentTools = tools

        _progress.value = TaskProgress(
            taskName = task.displayName,
            status = TaskProgress.Status.Running,
            steps = emptyList(),
            finalMarkdown = null,
        )

        val job = activity.lifecycleScope.launch {
            try {
                task.run(tools)
                val current = _progress.value
                if (current?.status == TaskProgress.Status.Running) {
                    // Task exited without calling finish()
                    _progress.value = current.copy(
                        status = TaskProgress.Status.Done,
                        finalMarkdown = current.finalMarkdown
                            ?: "(task ended without producing a result)",
                    )
                }
            } catch (e: CancellationException) {
                _progress.value = _progress.value?.copy(status = TaskProgress.Status.Cancelled)
            } catch (e: Exception) {
                Log.e(TAG, "Task failed", e)
                _progress.value = _progress.value?.copy(
                    status = TaskProgress.Status.Failed,
                    finalMarkdown = "Task failed: ${e.message}",
                )
            } finally {
                tools.dispose()
                if (currentTools === tools) currentTools = null
            }
        }
        currentJob = job
        return job
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        currentTools?.dispose()
        currentTools = null
    }

    private fun appendStep(line: TaskProgress.StepLine) {
        val current = _progress.value ?: return
        _progress.value = current.copy(steps = current.steps + line)
    }

    private fun markFinished(markdown: String) {
        val current = _progress.value ?: return
        _progress.value = current.copy(
            status = TaskProgress.Status.Done,
            finalMarkdown = markdown,
        )
    }

    companion object {
        private const val TAG = "TaskRunner"
    }
}
