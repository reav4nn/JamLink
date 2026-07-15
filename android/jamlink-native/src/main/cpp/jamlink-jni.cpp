#include <jni.h>
#include <string>
#include "jamlink-engine.h"
#include "ntp-math.h"

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_jamlink_nativelib_JamLinkBridgeModule_nativePing(
    JNIEnv *env,
    jobject /* this */,
    jstring input
) {
    const char *inputStr = env->GetStringUTFChars(input, nullptr);
    std::string result = jamlink::ping(std::string(inputStr));
    env->ReleaseStringUTFChars(input, inputStr);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_jamlink_nativelib_network_timesync_TimeSyncClient_nativeSetClockOffset(
    JNIEnv *env, jobject /* this */, jlong offsetNs
) {
    jamlink::ClockSync::instance().setOffset(offsetNs);
}

JNIEXPORT jlong JNICALL
Java_com_jamlink_nativelib_JamLinkBridgeModule_nativeGetClockOffset(
    JNIEnv *env, jobject /* this */
) {
    return jamlink::ClockSync::instance().getOffset();
}

} // extern "C"
