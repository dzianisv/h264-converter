package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileUriString: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val originalName: String,
    val qualityPreset: String,
    val timestamp: Long = System.currentTimeMillis()
)
