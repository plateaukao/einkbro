package info.plateaukao.einkbro.util

import android.os.Environment
import java.io.File

class CustomExceptionHandler(private val defaultHandler: Thread.UncaughtExceptionHandler) :
    Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val tempFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/crash_log.txt"
        )
        if (tempFile.exists()) {
            tempFile.delete()
        }
        tempFile.createNewFile()
        tempFile.appendText(throwable.stackTraceToString())

        defaultHandler.uncaughtException(thread, throwable)
    }
}
