package com.enterex.influenceria

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    companion object {
        const val PREFS_NAME = "influencer_ia_local"
        const val KEY_PACKAGE = "latest_package"
        const val KEY_TITLE = "latest_title"
        const val KEY_TEXT = "latest_text"
        const val KEY_TIME = "latest_time"
        private const val BACKEND_URL = "https://influencer-ia-backend.onrender.com"
    }

    private lateinit var statusText: TextView
    private lateinit var previewText: TextView
    private lateinit var packageText: TextView
    private lateinit var timeText: TextView
    private lateinit var messageInput: EditText
    private lateinit var replyText: TextView
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        refreshStatusAndPreview()
    }

    private fun buildScreen() {
        val background = Color.rgb(12, 15, 24)
        val panel = Color.rgb(23, 28, 42)
        val accent = Color.rgb(91, 115, 255)
        val white = Color.rgb(240, 243, 255)
        val muted = Color.rgb(166, 175, 199)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(dp(22), dp(28), dp(22), dp(24))
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(label("ENTEREX  /  ANDROID", 12, accent, true), matchWrap())
        val heading = label("Influencer IA", 30, white, true)
        heading.setPadding(0, dp(8), 0, dp(4))
        content.addView(heading, matchWrap())
        content.addView(label("Sua assistente de respostas com inteligência artificial", 14, muted, false), matchWrap())

        val statusCard = card(panel)
        statusCard.addView(label("STATUS DA PERMISSÃO", 11, muted, true), matchWrap())
        statusText = label("Verificando…", 18, white, true)
        statusText.setPadding(0, dp(10), 0, dp(6))
        statusCard.addView(statusText, matchWrap())
        statusCard.addView(label("O acesso às notificações é autorizado manualmente por você.", 13, muted, false), matchWrap())
        content.addView(statusCard, fullWidthWithTop())

        val permissionButton = Button(this).apply {
            text = "Configurar acesso às notificações"
            setTextColor(Color.WHITE)
            setBackgroundColor(accent)
            isAllCaps = false
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        content.addView(permissionButton, fullWidthWithTop())

        val refreshButton = Button(this).apply {
            text = "Atualizar notificações"
            isAllCaps = false
            setOnClickListener { refreshStatusAndPreview() }
        }
        content.addView(refreshButton, fullWidthWithTop())

        val previewCard = card(panel)
        previewCard.addView(label("ÚLTIMA NOTIFICAÇÃO DISPONÍVEL", 11, muted, true), matchWrap())
        packageText = label("Aplicativo: —", 13, accent, true)
        packageText.setPadding(0, dp(12), 0, dp(4))
        previewCard.addView(packageText, matchWrap())
        previewText = label("Nenhuma mensagem capturada ainda.", 16, white, false)
        previewText.setPadding(0, dp(6), 0, dp(6))
        previewCard.addView(previewText, matchWrap())
        timeText = label("", 12, muted, false)
        previewCard.addView(timeText, matchWrap())
        content.addView(previewCard, fullWidthWithTop())

        val chatCard = card(panel)
        chatCard.addView(label("TESTAR A INTELIGÊNCIA ARTIFICIAL", 11, muted, true), matchWrap())
        messageInput = EditText(this).apply {
            hint = "Escreva uma mensagem para a IA…"
            setTextColor(white)
            setHintTextColor(muted)
            textSize = 16f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minLines = 2
            maxLines = 5
            gravity = Gravity.TOP
            setBackgroundColor(Color.rgb(34, 40, 58))
        }
        val inputParams = matchWrap().apply { topMargin = dp(12) }
        chatCard.addView(messageInput, inputParams)

        sendButton = Button(this).apply {
            text = "Gerar resposta com IA"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(accent)
            setOnClickListener { generateReply() }
        }
        chatCard.addView(sendButton, fullWidthWithTop())

        replyText = label("A resposta da IA aparecerá aqui.", 15, white, false)
        replyText.setPadding(0, dp(14), 0, dp(4))
        chatCard.addView(replyText, matchWrap())
        content.addView(chatCard, fullWidthWithTop())

        content.addView(label(
            "PRIVACIDADE\nA leitura de notificações depende da permissão do Android e só acessa o conteúdo disponibilizado pelo próprio aviso. Nesta etapa, o app mostra a última notificação e permite testar a IA manualmente; ele ainda não envia respostas automaticamente para outros aplicativos.",
            12, muted, false
        ).apply { setPadding(dp(4), dp(18), dp(4), dp(8)) }, matchWrap())

        setContentView(root)
    }

    private fun generateReply() {
        val message = messageInput.text.toString().trim()
        if (message.isEmpty()) {
            replyText.text = "Escreve uma mensagem primeiro."
            return
        }
        sendButton.isEnabled = false
        sendButton.text = "Gerando resposta…"
        replyText.text = "Conectando ao servidor da IA…"

        Thread {
            var result = ""
            try {
                val connection = (URL("$BACKEND_URL/chat").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 35000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val payload = JSONObject()
                        .put("message", message)
                        .put("relationshipLevel", 0)
                        .toString()
                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(response) }.getOrNull()
                    result = if (status in 200..299) {
                        json?.optString("reply")?.takeIf { it.isNotBlank() }
                            ?: "O servidor respondeu, mas não enviou texto."
                    } else {
                        json?.optString("error")?.takeIf { it.isNotBlank() }
                            ?: "Erro HTTP $status ao consultar o servidor."
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                result = when (e) {
                    is java.net.UnknownHostException -> "Não consegui localizar o servidor. O endereço do Render pode ser diferente; precisamos confirmar o endereço público do serviço."
                    is java.net.SocketTimeoutException -> "O servidor demorou demais para responder. Tente novamente em alguns segundos."
                    else -> "Falha na conexão: ${e.localizedMessage ?: e.javaClass.simpleName}"
                }
            }
            runOnUiThread {
                replyText.text = result
                sendButton.isEnabled = true
                sendButton.text = "Gerar resposta com IA"
            }
        }.start()
    }

    private fun refreshStatusAndPreview() {
        if (!::statusText.isInitialized) return
        val component = ComponentName(this, NotificationReaderService::class.java)
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        val enabled = enabledListeners.split(':').any { it.equals(component.flattenToString(), ignoreCase = true) }
        statusText.text = if (enabled) "● Acesso autorizado" else "○ Acesso desativado"
        statusText.setTextColor(if (enabled) Color.rgb(101, 224, 167) else Color.rgb(255, 190, 90))

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "").orEmpty()
        val message = prefs.getString(KEY_TEXT, "").orEmpty()
        val packageName = prefs.getString(KEY_PACKAGE, "").orEmpty()
        val timestamp = prefs.getLong(KEY_TIME, 0L)

        packageText.text = if (packageName.isBlank()) "Aplicativo: —" else "Aplicativo: $packageName"
        previewText.text = when {
            title.isBlank() && message.isBlank() -> "Nenhuma mensagem capturada ainda."
            title.isBlank() -> message
            message.isBlank() -> title
            else -> "$title\n\n$message"
        }
        timeText.text = if (timestamp > 0L) {
            "Recebida: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
        } else ""
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun card(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(color)
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun fullWidthWithTop() = matchWrap().apply { topMargin = dp(14) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
