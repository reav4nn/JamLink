package com.jamlink.nativelib.network.timesync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import kotlinx.coroutines.CoroutineScope

class TimeSyncServer(private val scope: CoroutineScope) {
    private var socket: DatagramSocket? = null
    private var serverJob: Job? = null

    suspend fun start(port: Int = 8989) = withContext(Dispatchers.IO) {
        try {
            socket = DatagramSocket(port)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext
        }

        serverJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(TimeSyncPacket.PACKET_SIZE)
            try {
                while (isActive) {
                    val dgram = DatagramPacket(buffer, buffer.size)
                    socket?.receive(dgram) // blocking
                    val t2 = monoNs()

                    val request = TimeSyncPacket.fromByteArray(buffer)
                    if (request.type != TimeSyncPacket.TYPE_REQUEST) continue

                    val t3 = monoNs()
                    val response = TimeSyncPacket(
                        type = TimeSyncPacket.TYPE_RESPONSE,
                        t1 = request.t1,
                        t2 = t2,
                        t3 = t3,
                        sequence = request.sequence
                    )

                    val responseBytes = response.toByteArray()
                    socket?.send(DatagramPacket(
                        responseBytes, responseBytes.size,
                        dgram.address, dgram.port
                    ))
                }
            } catch (e: SocketException) {
                // Expected when socket is closed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        socket?.close()
    }

    private fun monoNs(): Long = System.nanoTime()
}
