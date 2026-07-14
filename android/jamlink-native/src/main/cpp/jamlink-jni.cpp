#include <jni.h>
#include <string>
#include "jamlink-engine.h"

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

} // extern "C"
