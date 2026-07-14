package com.jamlink.nativelib.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class TcpCommandClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var clientJob: Job? = null

    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val commands = _commands.asSharedFlow()

    suspend fun connect(ipAddress: String, port: Int = 8988) = withContext(Dispatchers.IO) {
        try {
            socket = Socket()
            socket?.connect(InetSocketAddress(ipAddress, port), 5000)
            
            writer = PrintWriter(socket!!.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            
            clientJob = launch {
                try {
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        _commands.emit(line)
                    }
                } catch (e: Exception) {
                    // SocketException expected when socket is closed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    suspend fun sendCommand(commandJson: String) = withContext(Dispatchers.IO) {
        try {
            writer?.println(commandJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        clientJob?.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        writer = null
    }
}
