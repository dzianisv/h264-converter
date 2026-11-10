package com.example.model

import java.util.Locale

/**
 * The supported formats of the universal converter (v0.2.0).
 *
 * ONE place defines what the engine can actually do: every
 * dropdown in the UI is built from these lists, and the lists
 * filter each other, so an impossible combination can never be
 * selected (e.g. picking WEBM narrows the video codecs to VP9
 * and the audio codecs to Opus/Vorbis).
 *
 * The engine is FFmpeg (ffmpeg-kit-maintained, full-gpl build):
 * libx264, libx265, libvpx-vp9, libaom-av1, mpeg4, libmp3lame,
 * libopus, libvorbis, flac, native aac. Media3 Transformer could
 * not do this — it muxes MP4 only, with device-dependent encoders.
 *
 * AV1 (libaom-av1) is deliberately NOT in this file yet: the
 * v0.2.0 spike measures its encode time on the emulator first.
 * If it converts a 3s clip in reasonable time it is added here
 * (enum entry + MKV/WEBM matrix rows + cpu-used presets); if
 * not, it stays out and the spike log says why.
 */
enum class VideoCodec(
    val label: String,
    /** Short token used in output file names, e.g. clip_vp9.mkv. */
    val shortName: String,
    /** ffmpeg encoder name, or "copy" for stream copy. */
    val encoder: String,
) {
    H264("H.264", "h264", "libx264"),
    H265("H.265", "h265", "libx265"),
    VP9("VP9", "vp9", "libvpx-vp9"),
    MPEG4("MPEG-4", "mpeg4", "mpeg4"),
    COPY("Copy (no re-encode)", "copy", "copy"),
}

enum class AudioCodec(
    val label: String,
    /** ffmpeg encoder name, "copy" for stream copy, null = no audio. */
    val encoder: String?,
) {
    AAC("AAC", "aac"),
    MP3("MP3", "libmp3lame"),
    OPUS("Opus", "libopus"),
    VORBIS("Vorbis", "libvorbis"),
    FLAC("FLAC", "flac"),
    COPY("Copy (no re-encode)", "copy"),
    NONE("No audio", null),
}

enum class Container(
    val label: String,
    val extension: String,
    /** ffmpeg muxer name. */
    val muxer: String,
    val mimeType: String,
) {
    MP4("MP4", "mp4", "mp4", "video/mp4"),
    MKV("Matroska (MKV)", "mkv", "matroska", "video/x-matroska"),
    MOV("MOV", "mov", "mov", "video/quicktime"),
    WEBM("WebM", "webm", "webm", "video/webm"),
}

/** Video bitrate presets. CUSTOM takes [ConversionSettings.customVideoBitrateKbps]. */
enum class QualityPreset(
    val label: String,
    val videoBitrateKbps: Int?,
) {
    LOW("Low · 2 Mbps", 2000),
    MEDIUM("Medium · 5 Mbps", 5000),
    HIGH("High · 10 Mbps", 10000),
    CUSTOM("Custom bitrate…", null),
}

/**
 * One encoder-preset option. [value] is what the settings store;
 * [args] are the ffmpeg arguments it expands to.
 */
data class EncoderPresetOption(
    val label: String,
    val value: String,
    val args: List<String>,
)

/** Encoder-preset options for a codec; empty = the codec has no preset. */
fun encoderPresetOptions(codec: VideoCodec): List<EncoderPresetOption> = when (codec) {
    VideoCodec.H264, VideoCodec.H265 -> listOf(
        "ultrafast", "superfast", "veryfast", "faster", "fast",
        "medium", "slow", "slower", "veryslow",
    ).map { EncoderPresetOption(label = it, value = it, args = listOf("-preset", it)) }
    VideoCodec.VP9 -> listOf(
        "0 · Best quality" to "0",
        "1" to "1",
        "2 · Balanced" to "2",
        "3" to "3",
        "4" to "4",
        "5 · Fastest" to "5",
    ).map { (label, value) ->
        EncoderPresetOption(label = label, value = value, args = listOf("-cpu-used", value))
    }
    VideoCodec.MPEG4, VideoCodec.COPY -> emptyList()
}

fun defaultEncoderPreset(codec: VideoCodec): String = when (codec) {
    VideoCodec.H264, VideoCodec.H265 -> "medium"
    VideoCodec.VP9 -> "2"
    VideoCodec.MPEG4, VideoCodec.COPY -> ""
}

/** Which video codecs a container can hold. */
fun videoCodecsFor(container: Container): List<VideoCodec> = when (container) {
    Container.MP4 -> listOf(VideoCodec.H264, VideoCodec.H265, VideoCodec.MPEG4, VideoCodec.COPY)
    Container.MKV -> VideoCodec.entries.toList()
    Container.MOV -> listOf(VideoCodec.H264, VideoCodec.H265, VideoCodec.MPEG4, VideoCodec.COPY)
    Container.WEBM -> listOf(VideoCodec.VP9, VideoCodec.COPY)
}

