package com.jamlink.native

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = JamLinkBridgeModule.NAME)
class JamLinkBridgeModule(reactContext: ReactApplicationContext) :
    NativeJamLinkBridgeSpec(reactContext) {

    companion object {
        const val NAME = "JamLinkBridge"

        init {
            System.loadLibrary("jamlink-native")
        }
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
}
