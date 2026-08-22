package io.github.edsuns.adblockclient

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.regex.PatternSyntaxException

/**
 * Kotlin-side matcher for ABP regex rules (`/pattern/$options` and
 * `@@/pattern/$options`).
 *
 * The native engine is built without `ENABLE_REGEX` because `std::regex` alone
 * costs ~250 KB per ABI in the statically linked libc++. Regex rules are rare
 * (AdGuard Base has ~200 of 150k lines), so they are handled here with
 * `java.util.regex`, which is already in the platform and costs nothing.
 *
 * Option semantics mirror `Filter::matchesOptions` in `filter.cc`:
 * - a rule with any unsupported option never matches;
 * - with a known resource type, a rule that names resource types must name this
 *   one, and a `~type` that names this one excludes the rule;
 * - with [ResourceType.UNKNOWN], any rule naming a resource type is skipped;
 * - `domain=` lists are matched against the first-party domain's label suffixes
 *   (`~domain` entries win over the rule applying by default);
 * - `third-party`/`~third-party` are checked against the request host.
 *
 * Every rule keeps a required literal substring extracted from its pattern
 * (see [requiredLiteral]) so the regex itself only runs for URLs that contain
 * that literal — most requests are rejected by an `indexOf`.
 */
class RegexFilterSet private constructor(
    private val blockRules: List<RegexRule>,
    private val exceptionRules: List<RegexRule>,
) {

    val size: Int get() = blockRules.size + exceptionRules.size

    fun isEmpty(): Boolean = blockRules.isEmpty() && exceptionRules.isEmpty()

    /**
     * Folds the regex rules into [native], the result from the native engine,
     * using the same precedence the engine uses internally: any matching
     * exception rule wins, otherwise any matching block rule blocks.
     */
    fun combine(
        native: MatchResult,
        url: String,
        firstPartyDomain: String,
        resourceType: ResourceType,
    ): MatchResult {
        if (isEmpty() || native.hasException || !isBlockableProtocol(url)) return native
        val context = MatchContext(url, firstPartyDomain, resourceType)
        val exception = firstMatch(exceptionRules, context)
        if (exception != null) {
            return MatchResult(false, native.matchedRule, exception.ruleText)
        }
        if (native.shouldBlock) return native
        val block = firstMatch(blockRules, context) ?: return native
        return MatchResult(true, block.ruleText, null)
    }

    private fun firstMatch(rules: List<RegexRule>, context: MatchContext): RegexRule? {
        for (rule in rules) {
            if (rule.matches(context)) return rule
        }
        return null
    }

    fun serialize(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(FORMAT_VERSION)
            out.writeInt(blockRules.size + exceptionRules.size)
            for (rule in blockRules) out.writeUTF(rule.ruleText)
            for (rule in exceptionRules) out.writeUTF(rule.ruleText)
        }
        return bytes.toByteArray()
    }

    companion object {
        private const val FORMAT_VERSION = 1

        /** Trailer appended after the regex section: section length + magic. */
        private const val TRAILER_SIZE = 8
        private val MAGIC = byteArrayOf('E'.code.toByte(), 'B'.code.toByte(), 'R'.code.toByte(), 'X'.code.toByte())

        val EMPTY = RegexFilterSet(emptyList(), emptyList())

        /** Collects the regex rules out of a raw filter list. */
        fun parse(rawList: CharSequence): RegexFilterSet {
            val block = ArrayList<RegexRule>()
            val exception = ArrayList<RegexRule>()
            for (line in rawList.lineSequence()) {
                val rule = RegexRule.parse(line.trim()) ?: continue
                if (rule.isException) exception.add(rule) else block.add(rule)
            }
            return if (block.isEmpty() && exception.isEmpty()) EMPTY else RegexFilterSet(block, exception)
        }

        /**
         * Appends [regexSet] to the native engine's processed data so that
         * [FilterDataLoader]'s single-blob storage keeps working. Blobs written
         * before this format existed have no trailer and load as "no regex rules".
         */
        fun pack(nativeData: ByteArray, regexSet: RegexFilterSet): ByteArray {
            if (regexSet.isEmpty()) return nativeData
            val section = regexSet.serialize()
            val out = ByteArrayOutputStream(nativeData.size + section.size + TRAILER_SIZE)
            out.write(nativeData)
            out.write(section)
            DataOutputStream(out).apply {
                writeInt(section.size)
                write(MAGIC)
            }
            return out.toByteArray()
        }

        /** Splits a blob written by [pack] into the native length and the regex set. */
        fun unpack(data: ByteArray): Unpacked {
            if (data.size < TRAILER_SIZE) return Unpacked(data.size, EMPTY)
            val magicStart = data.size - MAGIC.size
            for (i in MAGIC.indices) {
                if (data[magicStart + i] != MAGIC[i]) return Unpacked(data.size, EMPTY)
            }
            val lenStart = magicStart - 4
            val sectionLen = ((data[lenStart].toInt() and 0xff) shl 24) or
                ((data[lenStart + 1].toInt() and 0xff) shl 16) or
                ((data[lenStart + 2].toInt() and 0xff) shl 8) or
                (data[lenStart + 3].toInt() and 0xff)
            val nativeLen = lenStart - sectionLen
            if (sectionLen < 8 || nativeLen < 0) return Unpacked(data.size, EMPTY)
            val set = try {
                deserialize(data, nativeLen, sectionLen)
            } catch (e: Exception) {
                // A corrupt trailer most likely means this is a plain native blob
                // that happens to end with the magic; treat it as such.
                return Unpacked(data.size, EMPTY)
            }
            return Unpacked(nativeLen, set)
        }

        private fun deserialize(data: ByteArray, offset: Int, length: Int): RegexFilterSet {
            val input = DataInputStream(data.inputStream(offset, length))
            val version = input.readInt()
            require(version == FORMAT_VERSION) { "Unknown regex section version $version" }
            val count = input.readInt()
            require(count in 0..length) { "Bad regex rule count $count" }
            val block = ArrayList<RegexRule>()
            val exception = ArrayList<RegexRule>()
            repeat(count) {
                val rule = RegexRule.parse(input.readUTF()) ?: return@repeat
                if (rule.isException) exception.add(rule) else block.add(rule)
            }
            return if (block.isEmpty() && exception.isEmpty()) EMPTY else RegexFilterSet(block, exception)
        }

        /** Mirrors `isBlockableProtocol` in `protocol.cc`: http(s), ws(s) and blob: of those. */
        internal fun isBlockableProtocol(url: String): Boolean {
            var start = 0
            if (url.regionMatches(0, "blob:", 0, 5, ignoreCase = true)) start = 5
            return url.regionMatches(start, "http", 0, 4, ignoreCase = true) ||
                url.regionMatches(start, "ws", 0, 2, ignoreCase = true)
        }
    }

    class Unpacked(val nativeLength: Int, val regexSet: RegexFilterSet)
}

