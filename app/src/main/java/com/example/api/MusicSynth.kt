package com.example.api

data class SongSection(
    val name: String,
    val bars: Int,
    val chords: List<String>,
    val melody: List<String>
)

data class SongSpec(
    val bpm: Int,
    val style: String,
    val genre: String,
    val lyrics: String,
    val sections: List<SongSection>
)

object MusicSynth {
    private const val SR = 44100

    private val NOTE_BASE = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

    fun noteToFreq(note: String): Float? {
        if (note.length < 2) return null
        val base = NOTE_BASE[note[0]] ?: return null
        var i = 1
        var semitone = base
        if (note[i] == '#') { semitone += 1; i++ } else if (note[i] == 'b') { semitone -= 1; i++ }
        val octave = note.substring(i).toIntOrNull() ?: return null
        return midiToFreq((octave + 1) * 12 + semitone)
    }

    private fun midiToFreq(midi: Int): Float = (440f * Math.pow(2.0, (midi - 69) / 12.0)).toFloat()

    private fun parseChord(chord: String): List<Int> {
        val trimmed = chord.trim()
        if (trimmed.isEmpty()) return listOf(57)
        val root = NOTE_BASE[trimmed[0]] ?: return listOf(57)
        var idx = 1
        var semitone = root
        if (idx < trimmed.length && (trimmed[idx] == '#' || trimmed[idx] == 'b')) {
            semitone += if (trimmed[idx] == '#') 1 else -1
            idx++
        }
        val rest = trimmed.substring(idx)
        val isMinor = rest.contains("m") && !rest.contains("maj")
        val isDim = rest.contains("dim")
        val isSeven = rest.contains("7")
        val third = when { isDim -> semitone + 3; isMinor -> semitone + 3; else -> semitone + 4 }
        val fifth = if (isDim) semitone + 6 else semitone + 7
        val notes = mutableListOf(semitone + 48, third + 48, fifth + 48)
        if (isSeven) notes.add((if (isMinor) semitone + 10 else semitone + 11) + 48)
        return notes
    }

    private fun saw(t: Float, freq: Float): Float {
        val phase = (freq * t) % 1f
        return (2f * phase - 1f)
    }

    private fun square(t: Float, freq: Float): Float {
        val phase = (freq * t) % 1f
        return if (phase < 0.5f) 1f else -1f
    }

    private fun supersaw(t: Float, freq: Float): Float {
        var v = 0f
        val detunes = floatArrayOf(0.994f, 0.997f, 1f, 1.003f, 1.006f)
        for (d in detunes) v += saw(t, freq * d)
        return v / 5f
    }

    private fun softClip(x: Float): Float = Math.tanh(x.toDouble()).toFloat()

