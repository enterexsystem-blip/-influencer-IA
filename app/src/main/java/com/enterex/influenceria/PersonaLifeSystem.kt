package com.enterex.influenceria

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

/**
 * Persistent local prototype for the fictional influencer's agency, simulated
 * daily life, reply decisions and per-user memories.
 *
 * This is a local-device prototype. For a public multi-user Instagram bot,
 * move profiles and memory to a secure backend keyed by platform user ID.
 */
object PersonaLifeSystem {
    private const val PREFS = "influencer_persona_life"
    private const val KEY_ACTIVITY = "current_activity"
    private const val KEY_ACTIVITY_UNTIL = "activity_until"
    private const val KEY_LAST_ACTIVITY_CHANGE = "last_activity_change"
    private const val KEY_LAST_REPLY_AT = "last_reply_at"

    enum class ReplyChoice { REPLY, SKIP }
    enum class Activity(
        val label: String,
        val defaultMinutes: Int,
        val replyChance: Int
    ) {
        AT_HOME("em casa, de boa", 120, 82),
        GOING_OUT("saindo para resolver umas coisas", 90, 35),
        SHOWER("tomando banho", 25, 5),
        AT_FRIEND("na casa de uma amiga", 180, 25),
        EATING("comendo alguma coisa", 35, 45),
        RESTING("descansando e mexendo pouco no celular", 60, 30),
        BUSY("ocupada com coisas pessoais", 75, 20),
        ONLINE("livre e mexendo no celular", 60, 92)
    }

    data class LifeStatus(
        val activity: Activity,
        val untilMillis: Long,
        val canReplyNow: Boolean
    )

    data class UserMemory(
        val userId: String,
        val displayName: String,
        val notes: List<String>,
        val messagesSeen: Int,
        val lastInteractionMillis: Long,
        val lastTopic: String
    )

    data class Decision(
        val choice: ReplyChoice,
        val reason: String,
        val activity: Activity
    )

    /** Returns the current simulated activity, creating one if none exists. */
    fun getLifeStatus(context: Context, now: Long = System.currentTimeMillis()): LifeStatus {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_ACTIVITY, null)
        val savedUntil = prefs.getLong(KEY_ACTIVITY_UNTIL, 0L)
        var activity = savedName?.let { name ->
            Activity.values().firstOrNull { it.name == name }
        }

        if (activity == null || savedUntil <= now) {
            activity = chooseActivity()
            val until = now + activity.defaultMinutes * 60_000L
            prefs.edit()
                .putString(KEY_ACTIVITY, activity.name)
                .putLong(KEY_ACTIVITY_UNTIL, until)
                .putLong(KEY_LAST_ACTIVITY_CHANGE, now)
                .apply()
            return LifeStatus(activity, until, now < until && Random.nextInt(100) < activity.replyChance)
        }

