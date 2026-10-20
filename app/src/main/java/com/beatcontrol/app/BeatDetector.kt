
package com.beatcontrol.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Определяет примерный темп музыкального файла.
 *
 * Анализирует до 45 секунд аудио, преобразуя звук
 * в монофонический энергетический сигнал и проверяя
 * возможные темпы от 60 до 200 BPM.
 *
 * Возвращает BPM или null, если темп определить не удалось.
 */
object BeatDetector {

    private const val ANALYSIS_SECONDS = 45
    private const val ENVELOPE_RATE = 100
    private const val MIN_BPM = 60
    private const val MAX_BPM = 200

    fun detectBpm(context: Context, uri: Uri): Int? {
        val envelope = decodeEnvelope(context, uri) ?: return null

        if (envelope.size < ENVELOPE_RATE * 8) {
            return null
        }

        val onset = buildOnsetEnvelope(envelope)

        if (onset.size < ENVELOPE_RATE * 8) {
            return null
        }

        return estimateBpm(onset)
    }

    private fun decodeEnvelope(
        context: Context,
        uri: Uri
    ): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)

            var audioTrack = -1
            var inputFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrack < 0 || inputFormat == null) {
                return null
            }

            extractor.selectTrack(audioTrack)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: return null

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val envelope = ArrayList<Float>()
            val info = MediaCodec.BufferInfo()

            var inputDone = false
            var outputDone = false

            var outputSampleRate = inputFormat.getIntegerOrDefault(
                MediaFormat.KEY_SAMPLE_RATE,
                44100
            )

            var outputChannels = inputFormat.getIntegerOrDefault(
                MediaFormat.KEY_CHANNEL_COUNT,
                2
            ).coerceAtLeast(1)

            var pcmEncoding = 2 // PCM 16-bit

            var sampleCount = 0L
            var envelopeSum = 0.0
            var envelopeCount = 0
            var nextEnvelopeSamples = max(
                1,
                outputSampleRate / ENVELOPE_RATE
            )

            val maxSamples =
                outputSampleRate.toLong() * ANALYSIS_SECONDS

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)

                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)

                        if (inputBuffer == null) {
                            inputDone = true
                        } else {
                            val size = extractor.readSampleData(inputBuffer, 0)

                            if (size < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val timeUs = extractor.sampleTime.coerceAtLeast(0L)

                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    size,
                                    timeUs,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                when (val outputIndex =
                    codec.dequeueOutputBuffer(info, 10_000)) {

                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // Ждём следующего буфера декодера.
                    }

                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = codec.outputFormat

                        outputSampleRate =
                            format.getIntegerOrDefault(
                                MediaFormat.KEY_SAMPLE_RATE,
                                outputSampleRate
                            )

                        outputChannels =
                            format.getIntegerOrDefault(
                                MediaFormat.KEY_CHANNEL_COUNT,
                                outputChannels
                            ).coerceAtLeast(1)

                        pcmEncoding =
                            format.getIntegerOrDefault(
                                MediaFormat.KEY_PCM_ENCODING,
                                2
                            )

                        nextEnvelopeSamples = max(
                            1,
                            outputSampleRate / ENVELOPE_RATE
                        )
                    }

                    else -> {
                        if (outputIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)

                            if (outputBuffer != null && info.size > 0) {
                                outputBuffer.position(info.offset)
                                outputBuffer.limit(info.offset + info.size)

                                val pcm = outputBuffer.slice().order(
                                    ByteOrder.LITTLE_ENDIAN
                                )

                                val bytesPerSample =
                                    if (pcmEncoding == 4) 4 else 2

                                val frameBytes =
                                    bytesPerSample * outputChannels

                                while (
                                    pcm.remaining() >= frameBytes &&
                                    sampleCount < maxSamples
                                ) {
                                    var mono = 0.0

                                    repeat(outputChannels) {
                                        val value = if (bytesPerSample == 4) {
                                            pcm.float.toDouble()
                                        } else {
                                            pcm.short.toDouble() / 32768.0
                                        }

                                        mono += abs(value)
                                    }

                                    mono /= outputChannels

                                    envelopeSum += mono
                                    envelopeCount++
                                    sampleCount += 1L

                                    if (envelopeCount >= nextEnvelopeSamples) {
                                        envelope.add(
                                            (envelopeSum / envelopeCount)
                                                .toFloat()
                                        )
                                        envelopeSum = 0.0
                                        envelopeCount = 0
                                    }
                                }
                            }

                            val endOfStream =
                                info.flags and
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0

                            codec.releaseOutputBuffer(outputIndex, false)

                            if (endOfStream || sampleCount >= maxSamples) {
                                outputDone = true
                            }
                        }
                    }
                }
            }

            if (envelopeCount > 0) {
                envelope.add(
                    (envelopeSum / envelopeCount).toFloat()
                )
            }

            return envelope.toFloatArray()
        } catch (_: Exception) {
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun buildOnsetEnvelope(audio: FloatArray): FloatArray {
        if (audio.size < 3) return FloatArray(0)

        val result = FloatArray(audio.size)
        var baseline = 0f

        for (i in 1 until audio.size) {
            val current = audio[i]
            val previous = audio[i - 1]

            // Положительный скачок энергии может указывать на удар.
            val rise = max(0f, current - previous)

            // Медленно меняющийся фон убирается, чтобы усилить атаки.
            baseline = baseline * 0.96f + current * 0.04f

            result[i] = max(0f, rise + (current - baseline) * 0.15f)
        }

        val mean = result.average().toFloat()

        for (i in result.indices) {
            result[i] = max(0f, result[i] - mean)
        }

        return result
    }

    private fun estimateBpm(signal: FloatArray): Int? {
        val mean = signal.average().toFloat()
        val centered = FloatArray(signal.size) { signal[it] - mean }

        val energy = centered.sumOf { it.toDouble() * it.toDouble() }

        if (energy < 1e-8) return null

        var bestBpm = -1
        var bestScore = Double.NEGATIVE_INFINITY
        var secondBest = Double.NEGATIVE_INFINITY

        for (bpm in MIN_BPM..MAX_BPM) {
            val lag = (ENVELOPE_RATE * 60.0 / bpm).toInt()

            if (lag < 1 || lag >= centered.size / 2) continue

            var correlation = 0.0
            var leftEnergy = 0.0
            var rightEnergy = 0.0

            for (i in lag until centered.size) {
                val a = centered[i].toDouble()
                val b = centered[i - lag].toDouble()

                correlation += a * b
                leftEnergy += a * a
                rightEnergy += b * b
            }

            val denominator = sqrt(leftEnergy * rightEnergy)

            if (denominator <= 1e-12) continue

            var score = correlation / denominator

            // Мягкое предпочтение распространённым музыкальным темпам.
            if (bpm in 80..160) score *= 1.04

            if (score > bestScore) {
                secondBest = bestScore
                bestScore = score
                bestBpm = bpm
            } else if (score > secondBest) {
                secondBest = score
            }
        }

        if (bestBpm !in MIN_BPM..MAX_BPM || bestScore < 0.08) {
            return null
        }

        // Возвращаем ближайшее целое значение BPM.
        return bestBpm
    }

    private fun MediaFormat.getIntegerOrDefault(
        key: String,
        defaultValue: Int
    ): Int {
        return try {
            if (containsKey(key)) getInteger(key) else defaultValue
        } catch (_: Exception) {
            defaultValue
        }
    }
}
