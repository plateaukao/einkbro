package de.baumann.browser.util

import android.util.Log
import de.baumann.browser.Ninja.BuildConfig

class DebugT(private val tag: String) {
    private val currentTimeInMilli = System.currentTimeMillis()
    init {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[debugT][$tag]: start")
        }
    }

    fun printPath(intermediateTag: String) {
        if (BuildConfig.DEBUG) {
            val newTime = System.currentTimeMillis()
            Log.d(TAG, "[debugT][$tag][$intermediateTag]: ${(newTime - currentTimeInMilli)}")
        }
    }

    fun printTime() {
        if (BuildConfig.DEBUG) {
            val newTime = System.currentTimeMillis()
            Log.d(TAG, "[debugT][$tag]: ${(newTime - currentTimeInMilli)}")
        }
    }

    companion object {
        private const val TAG = "debugT"
    }
}