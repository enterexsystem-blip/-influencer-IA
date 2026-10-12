package com.enterex.influenceria

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    companion object {
        const val PREFS_NAME = "influencer_ia_local"
        const val KEY_PACKAGE = "latest_package"
        const val KEY_TITLE = "latest_title"
        const val KEY_TEXT = "latest_text"
        const val KEY_TIME = "latest_time"
    }

    private lateinit var statusText: TextView
    private lateinit var previewText: TextView
    private lateinit var packageText: TextView
    private lateinit var timeText: TextView

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

        val eyebrow = label("ENTEREX  /  ANDROID", 12, accent, true)
        content.addView(eyebrow, matchWrap())
        val heading = label("Influencer IA", 30, white, true)
        heading.setPadding(0, dp(8), 0, dp(4))
        content.addView(heading, matchWrap())
        content.addView(label("Central de acesso às notificações", 14, muted, false), matchWrap())

        val statusCard = card(panel)
        statusCard.addView(label("STATUS DA PERMISSÃO", 11, muted, true), matchWrap())
        statusText = label("Verificando…", 18, white, true)
        statusText.setPadding(0, dp(10), 0, dp(6))
        statusCard.addView(statusText, matchWrap())
        statusCard.addView(
            label("Você autoriza o acesso manualmente nas configurações do Android.", 13, muted, false),
            matchWrap()
        )
        content.addView(statusCard, fullWidthWithTop())

        val permissionButton = Button(this).apply {
            text = "Configurar acesso às notificações"
            setTextColor(Color.WHITE)
            setBackgroundColor(accent)
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        content.addView(permissionButton, fullWidthWithTop())

        val refreshButton = Button(this).apply {
            text = "Atualizar status e mensagem"
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

        val privacy = label(
            "PRIVACIDADE\nNesta etapa, a última notificação com texto é guardada apenas no armazenamento local do app. O conteúdo pode incluir mensagens pessoais. Não enviamos mensagens para nenhum servidor nesta versão. Notificações ocultas ou sem texto disponível não podem ser lidas.",
            12, muted, false
        )
        privacy.setPadding(dp(4), dp(18), dp(4), dp(8))
        content.addView(privacy, matchWrap())

        setContentView(root)
    }

    private fun refreshStatusAndPreview() {
        if (!::statusText.isInitialized) return
        val component = ComponentName(this, NotificationReaderService::class.java)
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val enabled = enabledListeners.split(':').any {
            it.equals(component.flattenToString(), ignoreCase = true)
        }

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
            "Recebida: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(timestamp))
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

    private fun fullWidthWithTop() = matchWrap().apply {
        topMargin = dp(14)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
