package de.baumann.browser

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.epub.EpubManager
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.DarkMode
import de.baumann.browser.view.dialog.DialogManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

class EinkBroApplication : Application() {

    private val sp: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    private val config: ConfigManager by lazy {
        ConfigManager(applicationContext, sp)
    }

    val myModule = module {
        single { config }
        single { sp }
        single { BookmarkManager(androidContext()) }

        single { DialogManager(it[0]) }
        single { EpubManager(it[0]) }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@EinkBroApplication)
            modules(myModule)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
