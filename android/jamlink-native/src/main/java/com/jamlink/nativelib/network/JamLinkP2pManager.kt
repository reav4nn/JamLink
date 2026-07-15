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
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.location.LocationManager
import android.os.Build
import android.provider.Settings

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
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    // Wifi P2P is enabled/disabled
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    @SuppressLint("MissingPermission")
                    manager?.requestPeers(channel) { peerList: WifiP2pDeviceList? ->
                        val devices = peerList?.deviceList?.toList() ?: emptyList()
                        _peers.value = devices
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    manager?.requestConnectionInfo(channel) { info ->
                        _connectionInfo.value = info
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    // This device details changed
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

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        }) ?: onFailure(WifiP2pManager.ERROR)
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
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        }) ?: onFailure(WifiP2pManager.ERROR)
    }

    fun disconnect(onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        }) ?: onFailure(WifiP2pManager.ERROR)
    }
}
