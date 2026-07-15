package com.jamlink.nativelib.network.timesync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Role { MASTER, CLIENT, NONE }

class TimeSyncManager(
    private val scope: CoroutineScope
) {
    private var role: Role = Role.NONE
    private var masterIp: String? = null
    private var server: TimeSyncServer? = null
    var client: TimeSyncClient? = null
        private set
        
    private var syncJob: Job? = null
    private var isRunning = false

    private val _stateFlow = MutableStateFlow(TimeSyncState())
    val stateFlow: StateFlow<TimeSyncState> = _stateFlow.asStateFlow()

    companion object {
        private const val TAG = "TimeSyncManager"
    }

    fun start(newRole: Role, newMasterIp: String? = null) {
        if (isRunning) return
        isRunning = true
        role = newRole
        masterIp = newMasterIp

        if (role == Role.MASTER) {
            Log.d(TAG, "Starting TimeSyncServer on port 8989")
            server = TimeSyncServer(scope)
            scope.launch {
                server?.start()
            }
        } else if (role == Role.CLIENT) {
            val ip = masterIp
            if (ip != null) {
                Log.d(TAG, "Starting TimeSyncClient, Master IP: $ip")
                client = TimeSyncClient(ip)
                scope.launch {
                    client?.currentState?.collect { state ->
                        _stateFlow.value = state
                    }
                }
                startClientLoop()
            }
        }
    }
    
    private fun startClientLoop() {
        syncJob = scope.launch(Dispatchers.IO) {
            // Initial 3 consecutive bursts
            for (i in 0 until 3) {
                if (!isActive) return@launch
                runBurstWithRetries()
                delay(500)
            }
            
            // Steady-state re-sync every 10 seconds
            while (isActive) {
                delay(10000)
                if (!isActive) break
                runBurstWithRetries()
            }
        }
    }
    
    private suspend fun runBurstWithRetries() {
        var attempts = 0
        var success = false
        while (attempts < 3 && !success) {
            attempts++
            success = client?.runBurst() ?: false
            if (!success && attempts < 3) {
                Log.w(TAG, "Burst attempt $attempts failed, retrying immediately...")
            }
        }
        
        if (!success) {
            Log.w(TAG, "All 3 burst attempts failed. Backing off to next interval.")
        }
    }

    fun forceSyncNow() {
        if (role != Role.CLIENT) return
        scope.launch(Dispatchers.IO) {
            runBurstWithRetries()
        }
    }

    fun stop() {
        isRunning = false
        syncJob?.cancel()
        server?.stop()
        server = null
        client = null
    }

    fun getTimeSyncStateJson(): String {
        val state = client?.currentState?.value ?: return "{}"
        return """{
            "status": "${state.syncStatus}",
            "offsetMs": ${state.offsetNs / 1_000_000},
            "rttMs": ${state.rttNs / 1_000_000},
            "sampleCount": ${state.sampleCount}
        }"""
    }
}
