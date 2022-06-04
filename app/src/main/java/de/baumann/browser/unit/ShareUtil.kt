package de.baumann.browser.unit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.view.NinjaToast
import kotlinx.coroutines.*
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import java.net.*

object ShareUtil: KoinComponent {
    private const val multicastIp = "239.10.10.100"
    private const val multicastPort = 54545
    private const val broadcastIntervalInMilli = 1000L
    private val group = InetAddress.getByName(multicastIp)
    private var broadcastJob: Job? = null
    private var socket: MulticastSocket? = null

    fun copyToClipboard(context: Context, url: String) {
        val clipboard = context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", url)
        clipboard.setPrimaryClip(clip)
        NinjaToast.show(context, R.string.toast_copy_successful)
    }

    fun startBroadcastingUrl(lifecycleCoroutineScope: LifecycleCoroutineScope, url: String) {
        broadcastJob = lifecycleCoroutineScope.launch(Dispatchers.IO) {
            socket = MulticastSocket(multicastPort).apply { joinGroup(group) }
            val bytes = url.toByteArray()
            while(true) {
                socket?.send(DatagramPacket(bytes, bytes.size, group, multicastPort))
                delay(broadcastIntervalInMilli) // 1 second
            }
        }
    }

    fun stopBroadcast() {
        socket?.leaveGroup(group)
        socket?.close()
        socket = null
        broadcastJob?.cancel()
        broadcastJob = null
    }

    fun startReceiving(lifecycleCoroutineScope: LifecycleCoroutineScope, receivedAction: (String) -> Unit) {
        val receiveData = ByteArray(4096)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        broadcastJob = lifecycleCoroutineScope.launch(Dispatchers.IO) {
            socket = MulticastSocket(multicastPort).apply { joinGroup(group) }
            try {
                socket?.receive(receivePacket)
            } catch (exception: SocketException) {
                // closed before receiving data
                return@launch
            }
            val receivedString = String(receivePacket.data, 0, receivePacket.length)
            val processedString = if (receivedString.startsWith("http")) {
                receivedString // EinkBro case
            } else {
                handleSharikScenario(receivePacket.address.toString(), receivedString)
            }
            receivedAction(processedString)
            stopBroadcast()
        }
    }

    private fun handleSharikScenario(address: String, jsonString: String): String {
        val jsonObject = JSONObject(jsonString)
        val type = jsonObject.getString("type")
        val port = jsonObject.getString("port")
        return when (type) {
            "file", "app" -> "http:/$address:$port/"
            else -> jsonObject.getString("name")
        }
    }
}