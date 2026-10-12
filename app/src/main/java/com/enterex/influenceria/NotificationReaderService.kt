package com.enterex.influenceria

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationReaderService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            )?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return

        // Keep only the latest notification locally. Do not log message content.
        getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(MainActivity.KEY_PACKAGE, sbn.packageName)
            .putString(MainActivity.KEY_TITLE, title)
            .putString(MainActivity.KEY_TEXT, text)
            .putLong(MainActivity.KEY_TIME, System.currentTimeMillis())
            .apply()
    }
}
