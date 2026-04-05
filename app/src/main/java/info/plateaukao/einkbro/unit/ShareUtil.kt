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
import java.io.File
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
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
        times: Int = 3
    ) {
        if (socket == null || socket?.isClosed == true) {
            socket?.leaveGroup(group)
            socket?.close()
            socket = MulticastSocket(multicastPort).apply { joinGroup(group) }
        }

        if (broadcastJob != null) {
            broadcastJob?.cancel()
        }

        broadcastJob = lifecycleCoroutineScope.launch(Dispatchers.IO) {
            try {
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

    fun stopBroadcast() {
        broadcastJob?.cancel()
        broadcastJob = null
        socket?.leaveGroup(group)
        socket?.close()
        socket = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    fun startReceiving(
        lifecycleCoroutineScope: CoroutineScope,
        receivedAction: (String) -> Unit
    ) {
        var receivedString = ""
        var lastReceivedTime = System.currentTimeMillis()
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
                if (newString == receivedString && System.currentTimeMillis() - lastReceivedTime < 5_000L) {
                    continue // Ignore duplicate messages within the interval
                }

                lastReceivedTime = System.currentTimeMillis()
                if (receivedString != newString) {
                    receivedString = newString

                    val processedString = if (receivedString.startsWith("http") || receivedString.startsWith("action")) {
                        receivedString // EinkBro case
                    } else {
                        convertSharikResponse(receivePacket.address.toString(), receivedString)
                    }
                    withContext(Dispatchers.Main) {
                        receivedAction(processedString)
                    }
                }

                delay(300L)
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

    private const val BACKUP_PREFIX = "einkbro-backup:"
    private var serverSocket: ServerSocket? = null

    fun startServingFile(
        scope: CoroutineScope,
        file: File,
        times: Int = 30,
    ) {
        stopBroadcast()

        scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(0)
                serverSocket = server
                val port = server.localPort
                val localIp = getLocalIpAddress() ?: return@launch

                // broadcast the server address via multicast
                startBroadcastingUrl(scope, "$BACKUP_PREFIX$localIp:$port", times)

                // serve the file to the first client that connects
                val client = server.accept()
                client.getOutputStream().use { os ->
                    file.inputStream().use { it.copyTo(os) }
                }
                client.close()
                server.close()
                serverSocket = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startReceivingFile(
        scope: CoroutineScope,
        outputFile: File,
        receivedAction: (File) -> Unit,
    ) {
        var receivedString = ""
        var lastReceivedTime = System.currentTimeMillis()
        val receiveData = ByteArray(4096)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)

        broadcastJob = scope.launch(Dispatchers.IO) {
            try {
                socket = MulticastSocket(multicastPort).apply { joinGroup(group) }
            } catch (e: SocketException) {
                return@launch
            }
            while (true) {
                try {
                    socket?.receive(receivePacket) ?: return@launch
                } catch (e: SocketException) {
                    return@launch
                }

                val message = String(receivePacket.data, 0, receivePacket.length)
                if (message == receivedString && System.currentTimeMillis() - lastReceivedTime < 5_000L) {
                    continue
                }
                lastReceivedTime = System.currentTimeMillis()
                receivedString = message

                if (!message.startsWith(BACKUP_PREFIX)) continue

                val address = message.removePrefix(BACKUP_PREFIX)
                val parts = address.split(":")
                if (parts.size != 2) continue

                val ip = parts[0]
                val port = parts[1].toIntOrNull() ?: continue

                try {
                    val client = Socket(ip, port)
                    client.getInputStream().use { input ->
                        outputFile.outputStream().use { fos ->
                            input.copyTo(fos)
                        }
                    }
                    client.close()

                    withContext(Dispatchers.Main) {
                        receivedAction(outputFile)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                break
            }
            stopBroadcast()
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            for (intf in NetworkInterface.getNetworkInterfaces()) {
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
