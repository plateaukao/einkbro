package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.view.compose.EmptyListPlaceholder
import info.plateaukao.einkbro.view.compose.ListScaffold
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.compose.HorizontalSeparator
import info.plateaukao.einkbro.view.compose.onTopBar

/**
 * Every stored site rule, grouped by host, so per-site settings can be
 * reviewed, edited or removed without visiting each site. Tapping a row opens
 * the regular site-settings editor for that rule.
 */
class SiteRuleListActivity : LocaleAwareComponentActivity() {

    private val dialogManager: DialogManager by lazy { DialogManager(this) }
    private var rules by mutableStateOf<List<DomainConfigurationData>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ListScaffold(
                title = stringResource(R.string.setting_title_site_rules),
                onBack = { finish() },
                actions = {
                    if (rules.isNotEmpty()) {
                        IconButton(onClick = ::confirmDeleteAll) {
                            Icon(
                                tint = MaterialTheme.colors.onTopBar,
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.menu_delete),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                SiteRuleList(
                    modifier = Modifier.padding(innerPadding),
                    rules = rules,
                    onEdit = { rule ->
                        startActivity(SiteSettingsActivity.createIntent(this, "https://${rule.domain}"))
                    },
                    onDelete = ::confirmDelete,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        rules = localeAwareConfig.domain.allRules()
    }

    private fun confirmDelete(rule: DomainConfigurationData) {
        dialogManager.showOkCancelDialog(
            message = getString(R.string.site_rules_delete_confirm, rule.domain),
            okAction = {
                localeAwareConfig.domain.deleteRule(rule.domain)
                reload()
            },
        )
    }

    private fun confirmDeleteAll() {
        dialogManager.showOkCancelDialog(
            messageResId = R.string.site_rules_delete_all_confirm,
            okAction = {
                rules.forEach { localeAwareConfig.domain.deleteRule(it.domain) }
                reload()
            },
        )
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, SiteRuleListActivity::class.java)
    }
}

@Composable
private fun SiteRuleList(
    modifier: Modifier,
    rules: List<DomainConfigurationData>,
    onEdit: (DomainConfigurationData) -> Unit,
    onDelete: (DomainConfigurationData) -> Unit,
) {
    if (rules.isEmpty()) {
        EmptyListPlaceholder(
            stringResource(R.string.list_empty) + "\n" + stringResource(R.string.site_rules_empty_hint)
        )
        return
    }

    // Keep host order, host rule first, then its path rules (allRules() is
    // already sorted that way); a divider separates hosts.
    val hosts = rules.map { it.host }.distinct()
    LazyColumn(modifier = modifier) {
        hosts.forEachIndexed { hostIndex, host ->
            val hostRules = rules.filter { it.host == host }
            if (hostIndex > 0) item(key = "sep-$host") { HorizontalSeparator() }
            items(hostRules.size, key = { hostRules[it].domain }) { index ->
                SiteRuleRow(rule = hostRules[index], onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun SiteRuleRow(
    rule: DomainConfigurationData,
    onEdit: (DomainConfigurationData) -> Unit,
    onDelete: (DomainConfigurationData) -> Unit,
) {
    val color = MaterialTheme.colors.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(rule) }
            .padding(start = if (rule.isHostRule) 16.dp else 32.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(
                text = if (rule.isHostRule) rule.host else rule.path,
                fontSize = 16.sp,
                fontWeight = if (rule.isHostRule) FontWeight.Bold else FontWeight.Normal,
                color = color,
            )
            Text(
                text = overrideSummary(rule),
                fontSize = 12.sp,
                color = color.copy(alpha = 0.6f),
            )
        }
        OverrideCountBadge(rule.overrideCount)
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { onDelete(rule) }) {
            Icon(
                tint = color,
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.menu_delete),
            )
        }
    }
}

/** Which settings a rule touches, e.g. "Font size, Desktop mode, Custom CSS". */
@Composable
private fun overrideSummary(rule: DomainConfigurationData): String {
    val parts = buildList {
        if (rule.fontSize != null) add(stringResource(R.string.font_size))
        if (rule.fontType != null) add(stringResource(R.string.font_type))
        if (rule.boldFontStyle != null) add(stringResource(R.string.bold_font))
        if (rule.fontBoldness != null) add(stringResource(R.string.bold_font))
        if (rule.blackFontStyle != null) add(stringResource(R.string.black_font))
        if (rule.shouldUseWhiteBackground != null) add(stringResource(R.string.white_background))
        if (rule.shouldInvertColor != null) add(stringResource(R.string.menu_invert_color))
        if (rule.desktopMode != null) add(stringResource(R.string.desktop_mode))
        if (rule.desktopViewportWidth != null) add(stringResource(R.string.site_force_viewport_width))
        if (rule.enableJavascript != null) add(stringResource(R.string.setting_title_javascript))
        if (rule.enableAdBlock != null) add(stringResource(R.string.setting_title_adblock))
        if (rule.enableCookies != null) add(stringResource(R.string.setting_title_cookie))
        if (rule.shouldTranslateSite != null) add(stringResource(R.string.action_category_translation))
        if (rule.translationMode != null) add(stringResource(R.string.translation_mode))
        if (!rule.customCss.isNullOrBlank()) add(stringResource(R.string.site_custom_css))
        if (!rule.postLoadJavascript.isNullOrBlank()) add(stringResource(R.string.site_post_load_js))
    }.distinct()
    return if (parts.isEmpty()) stringResource(R.string.site_rules_no_overrides) else parts.joinToString(", ")
}

@Composable
private fun OverrideCountBadge(count: Int) {
    if (count == 0) return
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
                shape = CircleShape,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackground,
        )
    }
}
