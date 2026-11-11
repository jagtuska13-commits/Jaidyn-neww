package com.example.ui.features

import android.media.MediaPlayer
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ChatMessagePayload
import com.example.api.MistralApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class ChatMode(val title: String) {
    LIVE("Live Chat"),
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

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

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

    private fun buildHistoryPayloads(): List<ChatMessagePayload> {
        return _chatHistory.value.map { msg ->
            ChatMessagePayload(
                role = if (msg.isUser) "user" else "assistant",
                text = msg.text,
                imageBase64 = msg.imageBase64,
                imageMimeType = msg.imageMimeType
            )
        }
    }

    private suspend fun performLiveChat() {
        val reply = MistralApi.chat(
            history = buildHistoryPayloads(),
            systemInstruction = _systemInstruction.value
        )
        _chatHistory.value = _chatHistory.value + ChatMessage(text = reply, isUser = false)
    }

    private suspend fun performTts(cacheDir: File) {
        val reply = MistralApi.chat(
            history = buildHistoryPayloads(),
            systemInstruction = _systemInstruction.value
        )
        val audioBytes = MistralApi.textToSpeech(reply)
        val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        _chatHistory.value = _chatHistory.value + ChatMessage(
            text = reply,
            isUser = false,
            audioBase64 = audioBase64
        )
        playAudioFromBase64(audioBase64, cacheDir)
    }

    fun transcribeAudio(audioFile: File, onResult: (String) -> Unit) {
        _isTranscribing.value = true
        viewModelScope.launch {
            try {
                val text = MistralApi.transcribe(audioFile)
                if (text.isNotBlank()) onResult(text)
            } catch (e: Exception) {
                onResult("")
            } finally {
                _isTranscribing.value = false
                audioFile.delete()
            }
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
