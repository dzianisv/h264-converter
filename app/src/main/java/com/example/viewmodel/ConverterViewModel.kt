package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ConversionHistoryEntity
import com.example.model.ConversionProgress
import com.example.model.ConversionResult
import com.example.model.ConversionSettings
import com.example.model.QueueItem
import com.example.model.QueueItemStatus
import com.example.model.coerceSettings
import com.example.transcoder.FfmpegTranscoder
import com.example.util.ReviewPrompt
import com.example.util.SampleVideoProvider
import com.example.util.VideoMetadataHelper
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The conversion queue (v0.2.0).
 *
 * One queue, one global settings set (the main screen's
 * dropdowns), per-item trim + per-item settings overrides.
 * Files are converted one by one, in order, by the FFmpeg
 * engine. Pause cancels the running encode and parks the item
 * as Paused — resume re-runs it from scratch, because ffmpeg
 * has no pause/resume of an in-progress encode (the progress
 * visibly resets; that is honest, not a bug). Stop cancels the
 * run and parks the current item back as Queued.
 */
class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val transcoder = FfmpegTranscoder(context)
    private val database = AppDatabase.getInstance(context)
    private val historyDao = database.conversionHistoryDao()

    val historyList: StateFlow<List<ConversionHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _globalSettings = MutableStateFlow(coerceSettings(ConversionSettings()))
    val globalSettings: StateFlow<ConversionSettings> = _globalSettings.asStateFlow()

    // Play review prompt (v0.2.1): count successful conversions in
    // SharedPreferences; the prompt is due once, at the 3rd success.
    private val prefs = context.getSharedPreferences("convert264", Context.MODE_PRIVATE)
    private val _reviewDue = MutableStateFlow(false)
    val reviewDue: StateFlow<Boolean> = _reviewDue.asStateFlow()

    private fun recordSuccessfulConversion() {
        val count = prefs.getInt("successful_conversions", 0) + 1
        prefs.edit().putInt("successful_conversions", count).apply()
        if (ReviewPrompt.shouldAsk(count, prefs.getBoolean("review_asked", false))) {
            _reviewDue.value = true
        }
    }

    /** The review flow ran (shown, skipped, or failed) — never ask again. */
    fun markReviewRequested() {
        prefs.edit().putBoolean("review_asked", true).apply()
        _reviewDue.value = false
    }

    private val _isLoadingVideo = MutableStateFlow(false)
    val isLoadingVideo: StateFlow<Boolean> = _isLoadingVideo.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var runJob: Job? = null
    private val pauseRequested = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    fun queueItem(id: String): QueueItem? = _queue.value.firstOrNull { it.id == id }

    private fun updateItem(id: String, transform: (QueueItem) -> QueueItem) {
        _queue.value = _queue.value.map { if (it.id == id) transform(it) else it }
    }

    // ---- Adding files -------------------------------------------------

    fun addVideo(uri: Uri) {
        viewModelScope.launch {
            _isLoadingVideo.value = true
            val info = VideoMetadataHelper.extractVideoInfo(context, uri)
            _isLoadingVideo.value = false
            if (info == null) return@launch
            appendItem(info)
        }
    }

    fun addVideos(uris: List<Uri>) {
        uris.forEach { addVideo(it) }
    }

    fun loadSampleVideo() {
        viewModelScope.launch {
            _isLoadingVideo.value = true
            val sampleUri = SampleVideoProvider.getOrCreateSampleVideo(context)
            _isLoadingVideo.value = false
            if (sampleUri != null) {
                val info = VideoMetadataHelper.extractVideoInfo(context, sampleUri)
                if (info != null) appendItem(info)
            }
        }
    }

    private fun appendItem(info: com.example.model.VideoInfo) {
        val item = QueueItem(
            id = UUID.randomUUID().toString(),
            video = info,
            trimRange = if (info.durationMs > 0) 0f..info.durationMs.toFloat() else 0f..1000f,
            settings = _globalSettings.value,
        )
        _queue.value = _queue.value + item
    }

    fun removeQueueItem(id: String) {
        val item = queueItem(id) ?: return
        if (item.status is QueueItemStatus.Converting) {
            stopRequested.set(true)
            transcoder.cancel()
        }
        _queue.value = _queue.value.filterNot { it.id == id }
    }

    fun clearQueue() {
        stopRequested.set(true)
        pauseRequested.set(false)
        runJob?.cancel()
        transcoder.cancel()
        _queue.value = emptyList()
        _isConverting.value = false
        _isPaused.value = false
    }

    // ---- Settings ------------------------------------------------------

    /**
     * Main-screen settings change: coerced to a producible
     * combination, then applied to every Queued item that has not
     * been customized per file on the detail screen. New files
     * always take the current global settings.
     */
    fun setGlobalSettings(settings: ConversionSettings) {
        val coerced = coerceSettings(settings)
        _globalSettings.value = coerced
        _queue.value = _queue.value.map { item ->
            if (!item.customized && item.status is QueueItemStatus.Queued) {
                item.copy(settings = coerced)
            } else {
                item
            }
        }
    }

    fun updateItemSettings(id: String, settings: ConversionSettings) {
        updateItem(id) {
            it.copy(settings = coerceSettings(settings), customized = true)
        }
    }

    fun setCustomBitrate(id: String, bitrateKbps: Int) {
        updateItem(id) {
            it.copy(
                settings = it.settings.copy(customVideoBitrateKbps = bitrateKbps),
                customized = true,
            )
        }
    }

    fun setGlobalCustomBitrate(bitrateKbps: Int) {
        setGlobalSettings(_globalSettings.value.copy(customVideoBitrateKbps = bitrateKbps))
    }

    // ---- Trim (per item — the detail screen edits one item) -----------

    private fun trimTotal(item: QueueItem): Float =
        item.video.durationMs.toFloat().takeIf { it > 0f } ?: 1000f

    fun onTrimSliderChange(id: String, range: ClosedFloatingPointRange<Float>) {
        val item = queueItem(id) ?: return
        val total = trimTotal(item)
        val start = range.start.coerceIn(0f, total)
        val end = range.endInclusive.coerceIn(start + 500f, total).coerceAtLeast(start + 100f)
        updateItem(id) { it.copy(trimRange = start..end) }
    }

    fun adjustTrimStart(id: String, deltaMs: Long) {
        val item = queueItem(id) ?: return
        val range = item.trimRange
        val total = trimTotal(item)
        val newStart = (range.start + deltaMs).coerceIn(0f, (range.endInclusive - 500f).coerceAtLeast(0f))
        updateItem(id) { it.copy(trimRange = newStart..range.endInclusive) }
    }

    fun adjustTrimEnd(id: String, deltaMs: Long) {
        val item = queueItem(id) ?: return
        val range = item.trimRange
        val total = trimTotal(item)
        val newEnd = (range.endInclusive + deltaMs).coerceIn(range.start + 500f, total)
        updateItem(id) { it.copy(trimRange = range.start..newEnd) }
    }

    fun setTrimStartMs(id: String, ms: Long) {
        val item = queueItem(id) ?: return
        val range = item.trimRange
        val total = trimTotal(item)
        val validStart = ms.toFloat().coerceIn(0f, (range.endInclusive - 500f).coerceAtLeast(0f))
        updateItem(id) { it.copy(trimRange = validStart..range.endInclusive) }
    }

    fun setTrimEndMs(id: String, ms: Long) {
        val item = queueItem(id) ?: return
        val range = item.trimRange
        val total = trimTotal(item)
        val validEnd = ms.toFloat().coerceIn(range.start + 500f, total)
        updateItem(id) { it.copy(trimRange = range.start..validEnd) }
    }

    // ---- Queue run ------------------------------------------------------

    private sealed interface RunOutcome {
        data class Success(val result: ConversionResult) : RunOutcome
        data class Error(val message: String) : RunOutcome
        data object Cancelled : RunOutcome
    }

    fun startQueue(fromId: String? = null) {
        if (_isConverting.value) return
        val items = _queue.value
        if (items.none {
                it.status is QueueItemStatus.Queued ||
                    it.status is QueueItemStatus.Paused ||
                    it.status is QueueItemStatus.Error
            }
        ) {
            return
        }
        _isConverting.value = true
        _isPaused.value = false
        pauseRequested.set(false)
        stopRequested.set(false)
        val startIndex = fromId?.let { id ->
            items.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        } ?: 0

        runJob = viewModelScope.launch {
            var idx = startIndex
            while (idx < _queue.value.size) {
                if (stopRequested.get() || pauseRequested.get()) break
                val item = _queue.value.getOrNull(idx) ?: break
                if (item.status !is QueueItemStatus.Queued &&
                    item.status !is QueueItemStatus.Paused &&
                    item.status !is QueueItemStatus.Error
                ) {
                    idx++
                    continue
                }
                updateItem(item.id) {
                    it.copy(
                        status = QueueItemStatus.Converting(
                            ConversionProgress(
                                percent = 0,
                                totalTrimDurationMs = (item.trimRange.endInclusive - item.trimRange.start)
                                    .toLong().coerceAtLeast(1000L),
                            ),
                        ),
                    )
                }
                when (val outcome = runOne(item)) {
                    is RunOutcome.Success -> {
                        updateItem(item.id) {
                            it.copy(status = QueueItemStatus.Done(outcome.result))
                        }
                        recordSuccessfulConversion()
                        viewModelScope.launch(Dispatchers.IO) {
                            historyDao.insert(
                                ConversionHistoryEntity(
                                    fileName = outcome.result.fileName,
                                    filePath = outcome.result.filePath,
                                    fileUriString = outcome.result.fileUri.toString(),
                                    sizeBytes = outcome.result.sizeBytes,
                                    durationMs = outcome.result.durationMs,
                                    originalName = item.video.name,
                                    qualityPreset = outcome.result.targetLabel,
                                ),
                            )
                        }
                    }
                    is RunOutcome.Error -> {
                        updateItem(item.id) {
                            it.copy(status = QueueItemStatus.Error(outcome.message))
                        }
                    }
                    RunOutcome.Cancelled -> {
                        if (pauseRequested.get()) {
                            updateItem(item.id) { it.copy(status = QueueItemStatus.Paused) }
                            _isPaused.value = true
                        } else {
                            updateItem(item.id) { it.copy(status = QueueItemStatus.Queued) }
                        }
                        break
                    }
                }
                if (pauseRequested.get() || stopRequested.get()) break
                idx++
            }
            _isConverting.value = false
            if (stopRequested.get()) _isPaused.value = false
        }
    }

    /** Puts an item back in the queue (e.g. "Convert now" on the detail screen for a Done/Error item). */
    fun requeueItem(id: String) {
        updateItem(id) { item ->
            if (item.status is QueueItemStatus.Converting) item
            else item.copy(status = QueueItemStatus.Queued)
        }
    }

    private suspend fun runOne(item: QueueItem): RunOutcome =
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { transcoder.cancel() }
            viewModelScope.launch {
                try {
                    transcoder.transcode(
                        videoInfo = item.video,
                        startMs = item.trimRange.start.toLong(),
                        endMs = item.trimRange.endInclusive.toLong(),
                        settings = item.settings,
                        onProgress = { progress ->
                            updateItem(item.id) { current ->
                                if (current.status is QueueItemStatus.Converting) {
                                    current.copy(status = QueueItemStatus.Converting(progress))
                                } else {
                                    current
                                }
                            }
                        },
                        onSuccess = { result ->
                            if (cont.isActive) cont.resume(RunOutcome.Success(result))
                        },
                        onError = { message ->
                            if (cont.isActive) cont.resume(RunOutcome.Error(message))
                        },
                        onCancelled = {
                            if (cont.isActive) cont.resume(RunOutcome.Cancelled)
                        },
                    )
                } catch (e: CancellationException) {
                    // Job cancellation must propagate — it is how
                    // pause/stop reaches this coroutine.
                    throw e
                } catch (e: Throwable) {
                    // Engine failures are Errors as well as
                    // Exceptions (e.g. NoClassDefFoundError when a
                    // native/library dependency is missing at
                    // runtime). They must land as a queue-item
                    // Error, never crash the app.
                    if (cont.isActive) {
                        cont.resume(RunOutcome.Error(e.localizedMessage ?: "Conversion failed"))
                    }
                }
            }
        }

    /** Pause = cancel the running encode; the item parks as Paused. */
    fun pauseQueue() {
        if (!_isConverting.value) return
        pauseRequested.set(true)
        transcoder.cancel()
    }

    /** Stop = cancel the run; the current item parks back as Queued. */
    fun stopQueue() {
        if (!_isConverting.value && !_isPaused.value) return
        stopRequested.set(true)
        pauseRequested.set(false)
        runJob?.cancel()
        transcoder.cancel()
        _isConverting.value = false
        _isPaused.value = false
        // Any item left mid-Converting by the cancelled job goes
        // back to Queued so Play can run it again.
        _queue.value = _queue.value.map { item ->
            if (item.status is QueueItemStatus.Converting) {
                item.copy(status = QueueItemStatus.Queued)
            } else {
                item
            }
        }
    }

    fun deleteHistoryItem(item: ConversionHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.delete(item)
            try {
                val f = File(item.filePath)
                if (f.exists()) {
                    f.delete()
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.clearAll()
        }
    }

    override fun onCleared() {
        runJob?.cancel()
        transcoder.cancel()
        super.onCleared()
    }
}
