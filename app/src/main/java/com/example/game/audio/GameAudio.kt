package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class GameAudio(private val context: Context) {
  private val scope = CoroutineScope(Dispatchers.Default)
  var soundEnabled: Boolean = true
  var vibrationEnabled: Boolean = true

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun playBloop() {
    vibrate(18)
    if (!soundEnabled) return

    scope.launch {
      try {
        val sampleRate = 22050
        val durationMs = 120
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val startFreq = 320.0
        val endFreq = 720.0

        for (i in 0 until numSamples) {
          val t = i.toDouble() / sampleRate
          val progress = i.toDouble() / numSamples
          val currentFreq = startFreq + (endFreq - startFreq) * progress
          val envelope = sin(progress * PI)
          val sample = sin(2.0 * PI * currentFreq * t) * envelope
          buffer[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
        }

        playBuffer(buffer, sampleRate)
      } catch (_: Exception) {
        // Audio synthesis fallback
      }
    }
  }

  fun playScore() {
    vibrate(10)
    if (!soundEnabled) return

    scope.launch {
      try {
        val sampleRate = 22050
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val freq1 = 880.0
        val freq2 = 1320.0

        for (i in 0 until numSamples) {
          val t = i.toDouble() / sampleRate
          val progress = i.toDouble() / numSamples
          val envelope = exp(-progress * 4.0)
          val sample = (0.6 * sin(2.0 * PI * freq1 * t) + 0.4 * sin(2.0 * PI * freq2 * t)) * envelope
          buffer[i] = (sample * 18000).toInt().coerceIn(-32767, 32767).toShort()
        }

        playBuffer(buffer, sampleRate)
      } catch (_: Exception) {
        // Audio synthesis fallback
      }
    }
  }

  fun playSplash() {
    vibrate(60)
    if (!soundEnabled) return

    scope.launch {
      try {
        val sampleRate = 22050
        val durationMs = 220
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val startFreq = 180.0
        val endFreq = 45.0

        for (i in 0 until numSamples) {
          val t = i.toDouble() / sampleRate
          val progress = i.toDouble() / numSamples
          val currentFreq = startFreq + (endFreq - startFreq) * progress
          val envelope = exp(-progress * 3.5)
          val noise = (Math.random() * 2.0 - 1.0) * 0.25
          val sample = (sin(2.0 * PI * currentFreq * t) * 0.75 + noise) * envelope
          buffer[i] = (sample * 20000).toInt().coerceIn(-32767, 32767).toShort()
        }

        playBuffer(buffer, sampleRate)
      } catch (_: Exception) {
        // Audio synthesis fallback
      }
    }
  }

  private fun playBuffer(buffer: ShortArray, sampleRate: Int) {
    try {
      val minBufSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(maxOf(buffer.size * 2, minBufSize))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(buffer, 0, buffer.size)
      track.play()

      scope.launch {
        kotlinx.coroutines.delay(350)
        try {
          track.stop()
          track.release()
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
  }

  private fun vibrate(durationMs: Long) {
    if (!vibrationEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs)
      }
    } catch (_: Exception) {}
  }
}
