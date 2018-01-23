/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class idv_markkuo_ambitsync_MainActivity */

#ifndef _Included_idv_markkuo_ambitsync_MainActivity
#define _Included_idv_markkuo_ambitsync_MainActivity
#ifdef __cplusplus
extern "C" {
#endif



JNIEXPORT jlong JNICALL
Java_idv_markkuo_ambitsync_MainActivity_notifyDeviceAttached(JNIEnv *env, jclass type,
                                                                 jint vid, jint pid, jint fd,
                                                                 jstring path_);

JNIEXPORT void JNICALL
Java_idv_markkuo_ambitsync_MainActivity_notifyDeviceDetached(JNIEnv *env, jclass type,
                                                                 jlong device);


JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_getBatteryPercent(JNIEnv *env, jclass type, jlong device);

JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_syncHeader(JNIEnv *env, jclass type, jlong device, jobject record);

JNIEXPORT void JNICALL
Java_idv_markkuo_ambitsync_MainActivity_stopSync(JNIEnv *env, jclass type, jlong device);

JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_startSync(JNIEnv *env, jclass type, jlong device, jobject record);

JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_getEntryCount(JNIEnv *env, jclass type, jlong device);

JNIEXPORT void JNICALL
Java_idv_markkuo_ambitsync_MainActivity_nativeInit(JNIEnv *env, jclass type);

#ifdef __cplusplus
}
#endif
#endif
