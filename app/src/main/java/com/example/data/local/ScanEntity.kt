package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String,
    val make: String,
    val model: String,
    val colour: String,
    val colourHex: String,
    val detectionConfidence: Float,
    val classificationConfidence: Float,
    val vehicleCount: Int = 1,
    val annotatedImageBase64: String? = null,
    val status: String = "detected",
    val topCandidatesJson: String = ""
)
