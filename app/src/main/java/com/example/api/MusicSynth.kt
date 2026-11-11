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
    val sections: List<SongSection>
)

object MusicSynth {
    private const val SAMPLE_RATE = 44100

    private val NOTE_BASE = mapOf(
        'C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11
    )

    fun noteToFreq(note: String): Float? {
        if (note.length < 2) return null
        val base = NOTE_BASE[note[0]] ?: return null
        var i = 1
        var semitone = base
        if (note[i] == '#') { semitone += 1; i++ } else if (note[i] == 'b') { semitone -= 1; i++ }
        val octave = note.substring(i).toIntOrNull() ?: return null
        val midi = (octave + 1) * 12 + semitone
        return (440f * Math.pow(2.0, (midi - 69) / 12.0)).toFloat()
    }

    private fun parseChord(chord: String): List<Float> {
        val trimmed = chord.trim()
        if (trimmed.isEmpty()) return emptyList()
        val root = trimmed[0]
        var idx = 1
        var semitone = NOTE_BASE[root] ?: return emptyList()
        if (idx < trimmed.length && (trimmed[idx] == '#' || trimmed[idx] == 'b')) {
            semitone += if (trimmed[idx] == '#') 1 else -1
            idx++
        }
        val isMinor = trimmed.substring(idx).contains("m") && !trimmed.substring(idx).contains("maj")
        val third = if (isMinor) semitone + 3 else semitone + 4
        val fifth = semitone + 7
        fun freq(m: Int, octaveShift: Int): Float {
            val midi = m + 60 + octaveShift * 12
            return (440f * Math.pow(2.0, (midi - 69) / 12.0)).toFloat()
        }
        return listOf(freq(semitone, 0), freq(third, 0), freq(fifth, 0))
    }

    fun render(spec: SongSpec): ByteArray {
        val beatDur = 60f / spec.bpm.coerceIn(50, 180)
        val totalBars = spec.sections.sumOf { it.bars.coerceIn(1, 16) }
        val totalBeats = totalBars * 4
        val totalSamples = (totalBeats * beatDur * SAMPLE_RATE).toInt() + SAMPLE_RATE
        val mix = FloatArray(totalSamples)

        val relaxed = spec.style.contains("chill") || spec.style.contains("lofi") ||
            spec.style.contains("ambient") || spec.style.contains("ballad")

        var globalBar = 0
        for (section in spec.sections) {
            val bars = section.bars.coerceIn(1, 16)
            val chords = if (section.chords.isEmpty()) listOf("Am") else section.chords
            val melody = section.melody.filter { it.isNotBlank() }

            for (b in 0 until bars) {
                val barStartBeat = globalBar * 4f
                val triad = parseChord(chords[b % chords.size])

                triad.forEach { freq ->
                    addPad(mix, barStartBeat * beatDur, 4f * beatDur, freq, 0.055f)
                }

                val root = triad.firstOrNull()
                if (root != null) {
                    val bassFreq = root / 4f
                    addTone(mix, barStartBeat * beatDur, 1.5f * beatDur, bassFreq, 0.16f, 2)
                    addTone(mix, (barStartBeat + 2.5f) * beatDur, 1.0f * beatDur, bassFreq, 0.13f, 2)
                }

                addKick(mix, barStartBeat * beatDur, 0.4f)
                addKick(mix, (barStartBeat + 2f) * beatDur, 0.4f)
                addNoise(mix, (barStartBeat + 1f) * beatDur, 0.16f, 0.10f, 1400f)
                addNoise(mix, (barStartBeat + 3f) * beatDur, 0.16f, 0.10f, 1400f)
                val hatStep = if (relaxed) 1f else 0.5f
                var hb = 0f
                while (hb < 4f) {
                    addNoise(mix, (barStartBeat + hb) * beatDur, 0.05f, 0.05f, 8000f)
                    hb += hatStep
                }
                globalBar++
            }

            if (melody.isNotEmpty()) {
                var startBeat = 0f
                var noteIdx = 0
                val sectionBeats = bars * 4f
                while (startBeat < sectionBeats) {
                    val token = melody[noteIdx % melody.size]
                    val parts = token.split(":")
                    val freq = noteToFreq(parts[0].trim())
                    val dur = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0.25f, 8f) ?: 1f
                    if (freq != null) {
                        addLead(mix, startBeat * beatDur, dur * beatDur * 0.9f, freq, 0.11f)
                    }
                    startBeat += dur
                    noteIdx++
                }
            }
        }

