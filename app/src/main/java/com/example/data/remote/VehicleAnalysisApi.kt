package com.example.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VehicleAnalysisApi {
    @Multipart
    @POST("api/v1/analyze")
    suspend fun analyzeImage(
        @Part file: MultipartBody.Part
    ): Response<AnalysisResponseDto>

    @GET("api/v1/health")
    suspend fun getHealth(): Response<HealthResponseDto>

    @GET("api/v1/models")
    suspend fun getModels(): Response<ModelsResponseDto>
}
