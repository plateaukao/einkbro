package info.plateaukao.einkbro.view.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import info.plateaukao.einkbro.R

/**
 * Shared chrome for the config/list activities: themed Scaffold with a
 * TopAppBar, back arrow, and optional actions. Replaces ~10 near-identical
 * copies with three different back-navigation styles and inconsistent icon
 * tinting.
 */
@Composable
fun ListScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    MyTheme {
        SystemBarIconsForBlackTopBar()
        Scaffold(
            modifier = Modifier.scaffoldEdgeToEdgePadding(),
            topBar = {
                TopAppBar(
                    title = { Text(title, color = MaterialTheme.colors.onPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                tint = MaterialTheme.colors.onPrimary,
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = actions,
                    windowInsets = AppBarDefaults.topAppBarWindowInsets,
                )
            },
            content = content,
        )
    }
}

/**
 * Edge-to-edge insets for a Scaffold whose TopAppBar is given
 * AppBarDefaults.topAppBarWindowInsets: the body stays clear of the navigation
 * bar while the app bar background extends behind the status bar. A no-op
 * before Android 15, where the decor still consumes these insets.
 */
@Composable
fun Modifier.scaffoldEdgeToEdgePadding(): Modifier = windowInsetsPadding(
    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
)

/** Centered placeholder for empty lists (was copy-pasted in four activities). */
@Composable
fun EmptyListPlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
        )
    }
}
