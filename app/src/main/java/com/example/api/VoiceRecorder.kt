package com.example.api

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.io.File

class VoiceRecorder(private val outputFile: File) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var isRecording = false

    val sampleRate = 16000

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return false
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 4
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return false
        }

        isRecording = true
        audioRecord?.startRecording()

        val pcmData = ByteArrayOutputStream()
        recordingThread = Thread {
            val buffer = ShortArray(4096)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    for (i in 0 until read) {
                        pcmData.write((buffer[i].toInt() and 0xFF))
                        pcmData.write((buffer[i].toInt() shr 8) and 0xFF)
                    }
                }
            }
            writeWavFile(pcmData.toByteArray())
        }.apply { start() }

        return true
    }

    fun stop(onSaved: (File) -> Unit) {
        isRecording = false
        recordingThread?.join()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread?.let { onSaved(outputFile) }
    }

    private fun writeWavFile(pcm: ByteArray) {
        val totalDataLen = pcm.size + 36
        val byteRate = sampleRate * 2

        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(pcm.size)

        outputFile.outputStream().use { out ->
            out.write(header.array())
            out.write(pcm)
        }
    }
}
