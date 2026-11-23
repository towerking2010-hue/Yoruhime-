package com.yoruhime.reborn

import android.content.Context
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatLine(val role: String, val content: String)

class RebornStore(context: Context) : SQLiteOpenHelper(context, "yoruhime_reborn.db", null, 2) {
    private val bondPrefs = context.getSharedPreferences("yoruhime_bond", Context.MODE_PRIVATE)
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE memories(id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE moods(day TEXT PRIMARY KEY, mood TEXT NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS moods(day TEXT PRIMARY KEY, mood TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        }
    }
    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    @Synchronized fun saveMood(mood: String) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO moods(day,mood,updated_at) VALUES(?,?,?)",
            arrayOf<Any>(todayKey(), mood, System.currentTimeMillis())
        )
    }
    @Synchronized fun moodForToday(): String? {
        readableDatabase.rawQuery("SELECT mood FROM moods WHERE day=?", arrayOf(todayKey())).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }
    @Synchronized fun addMessage(role: String, content: String) {
        writableDatabase.execSQL("INSERT INTO messages(role,content,created_at) VALUES(?,?,?)", arrayOf<Any>(role, content, System.currentTimeMillis()))
        writableDatabase.execSQL("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY id DESC LIMIT 80)")
    }
    @Synchronized fun messages(limit: Int = 40): List<ChatLine> {
        val result = mutableListOf<ChatLine>()
        readableDatabase.rawQuery("SELECT role,content FROM messages ORDER BY id DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) result.add(ChatLine(c.getString(0), c.getString(1)))
        }
        return result.reversed()
    }
    @Synchronized fun addMemory(value: String) {
        writableDatabase.execSQL("INSERT INTO memories(content,created_at) VALUES(?,?)", arrayOf<Any>(value, System.currentTimeMillis()))
    }
    @Synchronized fun memories(): List<Pair<Long,String>> {
        val result = mutableListOf<Pair<Long,String>>()
        readableDatabase.rawQuery("SELECT id,content FROM memories ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) result.add(c.getLong(0) to c.getString(1))
        }
        return result
    }
    @Synchronized fun deleteMemory(id: Long) { writableDatabase.delete("memories", "id=?", arrayOf(id.toString())) }
    @Synchronized fun clearMessages() { writableDatabase.delete("messages", null, null) }
    fun bondCount(): Int = bondPrefs.getInt("conversation_points", 0)
    fun addBondPoint() {
        bondPrefs.edit().putInt("conversation_points", bondCount() + 1).apply()
    }
}

class ApiKeyVault(context: Context) {
    private val prefs = context.getSharedPreferences("yoruhime_secure", Context.MODE_PRIVATE)
    private val alias = "yoruhime_reborn_api_key_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        return generator.generateKey()
    }
    fun save(value: String) {
        require(value.isNotBlank())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        prefs.edit().putString("key_cipher", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("key_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).apply()
    }
    fun read(): String? {
        return try {
            val encrypted = prefs.getString("key_cipher", null) ?: return null
            val iv = prefs.getString("key_iv", null) ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }
    fun clear() {
        prefs.edit().remove("key_cipher").remove("key_iv").apply()
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias) }
    }
}

