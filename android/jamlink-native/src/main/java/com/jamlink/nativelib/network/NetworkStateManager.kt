package com.jamlink.nativelib.network

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class NetworkStateManager(private val reactContext: ReactApplicationContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    val p2pManager = JamLinkP2pManager(reactContext)
    private var tcpServer: TcpCommandServer? = null
    private var tcpClient: TcpCommandClient? = null

    private var currentRole: String = "NONE" // "MASTER", "CLIENT", "NONE"

    fun initialize() {
        p2pManager.initialize()
        
        scope.launch {
            p2pManager.peers.collectLatest { peers ->
                val array = Arguments.createArray()
                peers.forEach { peer ->
                    val map = Arguments.createMap()
                    map.putString("address", peer.deviceAddress)
                    map.putString("name", peer.deviceName)
                    map.putInt("status", peer.status)
                    array.pushMap(map)
                }
                sendEvent("onPeersUpdated", array)
            }
        }

        scope.launch {
            p2pManager.connectionInfo.collectLatest { info ->
                if (info == null) {
                    currentRole = "NONE"
                    sendConnectionState("DISCONNECTED")
                    stopTcp()
                    return@collectLatest
                }

                if (info.groupFormed && info.isGroupOwner) {
                    currentRole = "MASTER"
                    sendConnectionState("CONNECTED", "MASTER", info.groupOwnerAddress?.hostAddress)
                    startTcpServer()
                } else if (info.groupFormed) {
                    currentRole = "CLIENT"
                    sendConnectionState("CONNECTED", "CLIENT", info.groupOwnerAddress?.hostAddress)
                    startTcpClient(info.groupOwnerAddress?.hostAddress ?: return@collectLatest)
                } else {
                    currentRole = "NONE"
                    sendConnectionState("DISCONNECTED")
                    stopTcp()
                }
            }
        }
    }

    private fun startTcpServer() {
        stopTcp()
        tcpServer = TcpCommandServer()
        scope.launch {
            tcpServer?.start()
        }
        scope.launch {
            tcpServer?.commands?.collectLatest { cmd ->
                sendEvent("onCommandReceived", cmd)
            }
        }
    }

    private fun startTcpClient(ip: String) {
        stopTcp()
        tcpClient = TcpCommandClient()
        scope.launch {
            tcpClient?.connect(ip)
        }
        scope.launch {
            tcpClient?.commands?.collectLatest { cmd ->
                sendEvent("onCommandReceived", cmd)
            }
        }
    }

    private fun stopTcp() {
        tcpServer?.stop()
        tcpServer = null
        tcpClient?.stop()
        tcpClient = null
    }

    fun sendCommand(json: String) {
        scope.launch {
            if (currentRole == "MASTER") {
                tcpServer?.broadcastCommand(json)
            } else if (currentRole == "CLIENT") {
                tcpClient?.sendCommand(json)
            }
        }
    }

    fun cleanup() {
        p2pManager.cleanup()
        stopTcp()
    }

    private fun sendConnectionState(state: String, role: String = "NONE", masterIp: String? = null) {
        val map = Arguments.createMap()
        map.putString("state", state)
        map.putString("role", role)
        if (masterIp != null) {
            map.putString("masterIp", masterIp)
        }
        sendEvent("onConnectionStateChanged", map)
    }

    private fun sendEvent(eventName: String, data: Any) {
        if (reactContext.hasActiveReactInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, data)
        }
    }
}
