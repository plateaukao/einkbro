package de.baumann.browser

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class EinkBroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}