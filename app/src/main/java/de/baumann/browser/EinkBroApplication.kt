package de.baumann.browser

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import de.baumann.browser.browser.AdBlock
import de.baumann.browser.browser.Cookie
import de.baumann.browser.browser.Javascript
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.preference.ConfigManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
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
        single { AdBlock(androidContext())}
        single { Javascript(androidContext())}
        single { Cookie(androidContext()) }
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
