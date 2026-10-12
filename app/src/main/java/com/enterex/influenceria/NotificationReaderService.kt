package com.enterex.influenceria

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.random.Random

class NotificationReaderService : NotificationListenerService() {
    companion object {
        const val AUTO_REPLY_ENABLED = "auto_reply_enabled"
        private const val BACKEND_URL = "https://influencer-ia-backend.onrender.com"
        private const val CREATOR_HANDLE = "salem.logic"
        private const val DEFAULT_FRIENDSHIP = 65
    }

    private fun normalized(value: String): String =
        value.lowercase().replace("@", "").replace(Regex("[^a-z0-9._]"), "")

    private fun isCreator(title: String, text: String): Boolean {
        val joined = normalized("$title $text")
        return joined.contains(CREATOR_HANDLE)
    }

    private fun contactMemoryId(packageName: String, title: String): String {
        val raw = "$packageName|${title.trim().lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        val shortHash = digest.take(16).joinToString("") { "%02x".format(it) }
        return "contact:$shortHash"
    }

    private fun friendshipKey(packageName: String, title: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$packageName|${title.trim().lowercase()}".toByteArray(Charsets.UTF_8))
        return "friendship_" + digest.take(12).joinToString("") { "%02x".format(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val selectedPackage = prefs.getString(MainActivity.KEY_SELECTED_PACKAGE, "").orEmpty()
        if (selectedPackage.isBlank() || sbn.packageName != selectedPackage) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            )?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return

        prefs.edit()
            .putString(MainActivity.KEY_PACKAGE, sbn.packageName)
            .putString(MainActivity.KEY_TITLE, title)
            .putString(MainActivity.KEY_TEXT, text)
            .putLong(MainActivity.KEY_TIME, System.currentTimeMillis())
            .apply()

        if (!prefs.getBoolean(AUTO_REPLY_ENABLED, false)) return
        if (text.isBlank() || sbn.isGroup || sbn.notification.category == Notification.CATEGORY_SERVICE) return

        val action = sbn.notification.actions?.firstOrNull { candidate ->
            !candidate.remoteInputs.isNullOrEmpty() && candidate.actionIntent != null
        } ?: return
        val remoteInputs = action.remoteInputs ?: return

        val dedupeKey = "auto_reply_seen_" + sbn.key.hashCode() + "_" + sbn.postTime
        if (prefs.getBoolean(dedupeKey, false)) return
        prefs.edit().putBoolean(dedupeKey, true).apply()

        val creator = isCreator(title, text)
        val fKey = friendshipKey(sbn.packageName, title)
        val friendship = if (creator) 100 else prefs.getInt(fKey, DEFAULT_FRIENDSHIP).coerceIn(0, 100)

        if (!creator) {
            val replyChance = (45 + friendship / 2).coerceIn(45, 95)
            if (Random.nextInt(100) >= replyChance) {
                prefs.edit().putInt(fKey, (friendship - 5).coerceAtLeast(10)).apply()
                return
            }
        }

        Thread {
            try {
                val delayMs = if (creator) {
                    Random.nextLong(900, 2200)
                } else {
                    val baseDelay = 4_000L + ((100 - friendship) * 900L)
                    Random.nextLong(baseDelay, baseDelay + 8_000L).coerceAtMost(120_000L)
                }
                Thread.sleep(delayMs)

                val connection = (URL("$BACKEND_URL/chat").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 35000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                val reply = try {
                    val relationshipLevel = if (creator) 100 else friendship
                    val relationshipNote = if (creator) {
                        "A pessoa que escreveu é Salem, o criador @salem.logic e namorado da personagem. Responda com carinho, intimidade natural e prioridade, sem exagerar."
                    } else {
                        "Nível atual de amizade com esta pessoa: $relationshipLevel/100. Ajuste a intimidade ao nível: se baixo, seja mais distante; se alto, mais próxima. Não mencione pontuação nem explique que está escolhendo quando responder."
                    }
                    val payload = JSONObject()
                        .put("message", "$relationshipNote\nNome exibido do contato: $title. Mensagem recebida: $text")
                        .put("relationshipLevel", relationshipLevel)
                        .put("userId", if (creator) "creator:salem.logic" else contactMemoryId(sbn.packageName, title))
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

                if (!creator) {
                    prefs.edit().putInt(fKey, (friendship + 2).coerceAtMost(100)).apply()
                }
            } catch (_: Exception) {
                // Avoid logging private notification contents or generated replies.
            }
        }.start()
    }
}
