package com.enterex.influenceria

import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.content.ComponentName
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date
import java.util.UUID

class MainActivity : Activity() {
    private data class AppChoice(val label: String, val packageName: String?) {
        override fun toString(): String = label
    }

    companion object {
        const val PREFS_NAME = "influencer_ia_local"
        const val KEY_PACKAGE = "latest_package"
        const val KEY_TITLE = "latest_title"
        const val KEY_TEXT = "latest_text"
        const val KEY_TIME = "latest_time"
        const val KEY_SELECTED_PACKAGE = "selected_source_package"
        private const val BACKEND_URL = "https://influencer-ia-backend.onrender.com"
    }

    private lateinit var statusText: TextView
    private lateinit var previewText: TextView
    private lateinit var packageText: TextView
    private lateinit var timeText: TextView
    private lateinit var autoReplyStatusText: TextView
    private lateinit var messageInput: EditText
    private lateinit var replyText: TextView
    private lateinit var sendButton: Button
    private lateinit var autoReplyButton: Button
    private lateinit var appPicker: Spinner
    private var populatingAppPicker = false
    private var appChoices: List<AppChoice> = emptyList()
    private lateinit var moodPicker: Spinner
    private lateinit var moodDescription: TextView
    private lateinit var folderPicker: Spinner
    private lateinit var folderContents: TextView
    private var libraryFolders: MutableList<String> = mutableListOf()
    private var selectedLibraryFolder: String = "Fotos"
    private val importFileRequest = 7412

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        refreshStatusAndPreview()
    }

    private fun buildScreen() {
        val background = Color.rgb(9, 12, 22)
        val panel = Color.rgb(20, 26, 42)
        val accent = Color.rgb(139, 111, 255)
        val white = Color.rgb(244, 246, 255)
        val muted = Color.rgb(158, 169, 197)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(28))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = roundedGradient(intArrayOf(Color.rgb(52, 43, 101), Color.rgb(26, 34, 64)), dp(24))
        }
        hero.addView(label("ENTEREX  /  AYLA AI", 11, Color.rgb(203, 193, 255), true), matchWrap())
        hero.addView(label("Sua Ayla.", 32, white, true).apply { setPadding(0, dp(8), 0, dp(3)) }, matchWrap())
        hero.addView(label("Um espaço para conversar, configurar e guardar as coisas dela.", 14, Color.rgb(218, 222, 245), false), matchWrap())
        hero.addView(label("●  PAINEL PESSOAL", 10, Color.rgb(153, 235, 205), true).apply { setPadding(0, dp(16), 0, 0) }, matchWrap())
        content.addView(hero, fullWidthWithTop())

        val moodCard = card(panel)
        moodCard.addView(sectionTitle("ESTADO EMOCIONAL", "Como a Ayla está se sentindo"), matchWrap())
        moodPicker = Spinner(this)
        val moods = listOf("😊  Feliz", "🥰  Carinhosa", "😌  Tranquila", "🤔  Curiosa", "😴  Cansada", "🥺  Sensível", "😎  Confiante", "😔  Triste", "😤  Irritada", "✨  Animada")
        moodPicker.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, moods)
        val savedMood = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("ayla_mood", moods[0]).orEmpty()
        moodPicker.setSelection(moods.indexOf(savedMood).coerceAtLeast(0))
        moodDescription = label("", 13, muted, false).apply { setPadding(0, dp(10), 0, 0) }
        moodCard.addView(moodPicker, fullWidthWithTop())
        moodCard.addView(moodDescription, matchWrap())
        moodPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mood = moods.getOrElse(position) { moods[0] }
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString("ayla_mood", mood).apply()
                moodDescription.text = when (mood.substringAfter("  ")) {
                    "Feliz" -> "Hoje ela está leve e de bom humor."
                    "Carinhosa" -> "Um jeito mais doce e acolhedor de conversar."
                    "Tranquila" -> "No ritmo calmo, sem pressa."
                    "Curiosa" -> "Atenta, fazendo perguntas e querendo entender."
                    "Cansada" -> "Um clima mais quietinho e respostas mais suaves."
                    "Sensível" -> "Mais delicada com assuntos e sentimentos."
                    "Confiante" -> "Segura de si e direta, sem perder a gentileza."
                    "Triste" -> "Um estado registrado por você; não significa que a IA realmente sinta tristeza."
                    "Irritada" -> "Um tom mais sério, sem ofender ninguém."
                    else -> "Cheia de energia e pronta para conversar."
                }
            }
        }
        content.addView(moodCard, fullWidthWithTop())

        val statusCard = card(panel)
        statusCard.addView(sectionTitle("CONEXÃO", "Permissões e aplicativo"), matchWrap())
        statusText = label("Verificando…", 16, white, true).apply { setPadding(0, dp(12), 0, dp(4)) }
        statusCard.addView(statusText, matchWrap())
        statusCard.addView(label("Escolhe o app que ela pode monitorar. As permissões são controladas por você nas configurações do Android.", 13, muted, false), matchWrap())
        val permissionButton = actionButton("Configurar acesso às notificações", accent) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        statusCard.addView(permissionButton, fullWidthWithTop())
        val accessibilityButton = actionButton("Configurar envio assistido", Color.rgb(48, 59, 88)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        statusCard.addView(accessibilityButton, fullWidthWithTop())
        content.addView(statusCard, fullWidthWithTop())

        val appCard = card(panel)
        appCard.addView(sectionTitle("APLICATIVO MONITORADO", "Escolha de onde chegam as notificações"), matchWrap())
        appPicker = Spinner(this)
        appCard.addView(appPicker, fullWidthWithTop())
        appCard.addView(actionButton("Atualizar lista de aplicativos", Color.rgb(48, 59, 88)) { loadAppChoices() }, fullWidthWithTop())
        content.addView(appCard, fullWidthWithTop())
        loadAppChoices()

        val automationCard = card(panel)
        automationCard.addView(sectionTitle("RESPOSTAS AUTOMÁTICAS", "Controle de segurança"), matchWrap())
        autoReplyButton = actionButton("Ativar respostas automáticas", accent) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val selected = prefs.getString(KEY_SELECTED_PACKAGE, "").orEmpty()
            if (selected.isBlank()) {
                statusText.text = "Escolha primeiro um aplicativo monitorado."
                statusText.setTextColor(Color.rgb(255, 190, 90))
            } else {
                val enabled = !prefs.getBoolean(NotificationReaderService.AUTO_REPLY_ENABLED, false)
                prefs.edit().putBoolean(NotificationReaderService.AUTO_REPLY_ENABLED, enabled).apply()
                updateAutoReplyButton()
            }
        }
        automationCard.addView(autoReplyButton, fullWidthWithTop())
        automationCard.addView(label("Deixe desligado enquanto testa. O app mostra o status da última tentativa e não deve enviar mensagens em sequência.", 12, muted, false).apply { setPadding(0, dp(10), 0, 0) }, matchWrap())
        content.addView(automationCard, fullWidthWithTop())

        val inboxCard = card(panel)
        inboxCard.addView(sectionTitle("CAIXA DE ENTRADA", "Última notificação capturada"), matchWrap())
        packageText = label("Aplicativo: —", 12, accent, true).apply { setPadding(0, dp(10), 0, dp(6)) }
        inboxCard.addView(packageText, matchWrap())
        previewText = label("Nenhuma mensagem capturada ainda.", 15, white, false)
        inboxCard.addView(previewText, matchWrap())
        timeText = label("", 11, muted, false).apply { setPadding(0, dp(8), 0, 0) }
        inboxCard.addView(timeText, matchWrap())
        autoReplyStatusText = label("Ayla ainda não tentou responder nenhuma notificação.", 12, muted, false).apply { setPadding(0, dp(10), 0, 0); setTextIsSelectable(true) }
        inboxCard.addView(autoReplyStatusText, matchWrap())
        inboxCard.addView(actionButton("Atualizar notificações", Color.rgb(48, 59, 88)) { refreshStatusAndPreview() }, fullWidthWithTop())
        inboxCard.addView(actionButton("Copiar última resposta", Color.rgb(48, 59, 88)) {
            val draft = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("last_auto_reply_draft", "").orEmpty()
            if (draft.isBlank()) Toast.makeText(this, "Ainda não existe uma resposta gerada.", Toast.LENGTH_SHORT).show()
            else {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Resposta da Ayla", draft))
                Toast.makeText(this, "Resposta copiada.", Toast.LENGTH_LONG).show()
            }
        }, fullWidthWithTop())
        content.addView(inboxCard, fullWidthWithTop())

        val chatCard = card(panel)
        chatCard.addView(sectionTitle("CONVERSAR COM A AYLA", "Testar uma resposta"), matchWrap())
        messageInput = EditText(this).apply {
            hint = "Escreve uma mensagem aqui…"
            setTextColor(white); setHintTextColor(muted); textSize = 15f
            setPadding(dp(14), dp(12), dp(14), dp(12)); minLines = 3; maxLines = 6
            gravity = Gravity.TOP
            background = roundedSolid(Color.rgb(30, 37, 57), dp(14))
        }
        chatCard.addView(messageInput, fullWidthWithTop())
        sendButton = actionButton("Gerar resposta", accent) { generateReply() }
        chatCard.addView(sendButton, fullWidthWithTop())
        replyText = label("A resposta da Ayla aparecerá aqui.", 14, white, false).apply {
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedSolid(Color.rgb(30, 37, 57), dp(14))
        }
        chatCard.addView(replyText, fullWidthWithTop())
        content.addView(chatCard, fullWidthWithTop())

        val libraryCard = card(panel)
        libraryCard.addView(sectionTitle("BIBLIOTECA DA AYLA", "Pastas para fotos, referências e arquivos"), matchWrap())
        libraryCard.addView(label("Os arquivos ficam guardados dentro do armazenamento privado do app. Escolhe uma pasta e importa imagens ou outros arquivos pelo seletor do Android.", 12, muted, false).apply { setPadding(0, dp(8), 0, 0) }, matchWrap())
        folderPicker = Spinner(this)
        libraryCard.addView(folderPicker, fullWidthWithTop())
        libraryCard.addView(actionButton("＋ Criar pasta", Color.rgb(48, 59, 88)) { showCreateFolderDialog() }, fullWidthWithTop())
        libraryCard.addView(actionButton("＋ Importar foto ou arquivo", accent) { openImportPicker() }, fullWidthWithTop())
        folderContents = label("Carregando pastas…", 13, white, false).apply {
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedSolid(Color.rgb(30, 37, 57), dp(14))
        }
        libraryCard.addView(folderContents, fullWidthWithTop())
        content.addView(libraryCard, fullWidthWithTop())
        content.addView(label("ENTEREX • AYLA PERSONAL SPACE\nAs emoções são estados escolhidos/configurados, não sentimentos reais da IA.", 11, muted, false).apply {
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(22), dp(6), dp(10))
        }, matchWrap())

        setContentView(root)
        setupLibrary()
        updateAutoReplyButton()
    }

    private fun roundedSolid(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }

    private fun roundedGradient(colors: IntArray, radius: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = radius.toFloat() }

    private fun sectionTitle(kicker: String, title: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(kicker, 10, Color.rgb(167, 151, 255), true), matchWrap())
            addView(label(title, 18, Color.rgb(244, 246, 255), true).apply { setPadding(0, dp(5), 0, 0) }, matchWrap())
        }

    private fun actionButton(textValue: String, color: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = textValue
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            background = roundedSolid(color, dp(14))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { action() }
        }

    private fun setupLibrary() {
        val root = java.io.File(filesDir, "AylaLibrary")
        if (!root.exists()) root.mkdirs()
        val defaults = listOf("Fotos", "Referências", "Avatares", "Memórias visuais", "Documentos", "Outros")
        defaults.forEach { name -> java.io.File(root, safeFolderName(name)).mkdirs() }
        reloadLibraryFolders()
        folderPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLibraryFolder = libraryFolders.getOrNull(position) ?: "Fotos"
                refreshFolderContents()
            }
        }
    }

    private fun safeFolderName(name: String): String =
        name.trim().replace(Regex("[^A-Za-zÀ-ÿ0-9 _-]"), "").replace(Regex("\\s+"), " ").take(40).ifBlank { "Nova pasta" }

    private fun reloadLibraryFolders() {
        val root = java.io.File(filesDir, "AylaLibrary")
        if (!root.exists()) root.mkdirs()
        libraryFolders = (root.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList())
            .sortedBy { it.lowercase() }.toMutableList()
        if (libraryFolders.isEmpty()) {
            listOf("Fotos", "Referências", "Avatares", "Memórias visuais", "Documentos", "Outros").forEach {
                java.io.File(root, it).mkdirs()
            }
            libraryFolders = root.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sortedBy { it.lowercase() }?.toMutableList() ?: mutableListOf("Fotos")
        }
        if (!::folderPicker.isInitialized) return
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, libraryFolders)
        folderPicker.adapter = adapter
        val index = libraryFolders.indexOf(selectedLibraryFolder).coerceAtLeast(0)
        folderPicker.setSelection(index, false)
        selectedLibraryFolder = libraryFolders[index]
        refreshFolderContents()
    }

    private fun refreshFolderContents() {
        if (!::folderContents.isInitialized) return
        val folder = java.io.File(java.io.File(filesDir, "AylaLibrary"), safeFolderName(selectedLibraryFolder))
        if (!folder.exists()) folder.mkdirs()
        val files = folder.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        folderContents.text = if (files.isEmpty()) {
            "📁  $selectedLibraryFolder\n\nEsta pasta está vazia. Usa “Importar foto ou arquivo” para adicionar conteúdo."
        } else {
            "📁  $selectedLibraryFolder  ·  ${files.size} arquivo(s)\n\n" + files.joinToString("\n") { file ->
                val kb = (file.length() / 1024).coerceAtLeast(1)
                "• ${file.name}  ·  ${kb} KB"
            }
        }
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply { hint = "Nome da pasta"; setSingleLine(true) }
        AlertDialog.Builder(this)
            .setTitle("Criar pasta")
            .setMessage("Ex.: selfies, roupas, expressões, referências…")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Criar") { _, _ ->
                val name = safeFolderName(input.text.toString())
                val folder = java.io.File(java.io.File(filesDir, "AylaLibrary"), name)
                if (folder.exists()) {
                    Toast.makeText(this, "Essa pasta já existe.", Toast.LENGTH_SHORT).show()
                } else {
                    folder.mkdirs()
                    selectedLibraryFolder = name
                    reloadLibraryFolders()
                    Toast.makeText(this, "Pasta criada: $name", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun openImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf", "text/plain", "audio/*", "video/*"))
        }
        startActivityForResult(intent, importFileRequest)
    }

    @Deprecated("Deprecated in Android API, retained for compatibility with Android 10")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != importFileRequest || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        val displayName = queryDisplayName(uri) ?: "arquivo_${System.currentTimeMillis()}"
        Thread {
            try {
                val folder = java.io.File(java.io.File(filesDir, "AylaLibrary"), safeFolderName(selectedLibraryFolder))
                folder.mkdirs()
                val cleanName = displayName.map { ch -> if (ch == '/' || ch == '\\' || ch == ':' || ch == '*' || ch == '?' || ch == '"' || ch == '<' || ch == '>' || ch == '|') '_' else ch }.joinToString("").take(100)
                var target = java.io.File(folder, cleanName)
                if (target.exists()) {
                    val dot = cleanName.lastIndexOf('.')
                    val base = if (dot > 0) cleanName.substring(0, dot) else cleanName
                    val ext = if (dot > 0) cleanName.substring(dot) else ""
                    target = java.io.File(folder, "${base}_${System.currentTimeMillis()}${ext}")
                }
                contentResolver.openInputStream(uri).use { input ->
                    if (input == null) throw IllegalStateException("Não consegui abrir esse arquivo.")
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                runOnUiThread {
                    refreshFolderContents()
                    Toast.makeText(this, "Arquivo salvo em $selectedLibraryFolder.", Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                runOnUiThread { Toast.makeText(this, "Não consegui importar: ${error.localizedMessage ?: "erro"}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun loadAppChoices() {
        if (!::appPicker.isInitialized) return
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val previouslySelected = prefs.getString(KEY_SELECTED_PACKAGE, "").orEmpty()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info: ResolveInfo ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == packageNameForSelf()) return@mapNotNull null
                val label = info.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() } ?: packageName
                packageName to label
            }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        val choices = mutableListOf(AppChoice("— Escolha um aplicativo —", null))
        choices.addAll(apps.map { (pkg, appLabel) -> AppChoice("$appLabel  ·  $pkg", pkg) })
        appChoices = choices
        populatingAppPicker = true
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, choices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        appPicker.adapter = adapter
        val selectedIndex = choices.indexOfFirst { it.packageName == previouslySelected }.let { if (it >= 0) it else 0 }
        appPicker.setSelection(selectedIndex, false)
        appPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (populatingAppPicker) {
                    populatingAppPicker = false
                    return
                }
                val selectedPackage = appChoices.getOrNull(position)?.packageName.orEmpty()
                val oldPackage = prefs.getString(KEY_SELECTED_PACKAGE, "").orEmpty()
                if (selectedPackage == oldPackage) return
                prefs.edit()
                    .putString(KEY_SELECTED_PACKAGE, selectedPackage)
                    .putBoolean(NotificationReaderService.AUTO_REPLY_ENABLED, false)
                    .remove(KEY_TITLE)
                    .remove(KEY_TEXT)
                    .remove(KEY_PACKAGE)
                    .remove(KEY_TIME)
                    .apply()
                updateAutoReplyButton()
                refreshStatusAndPreview()
                if (selectedPackage.isBlank()) {
                    statusText.text = "Escolha o aplicativo que deseja monitorar."
                    statusText.setTextColor(Color.rgb(255, 190, 90))
                } else {
                    statusText.text = "App escolhido. Ative o acesso às notificações se ainda não fez isso."
                    statusText.setTextColor(Color.rgb(101, 224, 167))
                }
            }
        }
    }

    private fun packageNameForSelf(): String = packageName

    private fun updateAutoReplyButton() {
        if (!::autoReplyButton.isInitialized) return
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val enabled = prefs.getBoolean(NotificationReaderService.AUTO_REPLY_ENABLED, false)
        val selected = prefs.getString(KEY_SELECTED_PACKAGE, "").orEmpty().isNotBlank()
        autoReplyButton.text = when {
            !selected -> "Escolha um app antes de ativar as respostas"
            enabled -> "Respostas automáticas: ATIVADAS"
            else -> "Ativar respostas automáticas"
        }
        autoReplyButton.isEnabled = selected
        autoReplyButton.setBackgroundColor(if (enabled) Color.rgb(183, 72, 82) else Color.rgb(91, 115, 255))
        autoReplyButton.setTextColor(Color.WHITE)
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
                    val payload = JSONObject().put("message", message).put("relationshipLevel", 0).put("userId", getOrCreateUserId()).toString()
                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(response) }.getOrNull()
                    result = if (status in 200..299) {
                        json?.optString("reply")?.takeIf { it.isNotBlank() } ?: "O servidor respondeu, mas não enviou texto."
                    } else {
                        json?.optString("error")?.takeIf { it.isNotBlank() } ?: "Erro HTTP $status ao consultar o servidor."
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                result = when (e) {
                    is java.net.UnknownHostException -> "Não consegui localizar o servidor. O endereço público do Render pode ser diferente."
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

    private fun getOrCreateUserId(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val existing = prefs.getString("ai_user_id", null)
        if (!existing.isNullOrBlank()) return existing
        val created = "app:" + UUID.randomUUID().toString()
        prefs.edit().putString("ai_user_id", created).apply()
        return created
    }

    private fun refreshStatusAndPreview() {
        if (!::statusText.isInitialized) return
        val component = ComponentName(this, NotificationReaderService::class.java)
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        val enabled = enabledListeners.split(':').any { it.equals(component.flattenToString(), ignoreCase = true) }
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val selected = prefs.getString(KEY_SELECTED_PACKAGE, "").orEmpty()
        statusText.text = when {
            !enabled -> "○ Acesso às notificações desativado"
            selected.isBlank() -> "○ Escolha o aplicativo monitorado"
            else -> "● Acesso autorizado · app escolhido"
        }
        statusText.setTextColor(if (enabled && selected.isNotBlank()) Color.rgb(101, 224, 167) else Color.rgb(255, 190, 90))

        val title = prefs.getString(KEY_TITLE, "").orEmpty()
        val message = prefs.getString(KEY_TEXT, "").orEmpty()
        val packageName = prefs.getString(KEY_PACKAGE, "").orEmpty()
        val timestamp = prefs.getLong(KEY_TIME, 0L)

        packageText.text = if (packageName.isBlank()) "Aplicativo: —" else "Aplicativo: $packageName"
        previewText.text = when {
            title.isBlank() && message.isBlank() -> "Nenhuma mensagem capturada ainda do aplicativo escolhido."
            title.isBlank() -> message
            message.isBlank() -> title
            else -> "$title\n\n$message"
        }
        timeText.text = if (timestamp > 0L) {
            "Recebida: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
        } else ""
        autoReplyStatusText.text = prefs.getString("last_auto_reply_status", "Ayla ainda não tentou responder nenhuma notificação.").orEmpty()
        updateAutoReplyButton()
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
