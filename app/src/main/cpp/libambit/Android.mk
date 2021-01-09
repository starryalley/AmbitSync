LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
# for libusb
#LIBUSB_PATH := $(LOCAL_PATH)/../libusb
#include $(LIBUSB_PATH)/android/jni/libusb.mk
#LOCAL_C_INCLUDES += $(LIBUSB_PATH)
LOCAL_SHARED_LIBRARIES += libusb-android libiconv

# for libambit

LOCAL_LDLIBS    := -llog

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/../iconv-android/include/ \
	$(LOCAL_PATH)/android/ \
	$(LOCAL_PATH)/../libusb-andro/libusb/ \
	$(LOCAL_PATH)/../libusb-andro/ \
	$(LOCAL_PATH)

LOCAL_SRC_FILES := \
	$(LOCAL_PATH)/hidapi/hidapi.h \
	$(LOCAL_PATH)/hidapi/hid-libusb.c \
	$(LOCAL_PATH)/crc16.c \
	$(LOCAL_PATH)/crc16.h \
	$(LOCAL_PATH)/debug.c \
	$(LOCAL_PATH)/debug.h \
	$(LOCAL_PATH)/device_driver_ambit3.c \
	$(LOCAL_PATH)/device_driver_ambit.c \
	$(LOCAL_PATH)/device_driver_common.c \
	$(LOCAL_PATH)/device_driver_common.h \
	$(LOCAL_PATH)/device_driver_ambit_navigation.c \
	$(LOCAL_PATH)/device_driver_ambit_navigation.h \
	$(LOCAL_PATH)/device_driver.h \
	$(LOCAL_PATH)/device_support.c \
	$(LOCAL_PATH)/device_support.h \
	$(LOCAL_PATH)/libambit.c \
	$(LOCAL_PATH)/libambit.h \
	$(LOCAL_PATH)/libambit_int.h \
	$(LOCAL_PATH)/personal.c \
	$(LOCAL_PATH)/personal.h \
	$(LOCAL_PATH)/sport_mode_serialize.c \
	$(LOCAL_PATH)/sport_mode_serialize.h \
	$(LOCAL_PATH)/pmem20.c \
	$(LOCAL_PATH)/pmem20.h \
	$(LOCAL_PATH)/distance.c \
	$(LOCAL_PATH)/distance.h \
	$(LOCAL_PATH)/protocol.c \
	$(LOCAL_PATH)/protocol.h \
	$(LOCAL_PATH)/sbem0102.c \
	$(LOCAL_PATH)/sbem0102.h \
	$(LOCAL_PATH)/sha256.c \
	$(LOCAL_PATH)/sha256.h \
	$(LOCAL_PATH)/utils.c \
	$(LOCAL_PATH)/utils.h

LOCAL_MODULE := ambit

include $(BUILD_SHARED_LIBRARY)