    fun render(spec: SongSpec): ByteArray {
        val bpm = spec.bpm.coerceIn(50, 180)
        val beat = 60f / bpm
        val totalBars = spec.sections.sumOf { it.bars.coerceIn(1, 16) }
        val totalBeats = totalBars * 4f
        val n = (totalBeats * beat * SR).toInt() + SR * 2
        val drums = FloatArray(n)
        val music = FloatArray(n)
        val wet = FloatArray(n)
        val vocals = FloatArray(n)

        val lyricLines = spec.lyrics
            .replace(Regex("\\[[^]]*\\]"), " ")
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        var lineIdx = 0

        val genre = spec.genre.lowercase()
        val isTrap = genre.contains("trap") || genre.contains("hip") || genre.contains("drill")
        val isHouse = genre.contains("house") || genre.contains("edm") || genre.contains("techno") || genre.contains("dance")
        val isLofi = genre.contains("lofi") || genre.contains("chill") || genre.contains("jazz")
        val isRock = genre.contains("rock") || genre.contains("metal") || genre.contains("punk")
        val isAmbient = genre.contains("ambient")
        val swing = if (isLofi) 0.10f else 0f

        var globalBar = 0
        for (section in spec.sections) {
            val bars = section.bars.coerceIn(1, 16)
            val chords = if (section.chords.isEmpty()) listOf("Am") else section.chords
            val melody = section.melody.filter { it.isNotBlank() }.ifEmpty { chordsToMelody(chords) }
            val isDrop = section.name.contains("drop") || section.name.contains("chorus")
            val intensity = if (isDrop) 1.15f else if (section.name.contains("intro") || section.name.contains("outro")) 0.7f else 1f
            val sectionStartBeat = globalBar * 4f

            for (b in 0 until bars) {
                val barStart = globalBar * 4f
                val chordMidis = parseChord(chords[b % chords.size])
                val rootFreq = midiToFreq(chordMidis[0] - 24)

                if (isTrap) {
                    hitKick(drums, (barStart) * beat, 0.5f * intensity)
                    hitKick(drums, (barStart + 2.75f) * beat, 0.4f * intensity)
                    hitSnare(drums, (barStart + 2f) * beat, 0.35f * intensity, 1800f)
                    var hb = 0f
                    while (hb < 4f) {
                        val roll = (hb % 1f == 0.5f) && (b % 2 == 1)
                        hitHat(drums, (barStart + hb + (if (hb % 1f == 0.5f) swing else 0f)) * beat, 0.05f, 0.08f * intensity, if (roll) 0.5f else 1f)
                        if (roll) hitHat(drums, (barStart + hb + 0.25f) * beat, 0.04f, 0.06f * intensity, 1f)
                        hb += 0.5f
                    }
                    glide808(music, barStart * beat, 2.2f * beat, rootFreq, 0.30f * intensity)
                    chordMidis.forEach { m -> padVoice(wet, barStart * beat, 4f * beat, midiToFreq(m), 0.045f * intensity) }
                } else if (isHouse) {
                    for (k in 0 until 4) hitKick(drums, (barStart + k) * beat, 0.5f * intensity)
                    for (oh in 0 until 4) hitHat(drums, (barStart + oh + 0.5f) * beat, 0.12f, 0.09f * intensity, 1f)
                    hitSnare(drums, (barStart + 1f) * beat, 0.16f * intensity, 2600f)
                    hitSnare(drums, (barStart + 3f) * beat, 0.16f * intensity, 2600f)
                    chordMidis.forEach { m -> stabVoice(music, (barStart + 0.5f) * beat, 0.4f * beat, midiToFreq(m), 0.10f * intensity) }
                    chordMidis.forEach { m -> stabVoice(music, (barStart + 2.5f) * beat, 0.4f * beat, midiToFreq(m), 0.10f * intensity) }
                    for (e in 0 until 8) {
                        tone(music, (barStart + e * 0.5f) * beat, 0.22f * beat, rootFreq, 0.14f * intensity, 3)
                    }
                    chordMidis.forEach { m -> padVoice(wet, barStart * beat, 4f * beat, midiToFreq(m), 0.05f * intensity) }
                } else if (isRock) {
                    hitKick(drums, (barStart) * beat, 0.5f * intensity)
                    hitKick(drums, (barStart + 2f) * beat, 0.5f * intensity)
                    hitKick(drums, (barStart + 2.5f) * beat, 0.3f * intensity)
                    hitSnare(drums, (barStart + 1f) * beat, 0.35f * intensity, 1500f)
                    hitSnare(drums, (barStart + 3f) * beat, 0.35f * intensity, 1500f)
                    for (e in 0 until 8) hitHat(drums, (barStart + e * 0.5f) * beat, 0.04f, 0.05f * intensity, 1f)
                    chordMidis.forEach { m ->
                        powerChord(music, barStart * beat, 3.8f * beat, midiToFreq(m), 0.075f * intensity)
                    }
                    for (e in 0 until 8) {
                        tone(music, (barStart + e * 0.5f) * beat, 0.3f * beat, rootFreq * 2f, 0.13f * intensity, 2)
                    }
                } else if (isAmbient) {
                    chordMidis.forEach { m -> padVoice(wet, barStart * beat, 4.2f * beat, midiToFreq(m), 0.07f * intensity) }
                    tone(music, barStart * beat, 3.8f * beat, rootFreq / 2f, 0.10f * intensity, 1)
                } else {
                    hitKick(drums, (barStart) * beat, 0.45f * intensity)
                    hitKick(drums, (barStart + 2.5f) * beat, 0.35f * intensity)
                    hitSnare(drums, (barStart + 1f) * beat, 0.22f * intensity, 2000f)
                    hitSnare(drums, (barStart + 3f) * beat, 0.22f * intensity, 2000f)
                    var hb = 0f
                    while (hb < 4f) {
                        hitHat(drums, (barStart + hb + (if (hb % 1f == 0.5f) swing else 0f)) * beat, 0.05f, 0.06f * intensity, 1f)
                        hb += 0.5f
                    }
                    chordMidis.forEach { m -> padVoice(wet, barStart * beat, 4f * beat, midiToFreq(m), 0.06f * intensity) }
                    var arpIdx = 0
                    for (e in 0 until 8) {
                        val m = chordMidis[arpIdx % chordMidis.size]
                        pianoTone(music, (barStart + e * 0.5f + (if (e % 2 == 1) swing else 0f)) * beat, midiToFreq(m + 12), 0.09f * intensity)
                        arpIdx++
                    }
                    tone(music, barStart * beat, 1.8f * beat, rootFreq, 0.15f * intensity, 2)
                    tone(music, (barStart + 2.5f) * beat, 1.2f * beat, rootFreq, 0.12f * intensity, 2)
                }
                globalBar++
            }

            if (melody.isNotEmpty()) {
                var sungBeats = 0f
                val sectionBeatsAvail = bars * 4f
                while (sungBeats < sectionBeatsAvail * 0.9f && lyricLines.isNotEmpty()) {
                    val line = lyricLines[lineIdx % lyricLines.size]
                    lineIdx++
                    val sung = singLine(vocals, (sectionStartBeat + sungBeats) * beat, (sectionBeatsAvail - sungBeats) * beat, melody, beat, line, 0.26f * intensity)
                    if (sung <= 0f) break
                    sungBeats += sung
                }
                var startBeat = 0f
                var noteIdx = 0
                val sectionBeats = bars * 4f
                while (startBeat < sectionBeats) {
                    val token = melody[noteIdx % melody.size]
                    val parts = token.split(":")
                    val freq = noteToFreq(parts[0].trim())
                    val dur = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0.25f, 8f) ?: 1f
                    if (freq != null) {
                        leadVoice(wet, startBeat * beat, dur * beat * 0.9f, freq, 0.13f * intensity, isRock)
                    }
                    startBeat += dur
                    noteIdx++
                }
            }
        }

