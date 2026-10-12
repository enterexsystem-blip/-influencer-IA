package com.enterex.influenceria

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.random.Random

class InstagramLiteAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var attemptInProgress = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(NotificationReaderService.AUTO_REPLY_ENABLED, false)) return
        if (!prefs.getBoolean(NotificationReaderService.PENDING_REPLY, false)) return
        val queuedAt = prefs.getLong(NotificationReaderService.PENDING_REPLY_TIME, 0L)
        if (queuedAt <= 0L || System.currentTimeMillis() - queuedAt > 60_000L) {
            prefs.edit()
                .putBoolean(NotificationReaderService.PENDING_REPLY, false)
                .remove(NotificationReaderService.PENDING_REPLY_TEXT)
                .remove(NotificationReaderService.PENDING_REPLY_TITLE)
                .remove(NotificationReaderService.PENDING_REPLY_PACKAGE)
                .remove(NotificationReaderService.PENDING_REPLY_MESSAGE)
                .remove(NotificationReaderService.PENDING_REPLY_TIME)
                .putString("last_auto_reply_status", "Resposta pendente expirou por segurança. Envie uma nova mensagem para testar novamente.")
                .apply()
            return
        }

        val selectedPackage = prefs.getString(MainActivity.KEY_SELECTED_PACKAGE, "").orEmpty()
        val pendingPackage = prefs.getString(NotificationReaderService.PENDING_REPLY_PACKAGE, "").orEmpty()
        if (selectedPackage.isBlank() || pendingPackage != selectedPackage) return
        if (event.packageName?.toString() != selectedPackage) return
        if (attemptInProgress) return

        val title = prefs.getString(NotificationReaderService.PENDING_REPLY_TITLE, "").orEmpty()
        val reply = prefs.getString(NotificationReaderService.PENDING_REPLY_TEXT, "").orEmpty()
        if (title.isBlank() || reply.isBlank()) return

        val root = rootInActiveWindow ?: return
        if (!isMatchingConversation(root, title)) {
            root.recycle()
            return
        }

        val editor = findComposer(root)
        root.recycle()
        if (editor == null) return

        attemptInProgress = true
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, reply)
        }
        val textSet = editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        editor.recycle()
        if (!textSet) {
            attemptInProgress = false
            prefs.edit().putString("last_auto_reply_status", "A conversa foi reconhecida, mas o campo de mensagem do Instagram Lite não aceitou o texto.").apply()
            return
        }

        prefs.edit()
            .putString("last_auto_reply_status", "Ayla está digitando… preparando o envio no Instagram.")
            .putLong("last_auto_reply_status_time", System.currentTimeMillis())
            .apply()

        handler.postDelayed({
            try {
                val currentPrefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
                val currentRoot = rootInActiveWindow
                if (currentRoot == null) {
                    currentPrefs.edit().putString("last_auto_reply_status", "Texto preparado, mas a conversa saiu da tela antes do envio.").apply()
                    return@postDelayed
                }
                if (!isMatchingConversation(currentRoot, title)) {
                    currentRoot.recycle()
                    currentPrefs.edit().putString("last_auto_reply_status", "Envio cancelado: a conversa visível mudou.").apply()
                    return@postDelayed
                }

                val composer = findComposer(currentRoot)
                val composerHasReply = composer?.text?.toString() == reply
                composer?.recycle()
                if (!composerHasReply) {
                    currentRoot.recycle()
                    currentPrefs.edit().putString("last_auto_reply_status", "O texto não permaneceu no campo de mensagem; nada foi enviado.").apply()
                    return@postDelayed
                }

                val sendButton = findSendButton(currentRoot)
                currentRoot.recycle()
                if (sendButton == null) {
                    currentPrefs.edit().putString("last_auto_reply_status", "Texto preparado, mas não encontrei um botão Enviar compatível. Nada foi enviado.").apply()
                    return@postDelayed
                }

                val sent = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                sendButton.recycle()
                if (sent) {
                    currentPrefs.edit()
                        .putBoolean(NotificationReaderService.PENDING_REPLY, false)
                        .remove(NotificationReaderService.PENDING_REPLY_TEXT)
                        .remove(NotificationReaderService.PENDING_REPLY_TITLE)
                        .remove(NotificationReaderService.PENDING_REPLY_PACKAGE)
                        .remove(NotificationReaderService.PENDING_REPLY_MESSAGE)
                        .putString("last_auto_reply_status", "Ayla tocou em Enviar na conversa reconhecida de '$title'. Confira no Instagram Lite se a mensagem apareceu.")
                        .apply()
                } else {
                    currentPrefs.edit().putString("last_auto_reply_status", "O botão Enviar foi localizado, mas o Android não conseguiu acioná-lo.").apply()
                }
            } finally {
                attemptInProgress = false
            }
        }, Random.nextLong(1400, 2400))
    }

    private fun isMatchingConversation(root: AccessibilityNodeInfo, expectedTitle: String): Boolean {
        val expected = normalize(expectedTitle)
        if (expected.isBlank()) return false
        val visibleTexts = mutableListOf<String>()
        collectText(root, visibleTexts)
        return visibleTexts.any { visible ->
            val candidate = normalize(visible)
            candidate == expected || (expected.length >= 4 && candidate.contains(expected))
        }
    }

    private fun collectText(node: AccessibilityNodeInfo, output: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(output::add)
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(output::add)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, output)
            child.recycle()
        }
    }

    private fun findComposer(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectEditable(root, candidates)
        return candidates.lastOrNull()?.also {
            candidates.dropLast(1).forEach(AccessibilityNodeInfo::recycle)
        }
    }

    private fun collectEditable(node: AccessibilityNodeInfo, output: MutableList<AccessibilityNodeInfo>) {
        if (node.isVisibleToUser && node.isEditable && node.isEnabled) {
            output.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectEditable(child, output)
            child.recycle()
        }
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectSendButtons(root, candidates)
        return candidates.firstOrNull()?.also {
            candidates.drop(1).forEach(AccessibilityNodeInfo::recycle)
        }
    }

    private fun collectSendButtons(node: AccessibilityNodeInfo, output: MutableList<AccessibilityNodeInfo>) {
        val label = normalize(listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" "))
        val sendLabel = listOf("enviar", "send", "mandar", "envoyer", "invia").any { label == it || label.endsWith(it) }
        if (node.isVisibleToUser && node.isEnabled && node.isClickable && sendLabel) {
            output.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectSendButtons(child, output)
            child.recycle()
        }
    }

    private fun normalize(value: String): String =
        value.lowercase().replace("@", "").replace(Regex("\\s+"), " ").trim()

    override fun onInterrupt() {
        attemptInProgress = false
    }
}
