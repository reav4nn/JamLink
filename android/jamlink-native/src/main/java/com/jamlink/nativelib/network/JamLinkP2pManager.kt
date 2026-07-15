package com.jamlink.nativelib.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.WpsInfo
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log

class JamLinkP2pManager(private val context: Context) {

    private val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    
    private var channel: WifiP2pManager.Channel? = null
    
    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d("JamLinkP2p", "BroadcastReceiver onReceive: action=$action")
            when (action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    Log.d("JamLinkP2p", "WIFI_P2P_STATE_CHANGED_ACTION: state=$state")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d("JamLinkP2p", "WIFI_P2P_PEERS_CHANGED_ACTION received, requesting peers...")
                    @SuppressLint("MissingPermission")
                    manager?.requestPeers(channel) { peerList: WifiP2pDeviceList? ->
                        val devices = peerList?.deviceList?.toList() ?: emptyList()
                        Log.d("JamLinkP2p", "requestPeers callback: found ${devices.size} peers")
                        devices.forEach { device ->
                            Log.d("JamLinkP2p", " - Peer: ${device.deviceName} (${device.deviceAddress}), status=${device.status}")
                        }
                        _peers.value = devices
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d("JamLinkP2p", "WIFI_P2P_CONNECTION_CHANGED_ACTION received, requesting connection info...")
                    manager?.requestConnectionInfo(channel) { info ->
                        Log.d("JamLinkP2p", "requestConnectionInfo callback: groupFormed=${info?.groupFormed}, isGroupOwner=${info?.isGroupOwner}, ownerAddress=${info?.groupOwnerAddress?.hostAddress}")
                        _connectionInfo.value = info
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    Log.d("JamLinkP2p", "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION received")
                }
            }
        }
    }
    
    val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    fun initialize() {
        channel = manager?.initialize(context, Looper.getMainLooper(), null)
        context.registerReceiver(receiver, intentFilter)
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(
        onSuccess: () -> Unit,
        onFailure: (Int) -> Unit,
        onLocationDisabled: (() -> Unit)? = null
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            val mode = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            mode != Settings.Secure.LOCATION_MODE_OFF
        }

        if (!isLocationEnabled) {
            onLocationDisabled?.invoke()
            return
        }

        Log.d("JamLinkP2p", "startDiscovery: Initiating peer discovery...")
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("JamLinkP2p", "discoverPeers: onSuccess callback fired")
                onSuccess()
            }
            override fun onFailure(reasonCode: Int) {
                Log.e("JamLinkP2p", "discoverPeers: onFailure callback fired with reasonCode=$reasonCode")
                onFailure(reasonCode)
            }
        }) ?: run {
            Log.e("JamLinkP2p", "discoverPeers failed: manager is null")
            onFailure(WifiP2pManager.ERROR)
        }
    }

    fun stopDiscovery(onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        }) ?: onFailure(WifiP2pManager.ERROR)
    }

    @SuppressLint("MissingPermission")
    fun createGroup(onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        }) ?: onFailure(WifiP2pManager.ERROR)
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String, onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            this.wps.setup = WpsInfo.PBC
        }
        
        Log.d("JamLinkP2p", "connectToDevice: Cancelling any pending connection first...")
        manager?.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("JamLinkP2p", "cancelConnect: onSuccess")
                executeConnect(config, onSuccess, onFailure)
            }
            override fun onFailure(reasonCode: Int) {
                Log.d("JamLinkP2p", "cancelConnect: onFailure (reasonCode=$reasonCode) - proceeding anyway")
                executeConnect(config, onSuccess, onFailure)
            }
        }) ?: run {
            Log.e("JamLinkP2p", "cancelConnect failed: manager is null")
            onFailure(WifiP2pManager.ERROR)
        }
    }

    @SuppressLint("MissingPermission")
    private fun executeConnect(config: WifiP2pConfig, onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        Log.d("JamLinkP2p", "executeConnect: Using config { deviceAddress=${config.deviceAddress}, wps.setup=${config.wps.setup} }")
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("JamLinkP2p", "connect: onSuccess callback fired")
                onSuccess()
            }
            override fun onFailure(reasonCode: Int) {
                Log.e("JamLinkP2p", "connect: onFailure callback fired with reasonCode=$reasonCode")
                onFailure(reasonCode)
            }
        }) ?: run {
            Log.e("JamLinkP2p", "connect failed: manager is null")
            onFailure(WifiP2pManager.ERROR)
        }
    }

    fun disconnect(onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        }) ?: onFailure(WifiP2pManager.ERROR)
    }
}