/** Per-request context, computed once and shared by every rule check. */
internal class MatchContext(
    val url: String,
    val firstPartyDomain: String,
    resourceType: ResourceType,
) {
    val resourceOption: Int = resourceType.filterOption
    val isThirdParty: Boolean = isThirdPartyHost(firstPartyDomain, urlHost(url))
    val lowerUrl: String by lazy(LazyThreadSafetyMode.NONE) { url.lowercase() }

    /**
     * Bit set of every character bigram in [lowerUrl]; the same trick the native
     * engine's per-input bloom filter uses. A rule whose literal has a bigram
     * missing here cannot match, which rejects nearly every rule in a couple
     * of bit probes instead of an `indexOf` each.
     */
    val bigrams: LongArray by lazy(LazyThreadSafetyMode.NONE) {
        val bits = LongArray(BIGRAM_BITS / 64)
        val s = lowerUrl
        for (i in 1 until s.length) {
            val h = bigramHash(s[i - 1], s[i])
            bits[h ushr 6] = bits[h ushr 6] or (1L shl (h and 63))
        }
        bits
    }

    fun hasBigram(hash: Int): Boolean = (bigrams[hash ushr 6] and (1L shl (hash and 63))) != 0L

    companion object {
        private const val BIGRAM_BITS = 4096

        internal fun bigramHash(a: Char, b: Char): Int = (a.code * 31 + b.code) and (BIGRAM_BITS - 1)

        internal fun bigramHashes(literal: String): IntArray =
            IntArray(maxOf(0, literal.length - 1)) { bigramHash(literal[it], literal[it + 1]) }

        /** Mirrors `getUrlHost` in `ad_block_client.cc`. */
        internal fun urlHost(url: String): String {
            var p = url.indexOf(':')
            p = if (p < 0) 0 else p + 1
            while (p < url.length && url[p] == '/') p++
            var q = p
            while (q < url.length && !isSeparatorChar(url[q])) q++
            return url.substring(p, q)
        }

        /** Mirrors `isSeparatorChar` in `ad_block_client.cc`. */
        private fun isSeparatorChar(c: Char): Boolean =
            c == '$' || c == '/' || c == ':' || c == '=' || c == '?' || c == '^'

        /** Mirrors `isThirdPartyHost` in `filter.cc`. */
        internal fun isThirdPartyHost(baseContextHost: String, testHost: String): Boolean {
            if (!testHost.endsWith(baseContextHost)) return true
            if (testHost.length == baseContextHost.length) return false
            return testHost[testHost.length - baseContextHost.length - 1] != '.'
        }
    }
}

