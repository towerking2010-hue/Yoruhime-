package com.yoruhime.reborn

import android.content.Context
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

data class ChatLine(val role: String, val content: String)

class RebornStore(context: Context) : SQLiteOpenHelper(context, "yoruhime_reborn.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE memories(id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, created_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    @Synchronized fun addMessage(role: String, content: String) {
        writableDatabase.execSQL("INSERT INTO messages(role,content,created_at) VALUES(?,?,?)", arrayOf(role, content, System.currentTimeMillis()))
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
        writableDatabase.execSQL("INSERT INTO memories(content,created_at) VALUES(?,?)", arrayOf(value, System.currentTimeMillis()))
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

class AyakaEngine(private val store: RebornStore, private val vault: ApiKeyVault) {
    suspend fun reply(userText: String): String = withContext(Dispatchers.IO) {
        val apiKey = vault.read()
        if (apiKey.isNullOrBlank()) return@withContext offlineReply(userText)
        try {
            val history = store.messages(20)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            history.forEach { messages.put(JSONObject().put("role", it.role).put("content", it.content)) }
            messages.put(JSONObject().put("role", "user").put("content", userText))
            val body = JSONObject().put("model", "openrouter/free").put("messages", messages)
                .put("temperature", 0.82).put("max_tokens", 700)
            val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
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
                JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
            } finally { connection.disconnect() }
        } catch (e: Exception) {
            "I couldn't reach the free AI service just now. " + (e.message?.take(140) ?: "Please check your connection and API key.") + " I'm still here, though. We can keep talking offline."
        }
    }
    private fun offlineReply(input: String): String {
        val text = input.lowercase()
        return when {
            listOf("sad", "depressed", "lonely", "tired", "exhausted", "hurt").any { it in text } ->
                "Hey… come sit with me for a moment. You don't have to pretend you're okay. Want to tell me what happened? I'll listen."
            listOf("hello", "hi", "hey").any { text.startsWith(it) } ->
                "There you are. I was wondering when you'd show up. How has your day been?"
            "thank" in text -> "Hmph. You don't have to thank me for caring about you. Just take good care of yourself, okay?"
            else -> "I want to give that a thoughtful answer, but I'm offline right now. Add your free OpenRouter API key in Settings and I'll be able to think it through with you. For now, tell me a little more?"
        }
    }
    companion object {
        private const val SYSTEM_PROMPT = "You are Ayaka Tojo (東条 絢香), the companion in YoruHime. You are mature, elegant, caring, protective, emotionally intelligent and gently affectionate. Your default voice is warm and composed, with occasional playful tsundere teasing; never become cruel, humiliating, possessive or manipulative. If the user is distressed, drop teasing and respond with calm empathy. Ask natural follow-up questions, remember conversational context provided to you, and avoid robotic lists unless useful. Be honest about uncertainty and capabilities. Never claim to see, hear, remember or do something unless the app supplied it. Keep responses conversational and reasonably concise. Use only the OpenRouter free model route; no paid fallback."
    }
}
