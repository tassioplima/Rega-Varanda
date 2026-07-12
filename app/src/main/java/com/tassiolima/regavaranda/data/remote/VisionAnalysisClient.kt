package com.tassiolima.regavaranda.data.remote

import com.tassiolima.regavaranda.data.model.PlantCategory

interface VisionAnalysisClient {
    suspend fun analyzePhoto(
        apiKey: String,
        imageBase64Jpeg: String,
        plantName: String,
        category: PlantCategory,
        userNotes: String,
        historySummary: String = ""
    ): PhotoAnalysisResult
}
