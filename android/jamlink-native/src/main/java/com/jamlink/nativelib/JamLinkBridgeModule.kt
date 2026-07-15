package com.jamlink.nativelib

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import com.jamlink.nativelib.network.NetworkStateManager

@ReactModule(name = JamLinkBridgeModule.NAME)
class JamLinkBridgeModule(private val reactContext: ReactApplicationContext) :
    NativeJamLinkBridgeSpec(reactContext) {

    private val networkManager = NetworkStateManager(reactContext)

    companion object {
        const val NAME = "JamLinkBridge"

        init {
            System.loadLibrary("jamlink-native")
        }
    }

    init {
        networkManager.initialize()
    }

    override fun invalidate() {
        super.invalidate()
        networkManager.cleanup()
    }

    override fun getName(): String = NAME

    // JNI declaration — implemented in jamlink-jni.cpp
    private external fun nativePing(input: String): String

    @ReactMethod
    override fun ping(input: String, promise: Promise) {
        try {
            val result = nativePing(input)
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("PING_ERROR", e.message, e)
        }
    }

    @ReactMethod
    override fun requestPermissions(promise: Promise) {
        val hasLocation = ContextCompat.checkSelfPermission(
            reactContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        var hasNearby = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNearby = ContextCompat.checkSelfPermission(
                reactContext, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        }

        promise.resolve(hasLocation && hasNearby)
    }

    @ReactMethod
    override fun startDiscovery(promise: Promise) {
        try {
            networkManager.p2pManager.startDiscovery(
                onSuccess = { promise.resolve(null) },
                onFailure = { promise.reject("DISCOVERY_ERROR", "Reason Code: $it") },
                onLocationDisabled = {
                    val map = com.facebook.react.bridge.Arguments.createMap()
                    map.putString("reason", "LOCATION_DISABLED")
                    reactContext
                        .getJSModule(com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("onDiscoveryBlocked", map)
                    promise.reject("LOCATION_DISABLED", "Location services are disabled")
                }
            )
        } catch (e: Exception) {
            promise.reject("DISCOVERY_CRASH", e.message, e)
        }
    }

    @ReactMethod
    override fun stopDiscovery(promise: Promise) {
        try {
            networkManager.p2pManager.stopDiscovery(
                onSuccess = { promise.resolve(null) },
                onFailure = { promise.reject("STOP_DISCOVERY_ERROR", "Reason Code: $it") }
            )
        } catch (e: Exception) {
            promise.reject("STOP_DISCOVERY_CRASH", e.message, e)
        }
    }

    @ReactMethod
    override fun createGroup(promise: Promise) {
        try {
            networkManager.p2pManager.createGroup(
                onSuccess = { promise.resolve(null) },
                onFailure = { promise.reject("CREATE_GROUP_ERROR", "Reason Code: $it") }
            )
        } catch (e: Exception) {
            promise.reject("CREATE_GROUP_CRASH", e.message, e)
        }
    }

    @ReactMethod
    override fun connectToDevice(deviceAddress: String, promise: Promise) {
        try {
            networkManager.p2pManager.connectToDevice(
                deviceAddress,
                onSuccess = { promise.resolve(null) },
                onFailure = { promise.reject("CONNECT_ERROR", "Reason Code: $it") }
            )
        } catch (e: Exception) {
            promise.reject("CONNECT_CRASH", e.message, e)
        }
    }

    @ReactMethod
    override fun disconnect(promise: Promise) {
        try {
            networkManager.p2pManager.disconnect(
                onSuccess = { promise.resolve(null) },
                onFailure = { promise.reject("DISCONNECT_ERROR", "Reason Code: $it") }
            )
        } catch (e: Exception) {
            promise.reject("DISCONNECT_CRASH", e.message, e)
        }
    }

    @ReactMethod
    override fun sendCommand(commandJson: String, promise: Promise) {
        try {
            networkManager.sendCommand(commandJson)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("SEND_COMMAND_CRASH", e.message, e)
        }
    }

    @ReactMethod
    override fun getTimeSyncState(promise: Promise) {
        try {
            val stateJson = networkManager.timeSyncManager.getTimeSyncStateJson()
            promise.resolve(stateJson)
        } catch (e: Exception) {
            promise.reject("GET_SYNC_STATE_CRASH", e.message, e)
        }
    }

    @ReactMethod
    override fun forceSyncNow(promise: Promise) {
        try {
            networkManager.timeSyncManager.forceSyncNow()
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("FORCE_SYNC_CRASH", e.message, e)
        }
    }
}
