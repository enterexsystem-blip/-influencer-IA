package com.enterex.influenceria

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL

class NotificationReaderService : NotificationListenerService() {
    companion object {
        const val AUTO_REPLY_ENABLED = "auto_reply_enabled"
        private const val BACKEND_URL = "https://influencer-ia-backend.onrender.com"
    }

    private fun contactMemoryId(packageName: String, title: String): String {
        val raw = "$packageName|${title.trim().lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        val shortHash = digest.take(16).joinToString("") { "%02x".format(it) }
        return "contact:$shortHash"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            )?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(MainActivity.KEY_PACKAGE, sbn.packageName)
            .putString(MainActivity.KEY_TITLE, title)
            .putString(MainActivity.KEY_TEXT, text)
            .putLong(MainActivity.KEY_TIME, System.currentTimeMillis())
            .apply()

        if (!prefs.getBoolean(AUTO_REPLY_ENABLED, false)) return
        if (text.isBlank() || sbn.isGroup || sbn.notification.category == Notification.CATEGORY_SERVICE) return

        // Only reply to notifications that explicitly expose an inline-reply action.
        val action = sbn.notification.actions?.firstOrNull { candidate ->
            !candidate.remoteInputs.isNullOrEmpty() && candidate.actionIntent != null
        } ?: return

        val remoteInputs = action.remoteInputs ?: return
        val dedupeKey = "auto_reply_seen_" + sbn.key.hashCode()
        if (prefs.getBoolean(dedupeKey, false)) return
        prefs.edit().putBoolean(dedupeKey, true).apply()

        Thread {
            try {
                val connection = (URL("$BACKEND_URL/chat").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 35000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                val reply = try {
                    val payload = JSONObject()
                        .put("message", "Responda naturalmente a esta mensagem recebida. Nome exibido do contato: $title. Mensagem: $text")
                        .put("relationshipLevel", 0)
                        .put("userId", contactMemoryId(sbn.packageName, title))
                        .toString()
                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    if (connection.responseCode !in 200..299) return@Thread
                    val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    JSONObject(response).optString("reply").trim()
                } finally {
                    connection.disconnect()
                }
                if (reply.isBlank()) return@Thread

                val intent = Intent()
                val results = Bundle()
                val remoteInput = remoteInputs.firstOrNull() ?: return@Thread
                results.putCharSequence(remoteInput.resultKey, reply)
                RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                action.actionIntent.send(this, 0, intent)
            } catch (_: Exception) {
                // Keep notification content private; do not log message text or generated replies.
            }
        }.start()
    }
}
