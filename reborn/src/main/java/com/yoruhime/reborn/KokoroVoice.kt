package com.yoruhime.reborn

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import com.google.mediapipe.tasks.audio.texttospeech.SynthesisOptions
import com.google.mediapipe.tasks.audio.texttospeech.TextToSpeech as KokoroTts
import com.google.mediapipe.tasks.audio.texttospeech.TextToSpeechOptions
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.sqrt

/**
 * Optional, fully on-device Kokoro voice. The ~295 MB model is downloaded only after
 * the user explicitly requests installation. Android system TTS remains the fallback.
 */
internal object KokoroVoice {
    const val MODEL_NAME = "kokoro_int8_fp16.litertlm"
    const val MODEL_BYTES_APPROX = 295L * 1024L * 1024L
    private const val MODEL_URL = "https://huggingface.co/litert-community/Kokoro-82M/resolve/main/kokoro_int8_fp16.litertlm"
    private const val MODEL_SHA256 = "e7b931ee5d4b7104012a838c9eee240d19ea69cb3ca269041a3274f83dbb7fa1"

    fun modelFile(context: Context) = File(context.filesDir, MODEL_NAME)
    fun isInstalled(context: Context) = modelFile(context).let { it.isFile && it.length() > 0L }

    suspend fun download(context: Context, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val target = modelFile(context)
        if (target.isFile && target.length() > 0L && sha256(target) == MODEL_SHA256) {
            onProgress(100)
            return@withContext
        }
        val temporary = File(context.filesDir, "$MODEL_NAME.download")
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "YoruHime-Android")
        }
        try {
            connection.connect()
            check(connection.responseCode in 200..299) { "Kokoro download failed: HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong.takeIf { it > 0L } ?: MODEL_BYTES_APPROX
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        onProgress(((copied * 100L) / total).toInt().coerceIn(0, 99))
                    }
                }
            }
            check(temporary.length() > 100_000_000L) { "Downloaded Kokoro model is incomplete." }
            check(sha256(temporary) == MODEL_SHA256) { "Kokoro model checksum verification failed." }
            check(temporary.renameTo(target) || temporary.copyTo(target, overwrite = true).let { temporary.delete(); true }) {
                "Could not save the Kokoro model."
            }
            onProgress(100)
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    suspend fun speak(
        context: Context,
        text: String,
        voiceId: String,
        speakingRate: Float,
        onStart: () -> Unit,
        onViseme: (String) -> Unit,
        onDone: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val model = modelFile(context)
        check(model.isFile && model.length() > 0L) { "Kokoro voice has not been downloaded." }
        val optionsFor: (Delegate) -> TextToSpeechOptions = { delegate ->
            TextToSpeechOptions(
                baseOptions = BaseOptions.builder()
                    .setModelAssetPath(model.absolutePath)
                    .setDelegate(delegate)
                    .build(),
                defaultVoiceId = voiceId,
                speakingRate = speakingRate.coerceIn(0.75f, 1.25f),
                targetBucket = 64,
                cacheDirectory = context.cacheDir.absolutePath
            )
        }
        val engine = try {
            KokoroTts.create(optionsFor(Delegate.GPU))
        } catch (_: Throwable) {
            KokoroTts.create(optionsFor(Delegate.CPU))
        }
        var track: AudioTrack? = null
        try {
            val audio = engine.synthesize(text, SynthesisOptions(voiceId = voiceId))
            val samples = audio.samples
            check(samples.isNotEmpty()) { "Kokoro returned empty audio." }
            val sampleRate = audio.sampleRateHz
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(samples.size * Float.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track = audioTrack
            check(audioTrack.state == AudioTrack.STATE_INITIALIZED) { "Android audio output could not be initialized." }
            val written = audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            check(written == samples.size) { "Only $written of ${samples.size} audio samples were queued." }
            onStart()
            audioTrack.play()
            val window = (sampleRate / 20).coerceAtLeast(1)
            var lastViseme = ""
            val started = SystemClock.elapsedRealtime()
            while (audioTrack.playbackHeadPosition.toLong() < samples.size.toLong()) {
                val position = audioTrack.playbackHeadPosition.coerceIn(0, samples.size)
                val from = (position - window).coerceAtLeast(0)
                val to = position.coerceAtMost(samples.size)
                var energy = 0.0
                for (index in from until to) energy += samples[index].toDouble() * samples[index].toDouble()
                val rms = if (to > from) sqrt(energy / (to - from)) else 0.0
                val progress = position.toFloat() / samples.size.toFloat()
                val charIndex = (progress * text.length).toInt().coerceIn(0, (text.length - 1).coerceAtLeast(0))
                val chunk = if (text.isEmpty()) "" else text.substring(charIndex, (charIndex + 3).coerceAtMost(text.length))
                val viseme = if (rms < 0.008) "neutral" else inferViseme(chunk)
                if (viseme != lastViseme) { lastViseme = viseme; onViseme(viseme) }
                SystemClock.sleep(35)
                if (SystemClock.elapsedRealtime() - started > 120_000L) error("Kokoro playback timed out.")
            }
            onDone()
        } finally {
            try { track?.stop() } catch (_: IllegalStateException) {}
            track?.release()
            engine.close()
        }
    }

    private fun inferViseme(text: String): String {
        val value = text.lowercase()
        return when {
            value.any { it == 'a' } -> "aa"
            value.any { it == 'e' } -> "ee"
            value.any { it == 'i' || it == 'y' } -> "ih"
            value.any { it == 'o' } -> "oh"
            value.any { it == 'u' } -> "ou"
            else -> "aa"
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
