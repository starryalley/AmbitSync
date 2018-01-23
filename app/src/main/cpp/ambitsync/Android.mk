LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/../libambit

LOCAL_SRC_FILES := \
	$(LOCAL_PATH)/ambitsync.c

LOCAL_SHARED_LIBRARIES += libambit
LOCAL_LDLIBS    := -landroid -llog

LOCAL_MODULE := ambitsync

include $(BUILD_SHARED_LIBRARY)

