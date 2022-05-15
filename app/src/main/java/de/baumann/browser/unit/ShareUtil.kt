package de.baumann.browser.unit

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.*
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.URL

object ShareUtil: KoinComponent {
    private const val multicastIp = "239.10.10.100"
    private const val multicastPort = 54545
    private const val broadcastIntervalInMilli = 1000L
    private val group = InetAddress.getByName(multicastIp)
    private var broadcastJob: Job? = null
    private var socket: MulticastSocket? = null

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

    fun startReceiving(lifecycleCoroutineScope: LifecycleCoroutineScope, afterAction: (String) -> Unit) {
        val receiveData = ByteArray(4096)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        broadcastJob = lifecycleCoroutineScope.launch(Dispatchers.IO) {
            socket = MulticastSocket(multicastPort).apply { joinGroup(group) }
            socket?.receive(receivePacket)
            val receivedString = String(receivePacket.data, 0, receivePacket.length)
            val processedString = if (receivedString.startsWith("http")) {
                receivedString // EinkBro case
            } else {
                handleSharikScenario(receivePacket.address.toString(), receivedString.toInt())
            }
            afterAction(processedString)
            stopBroadcast()
        }
    }

    private fun handleSharikScenario(address: String, port: Int): String {
        val jsonObject = JSONObject(fetchHttpJson(address, port))
        return when (jsonObject.getString("type")) {
            "file", "app" -> "http:/$address:$port/"
            else -> jsonObject.getString("name")
        }
    }

    private fun fetchHttpJson(address: String, port: Int): String {
        val stream = URL("http", address, port, "sharik.json").openStream()
        val jsonString = BufferedReader(InputStreamReader(stream)).readText()
        stream.close()
        return jsonString
    }

}