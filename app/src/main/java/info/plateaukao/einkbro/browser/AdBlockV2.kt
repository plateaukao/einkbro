package info.plateaukao.einkbro.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.RecordUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val context: Context
) : BaseWebConfig(context), KoinComponent {
    private val config: ConfigManager by inject()
    override val dbTable: String = RecordUnit.TABLE_WHITELIST

    init {
        setup()
    }

    private fun setup() {
        val file = File(context.getDir(FILES_DIR, Context.MODE_PRIVATE).toString() + "/" + FILE)
        GlobalScope.launch {
            // no file yet
            if (!file.exists()) {
                Log.d("Hosts file", "does not exist")
                file.createNewFile()
                downloadHosts(context)
                return@launch
            }

            // has file, check if auto update is enabled and it's expired
            if (config.autoUpdateAdblock) {
                // update once per week
                val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                if (Date(file.lastModified()).before(sevenDaysAgo.time) || getHostsDate(file).isBlank()) {
                    downloadHosts(context)
                    return@launch
                }
            }

            // other cases, load the hosts directly
            loadHosts(context)
        }
    }

    private suspend fun loadHosts(context: Context) =
        withContext(Dispatchers.IO) {
            val locale = Locale.getDefault()
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

    suspend fun downloadHosts(
        context: Context,
        postAction: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d("browser", "Download AdBlock hosts")
            val url = config.adblockHostUrl.ifBlank { ConfigManager.ADBLOCK_URL_DEFAULT }
            val connection = URL(url).openConnection().apply {
                readTimeout = 5000
                connectTimeout = 10000
            }

            val tempFile = File(
                context.getDir(FILES_DIR, Context.MODE_PRIVATE).toString() + "/temp.txt"
            )
            if (tempFile.exists()) {
                tempFile.delete()
            }
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
                            } else if (line?.startsWith("||") == true) {
                                // handle lines like: ||example.com^
                                line = line?.substring(2)
                                if (line?.endsWith("^") == true) {
                                    val length = (line?.length ?: 1) - 1
                                    line = line?.substring(0, length)
                                }
                            }
                            fileWriter.write("$line\n")
                        }
                    }
                }
            }
            tempFile.delete()
            hosts.clear()

            loadHosts(context)
            Log.w("browser", "AdBlock hosts updated")
            withContext(Dispatchers.Main) { postAction() }
        } catch (exception: IOException) {
            Log.w("browser", "Error updating AdBlock hosts", exception)
        }
    }


    @SuppressLint("ConstantLocale")
    private fun getHostsDate(file: File): String {
        try {
            if (!file.exists()) {
                return ""
            }
            FileReader(file).use { fileReader ->
                BufferedReader(fileReader).use { reader ->
                    var line: String
                    while (reader.readLine().also { line = it } != null) {
                        if (line.contains("Date:")) {
                            return "hosts.txt " + line.substring(2)
                        }
                    }
                    return ""
                }
            }
        } catch (exception: Exception) {
            Log.w("browser", "Error getting hosts date", exception)
        }
        return ""
    }

    companion object {
        private const val FILE = "hosts.txt"
        private const val FILES_DIR = "filesdir"
    }
}