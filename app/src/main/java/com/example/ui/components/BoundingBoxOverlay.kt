package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.domain.model.Vehicle
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan

@Composable
fun BoundingBoxOverlay(
    vehicles: List<Vehicle>,
    selectedVehicleId: Int,
    onVehicleSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(vehicles) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()

                    // Check which vehicle bounding box was tapped
                    for (v in vehicles) {
                        val box = v.boundingBox
                        val left = box.xmin * w
                        val top = box.ymin * h
                        val right = box.xmax * w
                        val bottom = box.ymax * h

                        if (tapOffset.x in left..right && tapOffset.y in top..bottom) {
                            onVehicleSelect(v.vehicleId)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (vehicle in vehicles) {
            val isSelected = vehicle.vehicleId == selectedVehicleId
            val box = vehicle.boundingBox

            val left = box.xmin * canvasWidth
            val top = box.ymin * canvasHeight
            val right = box.xmax * canvasWidth
            val bottom = box.ymax * canvasHeight
            val boxWidth = right - left
            val boxHeight = bottom - top

            val boxColor = if (isSelected) ElectricBlue else NeonCyan.copy(alpha = 0.6f)
            val strokeWidth = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx()

            // Draw bounding box outline
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = if (!isSelected) PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null
                )
            )

            // Draw corner reticle brackets for selected vehicle
            if (isSelected) {
                val cornerLength = (boxWidth * 0.15f).coerceIn(16f, 32f)

                // Top-left
                drawLine(
                    color = Color.White,
                    start = Offset(left, top),
                    end = Offset(left + cornerLength, top),
                    strokeWidth = 5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(left, top),
                    end = Offset(left, top + cornerLength),
                    strokeWidth = 5f
                )

                // Top-right
                drawLine(
                    color = Color.White,
                    start = Offset(right, top),
                    end = Offset(right - cornerLength, top),
                    strokeWidth = 5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(right, top),
                    end = Offset(right, top + cornerLength),
                    strokeWidth = 5f
                )

                // Bottom-left
                drawLine(
                    color = Color.White,
                    start = Offset(left, bottom),
                    end = Offset(left + cornerLength, bottom),
                    strokeWidth = 5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(left, bottom),
                    end = Offset(left, bottom - cornerLength),
                    strokeWidth = 5f
                )

                // Bottom-right
                drawLine(
                    color = Color.White,
                    start = Offset(right, bottom),
                    end = Offset(right - cornerLength, bottom),
                    strokeWidth = 5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(right, bottom),
                    end = Offset(right, bottom - cornerLength),
                    strokeWidth = 5f
                )
            }
        }
    }
}
