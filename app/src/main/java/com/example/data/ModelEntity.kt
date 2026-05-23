package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class CivitaiModel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "Checkpoint", "LORA", "TextualInversion"
    val baseModel: String, // "SD 1.5", "SDXL", "Pony"
    val triggerWords: String, // Comma separated
    val downloadUrl: String,
    val description: String,
    val rating: Float,
    val imageUrl: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0,
    val isCustom: Boolean = false // Added by pasting link
)