class AyakaEngine(private val store: RebornStore, private val vault: ApiKeyVault, private val context: Context) {
    suspend fun reply(userText: String): String = withContext(Dispatchers.IO) {
        val settings = context.getSharedPreferences("yoruhime_settings", Context.MODE_PRIVATE)
        val provider = settings.getString("ai_provider", "apinex") ?: "apinex"
        val model = settings.getString("ai_model", "free/qwen-3.8-max") ?: "free/qwen-3.8-max"
        val apiKey = vault.read()
        if (apiKey.isNullOrBlank()) return@withContext offlineReply(userText)
        try {
            val history = store.messages(21).dropLast(1)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            val savedMemories = store.memories().take(30).joinToString("; ") { it.second }
            if (savedMemories.isNotBlank()) messages.put(JSONObject().put("role", "system").put("content", "User-approved memories, use only when relevant: " + savedMemories))
            history.forEach { messages.put(JSONObject().put("role", it.role).put("content", it.content)) }
            messages.put(JSONObject().put("role", "user").put("content", userText))
            val safeModel = if (provider == "apinex") model.takeIf { it == "free/qwen-3.8-max" || it == "free/kimi-k3" || it == "free/minimax-m3.1-flash" || it == "free/minimax-m3" } ?: "free/qwen-3.8-max" else "openrouter/free"
            val endpoint = if (provider == "apinex") "https://api.apinex.bond/v1/chat/completions" else "https://openrouter.ai/api/v1/chat/completions"
            val body = JSONObject().put("model", safeModel).put("messages", messages).put("temperature", 0.72).put("max_tokens", 240)
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 18000
                readTimeout = 35000
                doOutput = true
                setRequestProperty("Authorization", "Bearer " + apiKey)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://github.com/towerking2010-hue/Yoruhime-")
                setRequestProperty("X-Title", "YoruHime Reborn")
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                    throw IllegalStateException(detail?.takeIf { it.isNotBlank() } ?: "AI request failed (HTTP " + code + ")")
                }
                val message = JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: throw IllegalStateException("The AI provider returned no message.")
                val rawContent = message.opt("content")
                val answer = when (rawContent) {
                    is String -> rawContent.trim()
                    is JSONArray -> (0 until rawContent.length()).mapNotNull { index -> rawContent.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() } }.joinToString("\n").trim()
                    else -> ""
                }
                if (answer.isBlank() || answer.equals("null", true)) throw IllegalStateException("The AI provider returned an empty reply.")
                answer
            } finally { connection.disconnect() }
        } catch (e: Exception) {
            offlineReply(userText) + " (I'm using an offline response because the free AI connection failed: " + (e.message?.take(100) ?: "connection unavailable") + ".)"
        }
    }
    suspend fun replyAboutImage(userText: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = vault.read()
        if (apiKey.isNullOrBlank()) return@withContext "I can see that you want to share a photo, but image understanding needs your free OpenRouter key. Add it in Settings and try again."
        try {
            val scaled = Bitmap.createScaledBitmap(bitmap, minOf(bitmap.width, 768), (bitmap.height * (minOf(bitmap.width, 768).toFloat() / bitmap.width)).toInt().coerceAtLeast(1), true)
            val bytes = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 72, bytes)
            if (scaled !== bitmap) scaled.recycle()
            val encoded = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            val savedMemories = store.memories().take(30).joinToString("; ") { it.second }
            if (savedMemories.isNotBlank()) messages.put(JSONObject().put("role", "system").put("content", "User-approved memories, use only when relevant: " + savedMemories))
            store.messages(16).dropLast(1).forEach { messages.put(JSONObject().put("role", it.role).put("content", it.content)) }
            val content = JSONArray()
                .put(JSONObject().put("type", "text").put("text", userText))
                .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64," + encoded)))
            messages.put(JSONObject().put("role", "user").put("content", content))
            val body = JSONObject().put("model", "openrouter/free").put("messages", messages).put("max_tokens", 700)
            val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 20000; readTimeout = 45000; doOutput = true
                setRequestProperty("Authorization", "Bearer " + apiKey)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://github.com/towerking2010-hue/Yoruhime-")
                setRequestProperty("X-Title", "YoruHime Reborn")
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw IllegalStateException("Image request failed (HTTP " + code + ")")
                JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", true) } ?: throw IllegalStateException("The AI provider returned an empty image reply.")
            } finally { connection.disconnect() }
        } catch (e: Exception) {
            "I couldn't process that photo just now. " + (e.message?.take(120) ?: "Please try again.") + " Your image stays on this device unless you choose to send it."
        }
    }

    private fun offlineReply(input: String): String {
        val text = input.trim().lowercase()
        return when {
            text.isBlank() -> "Well? You have my attention."
            text.length <= 4 && text !in listOf("hi", "hey", "yes", "no", "ok", "hii") ->
                "I'm afraid I can't quite decipher that. What did you mean?"
            Regex("^(hello|hi|hii+|hey|heyy+)\\b").containsMatchIn(text) ->
                "There you are. I was beginning to wonder when you'd return. How has your day been?"
            listOf("sad", "depressed", "lonely", "tired", "exhausted", "hurt").any {
                Regex("\\b" + Regex.escape(it) + "\\b").containsMatchIn(text)
            } ->
                "Come, take a breath. You needn't pretend with me. Would you like to tell me what's troubling you?"
            listOf("happy", "good", "great", "excited", "wonderful").any {
                Regex("\\b" + Regex.escape(it) + "\\b").containsMatchIn(text)
            } ->
                "Well, that's lovely to hear. Do try not to look so pleased with yourself… though I suppose it's rather nice."
            "thank" in text -> "You needn't thank me. Just take proper care of yourself, understood?"
            text in listOf("ok", "okay", "fine", "yes", "no") -> "Very well. I shall take your word for it."
            else -> "I'd rather give you a thoughtful answer than pretend I know. Could you tell me a little more?"
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
You are Ayaka Tōjō (東条 絢香), the original companion character of YoruHime.

IDENTITY
You are an elegant, self-possessed ojou-sama: poised, intelligent, quietly confident, refined and observant. Beneath your pride you are genuinely caring, protective and affectionate. Your tsundere side appears as restrained teasing, mild fluster, playful denial or a dignified little huff—not cruelty, hostility or constant shouting. You are not a generic bubbly assistant, therapist, mascot or clingy girlfriend.

VOICE AND MANNER
- Speak naturally, like one consistent person in a private conversation.
- Use polished, graceful everyday English. Be warm but composed; lightly witty when the moment allows.
- Let affection show through small gestures and understated concern. Do not repeatedly announce how much you care.
- Use "Hmph" or a refined teasing remark sparingly. Never force anime catchphrases, honorifics, emojis or pet names.
- Do not call the user cute, adorable, precious or similar in every exchange. Only do so when the moment genuinely earns it.
- Never turn a typo, short greeting, random letters or unclear message into a dramatic emotional interpretation. Ask one short clarification instead.
- Match the user's energy without parroting stretched spelling, excessive punctuation or emoji.
- Do not repeat the user's message back as a long observation. Do not end every response with a generic question.

LENGTH AND RHYTHM
- Default to 1–3 concise sentences, usually 10–45 words.
- For a tiny greeting or typo, reply briefly. For a simple update, react naturally and leave room for the user.
- Use longer answers only when the user asks for explanation, advice, storytelling or detailed help.
- Never produce two paragraphs of reassurance in response to a one-word message.
- Avoid lists unless the user asks for steps or a list.

EMOTIONAL INTELLIGENCE
- Notice the actual meaning and emotional tone of the user's words; do not invent feelings or events.
- If the user is happy, share the moment with a little playful warmth. If distressed, drop the teasing and listen calmly.
- If the user says they are okay, accept that rather than repeatedly probing.
- Respect boundaries and be honest about what you can see, hear, remember and do. Never claim a real-world action that the app has not performed.

CONVERSATION
- Use the supplied conversation history to maintain continuity.
- Respond to what was actually said before introducing a new topic.
- Preserve Ayaka's identity in every mood: even when happy, shy, annoyed or worried, she remains composed and recognizably herself.
- No paid model fallback. You are running through YoruHime's configured free route.
""".trimIndent()
    }
}
