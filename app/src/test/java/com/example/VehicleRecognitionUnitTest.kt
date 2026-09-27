package com.example

import com.example.data.remote.BoundingBoxDto
import com.example.data.remote.CandidateModelDto
import com.example.data.remote.VehicleDto
import com.example.domain.model.BoundingBox
import com.example.domain.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleRecognitionUnitTest {

    @Test
    fun testBoundingBoxDimensions() {
        val box = BoundingBox(
            ymin = 0.2f,
            xmin = 0.15f,
            ymax = 0.85f,
            xmax = 0.95f
        )
        assertEquals(0.80f, box.width, 0.001f)
        assertEquals(0.65f, box.height, 0.001f)
    }

    @Test
    fun testVehicleDtoToDomainMapping() {
        val dto = VehicleDto(
            vehicleId = 1,
            boundingBox = BoundingBoxDto(0.1f, 0.2f, 0.7f, 0.8f),
            detectionConfidence = 0.95f,
            make = "Porsche",
            model = "911 Coupe",
            classificationConfidence = 0.91f,
            colour = "Electric Blue",
            colourHex = "#2563EB",
            status = "identified",
            topCandidates = listOf(
                CandidateModelDto("Porsche", "911 Coupe", 0.91f),
                CandidateModelDto("Porsche", "Boxster", 0.06f)
            )
        )

        assertEquals("Porsche", dto.make)
        assertEquals("911 Coupe", dto.model)
        assertEquals("Electric Blue", dto.colour)
        assertEquals(2, dto.topCandidates.size)
        assertTrue(dto.classificationConfidence > 0.90f)
    }

    @Test
    fun testColorHexValidity() {
        val hex = "#2563EB"
        assertTrue(hex.startsWith("#"))
        assertEquals(7, hex.length)
    }

    @Test
    fun testSampleVehiclesProvider() {
        val cars = com.example.data.SampleVehicleProvider.sampleCars
        assertEquals(4, cars.size)

        val porsche = cars[0]
        assertEquals("Porsche", porsche.make)
        assertEquals("Red", porsche.colour)
        assertTrue(porsche.classificationConfidence >= 0.90f)

        val bmw = cars[1]
        assertEquals("BMW", bmw.make)
        assertEquals("Electric Blue", bmw.colour)

        val tesla = cars[2]
        assertEquals("Tesla", tesla.make)
        assertEquals("White", tesla.colour)

        val audi = cars[3]
        assertEquals("Audi", audi.make)
        assertEquals("Yellow", audi.colour)
    }
}
