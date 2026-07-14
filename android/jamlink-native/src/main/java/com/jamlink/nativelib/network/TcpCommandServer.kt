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
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class TcpCommandServer {
    private var serverSocket: ServerSocket? = null
    private val clientWriters = ConcurrentHashMap<String, PrintWriter>()
    private var serverJob: Job? = null

    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val commands = _commands.asSharedFlow()

    suspend fun start(port: Int = 8988) = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(port)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext
        }
        
        serverJob = launch {
            try {
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch { handleClient(socket) }
                }
            } catch (e: Exception) {
                // SocketException is expected when serverSocket is closed by stop()
            }
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val clientId = socket.inetAddress.hostAddress ?: "Unknown"
        try {
            val writer = PrintWriter(socket.getOutputStream(), true)
            clientWriters[clientId] = writer
            
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (isActive) {
                val line = reader.readLine() ?: break
                _commands.emit(line)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clientWriters.remove(clientId)
            socket.close()
        }
    }

    suspend fun broadcastCommand(commandJson: String) = withContext(Dispatchers.IO) {
        clientWriters.values.forEach { writer ->
            try {
                writer.println(commandJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverSocket?.close()
        clientWriters.clear()
    }
}
