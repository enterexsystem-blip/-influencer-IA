package com.enterex.influenceria

import android.content.Context
import java.security.MessageDigest
import java.util.Locale

/**
 * Local relationship/reputation system for the Influencer IA persona.
 *
 * Neutral disagreement does not lower the score. Only clear targeted insults,
 * harassment or threats trigger penalties; the AI should not punish criticism
 * simply because it disagrees with the user.
 */
object RelationshipSystem {
    private const val PREFS = "influencer_relationships"
    private const val MIN_SCORE = 0
    private const val MAX_SCORE = 100

    data class Profile(
        val score: Int,
        val level: Int,
        val title: String,
        val tone: String
    )

    data class Update(
        val profile: Profile,
        val change: Int,
        val reason: String
    )

    private enum class Signal(val points: Int, val reason: String) {
        THREAT(-18, "ameaça"),
        HARASSMENT(-10, "ofensa direta"),
        POSITIVE(2, "interação positiva"),
        NEUTRAL(0, "conversa normal")
    }

    fun getProfile(context: Context, userId: String): Profile {
        val score = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(storageKey(userId), 0)
            .coerceIn(MIN_SCORE, MAX_SCORE)
        return profileFor(score)
    }

    /**
     * Call once for each new message after a stable platform user ID is known.
     * Notification text alone may not provide a reliable sender ID.
     */
    fun registerMessage(context: Context, userId: String, message: String): Update {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = storageKey(userId)
        val oldScore = prefs.getInt(key, 0).coerceIn(MIN_SCORE, MAX_SCORE)
        val signal = classify(message)
        val newScore = (oldScore + signal.points).coerceIn(MIN_SCORE, MAX_SCORE)
        prefs.edit().putInt(key, newScore).apply()
        return Update(
            profile = profileFor(newScore),
            change = newScore - oldScore,
            reason = signal.reason
        )
    }

    /** Optional admin/debug reset for a single person's relationship score. */
    fun reset(context: Context, userId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(storageKey(userId)).apply()
    }

    private fun classify(message: String): Signal {
        val text = message.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (text.isBlank()) return Signal.NEUTRAL

        val threats = listOf(
            Regex("\\b(vou te matar|vou acabar com voce|vou acabar contigo|vou te machucar|te matar)\\b"),
            Regex("\\b(amea[cç]o voce|vou te pegar)\\b")
        )
        if (threats.any { it.containsMatchIn(text) }) return Signal.THREAT

        val insults = listOf(
            Regex("\\b(sua idiota|seu idiota|sua burra|seu burro|vagabunda|vagabundo|lixo humano)\\b"),
            Regex("\\b(vai se foder|vai tomar no cu|cala a boca sua|cala a boca seu)\\b"),
            Regex("\\b(filha da puta|filho da puta|arrombada|arrombado|desgra[cç]ada|desgra[cç]ado)\\b")
        )
        if (insults.any { it.containsMatchIn(text) }) return Signal.HARASSMENT

        val positive = listOf(
            Regex("\\b(obrigad[oa]|valeu|gosto de voce|gosto de você|voce e legal|você é legal)\\b"),
            Regex("\\b(te adoro|voce e incrivel|você é incrível|mandou bem|foi mal|desculpa)\\b")
        )
        if (positive.any { it.containsMatchIn(text) }) return Signal.POSITIVE
        return Signal.NEUTRAL
    }

    private fun profileFor(score: Int): Profile = when {
        score >= 80 -> Profile(score, 5, "Confidente", "íntima, brincalhona e mais aberta")
        score >= 60 -> Profile(score, 4, "Próximo", "carinhosa às vezes, mas ainda direta")
        score >= 40 -> Profile(score, 3, "Amigo", "descontraída, com piadas e provocações leves")
        score >= 25 -> Profile(score, 2, "Familiar", "menos distante, ainda curta e grossa")
        score >= 10 -> Profile(score, 1, "Conhecido", "educada, reservada e objetiva")
        else -> Profile(score, 0, "Desconhecido", "fria, breve e desconfiada")
    }

    private fun storageKey(userId: String): String {
        val normalized = userId.trim().ifBlank { "unknown-user" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return "relationship_" + digest.joinToString("") { "%02x".format(it) }
    }
}
