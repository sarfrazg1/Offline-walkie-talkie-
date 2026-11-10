package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import com.example.model.AudioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-performance, low-latency AudioRecord microphone capture engine.
 * Streams 16-bit PCM mono audio into an active stream while computing live visualizer amplitude.
 */
class AudioRecorder {
    private val TAG = "AudioRecorder"
    
    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var _isMuted: Boolean = false
    val isMuted: Boolean get() = _isMuted

    fun setMuted(muted: Boolean) {
        _isMuted = muted
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        config: AudioConfig,
        outputStream: OutputStream,
        onStopped: () -> Unit
    ): Boolean {
        if (_isRecording.value) return true

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                config.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $minBufferSize")
                return false
            }

            // 2x buffer size for stable streaming without drops
            val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                config.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            val audioSessionId = audioRecord?.audioSessionId ?: 0
            if (audioSessionId != 0) {
                if (config.enableEchoCancellation && AcousticEchoCanceler.isAvailable()) {
                    try {
                        echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                            enabled = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "AEC enable failed: ${e.message}")
                    }
                }
                if (config.enableNoiseSuppression && NoiseSuppressor.isAvailable()) {
                    try {
                        noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                            enabled = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "NoiseSuppressor enable failed: ${e.message}")
                    }
                }
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch {
                val pcmBuffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)
                
                try {
                    while (isActive && _isRecording.value) {
                        val shortsRead = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: -1
                        if (shortsRead > 0) {
                            if (isMuted) {
                                java.util.Arrays.fill(byteBuffer, 0, shortsRead * 2, 0.toByte())
                                _micLevel.value = 0f
                            } else {
                                var sumSquares = 0.0
                                val gain = config.micGainMultiplier
                                
                                for (i in 0 until shortsRead) {
                                    var sample = (pcmBuffer[i] * gain).toInt()
                                    sample = sample.coerceIn(-32768, 32767)
                                    val shortSample = sample.toShort()
                                    
                                    byteBuffer[i * 2] = (shortSample.toInt() and 0xFF).toByte()
                                    byteBuffer[i * 2 + 1] = ((shortSample.toInt() shr 8) and 0xFF).toByte()
                                    
                                    sumSquares += shortSample * shortSample
                                }
                                
                                // Calculate RMS amplitude normalized roughly 0.0 to 1.0
                                val rms = sqrt(sumSquares / shortsRead)
                                val normalized = (rms / 12000.0).toFloat().coerceIn(0f, 1f)
                                _micLevel.value = normalized
                            }
                            
                            outputStream.write(byteBuffer, 0, shortsRead * 2)
                            outputStream.flush()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Recording loop error: ${e.message}")
                } finally {
                    try {
                        outputStream.close()
                    } catch (ignored: Exception) {}
                    _micLevel.value = 0f
                    onStopped()
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            stopRecording()
            return false
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            audioRecord?.stop()
        } catch (ignored: Exception) {}
        try {
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null

        try {
            echoCanceler?.release()
        } catch (ignored: Exception) {}
        echoCanceler = null

        try {
            noiseSuppressor?.release()
        } catch (ignored: Exception) {}
        noiseSuppressor = null

        _micLevel.value = 0f
    }
}
