package com.example.ui.features

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
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

enum class DisputeStatus { DRAFTED, SENT, AWAITING, RESOLVED }

data class DisputeCase(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val issuer: String,
    val amount: String,
    val deadline: String,
    val summary: String,
    val letter: String,
    val followUpLetter: String? = null,
    val status: DisputeStatus = DisputeStatus.DRAFTED,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val audioBase64: String? = null,
    val imageBase64: String? = null,
    val imageMimeType: String? = null
)

class ShowcaseViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences = application.getSharedPreferences("jaggedgem_prefs", Context.MODE_PRIVATE)

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _personaName = MutableStateFlow(prefs.getString(PK_PERSONA_NAME, "") ?: "")
    val personaName: StateFlow<String> = _personaName.asStateFlow()

    private val _personaText = MutableStateFlow(prefs.getString(PK_PERSONA_TEXT, "") ?: "")
    val personaText: StateFlow<String> = _personaText.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString(PK_USER_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userAbout = MutableStateFlow(prefs.getString(PK_USER_ABOUT, "") ?: "")
    val userAbout: StateFlow<String> = _userAbout.asStateFlow()

    private val _speakReplies = MutableStateFlow(prefs.getBoolean(PK_SPEAK_REPLIES, true))
    val speakReplies: StateFlow<Boolean> = _speakReplies.asStateFlow()

    private val _useClonedVoice = MutableStateFlow(prefs.getBoolean(PK_USE_CLONED, false))
    val useClonedVoice: StateFlow<Boolean> = _useClonedVoice.asStateFlow()

    private val _clonedVoiceBase64 = MutableStateFlow(prefs.getString(PK_CLONED_VOICE, "") ?: "")
    val clonedVoiceBase64: StateFlow<String> = _clonedVoiceBase64.asStateFlow()

    private val _isSavingVoice = MutableStateFlow(false)
    val isSavingVoice: StateFlow<Boolean> = _isSavingVoice.asStateFlow()

    private val _disputes = MutableStateFlow(loadDisputes())
    val disputes: StateFlow<List<DisputeCase>> = _disputes.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun updatePersonaName(value: String) {
        _personaName.value = value
        prefs.edit().putString(PK_PERSONA_NAME, value).apply()
    }

    fun updatePersonaText(value: String) {
        _personaText.value = value
        prefs.edit().putString(PK_PERSONA_TEXT, value).apply()
    }

    fun updateUserName(value: String) {
        _userName.value = value
        prefs.edit().putString(PK_USER_NAME, value).apply()
    }

    fun updateUserAbout(value: String) {
        _userAbout.value = value
        prefs.edit().putString(PK_USER_ABOUT, value).apply()
    }

    fun setSpeakReplies(enabled: Boolean) {
        _speakReplies.value = enabled
        prefs.edit().putBoolean(PK_SPEAK_REPLIES, enabled).apply()
    }

    fun setUseClonedVoice(enabled: Boolean) {
        _useClonedVoice.value = enabled
        prefs.edit().putBoolean(PK_USE_CLONED, enabled).apply()
    }

    fun saveVoiceSample(wavFile: File) {
        _isSavingVoice.value = true
        viewModelScope.launch {
            try {
                val bytes = wavFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                _clonedVoiceBase64.value = base64
                prefs.edit().putString(PK_CLONED_VOICE, base64).apply()
                _useClonedVoice.value = true
                prefs.edit().putBoolean(PK_USE_CLONED, true).apply()
            } finally {
                wavFile.delete()
                _isSavingVoice.value = false
            }
        }
    }

    fun clearVoiceSample() {
        _clonedVoiceBase64.value = ""
        _useClonedVoice.value = false
        prefs.edit().remove(PK_CLONED_VOICE).putBoolean(PK_USE_CLONED, false).apply()
    }

    fun sendMessage(text: String, cacheDir: File, imageBase64: String? = null, imageMimeType: String? = null) {
        if (text.isBlank() && imageBase64 == null) return

        val userMsg = ChatMessage(text = text, isUser = true, imageBase64 = imageBase64, imageMimeType = imageMimeType)
        _chatHistory.value = _chatHistory.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val reply = MistralApi.chat(
                    history = buildHistoryPayloads(),
                    systemInstruction = buildSystemInstruction()
                )
                var audioBase64: String? = null
                if (_speakReplies.value) {
                    try {
                        val refAudio = if (_useClonedVoice.value && _clonedVoiceBase64.value.isNotBlank()) _clonedVoiceBase64.value else null
                        val audioBytes = MistralApi.textToSpeech(reply, refAudio)
                        audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    text = reply,
                    isUser = false,
                    audioBase64 = audioBase64
                )
                if (audioBase64 != null) {
                    playAudioFromBase64(audioBase64, cacheDir)
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

    private fun buildSystemInstruction(): String {
        val parts = mutableListOf<String>()
        if (_personaName.value.isNotBlank()) {
            parts.add("Your name is ${_personaName.value.trim()}.")
        }
        if (_personaText.value.isNotBlank()) {
            parts.add(_personaText.value.trim())
        }
        if (_userName.value.isNotBlank()) {
            parts.add("You are speaking with ${_userName.value.trim()}.")
        }
        if (_userAbout.value.isNotBlank()) {
            parts.add("About the person you are speaking with: ${_userAbout.value.trim()}")
        }
        return parts.joinToString("\n")
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

    fun createDispute(imageBase64: String, imageMimeType: String, onDone: (Boolean) -> Unit) {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val prompt = "You are a dispute resolution expert. Analyze the attached document " +
                    "(a bill, fine, rejection letter, or similar). Respond ONLY with valid JSON, no markdown, " +
                    "with these fields: title (short case name), issuer (who sent it), amount (money involved or 'N/A'), " +
                    "deadline (response deadline or 'Unknown'), summary (what the document is and the strongest " +
                    "grounds to dispute it), letter (a complete, formal, ready-to-send dispute letter that demands " +
                    "correction, refund, or cancellation, citing relevant consumer rights generally without inventing " +
                    "specific laws or case numbers)."
                val reply = MistralApi.chat(
                    history = listOf(
                        ChatMessagePayload(
                            role = "user",
                            text = prompt,
                            imageBase64 = imageBase64,
                            imageMimeType = imageMimeType
                        )
                    ),
                    systemInstruction = ""
                )
                val jsonStart = reply.indexOf('{')
                val jsonEnd = reply.lastIndexOf('}')
                if (jsonStart < 0 || jsonEnd <= jsonStart) throw Exception("Could not analyze document")
                val json = JSONObject(reply.substring(jsonStart, jsonEnd + 1))
                val newCase = DisputeCase(
                    title = json.optString("title", "Dispute"),
                    issuer = json.optString("issuer", "Unknown issuer"),
                    amount = json.optString("amount", "N/A"),
                    deadline = json.optString("deadline", "Unknown"),
                    summary = json.optString("summary", ""),
                    letter = json.optString("letter", "")
                )
                _disputes.value = listOf(newCase) + _disputes.value
                persistDisputes()
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun draftFollowUp(disputeId: String, onDone: (Boolean) -> Unit) {
        val dispute = _disputes.value.firstOrNull { it.id == disputeId } ?: return onDone(false)
        viewModelScope.launch {
            try {
                val prompt = "This dispute case is unresolved with no adequate response. " +
                    "Original letter sent:\n\n${dispute.letter}\n\n" +
                    "Write a firm follow-up letter that escalates: demands response within 14 days, " +
                    "threatens referral to the relevant ombudsman/regulator and small claims court. " +
                    "Return ONLY the letter text."
                val reply = MistralApi.chat(
                    history = listOf(ChatMessagePayload(role = "user", text = prompt)),
                    systemInstruction = ""
                )
                updateDispute(disputeId) { it.copy(followUpLetter = reply.trim()) }
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun setDisputeStatus(disputeId: String, status: DisputeStatus) {
        updateDispute(disputeId) { it.copy(status = status) }
    }

    fun deleteDispute(disputeId: String) {
        _disputes.value = _disputes.value.filterNot { it.id == disputeId }
        persistDisputes()
    }

    private fun updateDispute(disputeId: String, transform: (DisputeCase) -> DisputeCase) {
        _disputes.value = _disputes.value.map { if (it.id == disputeId) transform(it) else it }
        persistDisputes()
    }

    private fun persistDisputes() {
        val arr = JSONArray()
        _disputes.value.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("title", d.title)
                    .put("issuer", d.issuer)
                    .put("amount", d.amount)
                    .put("deadline", d.deadline)
                    .put("summary", d.summary)
                    .put("letter", d.letter)
                    .put("followUpLetter", d.followUpLetter ?: "")
                    .put("status", d.status.name)
                    .put("createdAt", d.createdAt)
            )
        }
        prefs.edit().putString(PK_DISPUTES, arr.toString()).apply()
    }

    private fun loadDisputes(): List<DisputeCase> {
        return try {
            val raw = prefs.getString(PK_DISPUTES, null) ?: return emptyList()
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val json = arr.getJSONObject(i)
                DisputeCase(
                    id = json.getString("id"),
                    title = json.getString("title"),
                    issuer = json.getString("issuer"),
                    amount = json.optString("amount", "N/A"),
                    deadline = json.optString("deadline", "Unknown"),
                    summary = json.optString("summary", ""),
                    letter = json.optString("letter", ""),
                    followUpLetter = json.optString("followUpLetter").ifBlank { null },
                    status = try {
                        DisputeStatus.valueOf(json.optString("status", "DRAFTED"))
                    } catch (e: Exception) {
                        DisputeStatus.DRAFTED
                    },
                    createdAt = json.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        _chatHistory.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }

    companion object {
        private const val PK_PERSONA_NAME = "persona_name"
        private const val PK_PERSONA_TEXT = "persona_text"
        private const val PK_USER_NAME = "user_name"
        private const val PK_USER_ABOUT = "user_about"
        private const val PK_SPEAK_REPLIES = "speak_replies"
        private const val PK_USE_CLONED = "use_cloned_voice"
        private const val PK_CLONED_VOICE = "cloned_voice_base64"
        private const val PK_DISPUTES = "disputes_json"
    }
}

