package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiMusicApi {
    private const val API_URL = BuildConfig.MUSIC_API_URL
    private const val TOKEN = BuildConfig.MUSIC_API_TOKEN
    private const val MODEL_VERSION = BuildConfig.MUSIC_MODEL_VERSION

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = TOKEN.isNotBlank() && MODEL_VERSION.isNotBlank()

    suspend fun generateTrack(
        prompt: String,
        lyrics: String,
        onStage: (String) -> Unit
    ): ByteArray = withContext(Dispatchers.IO) {
        if (!isConfigured()) throw Exception("Music API not configured")

        val body = JSONObject()
            .put("version", MODEL_VERSION)
            .put(
                "input",
                JSONObject()
                    .put("prompt", prompt)
                    .put("lyrics", lyrics)
                    .put("make_instrumental", false)
            )

        val submit = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer $TOKEN")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val id = client.newCall(submit).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("Music API submit failed ${response.code}: ${responseBody.take(300)}")
            }
            JSONObject(responseBody).optString("id").ifBlank { throw Exception("No job id returned") }
        }

        onStage("Rendering vocals and instruments...")

        var audioUrl: String? = null
        for (attempt in 0 until 60) {
            delay(5000)
            val poll = Request.Builder()
                .url("$API_URL/$id")
                .header("Authorization", "Bearer $TOKEN")
                .get()
                .build()
            client.newCall(poll).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw Exception("Music API poll failed ${response.code}")
                }
                val json = JSONObject(responseBody)
                when (json.optString("status")) {
                    "succeeded" -> {
                        val output = json.opt("output")
                        audioUrl = when (output) {
                            is String -> output
                            is org.json.JSONArray -> output.optString(0)
                            else -> throw Exception("No audio in output")
                        }
                    }
                    "failed", "canceled" -> throw Exception("Music generation failed: ${responseBody.take(200)}")
                    else -> onStage("Rendering... ${json.optString("status")}")
                }
            }
            if (audioUrl != null) break
        }

        val url = audioUrl ?: throw Exception("Timed out waiting for track")
        onStage("Downloading track...")

        val download = Request.Builder().url(url).get().build()
        client.newCall(download).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Download failed ${response.code}")
            response.body?.bytes() ?: throw Exception("Empty audio download")
        }
    }
}
