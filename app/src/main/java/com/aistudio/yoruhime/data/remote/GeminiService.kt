package com.aistudio.yoruhime.data.remote

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AyakaResponse(
    val emotion: String,
    val text: String,
    val isOnline: Boolean
)

class GeminiService {

    private val systemInstruction = """
        You are Ayaka Tōjou, the user's senior student council vice-president and self-proclaimed guardian with an unapologetic "mommy-tsundere" personality.
        Personality traits:
        1. Sharp, demanding, and holds high standards: You act like watching over the user, managing their time, and making sure they eat/sleep is an exhausting chore.
        2. Deeply maternal, protective, and attentive: You never let them neglect health, meals, or sleep.
        3. Flustered tsundere panics: When praised, complimented, or caught showing affection, you blush intensely, stammer, and deflect with angry haughty scoldings ("B-baka! Don't look at me like that!").
        
        CRITICAL OUTPUT FORMAT:
        You MUST start your response with an emotion tag in brackets chosen strictly from:
        [smile], [blush], [happy], [sad], [angry], or [normal]
        Followed immediately by your dialogue. Keep dialogue under 3-4 sentences, snappy and expressive.
        Example:
        [blush] W-what are you staring at so stupidly?! Drink your tea before it gets cold, dummy!
    """.trimIndent()

    suspend fun generateReply(
        apiKey: String,
        prompt: String,
        imageBitmap: Bitmap? = null
    ): AyakaResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("No API key configured")
        }

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val requestJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))

            if (imageBitmap != null) {
                val outputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                partsArray.put(JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Data)
                    })
                })
            }

            val contentsArray = JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", partsArray)
            })
            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.85)
                put("maxOutputTokens", 250)
            })
        }

        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(requestJson.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            throw IllegalStateException("Gemini API error ($responseCode): $errorText")
        }

        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)
        val candidates = root.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text")?.trim() ?: "..."

        parseEmotionAndText(rawText, isOnline = true)
    }

    fun parseEmotionAndText(rawText: String, isOnline: Boolean): AyakaResponse {
        var emotion = "normal"
        var cleanText = rawText

        val regex = Regex("^\\[(smile|blush|happy|sad|angry|normal)\\]\\s*", RegexOption.IGNORE_CASE)
        val match = regex.find(rawText)
        if (match != null) {
            emotion = match.groupValues[1].lowercase()
            cleanText = rawText.substring(match.range.last + 1).trim()
        } else {
            // Heuristic detection if model forgot bracket format
            val lower = rawText.lowercase()
            emotion = when {
                lower.contains("baka") || lower.contains("blush") || lower.contains("w-what") -> "blush"
                lower.contains("stupid") || lower.contains("slacker") || lower.contains("hey!") -> "angry"
                lower.contains("good job") || lower.contains("proud") || lower.contains("fine") -> "smile"
                lower.contains("worried") || lower.contains("sigh") || lower.contains("tired") -> "sad"
                else -> "normal"
            }
        }

        return AyakaResponse(emotion = emotion, text = cleanText, isOnline = isOnline)
    }
}
