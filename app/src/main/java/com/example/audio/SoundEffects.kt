package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Procedural walkie-talkie radio and offline phone call sound effect generator.
 * Produces crisp, authentic radio chirps, roger beeps, telephone ringtones, ringback cadences,
 * and call-end tones in-memory using pure PCM sine wave synthesis.
 */
object SoundEffects {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    private var ringtoneJob: Job? = null
    private var ringbackJob: Job? = null

    /**
     * Plays a high-pitched tactical mic click when PTT is pressed.
     */
    fun playPttStartChime() {
        scope.launch {
            val buffer1 = generateTone(1400.0, 35, 0.45f)
            val buffer2 = generateTone(1900.0, 35, 0.45f)
            playPcm(buffer1 + buffer2)
        }
    }

    /**
     * Classic walkie-talkie "Roger Beep" with a tail squelch tone on PTT release.
     */
    fun playRogerBeep() {
        scope.launch {
            val tone1 = generateTone(1100.0, 70, 0.5f)
            val silence = ByteArray((SAMPLE_RATE * 0.02).toInt() * 2)
            val tone2 = generateTone(1500.0, 90, 0.55f)
            val squelch = generateNoise(45, 0.15f)
            playPcm(tone1 + silence + tone2 + squelch)
        }
    }

    /**
     * Warning chirp when user tries to transmit while channel is occupied.
     */
    fun playBusyTone() {
        scope.launch {
            val b1 = generateTone(480.0, 60, 0.6f)
            val silence = ByteArray((SAMPLE_RATE * 0.04).toInt() * 2)
            val b2 = generateTone(480.0, 60, 0.6f)
            playPcm(b1 + silence + b2)
        }
    }

    /**
     * Uplifting chime when a new peer connects.
     */
    fun playConnectedChime() {
        scope.launch {
            val t1 = generateTone(784.0, 60, 0.4f)
            val t2 = generateTone(1046.5, 90, 0.5f)
            playPcm(t1 + t2)
        }
    }

    /**
     * Continuous incoming call telephone ringtone loop (dual tone 853Hz + 960Hz).
     */
    fun startRingtone() {
        stopRingtone()
        ringtoneJob = scope.launch {
            val ringPart1 = generateDualTone(853.0, 960.0, 800, 0.6f)
            val gap = ByteArray((SAMPLE_RATE * 0.2).toInt() * 2)
            val ringPart2 = generateDualTone(853.0, 960.0, 800, 0.6f)
            val fullBurst = ringPart1 + gap + ringPart2

            while (isActive) {
                playPcm(fullBurst)
                delay(2200)
            }
        }
    }

    fun stopRingtone() {
        ringtoneJob?.cancel()
        ringtoneJob = null
    }

    /**
     * Caller ringback tone loop (audible 440Hz + 480Hz US/Euro standard ringback).
     */
    fun startRingback() {
        stopRingback()
        ringbackJob = scope.launch {
            val burst = generateDualTone(440.0, 480.0, 1500, 0.35f)
            while (isActive) {
                playPcm(burst)
                delay(3000)
            }
        }
    }

    fun stopRingback() {
        ringbackJob?.cancel()
        ringbackJob = null
    }

    /**
     * 3 descending beeps played when a call ends or is rejected.
     */
    fun playCallEndTone() {
        scope.launch {
            val tone1 = generateTone(750.0, 80, 0.5f)
            val silence = ByteArray((SAMPLE_RATE * 0.05).toInt() * 2)
            val tone2 = generateTone(550.0, 80, 0.5f)
            val tone3 = generateTone(350.0, 140, 0.5f)
            playPcm(tone1 + silence + tone2 + silence + tone3)
        }
    }

    private fun generateTone(frequencyHz: Double, durationMs: Int, volume: Float): ByteArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ByteArray(numSamples * 2)
        val angularFreq = 2.0 * Math.PI * frequencyHz / SAMPLE_RATE

        for (i in 0 until numSamples) {
            val envelope = when {
                i < numSamples * 0.15 -> i / (numSamples * 0.15)
                i > numSamples * 0.85 -> (numSamples - i) / (numSamples * 0.15)
                else -> 1.0
            }
            val sample = (sin(i * angularFreq) * 32767.0 * volume * envelope).toInt()
            val clamped = sample.coerceIn(-32768, 32767).toShort()
            buffer[2 * i] = (clamped.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateDualTone(freq1Hz: Double, freq2Hz: Double, durationMs: Int, volume: Float): ByteArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ByteArray(numSamples * 2)
        val ang1 = 2.0 * Math.PI * freq1Hz / SAMPLE_RATE
        val ang2 = 2.0 * Math.PI * freq2Hz / SAMPLE_RATE

        for (i in 0 until numSamples) {
            val envelope = when {
                i < numSamples * 0.1 -> i / (numSamples * 0.1)
                i > numSamples * 0.9 -> (numSamples - i) / (numSamples * 0.1)
                else -> 1.0
            }
            val s1 = sin(i * ang1)
            val s2 = sin(i * ang2)
            val sample = ((s1 + s2) * 0.5 * 32767.0 * volume * envelope).toInt()
            val clamped = sample.coerceIn(-32768, 32767).toShort()
            buffer[2 * i] = (clamped.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateNoise(durationMs: Int, volume: Float): ByteArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ByteArray(numSamples * 2)
        val random = java.util.Random()

        for (i in 0 until numSamples) {
            val decay = 1.0 - (i.toDouble() / numSamples)
            val sample = ((random.nextFloat() * 2 - 1) * 32767.0 * volume * decay).toInt()
            val clamped = sample.coerceIn(-32768, 32767).toShort()
            buffer[2 * i] = (clamped.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun playPcm(pcmData: ByteArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track.play()
            scope.launch {
                val delayMs = ((pcmData.size / 2.0 / SAMPLE_RATE) * 1000).toLong() + 50
                kotlinx.coroutines.delay(delayMs)
                try {
                    track.stop()
                    track.release()
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            // AudioTrack error handling
        }
    }
}
