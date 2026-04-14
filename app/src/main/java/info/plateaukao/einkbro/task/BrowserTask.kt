package info.plateaukao.einkbro.task

/**
 * A multi-step browser workflow that uses [BrowserTools] to read pages, call the LLM,
 * and emit progress lines. Built-in templates implement this interface directly; the
 * free-form agent implements it by feeding the same tool surface to an LLM agent loop.
 */
interface BrowserTask {
    val id: String
    val displayName: String
    suspend fun run(tools: BrowserTools)
}
