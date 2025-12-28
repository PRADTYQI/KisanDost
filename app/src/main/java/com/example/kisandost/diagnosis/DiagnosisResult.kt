package com.example.kisandost.diagnosis

data class DiagnosisResult(
    val disease: String,
    val confidence: Float,
    val remedies: List<String>
)
