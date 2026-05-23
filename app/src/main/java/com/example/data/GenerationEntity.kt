package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generations")
data class GenerationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "IMAGE", "VIDEO"
    val promptArabic: String,
    val promptEnglish: String,
    val negativePrompt: String,
    val aspect: String, // "1:1", "16:9", "9:16", "4:3"
    val seed: Long,
    val modelName: String,
    val imageUrl: String, // URL of generated image/video
    val durationSeconds: Int = 0, // for videos
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val status: String = "SUCCESS" // "PENDING", "RENDERING", "SUCCESS", "FAILED"
)
