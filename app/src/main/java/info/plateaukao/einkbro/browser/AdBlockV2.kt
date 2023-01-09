package info.plateaukao.einkbro.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import info.plateaukao.einkbro.unit.RecordUnit
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

class AdBlockV2(context: Context) : BaseWebConfig(context) {
    override val dbTable: String = RecordUnit.TABLE_WHITELIST

    init {
        val file = File(context.getDir("filesdir", Context.MODE_PRIVATE).toString() + "/" + FILE)
        if (!file.exists()) {
            Log.d("Hosts file", "does not exist")
            file.createNewFile()
            downloadHosts(context) //try to update hosts.txt from internet
        } else {
            val time = Calendar.getInstance()
            time.add(Calendar.DAY_OF_YEAR, -7)
            val lastModified = Date(file.lastModified())
            if (lastModified.before(time.time) || getHostsDate(context) == "") {
                //also download again if something is wrong with the file
                //update if file is older than a day
                downloadHosts(context)
            } else {
                loadHosts(context)
            }
        }
    }

    private fun loadHosts(context: Context) {
        val thread = Thread {
            try {
                val file = File(context.getDir("filesdir", Context.MODE_PRIVATE).toString() + "/" + FILE)
                val `in` = FileReader(file)
                val reader = BufferedReader(`in`)
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val nonNullLine = line ?: break
                    if (nonNullLine.startsWith("#")) continue
                    hosts.add(nonNullLine.lowercase(locale))
                }
                `in`.close()
            } catch (i: IOException) {
                Log.w("browser", "Error loading adBlockHosts", i)
            }
        }
        thread.start()
    }

    private fun downloadHosts(context: Context) {
        val thread = Thread {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val hostURL = sp.getString("ab_hosts", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts")
            try {
                val url = URL(hostURL)
                Log.d("browser", "Download AdBlock hosts")
                val connection = url.openConnection()
                connection.readTimeout = 5000
                connection.connectTimeout = 10000
                val `is` = connection.getInputStream()
                val inStream = BufferedInputStream(`is`, 1024 * 5)
                val tempfile = File(context.getDir("filesdir", Context.MODE_PRIVATE).toString() + "/temp.txt")
                if (tempfile.exists()) {
                    tempfile.delete()
                }
                tempfile.createNewFile()
                val outStream = FileOutputStream(tempfile)
                val buff = ByteArray(5 * 1024)
                var len: Int
                while (inStream.read(buff).also { len = it } != -1) {
                    outStream.write(buff, 0, len)
                }
                outStream.flush()
                outStream.close()
                inStream.close()

                //now remove leading 0.0.0.0 from file
                val `in` = FileReader(tempfile)
                val reader = BufferedReader(`in`)
                val outfile = File(context.getDir("filesdir", Context.MODE_PRIVATE).toString() + "/" + FILE)
                if (!outfile.exists()) outfile.createNewFile()
                val out = FileWriter(outfile)
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("0.0.0.0 ") == true) {
                        line = line?.substring(8)
                    }
                    out.write("$line\n")
                }
                `in`.close()
                out.close()
                tempfile.delete()
                hosts.clear()
                loadHosts(context) //reload hosts after update
                Log.w("browser", "AdBlock hosts updated")
            } catch (i: IOException) {
                Log.w("browser", "Error updating AdBlock hosts", i)
            }
        }
        thread.start()
    }

    private val locale = Locale.getDefault()

    @SuppressLint("ConstantLocale")
    private fun getHostsDate(context: Context): String {
        val file = File(context.getDir("filesdir", Context.MODE_PRIVATE).toString() + "/" + FILE)
        var date = ""
        if (!file.exists()) {
            return ""
        }
        try {
            val `in` = FileReader(file)
            val reader = BufferedReader(`in`)
            var line: String
            while (reader.readLine().also { line = it } != null) {
                if (line.contains("Date:")) {
                    date = "hosts.txt " + line.substring(2)
                    `in`.close()
                    break
                }
            }
            `in`.close()
        } catch (i: IOException) {
            Log.w("browser", "Error getting hosts date", i)
        }
        return date
    }


    companion object {
        private const val FILE = "hosts.txt"
    }
}