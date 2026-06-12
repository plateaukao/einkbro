package info.plateaukao.einkbro.userscript

/**
 * Parsed `// ==UserScript== ... // ==/UserScript==` metadata block.
 */
data class UserScriptMetadata(
    val name: String,
    val namespace: String,
    val version: String,
    val description: String,
    val matches: List<String>,
    val includes: List<String>,
    val excludes: List<String>,
    val grants: List<String>,
    val requires: List<String>,
    val connects: List<String>,
    val runAt: RunAt,
    val noFrames: Boolean,
) {
    companion object {
        /** Fallback name used when a script has no `@name` metadata. */
        const val DEFAULT_NAME = "Unnamed script"

        private val BLOCK_REGEX =
            Regex("""//\s*==UserScript==\s*\n(.*?)//\s*==/UserScript==""", RegexOption.DOT_MATCHES_ALL)
        private val LINE_REGEX = Regex("""//\s*@([\w-]+)\s+(.*)""")

        fun parse(code: String): UserScriptMetadata {
            val block = BLOCK_REGEX.find(code)?.groupValues?.get(1).orEmpty()
            val tags = mutableMapOf<String, MutableList<String>>()
            block.lineSequence().forEach { line ->
                val m = LINE_REGEX.find(line) ?: return@forEach
                val key = m.groupValues[1].lowercase()
                val value = m.groupValues[2].trim()
                if (value.isNotEmpty()) tags.getOrPut(key) { mutableListOf() }.add(value)
            }

            fun first(vararg keys: String): String =
                keys.firstNotNullOfOrNull { tags[it]?.firstOrNull() }.orEmpty()

            fun all(vararg keys: String): List<String> =
                keys.flatMap { tags[it].orEmpty() }

            val runAt = when (first("run-at").lowercase()) {
                "document-start" -> RunAt.DOCUMENT_START
                "document-body" -> RunAt.DOCUMENT_START
                "document-idle" -> RunAt.DOCUMENT_END
                else -> RunAt.DOCUMENT_END // document-end is the Tampermonkey default
            }

            return UserScriptMetadata(
                name = first("name").ifEmpty { DEFAULT_NAME },
                namespace = first("namespace"),
                version = first("version"),
                description = first("description"),
                matches = all("match"),
                includes = all("include"),
                excludes = all("exclude", "exclude-match"),
                grants = all("grant"),
                requires = all("require"),
                connects = all("connect"),
                runAt = runAt,
                noFrames = tags.containsKey("noframes"),
            )
        }
    }
}

enum class RunAt { DOCUMENT_START, DOCUMENT_END }
