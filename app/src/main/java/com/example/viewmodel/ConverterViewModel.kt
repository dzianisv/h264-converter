package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ConversionHistoryEntity
import com.example.model.ConversionProgress
import com.example.model.ConversionResult
import com.example.model.ConversionState
import com.example.model.QualityPreset
import com.example.model.VideoInfo
import com.example.transcoder.VideoTranscoder
import com.example.util.FormatUtils
import com.example.util.VideoMetadataHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val transcoder = VideoTranscoder(context)
    private val database = AppDatabase.getInstance(context)
    private val historyDao = database.conversionHistoryDao()

    val historyList: StateFlow<List<ConversionHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedVideo = MutableStateFlow<VideoInfo?>(null)
    val selectedVideo: StateFlow<VideoInfo?> = _selectedVideo.asStateFlow()

    private val _isLoadingVideo = MutableStateFlow(false)
    val isLoadingVideo: StateFlow<Boolean> = _isLoadingVideo.asStateFlow()

    private val _trimRange = MutableStateFlow(0f..1000f)
    val trimRange: StateFlow<ClosedFloatingPointRange<Float>> = _trimRange.asStateFlow()

    private val _qualityPreset = MutableStateFlow(QualityPreset.ORIGINAL)
    val qualityPreset: StateFlow<QualityPreset> = _qualityPreset.asStateFlow()

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()

    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            _isLoadingVideo.value = true
            _conversionState.value = ConversionState.Idle
            val info = VideoMetadataHelper.extractVideoInfo(context, uri)
            _selectedVideo.value = info
            _isLoadingVideo.value = false

            if (info != null && info.durationMs > 0) {
                _trimRange.value = 0f..info.durationMs.toFloat()
            } else {
                _trimRange.value = 0f..1000f
            }
        }
    }

    fun loadSampleVideo() {
        viewModelScope.launch {
            _isLoadingVideo.value = true
            val sampleUri = com.example.util.SampleVideoProvider.getOrCreateSampleVideo(context)
            if (sampleUri != null) {
                loadVideo(sampleUri)
            } else {
                _isLoadingVideo.value = false
            }
        }
    }

    fun onTrimSliderChange(range: ClosedFloatingPointRange<Float>) {
        val total = _selectedVideo.value?.durationMs?.toFloat() ?: 1000f
        val start = range.start.coerceIn(0f, total)
        val end = range.endInclusive.coerceIn(start + 500f, total).coerceAtLeast(start + 100f)
        _trimRange.value = start..end
    }

    fun adjustTrimStart(deltaMs: Long) {
        val currentRange = _trimRange.value
        val total = _selectedVideo.value?.durationMs?.toFloat() ?: 1000f
        val newStart = (currentRange.start + deltaMs).coerceIn(0f, (currentRange.endInclusive - 500f).coerceAtLeast(0f))
        _trimRange.value = newStart..currentRange.endInclusive
    }

    fun adjustTrimEnd(deltaMs: Long) {
        val currentRange = _trimRange.value
        val total = _selectedVideo.value?.durationMs?.toFloat() ?: 1000f
        val newEnd = (currentRange.endInclusive + deltaMs).coerceIn(currentRange.start + 500f, total)
        _trimRange.value = currentRange.start..newEnd
    }

    fun setTrimStartMs(ms: Long) {
        val currentRange = _trimRange.value
        val total = _selectedVideo.value?.durationMs?.toFloat() ?: 1000f
        val validStart = ms.toFloat().coerceIn(0f, (currentRange.endInclusive - 500f).coerceAtLeast(0f))
        _trimRange.value = validStart..currentRange.endInclusive
    }

    fun setTrimEndMs(ms: Long) {
        val currentRange = _trimRange.value
        val total = _selectedVideo.value?.durationMs?.toFloat() ?: 1000f
        val validEnd = ms.toFloat().coerceIn(currentRange.start + 500f, total)
        _trimRange.value = currentRange.start..validEnd
    }

    fun setQualityPreset(preset: QualityPreset) {
        _qualityPreset.value = preset
    }

    fun startConversion() {
        val video = _selectedVideo.value ?: return
        val range = _trimRange.value
        val startMs = range.start.toLong()
        val endMs = range.endInclusive.toLong()
        val preset = _qualityPreset.value

        _conversionState.value = ConversionState.Converting(
            ConversionProgress(
                percent = 0,
                currentTrimProgressMs = 0L,
                totalTrimDurationMs = (endMs - startMs).coerceAtLeast(1000L),
                elapsedSeconds = 0L
            )
        )

        viewModelScope.launch {
            transcoder.transcode(
                videoInfo = video,
                startMs = startMs,
                endMs = endMs,
                qualityPreset = preset,
                onProgress = { progress ->
                    _conversionState.value = ConversionState.Converting(progress)
                },
                onSuccess = { result ->
                    _conversionState.value = ConversionState.Success(result)
                    // Persist to history database
                    viewModelScope.launch(Dispatchers.IO) {
                        historyDao.insert(
                            ConversionHistoryEntity(
                                fileName = result.fileName,
                                filePath = result.filePath,
                                fileUriString = result.fileUri.toString(),
                                sizeBytes = result.sizeBytes,
                                durationMs = result.durationMs,
                                originalName = video.name,
                                qualityPreset = preset.label
                            )
                        )
                    }
                },
                onError = { error ->
                    _conversionState.value = ConversionState.Error(error)
                }
            )
        }
    }

    fun cancelConversion() {
        transcoder.cancel()
        _conversionState.value = ConversionState.Idle
    }

    fun resetForNewConversion() {
        transcoder.cancel()
        _selectedVideo.value = null
        _conversionState.value = ConversionState.Idle
        _trimRange.value = 0f..1000f
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
        super.onCleared()
        transcoder.cancel()
    }
}
