package com.example.ui.features

import android.media.MediaPlayer
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.GoogleMapsTool
import com.example.api.GoogleSearchTool
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.PrebuiltVoiceConfig
import com.example.api.RetrofitClient
import com.example.api.SpeechConfig
import com.example.api.Tool
import com.example.api.VoiceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class ChatMode(val title: String) {
    LIVE("Live Chat"),
    SEARCH("Web Search"),
    MAPS("Maps Grounding"),
    TTS("Text to Speech")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val audioBase64: String? = null,
    val imageBase64: String? = null,
    val imageMimeType: String? = null
)

class ShowcaseViewModel : ViewModel() {
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentMode = MutableStateFlow(ChatMode.LIVE)
    val currentMode: StateFlow<ChatMode> = _currentMode.asStateFlow()

    private val _systemInstruction = MutableStateFlow("")
    val systemInstruction: StateFlow<String> = _systemInstruction.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun setMode(mode: ChatMode) {
        _currentMode.value = mode
    }

    fun updateSystemInstruction(instruction: String) {
        _systemInstruction.value = instruction
    }

    fun sendMessage(text: String, cacheDir: File, imageBase64: String? = null, imageMimeType: String? = null) {
        if (text.isBlank() && imageBase64 == null) return
        
        val userMsg = ChatMessage(text = text, isUser = true, imageBase64 = imageBase64, imageMimeType = imageMimeType)
        _chatHistory.value = _chatHistory.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                when (_currentMode.value) {
                    ChatMode.SEARCH -> performSearch()
                    ChatMode.MAPS -> performMaps()
                    ChatMode.TTS -> performTts(cacheDir)
                    ChatMode.LIVE -> performLiveChat()
                }
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    text = "Error: ${e.message}",
                    isUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildHistoryContents(): List<Content> {
        return _chatHistory.value.map { msg ->
            val parts = mutableListOf<Part>()
            if (msg.text.isNotBlank()) {
                parts.add(Part(text = msg.text))
            }
            if (msg.imageBase64 != null && msg.imageMimeType != null) {
                parts.add(Part(inlineData = InlineData(mimeType = msg.imageMimeType, data = msg.imageBase64)))
            }
            Content(role = if (msg.isUser) "user" else "model", parts = parts)
        }
    }

    private fun getSystemInstructionContent(): Content? {
        val instruction = _systemInstruction.value
        return if (instruction.isNotBlank()) {
            Content(role = "system", parts = listOf(Part(text = instruction)))
        } else {
            null
        }
    }
    
    private suspend fun performLiveChat() {
        val request = GenerateContentRequest(
            contents = buildHistoryContents(),
            systemInstruction = getSystemInstructionContent()
        )
        val response = RetrofitClient.service.generateContent(
            model = "gemini-3.1-flash-live-preview",
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )
        val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
        _chatHistory.value = _chatHistory.value + ChatMessage(text = reply, isUser = false)
    }

    private suspend fun performSearch() {
        val request = GenerateContentRequest(
            contents = buildHistoryContents(),
            systemInstruction = getSystemInstructionContent(),
            tools = listOf(Tool(googleSearch = GoogleSearchTool()))
        )
        val response = RetrofitClient.service.generateContent(
            model = "gemini-3.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )
        val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
        _chatHistory.value = _chatHistory.value + ChatMessage(text = reply, isUser = false)
    }

    private suspend fun performMaps() {
        val request = GenerateContentRequest(
            contents = buildHistoryContents(),
            systemInstruction = getSystemInstructionContent(),
            tools = listOf(Tool(googleMaps = GoogleMapsTool()))
        )
        val response = RetrofitClient.service.generateContent(
            model = "gemini-3.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )
        val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
        _chatHistory.value = _chatHistory.value + ChatMessage(text = reply, isUser = false)
    }

    private suspend fun performTts(cacheDir: File) {
        val request = GenerateContentRequest(
            contents = buildHistoryContents(),
            systemInstruction = getSystemInstructionContent(),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = SpeechConfig(
                    voiceConfig = VoiceConfig(
                        prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = "Aoede")
                    )
                )
            )
        )
        val response = RetrofitClient.service.generateContent(
            model = "gemini-3.1-flash-tts-preview",
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )
        val audioBase64 = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
        
        if (audioBase64 != null) {
            _chatHistory.value = _chatHistory.value + ChatMessage(
                text = "Audio response generated",
                isUser = false,
                audioBase64 = audioBase64
            )
            playAudioFromBase64(audioBase64, cacheDir)
        } else {
             _chatHistory.value = _chatHistory.value + ChatMessage(text = "No audio data received", isUser = false)
        }
    }

    fun playAudioFromBase64(base64Str: String, cacheDir: File) {
        try {
            val audioData = Base64.decode(base64Str, Base64.DEFAULT)
            val tempFile = File(cacheDir, "temp_audio.mp3")
            FileOutputStream(tempFile).use { it.write(audioData) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearHistory() {
        _chatHistory.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}

