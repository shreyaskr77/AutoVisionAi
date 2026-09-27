package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BoundingBoxDto(
    @Json(name = "ymin") val ymin: Float,
    @Json(name = "xmin") val xmin: Float,
    @Json(name = "ymax") val ymax: Float,
    @Json(name = "xmax") val xmax: Float
)

@JsonClass(generateAdapter = true)
data class CandidateModelDto(
    @Json(name = "make") val make: String,
    @Json(name = "model") val model: String,
    @Json(name = "confidence") val confidence: Float
)

@JsonClass(generateAdapter = true)
data class VehicleDto(
    @Json(name = "vehicle_id") val vehicleId: Int,
    @Json(name = "bounding_box") val boundingBox: BoundingBoxDto,
    @Json(name = "detection_confidence") val detectionConfidence: Float,
    @Json(name = "make") val make: String,
    @Json(name = "model") val model: String,
    @Json(name = "classification_confidence") val classificationConfidence: Float,
    @Json(name = "colour") val colour: String,
    @Json(name = "colour_hex") val colourHex: String,
    @Json(name = "status") val status: String = "detected",
    @Json(name = "top_candidates") val topCandidates: List<CandidateModelDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AnalysisResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "vehicles") val vehicles: List<VehicleDto> = emptyList(),
    @Json(name = "annotated_image") val annotatedImage: String? = null,
    @Json(name = "processing_time_ms") val processingTimeMs: Float = 0f,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponseDto(
    @Json(name = "status") val status: String,
    @Json(name = "version") val version: String,
    @Json(name = "device") val device: String,
    @Json(name = "detector_ready") val detectorReady: Boolean,
    @Json(name = "classifier_ready") val classifierReady: Boolean,
    @Json(name = "models_dir_exists") val modelsDirExists: Boolean,
    @Json(name = "weights_path") val weightsPath: String
)

@JsonClass(generateAdapter = true)
data class ModelsResponseDto(
    @Json(name = "detector") val detector: String,
    @Json(name = "classifier") val classifier: String,
    @Json(name = "weights_loaded") val weightsLoaded: Boolean,
    @Json(name = "num_classes") val numClasses: Int,
    @Json(name = "classes_sample") val classesSample: List<String> = emptyList(),
    @Json(name = "color_labels") val colorLabels: List<String> = emptyList()
)
