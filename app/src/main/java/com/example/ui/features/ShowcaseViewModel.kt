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
import com.example.api.MusicSynth
import com.example.api.SongSection
import com.example.api.SongSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SongResult(
    val title: String,
    val lyrics: String,
    val filePath: String,
    val style: String
)

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
    val imageMimeType: String? = null,
    val documentName: String? = null,
    val documentText: String? = null
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

    private val _isReadingDoc = MutableStateFlow(false)
    val isReadingDoc: StateFlow<Boolean> = _isReadingDoc.asStateFlow()

    fun processDocument(base64: String, mimeType: String, name: String, onDone: (String?) -> Unit) {
        if (_isReadingDoc.value) return
        _isReadingDoc.value = true
        viewModelScope.launch {
            try {
                val extracted = MistralApi.ocrDocument(base64, mimeType)
                if (extracted.isBlank()) onDone(null) else onDone(extracted.take(15000))
            } catch (e: Exception) {
                onDone(null)
            } finally {
                _isReadingDoc.value = false
            }
        }
    }

    private val _useLiveSearch = MutableStateFlow(prefs.getBoolean(PK_LIVE_SEARCH, true))
    val useLiveSearch: StateFlow<Boolean> = _useLiveSearch.asStateFlow()

    private val _memoryDigest = MutableStateFlow(prefs.getString(PK_MEMORY_DIGEST, "") ?: "")
    val memoryDigest: StateFlow<String> = _memoryDigest.asStateFlow()

    private var exchangeCount = prefs.getInt(PK_EXCHANGE_COUNT, 0)

    private val _isComposing = MutableStateFlow(false)
    val isComposing: StateFlow<Boolean> = _isComposing.asStateFlow()

    private val _currentSong = MutableStateFlow<SongResult?>(null)
    val currentSong: StateFlow<SongResult?> = _currentSong.asStateFlow()

    private val _composeStage = MutableStateFlow("")
    val composeStage: StateFlow<String> = _composeStage.asStateFlow()

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

    fun setUseLiveSearch(enabled: Boolean) {
        _useLiveSearch.value = enabled
        prefs.edit().putBoolean(PK_LIVE_SEARCH, enabled).apply()
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

    fun sendMessage(
        text: String,
        cacheDir: File,
        imageBase64: String? = null,
        imageMimeType: String? = null,
        documentName: String? = null,
        documentText: String? = null
    ) {
        if (text.isBlank() && imageBase64 == null && documentText == null) return

        val userMsg = ChatMessage(
            text = text,
            isUser = true,
            imageBase64 = imageBase64,
            imageMimeType = imageMimeType,
            documentName = documentName,
            documentText = documentText
        )
        _chatHistory.value = _chatHistory.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val searchContext = if (_useLiveSearch.value && text.isNotBlank()) {
                    val results = MistralApi.webSearch(text)
                    if (results.isNotBlank()) "Live web search results for the user's latest message (use for current facts, cite sources when relevant):\n$results" else ""
                } else ""
                val reply = MistralApi.chat(
                    history = buildHistoryPayloads(),
                    systemInstruction = buildSystemInstruction() + if (searchContext.isNotBlank()) "\n\n$searchContext" else ""
                )
                recordExchange(text, reply)
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

    private fun recordExchange(userText: String, assistantReply: String) {
        try {
            val log = JSONArray(prefs.getString(PK_MEMORY_LOG, "[]"))
            log.put(JSONObject().put("role", "user").put("text", userText.take(2000)).put("ts", System.currentTimeMillis()))
            log.put(JSONObject().put("role", "assistant").put("text", assistantReply.take(2000)).put("ts", System.currentTimeMillis()))
            while (log.length() > 200) log.remove(0)
            prefs.edit().putString(PK_MEMORY_LOG, log.toString()).apply()

            exchangeCount += 1
            prefs.edit().putInt(PK_EXCHANGE_COUNT, exchangeCount).apply()
            if (exchangeCount % 5 == 0) {
                updateMemoryDigest(log)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMemoryDigest(log: JSONArray) {
        viewModelScope.launch {
            try {
                val recent = (kotlin.math.max(0, log.length() - 20) until log.length())
                    .map { i ->
                        val e = log.getJSONObject(i)
                        "${e.optString("role")}: ${e.optString("text").take(500)}"
                    }
                    .joinToString("\n")
                val prompt = "Current memory profile of the user:\n${_memoryDigest.value.ifBlank { "(empty)" }}\n\n" +
                    "Recent conversation log:\n$recent\n\n" +
                    "Update the memory profile: key facts, preferences, ongoing matters, and how the user likes to be treated. " +
                    "Keep it under 300 words. Return ONLY the updated profile text."
                val updated = MistralApi.chat(
                    history = listOf(ChatMessagePayload(role = "user", text = prompt)),
                    systemInstruction = "You maintain a concise long-term memory profile of a user for a personal AI assistant."
                )
                if (updated.isNotBlank()) {
                    _memoryDigest.value = updated.trim()
                    prefs.edit().putString(PK_MEMORY_DIGEST, updated.trim()).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildSystemInstruction(): String {
        val parts = mutableListOf<String>()
        if (_memoryDigest.value.isNotBlank()) {
            parts.add("Your long-term memory of this user (from past sessions):\n${_memoryDigest.value.trim()}")
        }
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
            val text = if (msg.documentText != null) {
                (if (msg.text.isBlank()) "" else msg.text + "\n\n") +
                    "[Attached document: ${msg.documentName}]\n${msg.documentText.take(15000)}"
            } else {
                msg.text
            }
            ChatMessagePayload(
                role = if (msg.isUser) "user" else "assistant",
                text = text,
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
                var newCase = DisputeCase(
                    title = json.optString("title", "Dispute"),
                    issuer = json.optString("issuer", "Unknown issuer"),
                    amount = json.optString("amount", "N/A"),
                    deadline = json.optString("deadline", "Unknown"),
                    summary = json.optString("summary", ""),
                    letter = json.optString("letter", "")
                )
                try {
                    val searchResults = MistralApi.webSearch("${newCase.issuer} ${newCase.title} consumer rights dispute regulation complaint")
                    if (searchResults.isNotBlank()) {
                        val refined = MistralApi.chat(
                            history = listOf(
                                ChatMessagePayload(
                                    role = "user",
                                    text = "Here is a draft dispute letter:\n\n${newCase.letter}\n\n" +
                                        "Live web search results about the issuer and applicable consumer rights:\n$searchResults\n\n" +
                                        "Rewrite the letter to cite the real, current regulations and complaint procedures found in the results " +
                                        "where they apply. Only cite what the sources actually say. Return ONLY the updated letter."
                                )
                            ),
                            systemInstruction = "You are a dispute resolution expert who grounds letters in real, verifiable regulations."
                        )
                        if (refined.isNotBlank()) newCase = newCase.copy(letter = refined.trim())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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

    fun writeLyrics(description: String, genre: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val genreLine = if (genre != "Auto") "Genre: $genre.\n" else ""
                val prompt = "Write original song lyrics based on: $description\n\n$genreLine" +
                    "Return ONLY the lyrics text with [Verse]/[Chorus]/[Bridge] tags, no JSON, no commentary. " +
                    "Make them singable: strong rhythm, natural rhymes, lines of similar length."
                val lyrics = MistralApi.chat(
                    history = listOf(ChatMessagePayload(role = "user", text = prompt)),
                    systemInstruction = "You are a professional songwriter. Return only lyrics."
                )
                onDone(lyrics.trim().ifBlank { null })
            } catch (e: Exception) {
                onDone(null)
            }
        }
    }

    fun composeSong(description: String, genre: String, userLyrics: String?, onDone: (Boolean) -> Unit) {
        if (_isComposing.value || description.isBlank()) return
        _isComposing.value = true
        _composeStage.value = "Writing lyrics and composition..."
        viewModelScope.launch {
            try {
                val lyricsDirective = if (!userLyrics.isNullOrBlank()) {
                    "The song MUST sing exactly these lyrics (use them verbatim in the lyrics field):\n$userLyrics\n\n"
                } else ""
                val genreDirective = if (genre != "Auto") "The genre MUST be: $genre.\n" else ""
                val prompt = "Write an original song based on: $description\n\n$genreDirective$lyricsDirective" +
                    "Respond ONLY with valid JSON, no markdown: " +
                    "{\"title\": string, \"lyrics\": string (full lyrics with [Verse]/[Chorus] tags), " +
                    "\"bpm\": int 60-160, \"style\": short style description, " +
                    "\"genre\": one of: trap, hiphop, drill, house, edm, techno, dance, lofi, chill, jazz, rock, metal, punk, pop, ballad, ambient, reggaeton, afrobeats, " +
                    "\"sections\": [{\"name\": string (intro/verse/chorus/drop/bridge/outro), \"bars\": int 2-8, " +
                    "\"chords\": array of chord symbols like Am F C G7 (4 per section), " +
                    "\"melody\": array of melody tokens like A4:2 C5:1 E5:1 (note:beats, 8-16 tokens per section)}]}. " +
                    "Use 5-8 sections with a real song structure. " +
                    "Each section's melody should follow the phrasing of the lyrics sung in that section (roughly one melody note per syllable). " +
                    "Melody notes must fit the section's chords. Tempo and genre must match the user's request."
                val reply = MistralApi.chat(
                    history = listOf(ChatMessagePayload(role = "user", text = prompt)),
                    systemInstruction = "You are a professional songwriter and composer. Return only valid JSON."
                )
                val jsonStart = reply.indexOf('{')
                val jsonEnd = reply.lastIndexOf('}')
                if (jsonStart < 0 || jsonEnd <= jsonStart) throw Exception("Bad composition")
                val json = JSONObject(reply.substring(jsonStart, jsonEnd + 1))
                val sections = mutableListOf<SongSection>()
                val arr = json.getJSONArray("sections")
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    sections.add(
                        SongSection(
                            name = s.optString("name", "section"),
                            bars = s.optInt("bars", 4),
                            chords = s.optJSONArray("chords")?.let { c ->
                                (0 until c.length()).map { c.optString(it) }.filter { it.isNotBlank() }
                            } ?: emptyList(),
                            melody = s.optJSONArray("melody")?.let { m ->
                                (0 until m.length()).map { m.optString(it) }.filter { it.isNotBlank() }
                            } ?: emptyList()
                        )
                    )
                }
                if (sections.isEmpty()) throw Exception("No sections")
                var effectiveLyrics = if (!userLyrics.isNullOrBlank()) userLyrics else json.optString("lyrics", "")
                if (effectiveLyrics.isBlank()) {
                    _composeStage.value = "Writing lyrics..."
                    effectiveLyrics = MistralApi.chat(
                        history = listOf(
                            ChatMessagePayload(
                                role = "user",
                                text = "Write original song lyrics based on: $description. Return ONLY the lyrics with [Verse]/[Chorus] tags."
                            )
                        ),
                        systemInstruction = "You are a professional songwriter. Return only lyrics."
                    ).trim()
                }
                val effectiveGenre = if (genre != "Auto") genre.lowercase() else json.optString("genre", "")
                val spec = SongSpec(
                    bpm = json.optInt("bpm", 90),
                    style = json.optString("style", ""),
                    genre = effectiveGenre,
                    lyrics = effectiveLyrics,
                    sections = sections
                )
                _composeStage.value = "Rendering audio..."
                val wavBytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    MusicSynth.render(spec)
                }
                val file = File(getApplication<Application>().filesDir, "song_${System.currentTimeMillis()}.wav")
                FileOutputStream(file).use { it.write(wavBytes) }
                _currentSong.value = SongResult(
                    title = json.optString("title", "Untitled"),
                    lyrics = effectiveLyrics,
                    filePath = file.absolutePath,
                    style = "${spec.genre.ifBlank { spec.style }} · ${spec.bpm} BPM"
                )
                onDone(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(false)
            } finally {
                _isComposing.value = false
                _composeStage.value = ""
            }
        }
    }

    fun playSong(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSongToDownloads(song: SongResult): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "${song.title.replace(Regex("[^\\w -]"), "")}.wav")
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "audio/wav")
                }
                val resolver = getApplication<Application>().contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { it.write(File(song.filePath).readBytes()) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
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
        private const val PK_LIVE_SEARCH = "use_live_search"
        private const val PK_MEMORY_DIGEST = "memory_digest"
        private const val PK_MEMORY_LOG = "memory_log"
        private const val PK_EXCHANGE_COUNT = "exchange_count"
    }
}

