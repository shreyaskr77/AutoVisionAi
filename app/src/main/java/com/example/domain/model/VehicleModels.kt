package com.example.domain.model

data class BoundingBox(
    val ymin: Float,
    val xmin: Float,
    val ymax: Float,
    val xmax: Float
) {
    val width: Float get() = (xmax - xmin).coerceAtLeast(0f)
    val height: Float get() = (ymax - ymin).coerceAtLeast(0f)
}

data class CandidateModel(
    val make: String,
    val model: String,
    val confidence: Float
)

data class Vehicle(
    val vehicleId: Int,
    val boundingBox: BoundingBox,
    val detectionConfidence: Float,
    val make: String,
    val model: String,
    val classificationConfidence: Float,
    val colour: String,
    val colourHex: String,
    val status: String = "detected",
    val topCandidates: List<CandidateModel> = emptyList()
)

data class AnalysisResult(
    val success: Boolean,
    val vehicles: List<Vehicle>,
    val annotatedImageBase64: String?,
    val processingTimeMs: Float,
    val localImageUri: String,
    val error: String? = null
)

enum class AnalysisStage(val label: String, val description: String, val stepNumber: Int) {
    PREPROCESSING(
        "Preprocessing Image",
        "Normalizing EXIF orientation and scaling resolution...",
        1
    ),
    DETECTING_VEHICLE(
        "Detecting Vehicles",
        "Running YOLO detector to localize car coordinates...",
        2
    ),
    IDENTIFYING_MODEL(
        "Identifying Make & Model",
        "Classifying vehicle features via deep convolutional network...",
        3
    ),
    ANALYZING_COLOUR(
        "Analyzing Exterior Colour",
        "Sampling body panels in HSV & CIELAB colour space...",
        4
    ),
    GENERATING_RESULTS(
        "Finalizing Results",
        "Synthesizing detection boundaries and confidence metrics...",
        5
    )
}
