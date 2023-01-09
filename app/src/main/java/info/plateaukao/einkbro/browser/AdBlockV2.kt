package info.plateaukao.einkbro.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.RecordUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdBlockV2(
    context: Context
): BaseWebConfig(context), KoinComponent {
    private val config: ConfigManager by inject()
    override val dbTable: String = RecordUnit.TABLE_WHITELIST

    init {
        val file = File(context.getDir(FILES_DIR, Context.MODE_PRIVATE).toString() + "/" + FILE)
        if (!file.exists()) {
            Log.d("Hosts file", "does not exist")
            file.createNewFile()
            downloadHosts(context) //try to update hosts.txt from internet
        } else {
            // update once per week
            val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            if (Date(file.lastModified()).before(sevenDaysAgo.time) || getHostsDate(file).isBlank()) {
                downloadHosts(context)
            } else {
                loadHosts(context)
            }
        }
    }

    private fun loadHosts(context: Context) {
        val thread = Thread {
            try {
                val file = File(context.getDir(FILES_DIR, Context.MODE_PRIVATE).toString() + "/" + FILE)
                FileReader(file).use { fileReader ->
                    BufferedReader(fileReader).use { bufferReader ->
                        var line: String?
                        while (bufferReader.readLine().also { line = it } != null) {
                            if (line?.startsWith("#") == true) continue
                            line?.lowercase(locale)?.let { hosts.add(it) }
                        }
                    }
                }
            } catch (i: IOException) {
                Log.w("browser", "Error loading adBlockHosts", i)
            }
        }
        thread.start()
    }

    private fun downloadHosts(context: Context) {
        val thread = Thread {
            try {
                Log.d("browser", "Download AdBlock hosts")
                val connection = URL(config.adblockHostUrl).openConnection().apply {
                    readTimeout = 5000
                    connectTimeout = 10000
                }

                val tempFile = File(
                    context.getDir(FILES_DIR, Context.MODE_PRIVATE).toString() + "/temp.txt")
                if (tempFile.exists()) { tempFile.delete() }
                tempFile.createNewFile()

                connection.getInputStream().use { inputStream ->
                    BufferedInputStream(inputStream, 1024 * 5).use { bufferedInputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            val buff = ByteArray(5 * 1024)
                            var len: Int
                            while (bufferedInputStream.read(buff).also { len = it } != -1) {
                                outputStream.write(buff, 0, len)
                            }
                            outputStream.flush()
                        }
                    }
                }

                //now remove leading 0.0.0.0 from file
                FileReader(tempFile).use { fileReader ->
                    val outfile = File(context.getDir(FILES_DIR, Context.MODE_PRIVATE).toString() + "/" + FILE)
                    if (!outfile.exists()) outfile.createNewFile()

                    BufferedReader(fileReader).use { reader ->
                        FileWriter(outfile).use { fileWriter ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                if (line?.startsWith("0.0.0.0 ") == true) {
                                    line = line?.substring(8)
                                }
                                fileWriter.write("$line\n")
                            }
                        }
                    }
                }
                tempFile.delete()
                hosts.clear()

                loadHosts(context) //reload hosts after update
                Log.w("browser", "AdBlock hosts updated")
            } catch (exception: IOException) {
                Log.w("browser", "Error updating AdBlock hosts", exception)
            }
        }
        thread.start()
    }

    private val locale = Locale.getDefault()

    @SuppressLint("ConstantLocale")
    private fun getHostsDate(file: File): String {
        if (!file.exists()) { return "" }
        try {
            FileReader(file).use { fileReader ->
                BufferedReader(fileReader).use { reader ->
                    var line: String
                    while (reader.readLine().also { line = it } != null) {
                        if (line.contains("Date:")) {
                            return "hosts.txt " + line.substring(2)
                        }
                    }
                }
            }
        } catch (exception: IOException) {
            Log.w("browser", "Error getting hosts date", exception)
        }
        return ""
    }

    companion object {
        private const val FILE = "hosts.txt"
        private const val FILES_DIR = "filesdir"
    }
}