        applyReverb(wet, n, if (isAmbient) 0.5f else 0.28f)
        val vocalWet = vocals.copyOf()
        applyReverb(vocalWet, n, 0.3f)
        for (i in 0 until n) {
            vocalWet[i] *= 0.4f
        }

        if (isHouse) {
            duckAgainstKick(music, drums, n, 0.6f)
        }

        val mix = FloatArray(n)
        for (i in 0 until n) {
            mix[i] = softClip(drums[i] + music[i] + wet[i] + vocals[i] + vocalWet[i])
        }

        var peak = 1e-6f
        for (s in mix) if (Math.abs(s) > peak) peak = Math.abs(s)
        val gain = 0.9f / peak

        val out = ByteArray(44 + n * 2)
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + n * 2)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(SR)
        header.putInt(SR * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(n * 2)
        System.arraycopy(header.array(), 0, out, 0, 44)
        val buf = java.nio.ByteBuffer.wrap(out, 44, out.size - 44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (s in mix) {
            val v = (s * gain).coerceIn(-1f, 1f)
            buf.putShort((v * Short.MAX_VALUE).toInt().toShort())
        }
        return out
    }

    private fun idx(timeSec: Float) = (timeSec * SR).toInt()

    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    private fun chordsToMelody(chords: List<String>): List<String> {
        val tokens = mutableListOf<String>()
        for (chord in chords) {
            val midis = parseChord(chord)
            for (m in midis) {
                val name = noteNames[m % 12] + (m / 12 - 1)
                tokens.add("$name:1")
            }
        }
        return if (tokens.isEmpty()) listOf("A4:2") else tokens
    }

    private val vowelFormants = mapOf(
        'a' to floatArrayOf(800f, 1150f, 2900f),
        'e' to floatArrayOf(400f, 2000f, 2550f),
        'i' to floatArrayOf(350f, 2100f, 2800f),
        'o' to floatArrayOf(450f, 800f, 2830f),
        'u' to floatArrayOf(325f, 700f, 2530f)
    )

    private fun syllabify(line: String): List<Pair<Char, Boolean>> {
        val out = mutableListOf<Pair<Char, Boolean>>()
        val words = line.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        for (w in words) {
            val clean = w.replace(Regex("[^a-z']"), "")
            val sylls = Regex("[^aeiouy]*[aeiouy]+[^aeiouy]*").findAll(clean).map { it.value }.toList()
            if (sylls.isEmpty()) continue
            sylls.forEachIndexed { i, s ->
                val vowel = s.first { it in "aeiouy" }.let { if (it == 'y') 'i' else it }
                val isWordEnd = i == sylls.size - 1
                out.add(vowel to isWordEnd)
            }
        }
        return out
    }

    private fun singLine(
        vocals: FloatArray,
        startTime: Float,
        sectionDur: Float,
        melody: List<String>,
        beat: Float,
        line: String,
        amp: Float
    ): Float {
        val sylls = syllabify(line)
        if (sylls.isEmpty()) return 0f
        var cursor = startTime
        var noteIdx = 0
        var prevFreq = 0f
        var lastBeats = 0f
        for ((syllIdx, syl) in sylls.withIndex()) {
            if (cursor >= startTime + sectionDur) break
            val token = melody[noteIdx % melody.size]
            val parts = token.split(":")
            val freq = noteToFreq(parts[0].trim()) ?: continue
            val beats = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0.25f, 4f) ?: 1f
            val (vowel, isWordEnd) = syl
            val dur = beats * beat * if (isWordEnd) 0.95f else 0.72f
            singSyllable(vocals, cursor, dur, freq, prevFreq, vowel, amp)
            prevFreq = freq
            cursor += dur
            lastBeats += beats
            noteIdx++
            if (syllIdx == sylls.size - 1) break
        }
        return lastBeats
    }

    private fun singSyllable(
        vocals: FloatArray,
        start: Float,
        dur: Float,
        freq: Float,
        prevFreq: Float,
        vowel: Char,
        amp: Float
    ) {
        val s0 = idx(start)
        val len = idx(dur)
        val formants = vowelFormants[vowel] ?: vowelFormants['a']!!
        val attack = idx(0.03f)
        val release = idx(0.06f)

        val y1 = FloatArray(3)
        val y2 = FloatArray(3)
        val coeffs = formants.map { f ->
            val bw = 45f + f * 0.02f
            val r = Math.exp((-Math.PI * bw / SR).toDouble()).toFloat()
            val theta = (2 * Math.PI * f / SR).toFloat()
            floatArrayOf(2f * r * Math.cos(theta.toDouble()).toFloat(), -r * r)
        }

        for (i in 0 until len) {
            val p = i + s0
            if (p >= vocals.size) break
            val t = i.toFloat() / SR
            val env = when {
                i < attack -> i.toFloat() / attack
                i > len - release -> (len - i).toFloat() / release
                else -> 1f
            } * (0.75f + 0.25f * Math.sin((2 * Math.PI * 5.0 * t).toDouble()).toFloat())
            val glide = if (prevFreq > 0f && i < idx(0.05f)) {
                prevFreq + (freq - prevFreq) * (i.toFloat() / idx(0.05f))
            } else freq
            val vibrato = Math.sin((2 * Math.PI * 5.6 * t).toDouble()).toFloat() * 0.006f
            val f0 = glide + vibrato
            var source = 0f
            for (h in 1..10) {
                source += Math.sin((2 * Math.PI * f0 * h * t).toDouble()).toFloat() / h
            }
            source *= 0.5f
            var v = 0f
            for (fc in 0 until 3) {
                val y = source + coeffs[fc][0] * y1[fc] + coeffs[fc][1] * y2[fc]
                y2[fc] = y1[fc]
                y1[fc] = y
                v += y * (if (fc == 0) 1f else if (fc == 1) 0.55f else 0.3f)
            }
            vocals[p] += softClip(v * 0.22f) * env * amp
        }
    }

    private fun tone(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float, harmonics: Int) {
        val s0 = idx(start)
        val len = idx(dur)
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val env = Math.min(1f, i / 300f) * Math.exp((-2.2 * t).toDouble()).toFloat()
            var v = 0f
            for (h in 1..harmonics) v += Math.sin(2 * Math.PI * freq * h * t).toFloat() / h
            mix[p] += v * env * amp
        }
    }

    private fun padVoice(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float) {
        val s0 = idx(start)
        val len = idx(dur)
        val attack = idx(0.1f)
        val release = idx(0.5f)
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val env = when {
                i < attack -> i.toFloat() / attack
                i > len - release -> (len - i).toFloat() / release
                else -> 1f
            }
            val v = Math.sin(2 * Math.PI * freq * t).toFloat() +
                0.35f * Math.sin(2 * Math.PI * freq * 1.004 * t).toFloat() +
                0.25f * Math.sin(2 * Math.PI * freq * 2 * t).toFloat()
            mix[p] += v * env * amp * 0.5f
        }
    }

    private fun stabVoice(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float) {
        val s0 = idx(start)
        val len = idx(dur)
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val env = Math.min(1f, i / 200f) * Math.exp((-6 * t).toDouble()).toFloat()
            mix[p] += supersaw(t, freq) * env * amp
        }
    }

    private fun powerChord(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float) {
        val s0 = idx(start)
        val len = idx(dur)
        val attack = idx(0.01f)
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val env = Math.min(1f, i.toFloat() / attack) *
                Math.min(1f, (len - i).toFloat() / idx(0.15f))
            val v = 0.6f * saw(t, freq) + 0.4f * square(t, freq * 0.5f)
            mix[p] += softClip(v * 2.2f) * env * amp
        }
    }

    private fun glide808(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float) {
        val s0 = idx(start)
        val len = idx(dur)
        var phase = 0f
        var curFreq = freq * 0.7f
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            curFreq += (freq - curFreq) * 0.00012f
            phase += 2f * Math.PI.toFloat() * curFreq / SR
            val t = i.toFloat() / SR
            val env = Math.min(1f, i / 200f) * Math.exp((-1.2 * t).toDouble()).toFloat()
            val v = Math.sin(phase.toDouble()).toFloat() * 0.8f + softClip(Math.sin(phase.toDouble()).toFloat() * 2f) * 0.4f
            mix[p] += v * env * amp
        }
    }

    private fun pianoTone(mix: FloatArray, start: Float, freq: Float, amp: Float) {
        val s0 = idx(start)
        val len = idx(0.9f)
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val env = Math.min(1f, i / 80f) * Math.exp((-3.5 * t).toDouble()).toFloat()
            val v = Math.sin(2 * Math.PI * freq * t).toFloat() +
                0.5f * Math.sin(4 * Math.PI * freq * t).toFloat() +
                0.2f * Math.sin(6 * Math.PI * freq * t).toFloat()
            mix[p] += v * env * amp
        }
    }

    private fun leadVoice(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float, isRock: Boolean) {
        val s0 = idx(start)
        val len = idx(dur)
        val attack = idx(0.02f)
        val release = idx(0.1f)
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val env = when {
                i < attack -> i.toFloat() / attack
                i > len - release -> (len - i).toFloat() / release
                else -> 1f
            } * Math.exp((-0.7 * t).toDouble()).toFloat()
            val vibrato = Math.sin(2 * Math.PI * 5.5 * t).toFloat() * 0.004f
            val v = if (isRock) {
                0.7f * saw(t, freq + vibrato) + 0.3f * square(t, freq + vibrato)
            } else {
                Math.sin(2 * Math.PI * (freq + vibrato) * t).toFloat() +
                    0.35f * Math.sin(4 * Math.PI * freq * t).toFloat()
            }
            mix[p] += v * env * amp
        }
    }

    private fun hitKick(mix: FloatArray, start: Float, amp: Float) {
        val s0 = idx(start)
        val len = idx(0.25f)
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val f = 120f * Math.exp((-20 * t).toDouble()).toFloat() + 43f
            val env = Math.exp((-8 * t).toDouble()).toFloat()
            val click = if (i < 60) (Math.random() * 2 - 1).toFloat() * 0.3f * (1f - i / 60f) else 0f
            mix[p] += (Math.sin(2 * Math.PI * f * t).toFloat() + click) * env * amp
        }
    }

    private fun hitSnare(mix: FloatArray, start: Float, amp: Float, cutoff: Float) {
        val s0 = idx(start)
        val len = idx(0.18f)
        var lp = 0f
        val alpha = (2 * Math.PI * cutoff / SR).toFloat()
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val white = (Math.random() * 2 - 1).toFloat()
            lp += alpha * (white - lp)
            val env = Math.exp((-18 * t).toDouble()).toFloat()
            val body = Math.sin(2 * Math.PI * 190 * t).toFloat() * 0.4f
            mix[p] += (lp + body) * env * amp
        }
    }

    private fun hitHat(mix: FloatArray, start: Float, dur: Float, amp: Float, speed: Float) {
        val s0 = idx(start)
        val len = (idx(dur) / speed).toInt().coerceAtLeast(40)
        var hp = 0f
        var lp = 0f
        val alpha = (2 * Math.PI * 9000 / SR).toFloat()
        for (i in 0 until len) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SR
            val white = (Math.random() * 2 - 1).toFloat()
            lp += alpha * (white - lp)
            hp = lp - hp
            val env = Math.exp((-70 * t).toDouble()).toFloat()
            mix[p] += hp * env * amp
        }
    }

    private fun duckAgainstKick(music: FloatArray, drums: FloatArray, n: Int, depth: Float) {
        var i = 0
        while (i < n) {
            if (drums[i] > 0.25f) {
                var j = i
                while (j < n && j < i + idx(0.18f)) {
                    val duck = 1f - depth * (1f - (j - i).toFloat() / idx(0.18f))
                    music[j] *= duck
                    j++
                }
                i += idx(0.2f)
            } else {
                i++
            }
        }
    }

    private fun applyReverb(wet: FloatArray, n: Int, amount: Float) {
        val combDelays = intArrayOf(1111, 1187, 1277, 1357)
        val combFeedback = 0.78f
        val allpassDelays = intArrayOf(225, 556)
        val allpassFeedback = 0.7f
        val combs = Array(4) { FloatArray(combDelays[it]) }
        val combIdx = IntArray(4)
        val allpass = Array(2) { FloatArray(allpassDelays[it]) }
        val apIdx = IntArray(2)

        for (i in 0 until n) {
            var revOut = 0f
            for (c in 0 until 4) {
                val buf = combs[c]
                val ci = combIdx[c]
                val delayed = buf[ci]
                val filtered = delayed * 0.55f + delayed * 0.45f * 0.5f
                buf[ci] = wet[i] + filtered * combFeedback
                combIdx[c] = (ci + 1) % buf.size
                revOut += delayed
            }
            var apOut = revOut / 4f
            for (a in 0 until 2) {
                val buf = allpass[a]
                val ai = apIdx[a]
                val delayed = buf[ai]
                val input = apOut
                buf[ai] = input + delayed * allpassFeedback
                apIdx[a] = (ai + 1) % buf.size
                apOut = delayed - input * allpassFeedback
            }
            wet[i] = wet[i] + apOut * amount
        }
    }
}
