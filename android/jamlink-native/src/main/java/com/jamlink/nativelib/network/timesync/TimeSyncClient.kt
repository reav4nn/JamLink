package com.jamlink.nativelib.network.timesync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.delay

enum class SyncStatus { NOT_SYNCED, SYNCING, SYNCED, ERROR }

data class TimeSyncState(
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val offsetNs: Long = 0,
    val rttNs: Long = 0,
    val sampleCount: Int = 0,
    val lastSyncEpoch: Long = 0
)

class TimeSyncClient(private val masterIp: String) {
    private val _currentState = MutableStateFlow(TimeSyncState(syncStatus = SyncStatus.SYNCING))
    val currentState: StateFlow<TimeSyncState> = _currentState.asStateFlow()

    private var sequenceCounter: Long = 0
    private var smoothedOffsetNs: Long = 0
    private var totalValidSamples: Int = 0
    private var isFirstSample = true

    // JNI bindings for C++ ClockSync
    private external fun nativeSetClockOffset(offsetNs: Long)
    
    companion object {
        private const val TAG = "TimeSyncClient"
        private const val ALPHA = 0.3
    }

    suspend fun runBurst(port: Int = 8989): Boolean = withContext(Dispatchers.IO) {
        val serverAddr = InetAddress.getByName(masterIp)
        val socket = DatagramSocket()
        socket.soTimeout = 200 // 200ms timeout per packet
        
        val outstandingSequences = mutableSetOf<Long>()
        val results = mutableListOf<SyncResult>()
        
        try {
            for (i in 0 until 11) {
                val seq = sequenceCounter++
                outstandingSequences.add(seq)
                
                val t1 = System.nanoTime()
                val request = TimeSyncPacket(TimeSyncPacket.TYPE_REQUEST, t1, 0, 0, seq)
                val reqBytes = request.toByteArray()
                
                socket.send(DatagramPacket(reqBytes, reqBytes.size, serverAddr, port))
                
                val buffer = ByteArray(TimeSyncPacket.PACKET_SIZE)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                
                try {
                    socket.receive(responsePacket)
                    val t4 = System.nanoTime()
                    
                    val response = TimeSyncPacket.fromByteArray(buffer)
                    if (response.type == TimeSyncPacket.TYPE_RESPONSE && outstandingSequences.contains(response.sequence)) {
                        outstandingSequences.remove(response.sequence)
                        
                        val rtt = (t4 - response.t1) - (response.t3 - response.t2)
                        val offset = ((response.t2 - response.t1) + (response.t3 - t4)) / 2
                        
                        if (rtt > 0) {
                            results.add(SyncResult(rtt, offset))
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // Timeout, ignore and continue
                }
                
                delay(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _currentState.value = _currentState.value.copy(syncStatus = SyncStatus.ERROR)
            socket.close()
            return@withContext false
        } finally {
            socket.close()
        }
        
        if (results.size < 3) {
            Log.w(TAG, "Burst yielded < 3 valid samples (${results.size})")
            return@withContext false
        }
        
        // Outlier rejection (RTT > 3 * median)
        results.sortBy { it.rtt }
        val medianRtt = results[results.size / 2].rtt
        val threshold = medianRtt * 3
        
        val validResults = results.filter { it.rtt <= threshold }
        if (validResults.isEmpty()) return@withContext false
        
        // Pick minimum RTT
        val bestSample = validResults.minByOrNull { it.rtt } ?: return@withContext false
        
        // EMA Smoothing
        if (isFirstSample) {
            smoothedOffsetNs = bestSample.offset
            isFirstSample = false
        } else {
            smoothedOffsetNs = (ALPHA * bestSample.offset + (1 - ALPHA) * smoothedOffsetNs).toLong()
        }
        
        totalValidSamples += validResults.size
        
        try {
            // Push to C++ via JNI
            nativeSetClockOffset(smoothedOffsetNs)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to call nativeSetClockOffset - JNI not loaded?", e)
        }
        
        _currentState.value = TimeSyncState(
            syncStatus = SyncStatus.SYNCED,
            offsetNs = smoothedOffsetNs,
            rttNs = bestSample.rtt,
            sampleCount = totalValidSamples,
            lastSyncEpoch = System.currentTimeMillis()
        )
        
        return@withContext true
    }

    private data class SyncResult(val rtt: Long, val offset: Long)
}