internal class RegexRule private constructor(
    val ruleText: String,
    val isException: Boolean,
    private val pattern: String,
    private val matchCase: Boolean,
    /** Resource types named by `$type` options (bit set of [FilterOption]). */
    private val typeOptions: Int,
    /** Resource types named by `$~type` options. */
    private val antiTypeOptions: Int,
    private val thirdParty: Boolean,
    private val notThirdParty: Boolean,
    private val domains: List<String>,
    private val antiDomains: List<String>,
) {
    /** Substrings every matching URL must contain (longest first); empty when none can be proven. */
    private val literals: Array<String> = requiredLiterals(pattern)
        .map { if (matchCase) it else it.lowercase() }
        .toTypedArray()

    /** Bigrams of each case-folded literal, probed against [MatchContext.bigrams] first. */
    private val literalBigrams: Array<IntArray> = Array(literals.size) { MatchContext.bigramHashes(literals[it].lowercase()) }

    /**
     * A pattern that starts with `^` and has no alternation can only match at
     * offset 0, so it is tested with [Regex.matchesAt] instead of scanning
     * every start offset the way [Regex.containsMatchIn] does.
     */
    private val anchoredAtStart: Boolean = pattern.startsWith("^") && '|' !in pattern

    private val regex: Regex? by lazy {
        try {
            Regex(pattern, if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE))
        } catch (e: PatternSyntaxException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun matches(context: MatchContext): Boolean {
        if (!matchesOptions(context)) return false
        for (i in literals.indices) {
            for (hash in literalBigrams[i]) {
                if (!context.hasBigram(hash)) return false
            }
            val haystack = if (matchCase) context.url else context.lowerUrl
            if (!haystack.contains(literals[i])) return false
        }
        val regex = regex ?: return false
        return if (anchoredAtStart) regex.matchesAt(context.url, 0) else regex.containsMatchIn(context.url)
    }

    private fun matchesOptions(context: MatchContext): Boolean {
        val resource = context.resourceOption
        if (resource != FilterOption.NONE) {
            if (typeOptions != 0 && (typeOptions and resource) == 0) return false
            if ((antiTypeOptions and resource) != 0) return false
        } else {
            // Unknown resource type: never match rules that name one.
            if (typeOptions != 0 || antiTypeOptions != 0) return false
        }
        if (thirdParty && !context.isThirdParty) return false
        if (notThirdParty && context.isThirdParty) return false
        return contextDomainMatches(context.firstPartyDomain)
    }

    /** Mirrors `Filter::contextDomainMatchesFilter` in `filter.cc`, plus AdGuard `domain.*`. */
    private fun contextDomainMatches(contextDomain: String): Boolean {
        if (domains.isEmpty() && antiDomains.isEmpty()) return true
        var start = 0
        while (true) {
            val dot = contextDomain.indexOf('.', start)
            if (dot < 0) break
            for (entry in domains) if (domainEntryMatches(entry, contextDomain, start)) return true
            for (entry in antiDomains) if (domainEntryMatches(entry, contextDomain, start)) return false
            start = dot + 1
        }
        // Only anti-domains, none of which matched: the rule applies.
        return domains.isEmpty()
    }

    /** Allocation-free: does [entry] match the suffix of [host] starting at [start]? */
    private fun domainEntryMatches(entry: String, host: String, start: Int): Boolean {
        val suffixLen = host.length - start
        if (entry.endsWith(".*")) {
            // "site.*" matches "site." followed by any TLD, e.g. site.com or site.co.uk
            val prefixLen = entry.length - 1
            return suffixLen > prefixLen && host.regionMatches(start, entry, 0, prefixLen)
        }
        return suffixLen == entry.length && host.regionMatches(start, entry, 0, entry.length)
    }

    companion object {
        /**
         * Parses one filter-list line; returns null unless it is a regex rule
         * whose options the engine would consider (mirrors `Filter::parseOption`:
         * an unknown option disables the whole rule).
         */
        fun parse(line: String): RegexRule? {
            if (line.isEmpty() || line[0] == '!') return null
            val isException = line.startsWith("@@")
            val body = if (isException) line.substring(2) else line
            if (body.length < 3 || body[0] != '/') return null

            // "/pattern/$options": the options tail may itself end in '/'
            // (e.g. header=set-cookie:/^x=/), so prefer a "/$" split whose
            // tail looks like an option list over "the line ends in '/'".
            val pattern: String
            val options: String?
            val sep = body.lastIndexOf("/$")
            if (sep >= 1 && looksLikeOptions(body.substring(sep + 2))) {
                pattern = body.substring(1, sep)
                options = body.substring(sep + 2)
            } else if (body.endsWith("/")) {
                pattern = body.substring(1, body.length - 1)
                options = null
            } else {
                return null
            }
            if (pattern.isEmpty()) return null

            var matchCase = false
            var typeOptions = 0
            var antiTypeOptions = 0
            var thirdParty = false
            var notThirdParty = false
            var domains: List<String> = emptyList()
            var antiDomains: List<String> = emptyList()

            if (options != null) {
                for (raw in options.split(',')) {
                    var option = raw.trim()
                    if (option.isEmpty()) continue
                    val negated = option[0] == '~'
                    if (negated) option = option.substring(1)
                    when {
                        option.startsWith("domain=") -> {
                            val all = option.substring(7).split('|').filter { it.isNotEmpty() }
                            domains = all.filter { !it.startsWith("~") }
                            antiDomains = all.filter { it.startsWith("~") }.map { it.substring(1) }
                        }
                        option == "third-party" || option == "3p" ->
                            if (negated) notThirdParty = true else thirdParty = true
                        option == "first-party" || option == "1p" ->
                            if (negated) thirdParty = true else notThirdParty = true
                        option == "match-case" -> matchCase = true
                        // Behavioral only in the engine (FORedirect): still a blocking match.
                        option.startsWith("redirect=") -> Unit
                        else -> {
                            val bit = FilterOption.resourceBit(option)
                            when {
                                bit != 0 -> if (negated) antiTypeOptions = antiTypeOptions or bit
                                else typeOptions = typeOptions or bit
                                option in FilterOption.IGNORED -> Unit
                                else -> return null // unsupported option: engine skips the rule
                            }
                        }
                    }
                }
            }
            return RegexRule(
                ruleText = line,
                isException = isException,
                pattern = pattern,
                matchCase = matchCase,
                typeOptions = typeOptions,
                antiTypeOptions = antiTypeOptions,
                thirdParty = thirdParty,
                notThirdParty = notThirdParty,
                domains = domains,
                antiDomains = antiDomains,
            )
        }

        /** Longest of [requiredLiterals], or null when none is provable. */
        internal fun requiredLiteral(pattern: String): String? =
            requiredLiterals(pattern).firstOrNull()

        /**
         * Runs of literal characters that every match of [pattern] must
         * contain, longest first; empty when none is provable (e.g. top-level
         * alternation). Only top-level (ungrouped) runs count; a trailing `?`,
         * `*` or `{n,m}` makes the run's last character optional, so it is dropped.
         */
        internal fun requiredLiterals(pattern: String): List<String> {
            val runs = ArrayList<String>()
            val current = StringBuilder()
            fun flush(dropLast: Boolean) {
                if (dropLast && current.isNotEmpty()) current.setLength(current.length - 1)
                if (current.length >= MIN_LITERAL) runs.add(current.toString())
                current.setLength(0)
            }
            var depth = 0
            var inClass = false
            var i = 0
            while (i < pattern.length) {
                val c = pattern[i]
                when {
                    inClass -> when (c) {
                        '\\' -> i++
                        ']' -> inClass = false
                    }
                    c == '\\' -> {
                        val next = pattern.getOrNull(i + 1) ?: break
                        if (next.isLetterOrDigit()) flush(false) // \d \w \s \b \1 \x..: not a literal
                        else if (depth == 0) current.append(next)
                        i++
                    }
                    c == '[' -> { flush(false); inClass = true }
                    c == '(' -> { flush(false); depth++ }
                    c == ')' -> { flush(false); depth-- }
                    c == '|' -> { if (depth == 0) return emptyList(); flush(false) }
                    c == '?' || c == '*' -> flush(true)
                    c == '{' -> {
                        flush(true)
                        val close = pattern.indexOf('}', i)
                        if (close > 0) i = close
                    }
                    c == '+' || c == '.' || c == '^' || c == '$' -> flush(false)
                    else -> if (depth == 0) current.append(c)
                }
                i++
            }
            flush(false)
            runs.sortByDescending { it.length }
            return runs
        }

        private const val MIN_LITERAL = 3

        private val OPTION_TOKEN = Regex("~?[A-Za-z0-9_-]+(=.*)?")

        private fun looksLikeOptions(tail: String): Boolean =
            tail.isNotEmpty() && tail.split(',').all { OPTION_TOKEN.matches(it.trim()) }
    }
}

/** Subset of `FilterOption` from `filter.h` needed for regex rules. */
internal object FilterOption {
    const val NONE = 0
    private const val SCRIPT = 0x1
    private const val IMAGE = 0x2
    private const val STYLESHEET = 0x4
    private const val OBJECT = 0x8
    private const val XMLHTTPREQUEST = 0x10
    private const val OBJECT_SUBREQUEST = 0x20
    private const val SUBDOCUMENT = 0x40
    private const val DOCUMENT = 0x80
    private const val OTHER = 0x100
    private const val XBL = 0x200
    private const val PING = 0x8000
    private const val FONT = 0x80000
    private const val MEDIA = 0x100000
    private const val WEBRTC = 0x200000
    private const val WEBSOCKET = 0x2000000

    /** Options the engine parses but that never affect matching here. */
    val IGNORED = setOf("collapse", "donottrack", "important", "explicitcancel")

    fun resourceBit(option: String): Int = when (option) {
        "script" -> SCRIPT
        "image" -> IMAGE
        "stylesheet" -> STYLESHEET
        "object" -> OBJECT
        "xmlhttprequest" -> XMLHTTPREQUEST
        "object-subrequest" -> OBJECT_SUBREQUEST
        "subdocument" -> SUBDOCUMENT
        "document" -> DOCUMENT
        "other" -> OTHER
        "xbl" -> XBL
        "ping" -> PING
        "font" -> FONT
        "media" -> MEDIA
        "webrtc" -> WEBRTC
        "websocket" -> WEBSOCKET
        else -> 0
    }
}
