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
        const val PENDING_REPLY = "pending_accessibility_reply"
        const val PENDING_REPLY_TEXT = "pending_accessibility_reply_text"
        const val PENDING_REPLY_TITLE = "pending_accessibility_reply_title"
        const val PENDING_REPLY_PACKAGE = "pending_accessibility_reply_package"
        const val PENDING_REPLY_MESSAGE = "pending_accessibility_reply_message"
        const val PENDING_REPLY_TIME = "pending_accessibility_reply_time"
        private const val BACKEND_URL = "https://influencer-ia-backend.onrender.com"
        private const val CREATOR_HANDLE = "kyodai.logic"
        private const val DEFAULT_FRIENDSHIP = 65
    }

    private fun setReplyStatus(prefs: android.content.SharedPreferences, status: String) {
        prefs.edit().putString("last_auto_reply_status", status)
            .putLong("last_auto_reply_status_time", System.currentTimeMillis()).apply()
    }

    private fun normalized(value: String): String =
        value.lowercase().replace("@", "").replace(Regex("[^a-z0-9._]"), "")

    private fun isCreator(title: String, text: String): Boolean =
        normalized("$title $text").contains(CREATOR_HANDLE)

    private fun contactMemoryId(packageName: String, title: String): String {
        val raw = "$packageName|${title.trim().lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return "contact:" + digest.take(16).joinToString("") { "%02x".format(it) }
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
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            )?.toString().orEmpty().trim()
        if (title.isBlank() && text.isBlank()) return

        prefs.edit()
            .putString(MainActivity.KEY_PACKAGE, sbn.packageName)
            .putString(MainActivity.KEY_TITLE, title)
            .putString(MainActivity.KEY_TEXT, text)
            .putLong(MainActivity.KEY_TIME, System.currentTimeMillis())
            .apply()

        if (!prefs.getBoolean(AUTO_REPLY_ENABLED, false)) {
            setReplyStatus(prefs, "Notificação lida, mas as respostas automáticas estão desligadas.")
            return
        }
        if (text.isBlank()) {
            setReplyStatus(prefs, "Notificação recebida sem texto de mensagem.")
            return
        }
        if (sbn.notification.category == Notification.CATEGORY_SERVICE) {
            setReplyStatus(prefs, "Notificação ignorada por ser um serviço do Android.")
            return
        }

        // Instagram can repost/update the same notification after a reply. Do not
        // use postTime as the only dedupe key because it changes on notification updates.
        val now = System.currentTimeMillis()
        val messageFingerprint = MessageDigest.getInstance("SHA-256")
            .digest("${sbn.packageName}|${title.lowercase()}|${text.lowercase()}".toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
        val dedupeKey = "auto_reply_seen_message_$messageFingerprint"
        val previousSeenAt = prefs.getLong(dedupeKey, 0L)
        if (previousSeenAt > 0L && now - previousSeenAt < 10 * 60 * 1000L) {
            setReplyStatus(prefs, "Notificação repetida ignorada para evitar spam.")
            return
        }

        // Extra per-contact cooldown: even if Instagram changes the notification text,
        // do not auto-send multiple replies to the same chat in quick succession.
        val contactCooldownKey = "auto_reply_contact_cooldown_" +
            MessageDigest.getInstance("SHA-256")
                .digest("${sbn.packageName}|${title.lowercase()}".toByteArray(Charsets.UTF_8))
                .take(10).joinToString("") { "%02x".format(it) }
        val previousContactReplyAt = prefs.getLong(contactCooldownKey, 0L)
        if (previousContactReplyAt > 0L && now - previousContactReplyAt < 60_000L) {
            setReplyStatus(prefs, "Resposta automática pausada por segurança: aguarde antes de responder novamente a este contato.")
            return
        }
        prefs.edit()
            .putLong(dedupeKey, now)
            .putLong(contactCooldownKey, now)
            .apply()

        // O Instagram Lite pode não fornecer RemoteInput. Sem resposta rápida, deixamos
        // a resposta pronta e aguardamos a conversa correspondente aberta no app.
        val action = sbn.notification.actions?.firstOrNull { candidate ->
            !candidate.remoteInputs.isNullOrEmpty() && candidate.actionIntent != null
        }
        val remoteInputs = action?.remoteInputs
        setReplyStatus(prefs, if (action == null) {
            "Mensagem detectada. Preparando resposta para envio assistido no Instagram Lite…"
        } else {
            "Mensagem detectada. Preparando resposta…"
        })

        val creator = isCreator(title, text)
        val fKey = friendshipKey(sbn.packageName, title)
        val friendship = if (creator) 100 else prefs.getInt(fKey, DEFAULT_FRIENDSHIP).coerceIn(0, 100)

        Thread {
            try {
                val delayMs = if (creator) Random.nextLong(900, 2200) else {
                    val baseDelay = 2_000L + ((100 - friendship) * 150L)
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
                        "A pessoa que escreveu é Kyodai, o criador @kyodai.logic e namorado da personagem. Responda com carinho, intimidade natural e prioridade, sem exagerar."
                    } else {
                        "Nível atual de amizade com esta pessoa: $relationshipLevel/100. Ajuste a intimidade ao nível: se baixo, seja mais distante; se alto, mais próxima. Não mencione pontuação nem explique que está escolhendo quando responder."
                    }
                    val payload = JSONObject()
                        .put("message", "$relationshipNote\nNome exibido do contato: $title. Mensagem recebida: $text")
                        .put("relationshipLevel", relationshipLevel)
                        .put("userId", if (creator) "creator:kyodai.logic" else contactMemoryId(sbn.packageName, title))
                        .toString()
                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val status = connection.responseCode
                    if (status !in 200..299) {
                        setReplyStatus(prefs, "O servidor da Ayla falhou ao gerar a resposta (HTTP $status).")
                        return@Thread
                    }
                    val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    JSONObject(response).optString("reply").trim()
                } finally {
                    connection.disconnect()
                }

                if (reply.isBlank()) {
                    setReplyStatus(prefs, "O servidor não devolveu uma resposta de texto.")
                    return@Thread
                }

                if (action != null && !remoteInputs.isNullOrEmpty()) {
                    val intent = Intent()
                    val results = Bundle()
                    val remoteInput = remoteInputs.firstOrNull() ?: return@Thread
                    results.putCharSequence(remoteInput.resultKey, reply)
                    RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                    action.actionIntent.send(this, 0, intent)
                    setReplyStatus(prefs, if (creator) "Resposta enviada para Kyodai." else "Resposta enviada pelo botão de resposta da notificação.")
                } else {
                    prefs.edit()
                        .putString(PENDING_REPLY_TEXT, reply)
                        .putString(PENDING_REPLY_TITLE, title)
                        .putString(PENDING_REPLY_PACKAGE, sbn.packageName)
                        .putString(PENDING_REPLY_MESSAGE, text)
                        .putLong(PENDING_REPLY_TIME, System.currentTimeMillis())
                        .putBoolean(PENDING_REPLY, true)
                        .apply()
                    setReplyStatus(prefs, "Resposta pronta para '$title'. Abra essa conversa no Instagram Lite; a Ayla só tentará enviar se reconhecer o chat.")
                }

                if (!creator) prefs.edit().putInt(fKey, (friendship + 2).coerceAtMost(100)).apply()
            } catch (error: Exception) {
                setReplyStatus(prefs, "Falha ao responder: ${error.javaClass.simpleName}. Confira a conexão e a notificação.")
            }
        }.start()
    }
}
