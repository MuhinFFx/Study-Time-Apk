package com.example.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class AmbientSoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null

    fun startAmbient(sound: AmbientSound) {
        stopAmbient()
        if (sound == AmbientSound.NONE) return

        playJob = scope.launch {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                return@launch
            }

            audioTrack = track
            try {
                track.play()
                val buffer = ShortArray(2048)
                var lastOut = 0.0
                var phase = 0.0
                while (isActive) {
                    for (i in buffer.indices) {
                        val white = Random.nextDouble(-1.0, 1.0)
                        val sample = when (sound) {
                            AmbientSound.RAIN -> {
                                lastOut = (lastOut + 0.04 * white) / 1.04
                                (lastOut * 1.4 + white * 0.08) * 0.28
                            }
                            AmbientSound.FOREST -> {
                                phase += 0.00025
                                val swell = 0.4 + 0.6 * ((sin(phase) + 1.0) * 0.5)
                                lastOut = (lastOut + 0.025 * white) / 1.025
                                (lastOut * 2.2 * swell) * 0.22
                            }
                            AmbientSound.BROWN_NOISE -> {
                                lastOut = (lastOut + 0.018 * white) / 1.018
                                lastOut * 1.8 * 0.25
                            }
                            AmbientSound.NONE -> 0.0
                        }
                        buffer[i] = (sample.coerceIn(-0.8, 0.8) * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
                // Ignore audio hardware stop errors
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stopAmbient() {
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }

    fun playCompletionChime() {
        scope.launch {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 350)
                kotlinx.coroutines.delay(450)
                tone.release()
            } catch (_: Exception) {
            }
        }
    }
}