/** Which audio codecs a container can hold. */
fun audioCodecsFor(container: Container): List<AudioCodec> = when (container) {
    Container.MP4 -> listOf(AudioCodec.AAC, AudioCodec.MP3, AudioCodec.FLAC, AudioCodec.COPY, AudioCodec.NONE)
    Container.MKV -> AudioCodec.entries.toList()
    Container.MOV -> listOf(AudioCodec.AAC, AudioCodec.MP3, AudioCodec.FLAC, AudioCodec.COPY, AudioCodec.NONE)
    Container.WEBM -> listOf(AudioCodec.OPUS, AudioCodec.VORBIS, AudioCodec.COPY, AudioCodec.NONE)
}

/**
 * One conversion's settings. Audio bitrate is fixed at 128 kbps
 * (AAC/MP3/Opus/Vorbis; FLAC is lossless and takes no bitrate) —
 * the customizable bitrate is the video one, via [quality].
 * Resolution is always the source resolution in v0.2.0 (the old
 * 1080p/720p presets were dropped with the Media3 engine; the
 * codec/container/audio/bitrate matrix is the v0.2.0 scope).
 */
data class ConversionSettings(
    val container: Container = Container.MP4,
    val videoCodec: VideoCodec = VideoCodec.H264,
    val audioCodec: AudioCodec = AudioCodec.AAC,
    val quality: QualityPreset = QualityPreset.MEDIUM,
    val customVideoBitrateKbps: Int = 5000,
    /** Codec-specific preset value (x264 preset name, VP9 cpu-used). */
    val encoderPreset: String = defaultEncoderPreset(VideoCodec.H264),
) {
    val videoBitrateKbps: Int
        get() = quality.videoBitrateKbps ?: customVideoBitrateKbps.coerceIn(100, 100_000)

    /** Short target summary shown in queue rows, e.g. "MKV · VP9 · Opus". */
    val targetSummary: String
        get() = "${container.name} · ${videoCodec.label} · ${audioCodec.label}"

    /** Output file extension for these settings. */
    val outputExtension: String get() = container.extension
}

/**
 * Coerces settings to a combination the engine can actually
 * produce: a video/audio codec that does not fit the container is
 * replaced by the first allowed one, and a preset that does not
 * fit the codec is replaced by that codec's default. Call it on
 * every settings change — the UI never has to reason about the
 * matrix itself.
 */
fun coerceSettings(settings: ConversionSettings): ConversionSettings {
    var result = settings
    val allowedVideo = videoCodecsFor(result.container)
    if (result.videoCodec !in allowedVideo) {
        result = result.copy(videoCodec = allowedVideo.first())
    }
    val allowedAudio = audioCodecsFor(result.container)
    if (result.audioCodec !in allowedAudio) {
        result = result.copy(audioCodec = allowedAudio.first())
    }
    val presetOptions = encoderPresetOptions(result.videoCodec)
    val preset = if (presetOptions.none { it.value == result.encoderPreset }) {
        defaultEncoderPreset(result.videoCodec)
    } else {
        result.encoderPreset
    }
    return result.copy(encoderPreset = preset)
}

/**
 * Builds the ffmpeg command for one file. Paths are staging files
 * with safe (UUID) names, so no shell quoting is needed; the
 * command is a single string for FFmpegKit.execute().
 *
 * - `-ss` before `-i` = fast seek to the trim start; `-t` is the
 *   trimmed duration. (With `-c:v copy` the cut lands on the
 *   nearest keyframe before the trim start — the output can start
 *   a fraction early; a re-encode cut is exact.)
 * - `-map 0:a:0?` — the `?` makes audio optional, so files with
 *   no audio track (e.g. the generated sample clip) still convert.
 */
fun buildFfmpegCommand(
    inputPath: String,
    outputPath: String,
    startMs: Long,
    endMs: Long,
    settings: ConversionSettings,
): String {
    val startSec = "%.3f".format(Locale.US, startMs.coerceAtLeast(0L) / 1000.0)
    val durationSec = "%.3f".format(
        Locale.US,
        (endMs - startMs).coerceAtLeast(100L) / 1000.0,
    )
    val args = mutableListOf<String>()
    args += "-y"
    args += "-ss"; args += startSec
    args += "-i"; args += inputPath
    args += "-t"; args += durationSec
    args += "-map"; args += "0:v:0"
    args += "-map"; args += "0:a:0?"
    args += "-avoid_negative_ts"; args += "make_zero"
    // Video
    if (settings.videoCodec == VideoCodec.COPY) {
        args += "-c:v"; args += "copy"
    } else {
        args += "-c:v"; args += settings.videoCodec.encoder
        encoderPresetOptions(settings.videoCodec)
            .firstOrNull { it.value == settings.encoderPreset }
            ?.args?.let { args += it }
        args += "-b:v"; args += "${settings.videoBitrateKbps}k"
    }
    // Audio
    when (val audio = settings.audioCodec) {
        AudioCodec.NONE -> { args += "-an" }
        AudioCodec.COPY -> { args += "-c:a"; args += "copy" }
        else -> {
            args += "-c:a"; args += audio.encoder!!
            if (audio != AudioCodec.FLAC) {
                args += "-b:a"; args += "128k"
            }
        }
    }
    if (settings.container == Container.MP4) {
        args += "-movflags"; args += "+faststart"
    }
    args += "-f"; args += settings.container.muxer
    args += outputPath
    return args.joinToString(" ")
}
