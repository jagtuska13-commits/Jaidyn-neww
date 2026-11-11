package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class ChatMessagePayload(
    val role: String,
    val text: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = null
)

object MistralApi {
    private const val BASE_URL = "https://api.mistral.ai"
    private const val CHAT_MODEL = BuildConfig.CHAT_MODEL
    private const val TTS_MODEL = "voxtral-mini-tts-3-0"
    private const val TRANSCRIBE_MODEL = "voxtral-mini-latest"
    private val VOICE = when (BuildConfig.TTS_VOICE) {
        "", "amelia" -> "gb_jane_neutral"
        else -> BuildConfig.TTS_VOICE
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun authHeader(): String = "Bearer ${BuildConfig.MISTRAL_API_KEY}"

    private fun buildContent(msg: ChatMessagePayload): Any {
        if (msg.imageBase64 != null && msg.imageMimeType != null) {
            val parts = JSONArray()
            if (msg.text.isNotBlank()) {
                parts.put(JSONObject().put("type", "text").put("text", msg.text))
            }
            parts.put(
                JSONObject().put("type", "image_url").put(
                    "image_url",
                    JSONObject().put("url", "data:${msg.imageMimeType};base64,${msg.imageBase64}")
                )
            )
            return parts
        }
        return msg.text
    }

    suspend fun chat(
        history: List<ChatMessagePayload>,
        systemInstruction: String
    ): String = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        }
        history.forEach { msg ->
            messages.put(
                JSONObject()
                    .put("role", msg.role)
                    .put("content", buildContent(msg))
            )
        }

        val body = JSONObject()
            .put("model", CHAT_MODEL)
            .put("messages", messages)

        val request = Request.Builder()
            .url("$BASE_URL/v1/chat/completions")
            .header("Authorization", authHeader())
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("Mistral chat error ${response.code}: ${responseBody.take(300)}")
            }
            val json = JSONObject(responseBody)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content")
                .ifBlank { "No response" }
        }
    }

    suspend fun textToSpeech(text: String, refAudioBase64: String? = null): ByteArray = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", TTS_MODEL)
            .put("input", text)
            .put("response_format", "mp3")
        if (refAudioBase64 != null) {
            body.put("ref_audio", refAudioBase64)
        } else {
            body.put("voice_id", VOICE)
        }

        val request = Request.Builder()
            .url("$BASE_URL/v1/audio/speech")
            .header("Authorization", authHeader())
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("Mistral TTS error ${response.code}: ${responseBody.take(300)}")
            }
            val audioData = JSONObject(responseBody).optString("audio_data")
            if (audioData.isBlank()) throw Exception("No audio data received")
            android.util.Base64.decode(audioData, android.util.Base64.NO_WRAP)
        }
    }

    suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/wav".toMediaType()))
            .addFormDataPart("model", TRANSCRIBE_MODEL)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/v1/audio/transcriptions")
            .header("Authorization", authHeader())
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("Mistral transcription error ${response.code}: ${responseBody.take(300)}")
            }
            JSONObject(responseBody).optString("text").trim()
        }
    }

    suspend fun ocrDocument(base64: String, mimeType: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", "mistral-ocr-latest")
            .put(
                "document",
                JSONObject()
                    .put("type", "document_url")
                    .put("document_url", "data:$mimeType;base64,$base64")
            )

        val request = Request.Builder()
            .url("$BASE_URL/v1/ocr")
            .header("Authorization", authHeader())
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("Mistral OCR error ${response.code}: ${responseBody.take(300)}")
            }
            val pages = JSONObject(responseBody).getJSONArray("pages")
            (0 until pages.length()).joinToString("\n\n") { i ->
                pages.getJSONObject(i).optString("markdown")
            }
        }
    }

    suspend fun webSearch(query: String, maxResults: Int = 4): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://html.duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext ""
                val html = response.body?.string().orEmpty()
                val linkRegex = Regex("class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                val snippetRegex = Regex("class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                val links = linkRegex.findAll(html).take(maxResults).toList()
                val snippets = snippetRegex.findAll(html).take(maxResults).toList()
                val sb = StringBuilder()
                links.forEachIndexed { i, match ->
                    val rawUrl = match.groupValues[1]
                    val realUrl = Regex("uddg=([^&]+)").find(rawUrl)?.groupValues?.get(1)
                        ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: rawUrl
                    val title = stripHtml(match.groupValues[2])
                    val snippet = if (i < snippets.size) stripHtml(snippets[i].groupValues[1]) else ""
                    sb.append("${i + 1}. $title — $snippet\nSource: $realUrl\n")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun stripHtml(s: String): String {
        return s.replace("<[^>]*>".toRegex(), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\n+", " ")
            .trim()
    }

    private fun File.asRequestBody(mediaType: okhttp3.MediaType) =
        okhttp3.RequestBody.create(mediaType, this)
}
