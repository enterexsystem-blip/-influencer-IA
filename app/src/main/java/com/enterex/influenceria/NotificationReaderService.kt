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
        const val PENDING_REPLY_PACKAGE = "pending_reply_package"
        const val PENDING_REPLY_TITLE = "pending_reply_title"
        const val PENDING_REPLY_TEXT = "pending_reply_text"
        const val PENDING_REPLY_MESSAGE = "pending_reply_original_message"
        const val PENDING_REPLY_TIME = "pending_reply_time"
        private const val BACKEND_URL = "https://influencer-ia-backend.onrender.com"
        private const val CREATOR_HANDLE = "salem.logic"
        private const val DEFAULT_FRIENDSHIP = 65
    }

    private fun setReplyStatus(prefs: android.content.SharedPreferences, status: String) {
        prefs.edit().putString("last_auto_reply_status", status)
            .putLong("last_auto_reply_status_time", System.currentTimeMillis()).apply()
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

        // Instagram Lite pode notificar a mensagem sem expor uma ação RemoteInput.
        // Ainda assim, geramos a resposta e exibimos o motivo quando não for possível enviá-la.
        val action = sbn.notification.actions?.firstOrNull { candidate ->
            !candidate.remoteInputs.isNullOrEmpty() && candidate.actionIntent != null
        }
        val remoteInputs = action?.remoteInputs

        val dedupeKey = "auto_reply_seen_" + sbn.key.hashCode() + "_" + sbn.postTime
        if (prefs.getBoolean(dedupeKey, false)) return
        prefs.edit().putBoolean(dedupeKey, true).apply()
        setReplyStatus(prefs, "Mensagem detectada. Preparando resposta…")

        val creator = isCreator(title, text)
        val fKey = friendshipKey(sbn.packageName, title)
        val friendship = if (creator) 100 else prefs.getInt(fKey, DEFAULT_FRIENDSHIP).coerceIn(0, 100)

        Thread {
            try {
                // Responder em poucos segundos para que o teste seja fácil de verificar.
                val delayMs = if (creator) Random.nextLong(900, 1800) else Random.nextLong(2_000, 6_000)
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
                    val statusCode = connection.responseCode
                    if (statusCode !in 200..299) {
                        setReplyStatus(prefs, "O servidor de respostas falhou (HTTP $statusCode). Abra o app e teste a conexão.")
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
                    val remoteInput = remoteInputs.firstOrNull()
                    if (remoteInput == null) {
                        prefs.edit().putString("last_auto_reply_draft", reply).apply()
                        setReplyStatus(prefs, "Resposta gerada, mas o Instagram Lite não forneceu um campo compatível. Rascunho salvo no app: $reply")
                        return@Thread
                    }
                    results.putCharSequence(remoteInput.resultKey, reply)
                    RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                    action.actionIntent.send(this, 0, intent)
                    prefs.edit().putString("last_auto_reply_draft", reply).apply()
                    setReplyStatus(prefs, if (creator) "Resposta enviada para Salem." else "Resposta enviada pela notificação.")
                } else {
                    // Fallback assistido: preparar a resposta e abrir a conversa da notificação.
                    // O envio automático só será tentado se o usuário habilitar o serviço de acessibilidade.
                    prefs.edit()
                        .putString("last_auto_reply_draft", reply)
                        .putBoolean(PENDING_REPLY, true)
                        .putString(PENDING_REPLY_PACKAGE, sbn.packageName)
                        .putString(PENDING_REPLY_TITLE, title)
                        .putString(PENDING_REPLY_TEXT, reply)
                        .putString(PENDING_REPLY_MESSAGE, text)
                        .putLong(PENDING_REPLY_TIME, System.currentTimeMillis())
                        .apply()
                    val openedConversation = try {
                        sbn.notification.contentIntent?.send(this, 0, Intent())
                        sbn.notification.contentIntent != null
                    } catch (_: Exception) {
                        false
                    }
                    setReplyStatus(
                        prefs,
                        if (openedConversation) {
                            "O Lite não oferece resposta rápida. Resposta preparada e conversa aberta; habilite o serviço de acessibilidade para tentar enviar com segurança."
                        } else {
                            "O Lite não oferece resposta rápida. Rascunho salvo no app: $reply"
                        }
                    )
                }

                if (!creator) {
                    prefs.edit().putInt(fKey, (friendship + 2).coerceAtMost(100)).apply()
                }
            } catch (error: Exception) {
                setReplyStatus(prefs, "Falha ao responder: ${error.javaClass.simpleName}. Confira a conexão e a notificação.")
                // Não registrar conteúdo privado das notificações nem das respostas.
            }
        }.start()
    }
}
