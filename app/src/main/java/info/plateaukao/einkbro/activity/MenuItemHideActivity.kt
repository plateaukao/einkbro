package info.plateaukao.einkbro.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.LocalMenuHideConfig
import info.plateaukao.einkbro.view.dialog.compose.MenuHideConfig
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType
import info.plateaukao.einkbro.view.dialog.compose.MenuItems
import org.koin.android.ext.android.inject

class MenuItemHideActivity : ComponentActivity() {
    private val config: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyTheme {
                var hidden by remember {
                    mutableStateOf(
                        config.ui.hiddenMenuItems.mapNotNull { name ->
                            runCatching { MenuItemType.valueOf(name) }.getOrNull()
                        }.toSet()
                    )
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.setting_title_hide_menu_items)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            },
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        CompositionLocalProvider(
                            LocalMenuHideConfig provides MenuHideConfig(
                                hideMode = true,
                                hiddenItems = hidden,
                                onToggleHide = { type ->
                                    hidden = if (type in hidden) hidden - type else hidden + type
                                    config.ui.hiddenMenuItems = hidden.map { it.name }.toSet()
                                },
                            )
                        ) {
                            MenuItems(
                                hasWhiteBkd = false,
                                boldFont = false,
                                blackFont = false,
                                isSpeaking = false,
                                isAudioOnly = false,
                                // include video-only items so the user can hide them too
                                hasVideo = true,
                                showShareSaveMenu = true,
                                showContentMenu = true,
                                hasInvertedColor = false,
                                isTouchPaginationEnabled = false,
                                toggleShareSaveMenu = {},
                                toggleContentMenu = {},
                                onClicked = {},
                                onLongClicked = {},
                            )
                        }
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        if (config.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(LocaleManager.setLocale(newBase, config.uiLocaleLanguage))
        } else {
            super.attachBaseContext(newBase)
        }
    }
}
