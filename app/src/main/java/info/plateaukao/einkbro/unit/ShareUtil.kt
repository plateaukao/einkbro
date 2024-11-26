package info.plateaukao.einkbro.unit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.EBToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketException

object ShareUtil : KoinComponent {
    private const val multicastIp = "239.10.10.100"
    private const val multicastPort = 54545
    private const val broadcastIntervalInMilli = 1000L
    private val group = InetAddress.getByName(multicastIp)
    private var broadcastJob: Job? = null
    private var socket: MulticastSocket? = null

    fun copyToClipboard(context: Context, url: String) {
        val clipboard =
            context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", url)
        clipboard.setPrimaryClip(clip)
        EBToast.show(context, R.string.toast_copy_successful)
    }
    private var bytesToBeSent = ByteArray(0)
    fun startBroadcastingUrl(
        lifecycleCoroutineScope: CoroutineScope,
        url: String,
        times: Int = 999
    ) {
        if (broadcastJob != null && socket?.isConnected == true) {
            bytesToBeSent = url.toByteArray()
        } else {
            broadcastJob = lifecycleCoroutineScope.launch(Dispatchers.IO) {
                try {
                    socket = MulticastSocket(multicastPort).apply { joinGroup(group) }
                    bytesToBeSent = url.toByteArray()
                    repeat(times) {
                        socket?.send(DatagramPacket(bytesToBeSent, bytesToBeSent.size, group, multicastPort))
                        delay(broadcastIntervalInMilli) // 1 second
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        }
    }

    fun stopBroadcast() {
        broadcastJob?.cancel()
        broadcastJob = null
        socket?.leaveGroup(group)
        socket?.close()
        socket = null
    }

    fun startReceiving(
        lifecycleCoroutineScope: CoroutineScope,
        receivedAction: (String) -> Unit
    ) {
        var receivedString = ""
        val receiveData = ByteArray(4096)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)

        broadcastJob = lifecycleCoroutineScope.launch(Dispatchers.IO) {
            try {
                socket = MulticastSocket(multicastPort).apply { joinGroup(group) }
            } catch (exception: SocketException) {
                return@launch
            }
            while(true) {
                try {
                    socket?.receive(receivePacket)
                } catch (exception: SocketException) {
                    return@launch
                }
                val newString = String(receivePacket.data, 0, receivePacket.length)
                if (receivedString != newString) {
                    receivedString = newString

                    val processedString = if (receivedString.startsWith("http")) {
                        receivedString // EinkBro case
                    } else {
                        convertSharikResponse(receivePacket.address.toString(), receivedString)
                    }
                    withContext(Dispatchers.Main) {
                        receivedAction(processedString)
                    }
                }
            }
        }
    }

    private fun convertSharikResponse(address: String, jsonString: String): String {
        val jsonObject = JSONObject(jsonString)
        val type = jsonObject.getString("type")
        val port = jsonObject.getString("port")
        return when (type) {
            "file", "app" -> "http:/$address:$port/"
            else -> jsonObject.getString("name")
        }
    }
}
