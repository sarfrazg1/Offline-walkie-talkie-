package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.sqrt

/**
 * Low-latency AudioTrack player for incoming walkie-talkie voice streams.
 * Prevents local acoustic echo by supporting instant mute when transmitting.
 */
class AudioPlayer {
    private val TAG = "AudioPlayer"
    
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _speakerLevel = MutableStateFlow(0f)
    val speakerLevel: StateFlow<Float> = _speakerLevel.asStateFlow()

    fun playStream(
        inputStream: InputStream,
        sampleRate: Int = 16000,
        onFinished: () -> Unit
    ) {
        stopPlayback()
        
        playbackJob = scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()
                _isPlaying.value = true

                val byteBuffer = ByteArray(bufferSize)
                
                while (isActive && _isPlaying.value) {
                    val bytesRead = inputStream.read(byteBuffer)
                    if (bytesRead == -1) break
                    
                    if (bytesRead > 0) {
                        // Calculate RMS level for visualizer
                        var sumSquares = 0.0
                        val shortsCount = bytesRead / 2
                        for (i in 0 until shortsCount) {
                            val low = byteBuffer[i * 2].toInt() and 0xFF
                            val high = byteBuffer[i * 2 + 1].toInt()
                            val sample = (high shl 8) or low
                            sumSquares += sample * sample
                        }
                        if (shortsCount > 0) {
                            val rms = sqrt(sumSquares / shortsCount)
                            _speakerLevel.value = (rms / 12000.0).toFloat().coerceIn(0f, 1f)
                        }

                        audioTrack?.write(byteBuffer, 0, bytesRead)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Playback error: ${e.message}")
            } finally {
                _isPlaying.value = false
                _speakerLevel.value = 0f
                cleanupTrack()
                try {
                    inputStream.close()
                } catch (ignored: Exception) {}
                onFinished()
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        _speakerLevel.value = 0f
        playbackJob?.cancel()
        playbackJob = null
        cleanupTrack()
    }

    private fun cleanupTrack() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        } catch (ignored: Exception) {}
        try {
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
    }
}
