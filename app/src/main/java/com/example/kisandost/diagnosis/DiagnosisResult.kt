package com.example.kisandost.diagnosis

// This data class acts as the bridge between the AI and the UI
data class DiagnosisResult(
    val diseaseName: String,
    val confidence: Float,
    val chemical: List<String>,    // Matches 'chemical' in UI
    val organic: List<String>,     // Matches 'organic' in UI
    val traditional: List<String>  // Matches 'traditional' in UI
)