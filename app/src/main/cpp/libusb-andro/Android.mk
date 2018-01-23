LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  $(LOCAL_PATH)/libusb/core.c \
  $(LOCAL_PATH)/libusb/descriptor.c \
  $(LOCAL_PATH)/libusb/hotplug.c \
  $(LOCAL_PATH)/libusb/io.c \
  $(LOCAL_PATH)/libusb/sync.c \
  $(LOCAL_PATH)/libusb/strerror.c \
  $(LOCAL_PATH)/libusb/os/linux_usbfs.c \
  $(LOCAL_PATH)/libusb/os/poll_posix.c \
  $(LOCAL_PATH)/libusb/os/threads_posix.c \
  $(LOCAL_PATH)/libusb/os/linux_netlink.c

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)/libusb \
  $(LOCAL_PATH)/libusb/os

LOCAL_EXPORT_C_INCLUDES := \
  $(LOCAL_PATH)/libusb

LOCAL_LDLIBS := -llog
LOCAL_MODULE := libusb-android

include $(BUILD_SHARED_LIBRARY)
