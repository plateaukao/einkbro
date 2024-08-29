package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute.About
import info.plateaukao.einkbro.activity.SettingRoute.Backup
import info.plateaukao.einkbro.activity.SettingRoute.Behavior
import info.plateaukao.einkbro.activity.SettingRoute.ChatGPT
import info.plateaukao.einkbro.activity.SettingRoute.DataControl
import info.plateaukao.einkbro.activity.SettingRoute.Gesture
import info.plateaukao.einkbro.activity.SettingRoute.Main
import info.plateaukao.einkbro.activity.SettingRoute.Misc
import info.plateaukao.einkbro.activity.SettingRoute.Search
import info.plateaukao.einkbro.activity.SettingRoute.StartControl
import info.plateaukao.einkbro.activity.SettingRoute.Toolbar
import info.plateaukao.einkbro.activity.SettingRoute.Ui
import info.plateaukao.einkbro.activity.SettingRoute.UserAgent
import info.plateaukao.einkbro.activity.SettingRoute.valueOf
import info.plateaukao.einkbro.browser.AdBlockV2
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.LinkSettingItem
import info.plateaukao.einkbro.setting.ListSettingWithEnumItem
import info.plateaukao.einkbro.setting.ListSettingWithStringItem
import info.plateaukao.einkbro.setting.NavigateSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.SettingScreen
import info.plateaukao.einkbro.setting.ValueSettingItem
import info.plateaukao.einkbro.setting.VersionSettingItem
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.view.GestureType
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.PrinterDocumentPaperSizeDialog
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingActivity : ComponentActivity(), KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(this) }
    private val adBlock: AdBlockV2 by inject()
    private val backupUnit: BackupUnit by lazy { BackupUnit(this) }

    private lateinit var openBookmarkFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var createBookmarkFileLauncher: ActivityResultLauncher<Intent>

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openBookmarkFileLauncher = backupUnit.createOpenBookmarkFileLauncher(this)
        createBookmarkFileLauncher = backupUnit.createCreateBookmarkFileLauncher(this)

        setContent {
            val navController: NavHostController = rememberNavController()
            MyTheme {

                val backStackEntry = navController.currentBackStackEntryAsState()
                val currentScreen = valueOf(backStackEntry.value?.destination?.route ?: Main.name)

                Scaffold(
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                    },
                    topBar = {
                        SettingBar(
                            currentScreen = currentScreen,
                            navigateUp = {
                                if (navController.previousBackStackEntry != null) navController.navigateUp()
                                else finish()
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Main.name,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { fadeIn(animationSpec = tween(1)) },
                        exitTransition = { fadeOut(animationSpec = tween(1)) },
                    ) {
                        val action = this@SettingActivity::handleLink
                        composable(Main.name) {
                            SettingScreen(navController, mainSettings, dialogManager, action, 2)
                        }
                        composable(Ui.name) {
                            SettingScreen(navController, uiSettingItems, dialogManager, action, 1)
                        }
                        composable(Toolbar.name) {
                            SettingScreen(
                                navController,
                                toolbarSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(Behavior.name) {
                            SettingScreen(
                                navController,
                                behaviorSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(Gesture.name) {
                            SettingScreen(
                                navController,
                                gestureSettingItems,
                                dialogManager,
                                action,
                                2
                            )
                        }
                        composable(Backup.name) {
                            SettingScreen(navController, dataSettingItems, dialogManager, action, 1)
                        }
                        composable(StartControl.name) {
                            SettingScreen(
                                navController,
                                startSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(DataControl.name) {
                            SettingScreen(
                                navController,
                                clearDataSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(UserAgent.name) {
                            SettingScreen(
                                navController,
                                userAgentSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(Misc.name) {
                            SettingScreen(
                                navController,
                                miscSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(ChatGPT.name) {
                            SettingScreen(
                                navController,
                                chatGptSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(Search.name) {
                            SettingScreen(
                                navController,
                                searchSettingItems,
                                dialogManager,
                                action,
                                1
                            )
                        }
                        composable(About.name) {
                            SettingScreen(
                                navController,
                                mutableListOf<SettingItemInterface>().apply {
                                    addAll(LinkSettingItem.values().toList())
                                    add(DividerSettingItem())
                                    if (BuildConfig.showUpdateButton) {
                                        add(ActionSettingItem(
                                            R.string.setting_title_github_update,
                                            R.drawable.ic_data,
                                        ) {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                HelperUnit.upgradeToLatestRelease(this@SettingActivity)
                                            }
                                        })
                                        add(ActionSettingItem(
                                            R.string.setting_title_github_snapshot,
                                            R.drawable.ic_data,
                                        ) {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                HelperUnit.upgradeFromSnapshot(this@SettingActivity)
                                            }
                                        })
                                    }
                                    add(DividerSettingItem())
                                },
                                dialogManager,
                                action,
                                2
                            )
                        }
                    }
                }
            }
        }

        if (config.hideStatusbar) {
            hideStatusBar()
        }
    }

    override fun onResume() {
        super.onResume()

        overridePendingTransition(0, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        when (requestCode) {
            DialogManager.EXPORT_BOOKMARKS_REQUEST_CODE -> backupUnit.exportBookmarks(uri)
            DialogManager.IMPORT_BOOKMARKS_REQUEST_CODE -> backupUnit.importBookmarks(uri)
            DialogManager.EXPORT_BACKUP_REQUEST_CODE -> backupUnit.backupData(this, uri)
            DialogManager.IMPORT_BACKUP_REQUEST_CODE -> if (backupUnit.restoreBackupData(
                    this,
                    uri
                )
            ) {
                dialogManager.showRestartConfirmDialog()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.setLocale(
                newBase,
                config.localeLanguage.languageCode
            )
        )
    }

    private fun handleLink(url: String) {
        startActivity(
            Intent(this, BrowserActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
            }
        )
        finish()
        overridePendingTransition(0, 0)
    }

    private val mainSettings = listOf(
        NavigateSettingItem(R.string.setting_title_ui, R.drawable.ic_phone, destination = Ui),
        NavigateSettingItem(
            R.string.setting_title_toolbar,
            R.drawable.ic_toolbar,
            destination = Toolbar
        ),
        NavigateSettingItem(
            R.string.setting_title_behavior,
            R.drawable.icon_ui,
            destination = Behavior
        ),
        NavigateSettingItem(
            R.string.setting_gestures,
            R.drawable.gesture_tap,
            destination = Gesture
        ),
        DividerSettingItem(),
        NavigateSettingItem(
            R.string.setting_title_data,
            R.drawable.icon_backup,
            destination = Backup
        ),
        NavigateSettingItem(
            R.string.setting_title_start_control,
            R.drawable.icon_earth,
            destination = StartControl
        ),
        NavigateSettingItem(
            R.string.setting_title_clear_control,
            R.drawable.ic_data,
            destination = DataControl
        ),
        NavigateSettingItem(
            R.string.setting_title_search,
            R.drawable.icon_search,
            destination = Search
        ),
        DividerSettingItem(),
        NavigateSettingItem(
            R.string.misc,
            R.drawable.icon_dots,
            destination = Misc
        ),
        NavigateSettingItem(
            R.string.setting_title_chat_gpt,
            R.drawable.ic_chat_gpt,
            destination = ChatGPT
        ),
        VersionSettingItem(
            R.string.menu_other_info,
            R.drawable.icon_info,
            destination = About,
            span = 2
        ),
    )

    private val uiSettingItems = listOf(
        BooleanSettingItem(
            R.string.hide_statusbar,
            R.drawable.ic_page_height,
            R.string.setting_summary_hide_statusbar,
            config::hideStatusbar,
        ),
        BooleanSettingItem(
            R.string.desktop_mode,
            R.drawable.icon_desktop,
            R.string.setting_summary_desktop,
            config::desktop,
        ),
        BooleanSettingItem(
            R.string.always_enable_zoom,
            R.drawable.ic_enable_zoom,
            R.string.setting_summary_enable_zoom,
            config::enableZoom,
        ),
        BooleanSettingItem(
            R.string.show_default_text_menu,
            R.drawable.ic_menu,
            R.string.setting_summary_show_default_text_menu,
            config::showDefaultActionMenu,
        ),
        BooleanSettingItem(
            R.string.show_context_menu_icons,
            R.drawable.ic_menu,
            R.string.setting_summary_show_context_menu_icons,
            config::showActionMenuIcons,
        ),
        DividerSettingItem(),
        ValueSettingItem(
            R.string.setting_title_page_left_value,
            R.drawable.ic_page_height,
            R.string.setting_summary_page_left_value,
            config::pageReservedOffsetInString
        ),
        ListSettingWithEnumItem(
            R.string.dark_mode,
            R.drawable.ic_dark_mode,
            R.string.setting_summary_dark_mode,
            config::darkMode,
            listOf(
                R.string.dark_mode_follow_system,
                R.string.dark_mode_force_on,
                R.string.dark_mode_disabled,
            )
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_nav_pos,
            R.drawable.icon_arrow_expand,
            R.string.setting_summary_nav_pos,
            config::fabPosition,
            listOf(
                R.string.setting_summary_nav_pos_right,
                R.string.setting_summary_nav_pos_left,
                R.string.setting_summary_nav_pos_center,
                R.string.setting_summary_nav_pos_not_show,
                R.string.setting_summary_nav_pos_custom,
            )
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_plus_behavior,
            R.drawable.icon_plus,
            R.string.setting_summary_plus_behavior,
            config::newTabBehavior,
            listOf(
                R.string.plus_start_input_url,
                R.string.plus_show_homepage,
                R.string.plus_show_bookmarks,
            )
        ),
        ActionSettingItem(
            R.string.setting_clear_recent_bookmarks,
            R.drawable.ic_bookmarks,
            R.string.setting_summary_clear_recent_bookmarks,
        ) {
            config.clearRecentBookmarks()
        },
        ActionSettingItem(
            R.string.setting_app_locale,
            R.drawable.icon_settings,
            R.string.setting_summary_app_locale,
        ) {
            lifecycleScope.launch {
                TranslationLanguageDialog(this@SettingActivity).showAppLocale()
                config.restartChanged = true
            }
        },
    )

    private val behaviorSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_saveTabs,
            R.drawable.icon_tab_plus,
            R.string.setting_summary_saveTabs,
            config::shouldSaveTabs,
        ),
        BooleanSettingItem(
            R.string.setting_title_background_loading,
            R.drawable.icon_tab_plus,
            R.string.setting_summary_background_loading,
            config::enableWebBkgndLoad,
        ),
        BooleanSettingItem(
            R.string.setting_title_next_tab,
            R.drawable.icon_tab_plus,
            R.string.setting_summary_next_tab,
            config::shouldShowNextAfterRemoveTab,
        ),
        BooleanSettingItem(
            R.string.settings_title_back_key_behavior,
            R.drawable.icon_arrow_left_gest,
            R.string.settings_summary_back_key_behavior,
            config::closeTabWhenNoMoreBackHistory,
        ),
        BooleanSettingItem(
            R.string.setting_title_trim_input_url,
            R.drawable.icon_edit,
            R.string.setting_summary_trim_input_url,
            config::shouldTrimInputUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_prune_query_parameter,
            R.drawable.ic_filter,
            R.string.setting_summary_prune_query_parameter,
            config::shouldPruneQueryParameters,
        ),
        BooleanSettingItem(
            R.string.setting_title_video_auto_fullscreen,
            R.drawable.ic_video,
            R.string.setting_summary_video_auto_fullscreen,
            config::enableVideoAutoFullscreen,
        ),
        BooleanSettingItem(
            R.string.setting_title_video_pip,
            R.drawable.ic_video,
            R.string.setting_summary_video_pip,
            config::enableVideoPip,
        ),
        BooleanSettingItem(
            R.string.setting_title_screen_awake,
            R.drawable.ic_eye,
            R.string.setting_summary_screen_awake,
            config::keepAwake,
        ),
        BooleanSettingItem(
            R.string.setting_title_confirm_tab_close,
            R.drawable.icon_close,
            R.string.setting_summary_confirm_tab_close,
            config::confirmTabClose,
        ),
        BooleanSettingItem(
            R.string.setting_title_vi_binding,
            R.drawable.ic_keyboard,
            R.string.setting_summary_vi_binding,
            config::enableViBinding,
        ),
        BooleanSettingItem(
            R.string.setting_title_useUpDown,
            R.drawable.ic_page_down,
            R.string.setting_summary_useUpDownKey,
            config::useUpDownPageTurn,
        ),
        BooleanSettingItem(
            R.string.setting_title_show_bookmarks_input_bar,
            R.drawable.icon_edit,
            R.string.setting_summary_show_bookmarks_input_bar,
            config::showBookmarksInInputBar,
        ),
        BooleanSettingItem(
            R.string.setting_title_enable_ssl_error_dialog,
            R.drawable.icon_settings,
            R.string.setting_summary_enable_ssl_error_dialog,
            config::enableCertificateErrorDialog,
        ),
    )

    private val toolbarSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_toolbar_top,
            R.drawable.ic_page_height,
            R.string.setting_summary_toolbar_top,
            config::isToolbarOnTop,
        ),
        BooleanSettingItem(
            R.string.setting_title_hideToolbar,
            R.drawable.icon_fullscreen,
            R.string.setting_summary_hide,
            config::shouldHideToolbar,
        ),
        BooleanSettingItem(
            R.string.setting_title_toolbarShow,
            R.drawable.icon_show,
            R.string.setting_summary_toolbarShow,
            config::showToolbarFirst,
        ),
        BooleanSettingItem(
            R.string.setting_title_show_tab_bar,
            R.drawable.icon_tab_plus,
            R.string.setting_summary_show_tab_bar,
            config::shouldShowTabBar,
        ),
    )

    private val gestureSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_multitouch_use_title,
            R.drawable.ic_touch_disabled,
            R.string.setting_multitouch_use_summary,
            config::isMultitouchEnabled,
            span = 2,
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_up,
            R.drawable.icon_arrow_up_gest,
            config = config::multitouchUp,
            options = GestureType.values().map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_down,
            R.drawable.icon_arrow_down_gest,
            config = config::multitouchDown,
            options = GestureType.values().map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_left,
            R.drawable.icon_arrow_left_gest,
            config = config::multitouchLeft,
            options = GestureType.values().map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_right,
            R.drawable.icon_arrow_right_gest,
            config = config::multitouchRight,
            options = GestureType.values().map { it.resId },
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_gestures_use_title,
            R.drawable.ic_touch_disabled,
            R.string.setting_gestures_use_summary,
            config::enableNavButtonGesture,
            span = 2,
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_up,
            R.drawable.icon_arrow_up_gest,
            config = config::navGestureUp,
            options = GestureType.values().map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_down,
            R.drawable.icon_arrow_down_gest,
            config = config::navGestureDown,
            options = GestureType.values().map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_left,
            R.drawable.icon_arrow_left_gest,
            config = config::navGestureLeft,
            options = GestureType.values().map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_right,
            R.drawable.icon_arrow_right_gest,
            config = config::navGestureRight,
            options = GestureType.values().map { it.resId },
        ),
    )

    private val searchSettingItems = listOf(
        ListSettingWithStringItem(
            R.string.setting_title_search_engine,
            R.drawable.icon_search,
            config = config::searchEngine,
            options = listOf(
                R.string.setting_summary_search_engine_startpage,
                R.string.setting_summary_search_engine_startpage_de,
                R.string.setting_summary_search_engine_baidu,
                R.string.setting_summary_search_engine_bing,
                R.string.setting_summary_search_engine_duckduckgo,
                R.string.setting_summary_search_engine_google,
                R.string.setting_summary_search_engine_searx,
                R.string.setting_summary_search_engine_qwant,
                R.string.setting_summary_search_engine_ecosia,
                R.string.setting_title_searchEngine,
                R.string.setting_summary_search_engine_yandex,
            )
        ),
        ValueSettingItem(
            R.string.setting_title_searchEngine,
            R.drawable.icon_edit,
            R.string.setting_summary_search_engine,
            config = config::searchEngineUrl,
        ),
        DividerSettingItem(),
        ValueSettingItem(
            R.string.setting_title_process_text,
            R.drawable.icon_edit,
            R.string.setting_summary_custom_process_text_url,
            config = config::processTextUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_external_search_pop,
            R.drawable.icon_search,
            R.string.setting_summary_external_search_pop,
            config::externalSearchWithPopUp,
        ),
        DividerSettingItem(),
        ActionSettingItem(
            R.string.setting_title_split_search_setting,
            R.drawable.icon_edit,
            R.string.setting_summary_split_search_setting
        ) {
            startActivity(DataListActivity.createIntent(this, WhiteListType.SplitSearch))
        },
        BooleanSettingItem(
            R.string.setting_title_search_in_same_tab,
            R.drawable.icon_search,
            R.string.setting_summary_search_in_same_tab,
            config::isExternalSearchInSameTab,
        ),
    )

    private val dataSettingItems = listOf(
        ActionSettingItem(
            R.string.setting_title_export_appData,
            R.drawable.icon_export,
            R.string.setting_summary_export_appData
        ) { dialogManager.showBackupFilePicker() },
        ActionSettingItem(
            R.string.setting_title_import_appData,
            R.drawable.icon_import,
            R.string.setting_summary_import_appData
        ) { dialogManager.showImportBackupFilePicker() },
        DividerSettingItem(),
        ActionSettingItem(
            R.string.setting_title_export_bookmarks,
            R.drawable.icon_bookmark,
        ) { dialogManager.showBookmarkFilePicker() },
        ActionSettingItem(
            R.string.setting_title_import_bookmarks,
            R.drawable.ic_bookmark,
        ) { dialogManager.showImportBookmarkFilePicker() },
        ActionSettingItem(
            R.string.setting_title_setup_bookmarks_location,
            R.drawable.icon_settings,
            R.string.setting_summary_setup_bookmarks_location,
        ) {
            dialogManager.showCreateOrOpenBookmarkFileDialog(
                { BrowserUnit.createBookmarkFilePicker(createBookmarkFileLauncher) },
                { BrowserUnit.openBookmarkFilePicker(openBookmarkFileLauncher) }
            )
        },
        ActionSettingItem(
            R.string.setting_title_sync_bookmarks,
            R.drawable.ic_sync,
        ) { handleBookmarkSync(true) },
    )

    private val clearDataSettingItems = listOf(
        BooleanSettingItem(
            R.string.clear_title_cache,
            R.drawable.ic_save_data,
            config = config::clearCache,
        ),
        BooleanSettingItem(
            R.string.clear_title_history,
            R.drawable.icon_history,
            config = config::clearHistory,
        ),
        BooleanSettingItem(
            R.string.clear_title_indexedDB,
            R.drawable.icon_delete,
            config = config::clearIndexedDB,
        ),
        BooleanSettingItem(
            R.string.clear_title_cookie,
            R.drawable.icon_cookie,
            R.string.setting_summary_cookie_delete,
            config::clearCookies
        ),
        BooleanSettingItem(
            R.string.clear_title_quit,
            R.drawable.icon_exit,
            R.string.clear_summary_quit,
            config::clearWhenQuit
        ),
        ActionSettingItem(
            R.string.clear_title_deleteDatabase,
            R.drawable.icon_delete,
            R.string.clear_summary_deleteDatabase,
        ) {
            deleteDatabase("Ninja4.db")
            deleteDatabase("pass_DB_v01.db")
            config.restartChanged = true
            finish()
        }
    )

    private val miscSettingItems = listOf(
        ListSettingWithEnumItem(
            R.string.setting_title_highlight_style,
            R.drawable.ic_highlight,
            R.string.setting_summary_highlight_style,
            config = config::highlightStyle,
            options = HighlightStyle.entries.filter { it != HighlightStyle.BACKGROUND_NONE }
                .map { it.stringResId },
        ),
        NavigateSettingItem(
            R.string.setting_title_userAgent,
            R.drawable.icon_useragent,
            destination = UserAgent
        ),
        ValueSettingItem(
            R.string.setting_title_edit_homepage,
            R.drawable.ic_home,
            config = config::favoriteUrl,
            showValue = false
        ),
        ActionSettingItem(R.string.setting_title_pdf_paper_size, R.drawable.ic_pdf) {
            PrinterDocumentPaperSizeDialog(
                this
            ).show()
        },
//        ValueSettingItem(
//            R.string.translate_papago,
//            R.drawable.ic_papago,
//            R.string.translate_papago_summary,
//            config = config::papagoApiSecret,
//            showValue = false
//        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_enable_inplace_translate,
            R.drawable.ic_translate,
            R.string.setting_summary_enable_inplace_translate,
            config::enableInplaceParagraphTranslate
        ),
        ValueSettingItem(
            R.string.setting_title_translated_langs,
            R.drawable.ic_translate,
            R.string.setting_summary_translated_langs,
            config::preferredTranslateLanguageString
        ),
        ValueSettingItem(
            R.string.translate_image_key,
            R.drawable.ic_papago,
            R.string.translate_image_key_summary,
            config = config::imageApiKey,
            showValue = false
        ),
        ActionSettingItem(
            R.string.setting_dual_caption,
            R.drawable.ic_reselect,
            R.string.setting_summary_dual_caption,
        ) {
            lifecycleScope.launch {
                TranslationLanguageDialog(this@SettingActivity).showDualCaptionLocale()
            }
        },
    )
    private val userAgentSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_userAgent_toggle,
            R.drawable.icon_useragent,
            R.string.setting_summary_userAgent_toggle,
            config::enableCustomUserAgent
        ),
        ValueSettingItem(
            R.string.setting_title_userAgent,
            R.drawable.ic_page_height,
            R.string.setting_summary_userAgent,
            config::customUserAgent
        ),
    )

    private val chatGptSettingItems = listOf(
        ActionSettingItem(
            R.string.setting_title_gpt_query_list,
            R.drawable.icon_menu_save,
            R.string.setting_summary_gpt_query_list,
        ) {
            startActivity(GptQueryListActivity.createIntent(this))
        },
        ActionSettingItem(
            R.string.setting_title_gpt_action_list,
            R.drawable.icon_list,
            R.string.setting_summary_gpt_action_list,
        ) { GptActionsActivity.start(this) },
        BooleanSettingItem(
            R.string.use_it_on_dict_search,
            R.drawable.icon_search,
            R.string.setting_summary_search_in_dict,
            config::externalSearchWithGpt
        ),
        BooleanSettingItem(
            R.string.setting_title_chat_stream,
            R.drawable.ic_chat,
            R.string.setting_summary_chat_stream,
            config::enableOpenAiStream
        ),
        DividerSettingItem(),
        ValueSettingItem(
            R.string.setting_title_edit_gpt_api_key,
            R.drawable.ic_chat_gpt,
            R.string.setting_summary_edit_gpt_api_key,
            config::gptApiKey
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_model_name,
            R.drawable.ic_chat_gpt,
            R.string.setting_summary_gpt_model_name,
            config::gptModel
        ),
        BooleanSettingItem(
            R.string.use_it_on_tts,
            R.drawable.ic_tts,
            R.string.setting_summary_use_gpt_for_tts,
            config::useOpenAiTts
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_prompt_for_web_page,
            R.drawable.ic_chat_gpt,
            R.string.setting_summary_gpt_prompt_for_web_page,
            config::gptUserPromptForWebPage
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_use_custom_gpt_url,
            R.drawable.ic_ollama,
            R.string.setting_summary_use_custom_gpt_url,
            config::useCustomGptUrl
        ),
        ValueSettingItem(
            R.string.setting_title_other_model_name,
            R.drawable.ic_ollama,
            R.string.setting_summary_other_model_name,
            config::alternativeModel
        ),
        ValueSettingItem(
            R.string.setting_title_custom_gpt_url,
            R.drawable.ic_ollama,
            R.string.setting_summary_custom_gpt_url,
            config::gptUrl
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_use_gemini,
            R.drawable.ic_gemini,
            R.string.setting_summary_use_gemini,
            config::useGeminiApi
        ),
        ValueSettingItem(
            R.string.setting_title_gemini_key,
            R.drawable.ic_gemini,
            R.string.setting_summary_gemini_key,
            config::geminiApiKey
        ),
        ValueSettingItem(
            R.string.setting_title_gemini_model_name,
            R.drawable.ic_gemini,
            R.string.setting_summary_gemini_model_name,
            config::geminiModel
        ),
    )

    private val startSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_images,
            R.drawable.icon_image,
            R.string.setting_summary_images,
            config::enableImages
        ),
        BooleanSettingItem(
            R.string.setting_title_auto_fill_form,
            R.drawable.ic_input_url,
            R.string.setting_summary_auto_fill_form,
            config::autoFillForm
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_history,
            R.drawable.icon_history,
            R.string.setting_summary_history,
            config::saveHistoryMode,
            listOf(
                R.string.save_history_mode_save_when_open,
                R.string.save_history_mode_save_when_close,
                R.string.save_history_mode_disabled,
            )
        ),
        BooleanSettingItem(
            R.string.setting_title_debug,
            R.drawable.ic_eye,
            R.string.setting_summary_debug,
            config::debugWebView
        ),
        BooleanSettingItem(
            R.string.setting_title_remote,
            R.drawable.icon_remote,
            R.string.setting_summary_remote,
            config::enableRemoteAccess
        ),
        BooleanSettingItem(
            R.string.setting_title_location,
            R.drawable.ic_location,
            R.string.setting_summary_location,
            config::shareLocation
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_adblock,
            R.drawable.ic_block,
            R.string.setting_summary_adblock,
            config::adBlock
        ),
        BooleanSettingItem(
            R.string.setting_title_adblock_auto_update,
            R.drawable.ic_block,
            R.string.setting_summary_adblock_auto_update,
            config::autoUpdateAdblock
        ),
        ActionSettingItem(
            R.string.setting_title_whitelist,
            R.drawable.icon_list,
            R.string.setting_summary_whitelist,
        ) { startActivity(DataListActivity.createIntent(this, WhiteListType.Adblock)) },
        ActionSettingItem(
            R.string.setting_title_update_adblock,
            R.drawable.ic_receive,
            R.string.setting_summary_update_adblock,
        ) {
            lifecycleScope.launch {
                adBlock.downloadHosts(this@SettingActivity) {
                    NinjaToast.show(this@SettingActivity, R.string.toast_adblock_updated)
                }
            }
        },
        ValueSettingItem(
            R.string.setting_title_adblock_url,
            R.drawable.ic_input_url,
            R.string.setting_summary_adblock_url,
            config = config::adblockHostUrl,
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_javascript,
            R.drawable.icon_java,
            R.string.setting_summary_javascript,
            config::enableJavascript
        ),
        ActionSettingItem(
            R.string.setting_title_whitelistJS,
            R.drawable.icon_list,
            R.string.setting_summary_whitelistJS,
        ) { startActivity(DataListActivity.createIntent(this, WhiteListType.Javascript)) },
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_cookie,
            R.drawable.icon_cookie,
            R.string.setting_summary_cookie,
            config::cookies
        ),
        ActionSettingItem(
            R.string.setting_title_whitelistCookie,
            R.drawable.icon_list,
            R.string.setting_summary_whitelistCookie,
        ) { startActivity(DataListActivity.createIntent(this, WhiteListType.Cookie)) },
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_save_data,
            R.drawable.ic_save_data,
            R.string.setting_summary_save_data,
            config::enableSaveData
        ),
    )

    private fun handleBookmarkSync(forceUpload: Boolean) {
        backupUnit.handleBookmarkSync(forceUpload)
    }

    private fun hideStatusBar() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.setDecorFitsSystemWindows(false)
        }
    }
}

enum class SettingRoute(@StringRes val titleId: Int) {
    Main(R.string.settings),
    Ui(R.string.setting_title_ui),
    Toolbar(R.string.setting_title_toolbar),
    Behavior(R.string.setting_title_behavior),
    Gesture(R.string.setting_gestures),
    Backup(R.string.setting_title_data),
    StartControl(R.string.setting_title_start_control),
    DataControl(R.string.setting_title_clear_control),
    UserAgent(R.string.setting_title_userAgent),
    Search(R.string.setting_title_search),
    About(R.string.title_about),
    ChatGPT(R.string.setting_title_chat_gpt),
    Misc(R.string.misc);
}

@Composable
fun SettingBar(
    currentScreen: SettingRoute,
    navigateUp: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(currentScreen.titleId),
                color = MaterialTheme.colors.onPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = navigateUp) {
                Icon(
                    tint = MaterialTheme.colors.onPrimary,
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}