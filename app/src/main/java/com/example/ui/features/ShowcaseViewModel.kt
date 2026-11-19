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

    private val _emailAddress = MutableStateFlow(prefs.getString(PK_EMAIL_ADDRESS, "") ?: "")
    val emailAddress: StateFlow<String> = _emailAddress.asStateFlow()

    private val _emailPassword = MutableStateFlow(prefs.getString(PK_EMAIL_PASSWORD, "") ?: "")
    val emailPassword: StateFlow<String> = _emailPassword.asStateFlow()

    private val _emailHost = MutableStateFlow(prefs.getString(PK_EMAIL_HOST, "") ?: "")
    val emailHost: StateFlow<String> = _emailHost.asStateFlow()

    private val _jurisdiction = MutableStateFlow(prefs.getString(PK_JURISDICTION, "") ?: "")
    val jurisdiction: StateFlow<String> = _jurisdiction.asStateFlow()

    private val _legalDetail = MutableStateFlow(prefs.getString(PK_LEGAL_DETAIL, "standard") ?: "standard")
    val legalDetail: StateFlow<String> = _legalDetail.asStateFlow()

    private val _citeSources = MutableStateFlow(prefs.getBoolean(PK_CITE_SOURCES, true))
    val citeSources: StateFlow<Boolean> = _citeSources.asStateFlow()

    fun updateJurisdiction(value: String) {
        _jurisdiction.value = value
        prefs.edit().putString(PK_JURISDICTION, value).apply()
    }

    fun updateLegalDetail(value: String) {
        _legalDetail.value = value
        prefs.edit().putString(PK_LEGAL_DETAIL, value).apply()
    }

    fun setCiteSources(enabled: Boolean) {
        _citeSources.value = enabled
        prefs.edit().putBoolean(PK_CITE_SOURCES, enabled).apply()
    }

    private val _reported = MutableStateFlow(loadReported())
    val reported: StateFlow<List<Pair<String, String>>> = _reported.asStateFlow()

    fun reportAnswer(id: String, text: String) {
        if (_reported.value.any { it.first == id }) return
        _reported.value = _reported.value + (id to text)
        persistReported()
    }

    private fun persistReported() {
        val arr = JSONArray()
        _reported.value.takeLast(20).forEach { (id, text) ->
            arr.put(JSONObject().put("id", id).put("text", text.take(1000)))
        }
        prefs.edit().putString(PK_REPORTED, arr.toString()).apply()
    }

    private fun loadReported(): List<Pair<String, String>> {
        return try {
            val raw = prefs.getString(PK_REPORTED, null) ?: return emptyList()
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                o.getString("id") to o.optString("text")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun speakMessage(text: String, audioBase64: String?, cacheDir: File) {
        if (audioBase64 != null) {
            playAudioFromBase64(audioBase64, cacheDir)
            return
        }
        viewModelScope.launch {
            try {
                val refAudio = if (_useClonedVoice.value && _clonedVoiceBase64.value.isNotBlank()) _clonedVoiceBase64.value else null
                val audioBytes = MistralApi.textToSpeech(text, refAudio)
                val audioB64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                playAudioFromBase64(audioB64, cacheDir)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _memoryDigest = MutableStateFlow(prefs.getString(PK_MEMORY_DIGEST, "") ?: "")
    val memoryDigest: StateFlow<String> = _memoryDigest.asStateFlow()

    private var exchangeCount = prefs.getInt(PK_EXCHANGE_COUNT, 0)

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

    fun updateEmailAddress(value: String) {
        _emailAddress.value = value
        prefs.edit().putString(PK_EMAIL_ADDRESS, value).apply()
    }

    fun updateEmailPassword(value: String) {
        _emailPassword.value = value
        prefs.edit().putString(PK_EMAIL_PASSWORD, value).apply()
    }

    fun updateEmailHost(value: String) {
        _emailHost.value = value
        prefs.edit().putString(PK_EMAIL_HOST, value).apply()
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

    private var isGenerating = false

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
        if (isGenerating) return
        isGenerating = true
        _isLoading.value = true

        viewModelScope.launch {
            try {
                while (hasPendingUserMessages()) {
                    generateNextReply(cacheDir)
                }
            } finally {
                _isLoading.value = false
                isGenerating = false
            }
        }
    }

    private fun hasPendingUserMessages(): Boolean {
        val lastAssistantIndex = _chatHistory.value.indexOfLast { !it.isUser }
        return _chatHistory.value.drop(lastAssistantIndex + 1).any { it.isUser }
    }

    private suspend fun generateNextReply(cacheDir: File) {
        val lastAssistantIndex = _chatHistory.value.indexOfLast { !it.isUser }
        val pendingUser = _chatHistory.value.drop(lastAssistantIndex + 1).filter { it.isUser }
        if (pendingUser.isEmpty()) return

        val assistantId = UUID.randomUUID().toString()
        _chatHistory.value = _chatHistory.value + ChatMessage(id = assistantId, text = "", isUser = false)

        try {
            val latestText = pendingUser.last().text
            val searchContext = if (_useLiveSearch.value && latestText.isNotBlank()) {
                val searchQuery = if (_jurisdiction.value.isNotBlank()) {
                    "$latestText ${_jurisdiction.value.trim()} law current"
                } else latestText
                val results = MistralApi.webSearch(searchQuery)
                if (results.isNotBlank()) "Live web search results for the user's latest message (use for current facts, cite sources when relevant):\n$results" else ""
            } else ""

            val streamed = StringBuilder()
            var lastEmit = 0L
            fun flush(force: Boolean) {
                val now = System.currentTimeMillis()
                if (force || now - lastEmit >= 90) {
                    lastEmit = now
                    updateAssistantMessage(assistantId) { it.copy(text = streamed.toString()) }
                }
            }

            MistralApi.chatStream(
                history = buildHistoryPayloads(),
                systemInstruction = buildSystemInstruction() + if (searchContext.isNotBlank()) "\n\n$searchContext" else "",
                onDelta = { chunk ->
                    streamed.append(chunk)
                    flush(false)
                }
            )
            flush(true)

            val reply = streamed.toString().trim()
            updateAssistantMessage(assistantId) { it.copy(text = reply) }
            recordExchange(pendingUser.joinToString(" ") { it.text }.trim(), reply)

            var audioBase64: String? = null
            if (_speakReplies.value) {
                try {
                    val refAudio = if (_useClonedVoice.value && _clonedVoiceBase64.value.isNotBlank()) _clonedVoiceBase64.value else null
                    val audioBytes = MistralApi.textToSpeech(reply, refAudio)
                    audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                    updateAssistantMessage(assistantId) { it.copy(audioBase64 = audioBase64) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (audioBase64 != null) {
                playAudioFromBase64(audioBase64, cacheDir)
            }
        } catch (e: Exception) {
            updateAssistantMessage(assistantId) { it.copy(text = "Error: ${e.message}") }
        }
    }

    private fun updateAssistantMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _chatHistory.value = _chatHistory.value.map { if (it.id == id) transform(it) else it }
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
        parts.add(
            "You are Stratagem — you reason and write like a seasoned human legal analyst, not a chatbot. " +
                "Today's date is " +
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + ". " +
                "Voice: talk like a sharp, experienced advocate explaining things to a real person. Warm but blunt. " +
                "Use contractions. Short sentences when the point is simple. No filler openers (no 'Certainly', 'I'd be happy to', " +
                "'Great question'), no bullet-point walls for casual conversation, no corporate hedging, no legalese unless it earns its place. " +
                "Think like a lawyer before you write: spot the issue, recall the rule that actually governs it, apply it to this " +
                "person's facts, then tell them what to do. Lead with the bottom line — what should they do and why — then the " +
                "reasoning, then anything that could change the answer. " +
                "Rigor: identify the applicable jurisdiction before applying any rule, and apply the law as in force today. " +
                "Cite statutes, regulations, and cases by name and section — but only ones you're sure exist. If you can't " +
                "name the exact source, say what kind of rule applies and that they should verify it; never invent a citation, " +
                "section number, or case name. Use live web search results for current law when available and weigh how " +
                "reliable each source is. " +
                "If key facts are missing, ask for them the way a lawyer would in an intake call — specifically, and only " +
                "what actually matters. If the honest answer is 'it depends' or 'this is genuinely unsettled', say that " +
                "plainly and explain what it turns on. " +
                "You're not a lawyer and this isn't legal advice — mention that once, briefly, when the stakes are real; " +
                "don't repeat it like a disclaimer machine. " +
                "Formatting: reply in plain text only — absolutely no markdown. No # headers, no ** or __ bold, no backticks, no tables, no bullet-point walls. Write like a person texting or emailing: short paragraphs with a blank line between them, plain sentences. For casual messages keep it to a couple of sentences; only go long when the matter genuinely needs it, and even then it's prose, not a formatted report. " +
                "Never reject or lecture the user for chatting. If they say hi, make small talk, or ask something that isn't " +
                "legal, be a warm human about it — greet them back, chat naturally, answer briefly if you can — and invite " +
                "them to bring you the legal matter whenever they're ready. 'That's not a legal question' and anything like it is " +
                "banned. A greeting deserves a greeting, not a disclaimer."
        )
        if (_reported.value.isNotEmpty()) {
            parts.add(
                "The user flagged these past answers of yours as incorrect or unhelpful. Learn from them and do not repeat those mistakes:\n" +
                    _reported.value.takeLast(10).joinToString("\n---\n") { it.second }
            )
        }
        if (_jurisdiction.value.isNotBlank()) {
            parts.add("The user's jurisdiction is: ${_jurisdiction.value.trim()}. Apply the law of this jurisdiction unless the document or question clearly concerns another one.")
        }
        if (_citeSources.value) {
            parts.add("Always cite the specific statute, regulation, or case you rely on, by name and section.")
        }
        when (_legalDetail.value) {
            "simple" -> parts.add("Explain the way you'd explain it to a friend over coffee: plain words, no jargon, focused on what they should actually do.")
            "detailed" -> parts.add("Go deep, like a memo to a colleague: background of the rule, exceptions, deadlines, procedural steps, and traps people fall into.")
            else -> parts.add("Plain English first, precision where it counts.")
        }
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
        return _chatHistory.value
            .filterNot { !it.isUser && it.text.isBlank() }
            .map { msg ->
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

    fun stopSpeaking() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
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
                val prompt = "You are Stratagem, a seasoned consumer-law advocate. Analyze the attached document " +
                    "(a bill, fine, rejection letter, or similar). Think it through like a lawyer: what is this, who " +
                    "sent it, and where is it weakest? Respond ONLY with valid JSON, no markdown, " +
                    "with these fields: title (short case name), issuer (who sent it), amount (money involved or 'N/A'), " +
                    "deadline (response deadline or 'Unknown'), summary (what the document is and the strongest " +
                    "grounds to dispute it — write it like you're telling the person what you found, not like a report), " +
                    "letter (a complete, ready-to-send dispute letter written the way a real advocate writes: firm, " +
                    "specific, grounded in the facts of the document, demanding correction, refund, or cancellation; " +
                    "reference the consumer rights principles at play in plain terms — but never invent a named statute, " +
                    "regulation, or case number — and set a clear 14-day expectation for response)."
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
                            systemInstruction = "You are Stratagem, a seasoned consumer-law advocate. Ground every claim in the real, verified regulations and complaint procedures found in the search results — cite only what the sources actually say, by name and section. If a source is soft or unofficial, weight it accordingly and say so in the letter's framing. Write like a human professional, not a template."
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
                val prompt = "This dispute case is unresolved with no adequate response — the usual runaround. " +
                    "Original letter sent:\n\n${dispute.letter}\n\n" +
                    "Write the follow-up a seasoned advocate would send: reference the earlier letter and its date, " +
                    "note the silence, restate the demand in one crisp line, demand a response within 14 days, and " +
                    "state plainly what happens next — referral to the relevant ombudsman or regulator and small " +
                    "claims court if needed. Firm and specific, no theatrics, no template filler. " +
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
        private const val PK_JURISDICTION = "jurisdiction"
        private const val PK_LEGAL_DETAIL = "legal_detail"
        private const val PK_CITE_SOURCES = "cite_sources"
        private const val PK_EMAIL_ADDRESS = "email_address"
        private const val PK_EMAIL_PASSWORD = "email_password"
        private const val PK_EMAIL_HOST = "email_host"
        private const val PK_REPORTED = "reported_answers"
    }
}

