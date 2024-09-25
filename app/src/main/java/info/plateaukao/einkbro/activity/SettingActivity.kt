package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.FragmentActivity
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
import info.plateaukao.einkbro.view.dialog.compose.ToolbarConfigDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SettingActivity : FragmentActivity() {
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

        val routeName = intent.getStringExtra(KEY_ROUTE) ?: Main.name
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
                            },
                            close = { finish() }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = routeName,
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
                                    addAll(LinkSettingItem.entries.toList())
                                    add(DividerSettingItem())
                                    if (BuildConfig.showUpdateButton) {
                                        add(ActionSettingItem(
                                            R.string.setting_title_github_update,
                                            0,
                                        ) {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                HelperUnit.upgradeToLatestRelease(this@SettingActivity)
                                            }
                                        })
                                        add(ActionSettingItem(
                                            R.string.setting_title_github_snapshot,
                                            0,
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
        if (config.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(
                LocaleManager.setLocale(newBase, config.uiLocaleLanguage)
            )
        } else {
            super.attachBaseContext(newBase)
        }
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
        ActionSettingItem(
            R.string.setting_app_locale,
            0,
            R.string.setting_summary_app_locale,
        ) {
            lifecycleScope.launch {
                TranslationLanguageDialog(this@SettingActivity).showAppLocale()
                config.restartChanged = true
            }
        },
        BooleanSettingItem(
            R.string.hide_statusbar,
            0,
            R.string.setting_summary_hide_statusbar,
            config::hideStatusbar,
        ),
        BooleanSettingItem(
            R.string.desktop_mode,
            0,
            R.string.setting_summary_desktop,
            config::desktop,
        ),
        BooleanSettingItem(
            R.string.always_enable_zoom,
            0,
            R.string.setting_summary_enable_zoom,
            config::enableZoom,
        ),
        BooleanSettingItem(
            R.string.show_default_text_menu,
            0,
            R.string.setting_summary_show_default_text_menu,
            config::showDefaultActionMenu,
        ),
        BooleanSettingItem(
            R.string.show_context_menu_icons,
            0,
            R.string.setting_summary_show_context_menu_icons,
            config::showActionMenuIcons,
        ),
        DividerSettingItem(),
        ValueSettingItem(
            R.string.setting_title_page_left_value,
            0,
            R.string.setting_summary_page_left_value,
            config::pageReservedOffsetInString
        ),
        ListSettingWithEnumItem(
            R.string.dark_mode,
            0,
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
            0,
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
            0,
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
            0,
            R.string.setting_summary_clear_recent_bookmarks,
        ) {
            config.clearRecentBookmarks()
        },
    )

    private val behaviorSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_saveTabs,
            0,
            R.string.setting_summary_saveTabs,
            config::shouldSaveTabs,
        ),
        BooleanSettingItem(
            R.string.setting_title_background_loading,
            0,
            R.string.setting_summary_background_loading,
            config::enableWebBkgndLoad,
        ),
        BooleanSettingItem(
            R.string.setting_title_next_tab,
            0,
            R.string.setting_summary_next_tab,
            config::shouldShowNextAfterRemoveTab,
        ),
        BooleanSettingItem(
            R.string.settings_title_back_key_behavior,
            0,
            R.string.settings_summary_back_key_behavior,
            config::closeTabWhenNoMoreBackHistory,
        ),
        BooleanSettingItem(
            R.string.setting_title_trim_input_url,
            0,
            R.string.setting_summary_trim_input_url,
            config::shouldTrimInputUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_prune_query_parameter,
            0,
            R.string.setting_summary_prune_query_parameter,
            config::shouldPruneQueryParameters,
        ),
        BooleanSettingItem(
            R.string.setting_title_video_auto_fullscreen,
            0,
            R.string.setting_summary_video_auto_fullscreen,
            config::enableVideoAutoFullscreen,
        ),
        BooleanSettingItem(
            R.string.setting_title_video_pip,
            0,
            R.string.setting_summary_video_pip,
            config::enableVideoPip,
        ),
        BooleanSettingItem(
            R.string.setting_title_screen_awake,
            0,
            R.string.setting_summary_screen_awake,
            config::keepAwake,
        ),
        BooleanSettingItem(
            R.string.setting_title_confirm_tab_close,
            0,
            R.string.setting_summary_confirm_tab_close,
            config::confirmTabClose,
        ),
        BooleanSettingItem(
            R.string.setting_title_vi_binding,
            0,
            R.string.setting_summary_vi_binding,
            config::enableViBinding,
        ),
        BooleanSettingItem(
            R.string.setting_title_disable_long_press_toucharea,
            0,
            R.string.setting_summary_disable_long_press_toucharea,
            config::disableLongPressTouchArea,
        ),
        BooleanSettingItem(
            R.string.setting_title_useUpDown,
            0,
            R.string.setting_summary_useUpDownKey,
            config::useUpDownPageTurn,
        ),
        BooleanSettingItem(
            R.string.setting_title_show_bookmarks_input_bar,
            0,
            R.string.setting_summary_show_bookmarks_input_bar,
            config::showBookmarksInInputBar,
        ),
        BooleanSettingItem(
            R.string.setting_title_enable_ssl_error_dialog,
            0,
            R.string.setting_summary_enable_ssl_error_dialog,
            config::enableCertificateErrorDialog,
        ),
        BooleanSettingItem(
            R.string.setting_title_enable_web_cache,
            0,
            R.string.setting_summary_enabling_web_cache,
            config::webLoadCacheFirst,
        )
    )

    private val toolbarSettingItems = listOf(
        ActionSettingItem(
            R.string.toolbar_icons,
            0,
            R.string.toolbar_icons_description,
        ) {
            ToolbarConfigDialogFragment().show(
                this@SettingActivity.supportFragmentManager, "toolbar_config"
            )
        },
        BooleanSettingItem(
            R.string.setting_title_toolbar_top,
            0,
            R.string.setting_summary_toolbar_top,
            config::isToolbarOnTop,
        ),
        BooleanSettingItem(
            R.string.setting_title_hideToolbar,
            0,
            R.string.setting_summary_hide,
            config::shouldHideToolbar,
        ),
        BooleanSettingItem(
            R.string.setting_title_toolbarShow,
            0,
            R.string.setting_summary_toolbarShow,
            config::showToolbarFirst,
        ),
        BooleanSettingItem(
            R.string.setting_title_show_tab_bar,
            0,
            R.string.setting_summary_show_tab_bar,
            config::shouldShowTabBar,
        ),
    )

    private val gestureSettingItems = listOf(
        DividerSettingItem(
            R.string.setting_title_touch_area_actions,
        ),
        ListSettingWithEnumItem(
            R.string.setting_touch_up_click,
            0,
            config = config::upClickGesture,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_touch_up_long_click,
            0,
            config = config::upLongClickGesture,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_touch_down_click,
            0,
            config = config::downClickGesture,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_touch_down_long_click,
            0,
            config = config::downLongClickGesture,
            options = GestureType.entries.map { it.resId },
        ),
        DividerSettingItem(R.string.setting_multitouch_use_title),
        BooleanSettingItem(
            R.string.setting_multitouch_use_title,
            0,
            R.string.setting_multitouch_use_summary,
            config::isMultitouchEnabled,
            span = 2,
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_up,
            0,
            config = config::multitouchUp,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_down,
            0,
            config = config::multitouchDown,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_left,
            0,
            config = config::multitouchLeft,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_right,
            0,
            config = config::multitouchRight,
            options = GestureType.entries.map { it.resId },
        ),
        DividerSettingItem(R.string.gesture_on_floating_button),
        BooleanSettingItem(
            R.string.setting_gestures_use_title,
            0,
            R.string.setting_gestures_use_summary,
            config::enableNavButtonGesture,
            span = 2,
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_up,
            0,
            config = config::navGestureUp,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_down,
            0,
            config = config::navGestureDown,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_left,
            0,
            config = config::navGestureLeft,
            options = GestureType.entries.map { it.resId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_gesture_right,
            0,
            config = config::navGestureRight,
            options = GestureType.entries.map { it.resId },
        ),
    )

    private val searchSettingItems = listOf(
        ListSettingWithStringItem(
            R.string.setting_title_search_engine,
            0,
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
            0,
            R.string.setting_summary_search_engine,
            config = config::searchEngineUrl,
        ),
        DividerSettingItem(),
        ValueSettingItem(
            R.string.setting_title_process_text,
            0,
            R.string.setting_summary_custom_process_text_url,
            config = config::processTextUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_external_search_pop,
            0,
            R.string.setting_summary_external_search_pop,
            config::externalSearchWithPopUp,
        ),
        DividerSettingItem(),
        ActionSettingItem(
            R.string.setting_title_split_search_setting,
            0,
            R.string.setting_summary_split_search_setting
        ) {
            startActivity(DataListActivity.createIntent(this, WhiteListType.SplitSearch))
        },
        BooleanSettingItem(
            R.string.setting_title_search_in_same_tab,
            0,
            R.string.setting_summary_search_in_same_tab,
            config::isExternalSearchInSameTab,
        ),
    )

    private val dataSettingItems = listOf(
        ActionSettingItem(
            R.string.setting_title_export_appData,
            0,
            R.string.setting_summary_export_appData
        ) { dialogManager.showBackupFilePicker() },
        ActionSettingItem(
            R.string.setting_title_import_appData,
            0,
            R.string.setting_summary_import_appData
        ) { dialogManager.showImportBackupFilePicker() },
        DividerSettingItem(),
        ActionSettingItem(
            R.string.setting_title_export_bookmarks,
            0,
        ) { dialogManager.showBookmarkFilePicker() },
        ActionSettingItem(
            R.string.setting_title_import_bookmarks,
            0,
        ) { dialogManager.showImportBookmarkFilePicker() },
        ActionSettingItem(
            R.string.setting_title_setup_bookmarks_location,
            0,
            R.string.setting_summary_setup_bookmarks_location,
        ) {
            dialogManager.showCreateOrOpenBookmarkFileDialog(
                { BrowserUnit.createBookmarkFilePicker(createBookmarkFileLauncher) },
                { BrowserUnit.openBookmarkFilePicker(openBookmarkFileLauncher) }
            )
        },
        ActionSettingItem(
            R.string.setting_title_sync_bookmarks,
            0,
        ) { handleBookmarkSync(true) },
    )

    private val clearDataSettingItems = listOf(
        BooleanSettingItem(
            R.string.clear_title_cache,
            0,
            config = config::clearCache,
        ),
        BooleanSettingItem(
            R.string.clear_title_history,
            0,
            config = config::clearHistory,
        ),
        BooleanSettingItem(
            R.string.clear_title_indexedDB,
            0,
            config = config::clearIndexedDB,
        ),
        BooleanSettingItem(
            R.string.clear_title_cookie,
            0,
            R.string.setting_summary_cookie_delete,
            config::clearCookies
        ),
        BooleanSettingItem(
            R.string.clear_title_quit,
            0,
            R.string.clear_summary_quit,
            config::clearWhenQuit
        ),
        ActionSettingItem(
            R.string.clear_title_deleteDatabase,
            0,
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
            0,
            R.string.setting_summary_highlight_style,
            config = config::highlightStyle,
            options = HighlightStyle.entries.filter { it != HighlightStyle.BACKGROUND_NONE }
                .map { it.stringResId },
        ),
        NavigateSettingItem(
            R.string.setting_title_userAgent,
            0,
            destination = UserAgent
        ),
        ValueSettingItem(
            R.string.setting_title_edit_homepage,
            0,
            config = config::favoriteUrl,
            showValue = false
        ),
        ActionSettingItem(R.string.setting_title_pdf_paper_size, 0) {
            PrinterDocumentPaperSizeDialog(
                this
            ).show()
        },
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_enable_inplace_translate,
            0,
            R.string.setting_summary_enable_inplace_translate,
            config::enableInplaceParagraphTranslate
        ),
        ValueSettingItem(
            R.string.setting_title_translated_langs,
            0,
            R.string.setting_summary_translated_langs,
            config::preferredTranslateLanguageString
        ),
        ValueSettingItem(
            R.string.translate_image_key,
            0,
            R.string.translate_image_key_summary,
            config = config::imageApiKey,
            showValue = false
        ),
        ActionSettingItem(
            R.string.setting_dual_caption,
            0,
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
            0,
            R.string.setting_summary_userAgent_toggle,
            config::enableCustomUserAgent
        ),
        ValueSettingItem(
            R.string.setting_title_userAgent,
            0,
            R.string.setting_summary_userAgent,
            config::customUserAgent
        ),
    )

    private val chatGptSettingItems = listOf(
        ActionSettingItem(
            R.string.setting_title_gpt_query_list,
            0,
            R.string.setting_summary_gpt_query_list,
        ) {
            startActivity(GptQueryListActivity.createIntent(this))
        },
        ActionSettingItem(
            R.string.setting_title_gpt_action_list,
            0,
            R.string.setting_summary_gpt_action_list,
        ) { GptActionsActivity.start(this) },
        BooleanSettingItem(
            R.string.use_it_on_dict_search,
            0,
            R.string.setting_summary_search_in_dict,
            config::externalSearchWithGpt
        ),
        BooleanSettingItem(
            R.string.setting_title_chat_stream,
            0,
            R.string.setting_summary_chat_stream,
            config::enableOpenAiStream
        ),
        DividerSettingItem(R.string.openai),
        ValueSettingItem(
            R.string.setting_title_edit_gpt_api_key,
            0,
            R.string.setting_summary_edit_gpt_api_key,
            config::gptApiKey
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_model_name,
            0,
            R.string.setting_summary_gpt_model_name,
            config::gptModel
        ),
        BooleanSettingItem(
            R.string.use_it_on_tts,
            0,
            R.string.setting_summary_use_gpt_for_tts,
            config::useOpenAiTts
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_prompt_for_web_page,
            0,
            R.string.setting_summary_gpt_prompt_for_web_page,
            config::gptUserPromptForWebPage
        ),
        DividerSettingItem(R.string.openai_compatible_server),
        BooleanSettingItem(
            R.string.setting_title_use_custom_gpt_url,
            0,
            R.string.setting_summary_use_custom_gpt_url,
            config::useCustomGptUrl
        ),
        ValueSettingItem(
            R.string.setting_title_other_model_name,
            0,
            R.string.setting_summary_other_model_name,
            config::alternativeModel
        ),
        ValueSettingItem(
            R.string.setting_title_custom_gpt_url,
            0,
            R.string.setting_summary_custom_gpt_url,
            config::gptUrl
        ),
        DividerSettingItem(R.string.google_gemini),
        BooleanSettingItem(
            R.string.setting_title_use_gemini,
            0,
            R.string.setting_summary_use_gemini,
            config::useGeminiApi
        ),
        ValueSettingItem(
            R.string.setting_title_gemini_key,
            0,
            R.string.setting_summary_gemini_key,
            config::geminiApiKey
        ),
        ValueSettingItem(
            R.string.setting_title_gemini_model_name,
            0,
            R.string.setting_summary_gemini_model_name,
            config::geminiModel
        ),
    )

    private val startSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_images,
            0,
            R.string.setting_summary_images,
            config::enableImages
        ),
        BooleanSettingItem(
            R.string.setting_title_auto_fill_form,
            0,
            R.string.setting_summary_auto_fill_form,
            config::autoFillForm
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_history,
            0,
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
            0,
            R.string.setting_summary_debug,
            config::debugWebView
        ),
        BooleanSettingItem(
            R.string.setting_title_remote,
            0,
            R.string.setting_summary_remote,
            config::enableRemoteAccess
        ),
        BooleanSettingItem(
            R.string.setting_title_location,
            0,
            R.string.setting_summary_location,
            config::shareLocation
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_adblock,
            0,
            R.string.setting_summary_adblock,
            config::adBlock
        ),
        BooleanSettingItem(
            R.string.setting_title_adblock_auto_update,
            0,
            R.string.setting_summary_adblock_auto_update,
            config::autoUpdateAdblock
        ),
        ActionSettingItem(
            R.string.setting_title_whitelist,
            0,
            R.string.setting_summary_whitelist,
        ) { startActivity(DataListActivity.createIntent(this, WhiteListType.Adblock)) },
        ActionSettingItem(
            R.string.setting_title_update_adblock,
            0,
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
            0,
            R.string.setting_summary_adblock_url,
            config = config::adblockHostUrl,
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_javascript,
            0,
            R.string.setting_summary_javascript,
            config::enableJavascript
        ),
        ActionSettingItem(
            R.string.setting_title_whitelistJS,
            0,
            R.string.setting_summary_whitelistJS,
        ) { startActivity(DataListActivity.createIntent(this, WhiteListType.Javascript)) },
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_cookie,
            0,
            R.string.setting_summary_cookie,
            config::cookies
        ),
        ActionSettingItem(
            R.string.setting_title_whitelistCookie,
            0,
            R.string.setting_summary_whitelistCookie,
        ) { startActivity(DataListActivity.createIntent(this, WhiteListType.Cookie)) },
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.setting_title_save_data,
            0,
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

    companion object {
        private const val KEY_ROUTE = "route"

        // create an intent to navigate to desired setting screen route
        fun createIntent(context: Context, route: SettingRoute): Intent {
            return Intent(context, SettingActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(KEY_ROUTE, route.name)
            }
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
    close: () -> Unit,
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
        },
        actions = {
            IconButton(onClick = close) {
                Icon(
                    tint = MaterialTheme.colors.onPrimary,
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}