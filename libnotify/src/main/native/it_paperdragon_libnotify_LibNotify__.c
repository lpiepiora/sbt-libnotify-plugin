#include <stdio.h>
#include "it_paperdragon_libnotify_LibNotify__.h"

// We need to call notifications from JNI code
#include <glib.h>
#include <unistd.h>
#include <libnotify/notify.h>

// globally held notificatoin pointer to update the existing notificaiton
// this is required because ubuntu doesn't allow to hide notifications
NotifyNotification* notification  = NULL;

/*
 * Class:     it_paperdragon_libnotify_LibNotify__
 * Method:    notifyInitializeInternal
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_it_paperdragon_libnotify_LibNotify_00024_notifyInitializeInternal
  (JNIEnv* env, jobject this, jstring app_name) {
  
  const char* native_app_name = (*env)->GetStringUTFChars(env, app_name, NULL);
  gboolean success = notify_init(native_app_name);
  (*env)->ReleaseStringUTFChars(env, app_name, native_app_name);
    
  return success ? JNI_TRUE : JNI_FALSE;

}

/*
 * Class:     it_paperdragon_libnotify_LibNotify__
 * Method:    showNotificationInternal
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_it_paperdragon_libnotify_LibNotify_00024_showNotificationInternal
  (JNIEnv* env, jobject this, jstring summary, jstring body, jstring icon) {

  GError*             error         = NULL;
  gboolean            success;

  const char* native_summary = (*env)->GetStringUTFChars(env, summary, NULL);
  const char* native_body    = (*env)->GetStringUTFChars(env, body, NULL);
  const char* native_icon    = (*env)->GetStringUTFChars(env, icon, NULL);
  
  if (notification == NULL) {
    notification = notify_notification_new(native_summary, native_body, native_icon);
  } else {
    notify_notification_update(notification, native_summary, native_body, native_icon);
  }

  (*env)->ReleaseStringUTFChars(env, summary, native_summary);
  (*env)->ReleaseStringUTFChars(env, body, native_body);
  (*env)->ReleaseStringUTFChars(env, icon, native_icon);

  success = notify_notification_show (notification, &error);
  if (!success) {
    jstring jvm_error = (*env)->NewStringUTF(env, error->message);
    g_error_free (error);
    return jvm_error != NULL ? jvm_error : (*env)->NewStringUTF(env, "Error occurred!");
  } else {
    return NULL;
  }

}

/*
 * Class:     it_paperdragon_libnotify_LibNotify__
 * Method:    notifyDestroyInternal
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_it_paperdragon_libnotify_LibNotify_00024_notifyDestroyInternal
  (JNIEnv* env, jobject this) {
    notify_uninit ();
}


