package de.baumann.browser

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.epub.EpubManager
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.view.dialog.DialogManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class EinkBroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        startKoin {
            androidContext(this@EinkBroApplication)
            modules(myModule)
        }
    }
}

val myModule = module {
    single { ConfigManager(androidContext()) }
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    single { BookmarkManager(androidContext()) }

    single { DialogManager(it[0]) }
    single { EpubManager(it[0]) }
}