        var peak = 1e-6f
        for (s in mix) if (Math.abs(s) > peak) peak = Math.abs(s)
        val gain = 0.88f / peak
        val out = ByteArray(44 + mix.size * 2)
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + mix.size * 2)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(SAMPLE_RATE)
        header.putInt(SAMPLE_RATE * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(mix.size * 2)
        System.arraycopy(header.array(), 0, out, 0, 44)
        val buf = java.nio.ByteBuffer.wrap(out, 44, out.size - 44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (s in mix) {
            val v = (s * gain).coerceIn(-1f, 1f)
            buf.putShort((v * Short.MAX_VALUE).toInt().toShort())
        }
        return out
    }

    private fun idx(timeSec: Float) = (timeSec * SAMPLE_RATE).toInt()

    private fun addTone(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float, harmonics: Int) {
        val s0 = idx(start)
        val n = idx(dur)
        for (i in 0 until n) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SAMPLE_RATE
            val env = Math.min(1f, i / 400f) * Math.exp(-t * 2.5)
            var v = 0f
            for (h in 1..harmonics) v += Math.sin(2 * Math.PI * freq * h * t).toFloat() / h
            mix[p] += v * env * amp
        }
    }

    private fun addPad(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float) {
        val s0 = idx(start)
        val n = idx(dur)
        val attack = idx(0.08f)
        val release = idx(0.35f)
        for (i in 0 until n) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SAMPLE_RATE
            val env = when {
                i < attack -> i.toFloat() / attack
                i > n - release -> (n - i).toFloat() / release
                else -> 1f
            }
            val detune = Math.sin(2 * Math.PI * freq * 1.003 * t).toFloat() * 0.4f
            val v = Math.sin(2 * Math.PI * freq * t).toFloat() + detune
            mix[p] += v * env * amp
        }
    }

    private fun addLead(mix: FloatArray, start: Float, dur: Float, freq: Float, amp: Float) {
        val s0 = idx(start)
        val n = idx(dur)
        val attack = idx(0.02f)
        val release = idx(0.08f)
        for (i in 0 until n) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SAMPLE_RATE
            val env = when {
                i < attack -> i.toFloat() / attack
                i > n - release -> (n - i).toFloat() / release
                else -> 1f
            } * Math.exp(-t * 0.8).toFloat()
            val vibrato = Math.sin(2 * Math.PI * 5.5 * t).toFloat() * 0.004f
            val v = Math.sin(2 * Math.PI * (freq + vibrato) * t).toFloat() +
                0.4f * Math.sin(4 * Math.PI * freq * t).toFloat()
            mix[p] += v * env * amp
        }
    }

    private fun addKick(mix: FloatArray, start: Float, amp: Float) {
        val s0 = idx(start)
        val n = idx(0.22f)
        for (i in 0 until n) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SAMPLE_RATE
            val f = 110f * Math.exp(-t * 22).toFloat() + 42f
            val env = Math.exp(-t * 9).toFloat()
            mix[p] += Math.sin(2 * Math.PI * f * t).toFloat() * env * amp
        }
    }

    private fun addNoise(mix: FloatArray, start: Float, dur: Float, amp: Float, cutoff: Float) {
        val s0 = idx(start)
        val n = idx(dur)
        var lp = 0f
        val alpha = (2 * Math.PI * cutoff / SAMPLE_RATE).toFloat()
        for (i in 0 until n) {
            val p = i + s0
            if (p >= mix.size) break
            val t = i.toFloat() / SAMPLE_RATE
            val white = (Math.random() * 2 - 1).toFloat()
            lp += alpha * (white - lp)
            val env = Math.exp(-t * (if (cutoff > 4000f) 60 else 22)).toFloat()
            mix[p] += lp * env * amp
        }
    }
}
