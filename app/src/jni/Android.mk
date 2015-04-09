LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := api
LOCAL_SRC_FILES := api.c

include $(BUILD_SHARED_LIBRARY)