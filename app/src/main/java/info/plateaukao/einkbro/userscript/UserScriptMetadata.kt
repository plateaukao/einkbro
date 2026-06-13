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
    /** `@updateURL` — points at a (often metadata-only) doc used to check for a newer version. */
    val updateUrl: String,
    /** `@downloadURL` — points at the full script to fetch when an update is available. */
    val downloadUrl: String,
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
                updateUrl = first("updateurl"),
                downloadUrl = first("downloadurl"),
            )
        }

        /**
         * True if [remote] is a strictly newer version than [installed]. Versions are compared
         * segment-by-segment on `.` (so date-style "2024.10.07" and dotted "1.2.3" both work),
         * each segment by its leading integer then by any suffix — a pre-release suffix like
         * "-beta" sorts before the plain release. An empty [installed] makes any non-empty
         * [remote] newer; an empty [remote] is never newer (no version means nothing to update to).
         */
        fun isNewer(remote: String, installed: String): Boolean {
            if (remote.isBlank()) return false
            if (installed.isBlank()) return true
            return compareVersions(remote, installed) > 0
        }

        private fun compareVersions(a: String, b: String): Int {
            val pa = a.trim().split('.')
            val pb = b.trim().split('.')
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val sa = pa.getOrElse(i) { "0" }
                val sb = pb.getOrElse(i) { "0" }
                val na = sa.takeWhile { it.isDigit() }.toLongOrNull() ?: 0L
                val nb = sb.takeWhile { it.isDigit() }.toLongOrNull() ?: 0L
                if (na != nb) return na.compareTo(nb)
                val ra = sa.dropWhile { it.isDigit() }
                val rb = sb.dropWhile { it.isDigit() }
                if (ra != rb) {
                    // A plain release (empty suffix) outranks any pre-release suffix.
                    if (ra.isEmpty()) return 1
                    if (rb.isEmpty()) return -1
                    return ra.compareTo(rb)
                }
            }
            return 0
        }
    }
}

enum class RunAt { DOCUMENT_START, DOCUMENT_END }
