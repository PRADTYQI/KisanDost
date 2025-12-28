package com.example.kisandost.safety

import android.content.Context

data class SafetyWarning(
    val isRestricted: Boolean,
    val warningMessage: String? = null,
    val mandatoryGear: List<String>? = null
)

class SafetyGuard private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: SafetyGuard? = null
        
        fun getInstance(): SafetyGuard {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SafetyGuard().also { INSTANCE = it }
            }
        }
        
        // CIBRC 2025 Banned/Restricted Chemicals List
        // Based on Central Insecticides Board & Registration Committee (CIBRC) 2025 guidelines
        private val BANNED_CHEMICALS_2025 = setOf(
            "Monocrotophos",           // Banned for vegetables and food crops
            "Endosulfan",              // Banned for all uses
            "Carbofuran",               // Banned for food crops
            "Phorate",                 // Banned for vegetables
            "Triazophos",              // Banned for vegetables
            "Methomyl",                // Banned for vegetables
            "Aluminium Phosphide",     // Restricted use only
            "Zinc Phosphide",          // Restricted use only
            "Dichlorvos",              // Banned for vegetables
            "Chlorpyrifos",            // Banned for vegetables (restricted for other uses)
            "Acephate",                // Banned for vegetables
            "Fenitrothion",            // Banned for vegetables
            "Quinalphos",              // Banned for vegetables
            "Phosphamidon",            // Banned
            "Methyl Parathion",        // Banned
            "Ethyl Parathion"          // Banned
        )
        
        // Chemicals banned specifically for vegetables
        private val BANNED_FOR_VEGETABLES = setOf(
            "Monocrotophos",
            "Chlorpyrifos",
            "Acephate",
            "Fenitrothion",
            "Quinalphos",
            "Dichlorvos",
            "Phorate",
            "Triazophos",
            "Methomyl"
        )
    }
    
    fun checkChemicalSafety(chemicalName: String, cropType: String = "vegetable"): SafetyWarning {
        val normalizedName = chemicalName.trim().lowercase()
        val normalizedCrop = cropType.trim().lowercase()
        
        // Check if chemical is completely banned
        val isBanned = BANNED_CHEMICALS_2025.any { 
            it.lowercase() in normalizedName || normalizedName.contains(it.lowercase())
        }
        
        // Check if chemical is banned specifically for vegetables
        val isBannedForVegetables = normalizedCrop.contains("vegetable") || 
                                    normalizedCrop.contains("tomato") ||
                                    normalizedCrop.contains("potato") ||
                                    normalizedCrop.contains("brinjal") ||
                                    normalizedCrop.contains("chilli") ||
                                    normalizedCrop.contains("okra")
        
        val isBannedForCrop = isBannedForVegetables && BANNED_FOR_VEGETABLES.any {
            it.lowercase() in normalizedName || normalizedName.contains(it.lowercase())
        }
        
        val isRestricted = isBanned || isBannedForCrop
        
        return if (isRestricted) {
            val warningMsg = when {
                isBannedForCrop -> "⚠️ BANNED CHEMICAL DETECTED!\n\n" +
                        "This chemical (${chemicalName}) is BANNED for ${cropType} crops as per CIBRC 2025 guidelines. " +
                        "Please use safe alternatives recommended by agricultural experts."
                isBanned -> "⚠️ BANNED CHEMICAL DETECTED!\n\n" +
                        "This chemical (${chemicalName}) is BANNED as per CIBRC 2025 guidelines. " +
                        "It is prohibited for use in agriculture. Please use safe alternatives."
                else -> "⚠️ RESTRICTED CHEMICAL DETECTED!\n\n" +
                        "This chemical requires special handling and safety equipment as per CIBRC 2025 guidelines."
            }
            
            SafetyWarning(
                isRestricted = true,
                warningMessage = warningMsg,
                mandatoryGear = if (isBanned) null else listOf(
                    "Full body protective suit",
                    "N95 or higher grade respirator mask",
                    "Chemical-resistant gloves",
                    "Safety goggles or face shield",
                    "Rubber boots",
                    "Head protection"
                )
            )
        } else {
            SafetyWarning(
                isRestricted = false,
                warningMessage = null,
                mandatoryGear = null
            )
        }
    }
    
    fun extractChemicalsFromRecommendations(recommendations: List<String>): List<String> {
        // Extract chemical names from recommendations
        val chemicals = mutableListOf<String>()
        val allBannedChemicals = (BANNED_CHEMICALS_2025 + BANNED_FOR_VEGETABLES).map { it.lowercase() }
        
        recommendations.forEach { recommendation ->
            val lowerRecommendation = recommendation.lowercase()
            
            // Check for banned chemicals by name
            allBannedChemicals.forEach { bannedChemical ->
                if (lowerRecommendation.contains(bannedChemical)) {
                    // Extract the full chemical name (capitalize first letter of each word)
                    val parts = bannedChemical.split(" ")
                    val capitalized = parts.joinToString(" ") { 
                        it.replaceFirstChar { char -> char.uppercaseChar() }
                    }
                    chemicals.add(capitalized)
                }
            }
            
            // Also check for common patterns
            val chemicalPatterns = listOf(
                "fungicide", "pesticide", "insecticide", "herbicide"
            )
            
            chemicalPatterns.forEach { pattern ->
                if (lowerRecommendation.contains(pattern)) {
                    // Try to extract chemical name before the pattern
                    val index = lowerRecommendation.indexOf(pattern)
                    if (index > 0) {
                        val beforePattern = recommendation.substring(0, index).trim()
                        val words = beforePattern.split(" ")
                        // Take the last 1-2 capitalized words as potential chemical name
                        words.takeLast(2).forEach { word ->
                            if (word.isNotEmpty() && word[0].isUpperCase() && word.length > 3) {
                                chemicals.add(word)
                            }
                        }
                    }
                }
            }
        }
        
        return chemicals.distinct()
    }
}