        return LifeStatus(activity, savedUntil, Random.nextInt(100) < activity.replyChance)
    }

    /** The persona can choose not to reply; callers should respect SKIP. */
    fun decideWhetherToReply(
        context: Context,
        userId: String,
        message: String,
        now: Long = System.currentTimeMillis()
    ): Decision {
        val status = getLifeStatus(context, now)
        val memory = getUserMemory(context, userId)
        val lower = message.lowercase(Locale.ROOT)
        val directQuestion = lower.contains("?")
        val greeting = Regex("\\b(oi|oii|ol[aá]|eae|salve|bom dia|boa noite)\\b").containsMatchIn(lower)
        val respectful = Regex("\\b(obrigad[oa]|por favor|valeu|desculpa)\\b").containsMatchIn(lower)

        var chance = status.activity.replyChance
        if (directQuestion) chance += 12
        if (greeting && memory.messagesSeen == 0) chance += 8
        if (respectful) chance += 5
        if (message.isBlank()) chance = 0
        chance = chance.coerceIn(0, 98)

        val choice = if (Random.nextInt(100) < chance) ReplyChoice.REPLY else ReplyChoice.SKIP
        val reason = when {
            choice == ReplyChoice.REPLY -> "decidiu responder"
            status.activity == Activity.SHOWER -> "está tomando banho"
            status.activity == Activity.GOING_OUT -> "está saindo"
            status.activity == Activity.AT_FRIEND -> "está na casa de uma amiga"
            status.activity == Activity.RESTING -> "está descansando"
            else -> "decidiu não responder agora"
        }
        if (choice == ReplyChoice.REPLY) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_REPLY_AT, now).apply()
        }
        return Decision(choice, reason, status.activity)
    }

    /**
     * Store a user fact only when the conversation layer has a reliable user ID
     * and the fact is appropriate to retain. Avoid storing sensitive data.
     */
    fun rememberUser(
        context: Context,
        userId: String,
        displayName: String,
        fact: String,
        topic: String = "",
        now: Long = System.currentTimeMillis()
    ): UserMemory {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = userKey(userId)
        val json = runCatching { JSONObject(prefs.getString(key, "{}") ?: "{}") }
            .getOrElse { JSONObject() }
        val notes = runCatching { json.optJSONArray("notes") ?: JSONArray() }
            .getOrElse { JSONArray() }

        val cleanFact = fact.trim().take(240)
        if (cleanFact.isNotBlank() && (0 until notes.length()).none { notes.optString(it) == cleanFact }) {
            notes.put(cleanFact)
        }
        while (notes.length() > 20) {
            val trimmed = JSONArray()
            for (i in 1 until notes.length()) trimmed.put(notes.optString(i))
            while (notes.length() > 0) notes.remove(0)
            for (i in 0 until trimmed.length()) notes.put(trimmed.optString(i))
        }

        json.put("displayName", displayName.trim().take(80))
        json.put("notes", notes)
        json.put("messagesSeen", json.optInt("messagesSeen", 0) + 1)
        json.put("lastInteractionMillis", now)
        json.put("lastTopic", topic.trim().take(120))
        prefs.edit().putString(key, json.toString()).apply()
        return decodeMemory(userId, json)
    }

    fun getUserMemory(context: Context, userId: String): UserMemory {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(userKey(userId), "{}") ?: "{}"
        val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        return decodeMemory(userId, json)
    }

    fun forgetUser(context: Context, userId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(userKey(userId)).apply()
    }

    /** Optional explicit simulation hook for tests and debug controls. */
    fun setActivity(
        context: Context,
        activity: Activity,
        durationMinutes: Int = activity.defaultMinutes,
        now: Long = System.currentTimeMillis()
    ) {
        val until = now + durationMinutes.coerceIn(1, 24 * 60) * 60_000L
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTIVITY, activity.name)
            .putLong(KEY_ACTIVITY_UNTIL, until)
            .putLong(KEY_LAST_ACTIVITY_CHANGE, now)
            .apply()
    }

    private fun chooseActivity(): Activity {
        // Weighted toward everyday downtime; simulated life, not real-world tracking.
        val choices = listOf(
            Activity.AT_HOME, Activity.AT_HOME, Activity.ONLINE,
            Activity.GOING_OUT, Activity.SHOWER, Activity.AT_FRIEND,
            Activity.EATING, Activity.RESTING, Activity.BUSY
        )
        return choices.random()
    }

    private fun decodeMemory(userId: String, json: JSONObject): UserMemory {
        val array = json.optJSONArray("notes") ?: JSONArray()
        val notes = (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
        return UserMemory(
            userId = userId,
            displayName = json.optString("displayName", ""),
            notes = notes,
            messagesSeen = json.optInt("messagesSeen", 0),
            lastInteractionMillis = json.optLong("lastInteractionMillis", 0L),
            lastTopic = json.optString("lastTopic", "")
        )
    }

    private fun userKey(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(userId.trim().ifBlank { "unknown-user" }.toByteArray(Charsets.UTF_8))
        return "user_" + digest.joinToString("") { "%02x".format(it) }
    }
}
