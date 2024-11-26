package io.github.edsuns.adblockclient

/**
 * Created by Edsuns@qq.com on 2021/1/23.
 */
data class MatchResult(
    /**
     * true if has matched rule and no matched exception rule
     */
    val shouldBlock: Boolean,

    /**
     * rule text is "-" if loading processed data without preserveRules enabled
     */
    val matchedRule: String?,

    /**
     * rule text is "-" if loading processed data without preserveRules enabled
     */
    val matchedExceptionRule: String?
)

val MatchResult.hasException: Boolean get() = matchedExceptionRule != null
