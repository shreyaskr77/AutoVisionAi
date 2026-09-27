package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.ScanEntity
import com.example.domain.model.AnalysisResult
import com.example.domain.model.BoundingBox
import com.example.domain.model.CandidateModel
import com.example.domain.model.Vehicle
import java.io.File
import java.io.FileOutputStream

data class PreIdentifiedCar(
    val id: Int,
    val title: String,
    val make: String,
    val model: String,
    val colour: String,
    val colourHex: String,
    val detectionConfidence: Float,
    val classificationConfidence: Float,
    val drawableResId: Int,
    val tag: String,
    val topCandidates: List<CandidateModel>
)

object SampleVehicleProvider {

    val sampleCars = listOf(
        PreIdentifiedCar(
            id = 1,
            title = "Porsche 911 GT3",
            make = "Porsche",
            model = "911 Coupe",
            colour = "Red",
            colourHex = "#DC2626",
            detectionConfidence = 0.96f,
            classificationConfidence = 0.94f,
            drawableResId = R.drawable.sample_car_porsche,
            tag = "Rear-Engine Sports Car",
            topCandidates = listOf(
                CandidateModel("Porsche", "911 Coupe", 0.94f),
                CandidateModel("Porsche", "Boxster Convertible", 0.04f),
                CandidateModel("Audi", "R8 Coupe", 0.01f)
            )
        ),
        PreIdentifiedCar(
            id = 2,
            title = "BMW 3 Series M",
            make = "BMW",
            model = "3 Series Sedan",
            colour = "Electric Blue",
            colourHex = "#2563EB",
            detectionConfidence = 0.95f,
            classificationConfidence = 0.92f,
            drawableResId = R.drawable.sample_car_bmw,
            tag = "Executive Sports Sedan",
            topCandidates = listOf(
                CandidateModel("BMW", "3 Series Sedan", 0.92f),
                CandidateModel("BMW", "M3 Coupe", 0.05f),
                CandidateModel("Audi", "S4 Sedan", 0.02f)
            )
        ),
        PreIdentifiedCar(
            id = 3,
            title = "Tesla Model S",
            make = "Tesla",
            model = "Model S Sedan",
            colour = "White",
            colourHex = "#F8FAFC",
            detectionConfidence = 0.97f,
            classificationConfidence = 0.95f,
            drawableResId = R.drawable.sample_car_tesla,
            tag = "Electric Performance",
            topCandidates = listOf(
                CandidateModel("Tesla", "Model S Sedan", 0.95f),
                CandidateModel("Porsche", "Panamera Sedan", 0.03f),
                CandidateModel("Audi", "A5 Coupe", 0.01f)
            )
        ),
        PreIdentifiedCar(
            id = 4,
            title = "Audi R8 V10",
            make = "Audi",
            model = "R8 Coupe",
            colour = "Yellow",
            colourHex = "#EAB308",
            detectionConfidence = 0.94f,
            classificationConfidence = 0.91f,
            drawableResId = R.drawable.sample_car_audi,
            tag = "Mid-Engine Supercar",
            topCandidates = listOf(
                CandidateModel("Audi", "R8 Coupe", 0.91f),
                CandidateModel("Lamborghini", "Gallardo LP 570-4", 0.06f),
                CandidateModel("Ferrari", "458 Italia Coupe", 0.02f)
            )
        )
    )

    fun toAnalysisResult(car: PreIdentifiedCar, imageUri: String): AnalysisResult {
        val vehicle = Vehicle(
            vehicleId = 1,
            boundingBox = BoundingBox(
                ymin = 0.20f,
                xmin = 0.08f,
                ymax = 0.82f,
                xmax = 0.94f
            ),
            detectionConfidence = car.detectionConfidence,
            make = car.make,
            model = car.model,
            classificationConfidence = car.classificationConfidence,
            colour = car.colour,
            colourHex = car.colourHex,
            status = "identified",
            topCandidates = car.topCandidates
        )

        return AnalysisResult(
            success = true,
            vehicles = listOf(vehicle),
            annotatedImageBase64 = null,
            processingTimeMs = 86f,
            localImageUri = imageUri,
            error = null
        )
    }

    /**
     * Converts a drawable resource to a cached file so Coil / Image loaders and URIs work seamlessly.
     */
    fun getOrCreateSampleImageFile(context: Context, car: PreIdentifiedCar): File {
        val file = File(context.cacheDir, "sample_car_${car.id}.png")
        if (!file.exists()) {
            val drawable = ContextCompat.getDrawable(context, car.drawableResId)
            if (drawable != null) {
                val bitmap = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
        return file
    }
}
