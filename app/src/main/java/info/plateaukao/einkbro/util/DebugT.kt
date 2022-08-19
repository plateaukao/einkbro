package info.plateaukao.einkbro.util

import android.util.Log
import info.plateaukao.einkbro.BuildConfig

class DebugT(private val tag: String) {
    private val startTimeMillis = System.currentTimeMillis()
    init {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[debugT][$tag]: start")
        }
    }

    fun printPath(intermediateTag: String) {
        if (BuildConfig.DEBUG) {
            val newTime = System.currentTimeMillis()
            Log.d(TAG, "[debugT][$tag][$intermediateTag]: ${(newTime - startTimeMillis)}")
        }
    }

    fun printTime() {
        if (BuildConfig.DEBUG) {
            val newTime = System.currentTimeMillis()
            Log.d(TAG, "[debugT][$tag]: ${(newTime - startTimeMillis)}")
        }
    }

    companion object {
        private const val TAG = "debugT"
    }
}