package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SampleVideoProvider {

    /**
     * Generates a small 3-second 640x360 H.264 test video in the cache directory
     * so users on emulators can test Convert264 immediately even if no local files exist.
     */
    suspend fun getOrCreateSampleVideo(context: Context): Uri? = withContext(Dispatchers.IO) {
        val sampleFile = File(context.cacheDir, "sample_clip.mp4")
        if (sampleFile.exists() && sampleFile.length() > 5000L) {
            return@withContext getFileUri(context, sampleFile)
        }

        try {
            val width = 640
            val height = 360
            val bitRate = 1_000_000
            val frameRate = 30
            val durationSeconds = 3
            val totalFrames = frameRate * durationSeconds

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(sampleFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 32f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = Color.CYAN
                textSize = 20f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            for (frame in 0 until totalFrames) {
                val canvas: Canvas = inputSurface.lockCanvas(null)
                // Animated gradient-like background
                val hue = (frame * 360f / totalFrames) % 360f
                val bgColor = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.35f))
                canvas.drawColor(bgColor)

                canvas.drawText("Convert264 Sample", width / 2f, height / 2f - 20, paint)
                val timeSec = frame.toFloat() / frameRate
                canvas.drawText(String.format("Frame: %d/%d (%.1fs)", frame, totalFrames, timeSec), width / 2f, height / 2f + 25, subPaint)

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                while (true) {
                    val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) throw RuntimeException("format changed twice")
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (encoderStatus >= 0) {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                            ?: throw RuntimeException("encoderOutputBuffer was null")

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()

            // Drain remaining
            var done = false
            while (!done) {
                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    done = true
                } else if (encoderStatus >= 0) {
                    val encodedData = encoder.getOutputBuffer(encoderStatus) ?: break
                    if (bufferInfo.size != 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        done = true
                    }
                }
            }

            encoder.stop()
            encoder.release()
            inputSurface.release()

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()

            getFileUri(context, sampleFile)
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
    }
}
