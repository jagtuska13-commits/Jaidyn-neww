package com.example.api

import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val tools: List<Tool>? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Tool(
    val googleSearch: GoogleSearchTool? = null,
    val googleMaps: GoogleMapsTool? = null
)

@JsonClass(generateAdapter = true)
class GoogleSearchTool

@JsonClass(generateAdapter = true)
class GoogleMapsTool

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String? = null,
    val data: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    val voiceConfig: VoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    val voiceName: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val groundingMetadata: GroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val searchEntryPoint: SearchEntryPoint? = null
)

@JsonClass(generateAdapter = true)
data class SearchEntryPoint(
    val renderedContent: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/{model}:streamGenerateContent")
    @Streaming
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): ResponseBody
}
