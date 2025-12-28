package com.example.kisandost.diagnosis

/**
 * Enum representing different crop types supported by the app.
 * Each crop type has an associated model file and drawable resource.
 */
enum class CropType(
    val displayName: String,
    val modelFileName: String,
    val drawableResourceName: String
) {
    APPLE(
        displayName = "Apple",
        modelFileName = ModelManager.MODEL_APPLE,
        drawableResourceName = "Apple"
    ),
    TOMATO(
        displayName = "Tomato",
        modelFileName = ModelManager.MODEL_TOMATO,
        drawableResourceName = "Tomato"
    ),
    POTATO(
        displayName = "Potato",
        modelFileName = ModelManager.MODEL_POTATO,
        drawableResourceName = "Porato" // Note: Keeping the typo to match existing drawable
    ),
    MANGO(
        displayName = "Mango",
        modelFileName = ModelManager.MODEL_MANGO,
        drawableResourceName = "Mango"
    ),
    GUAVA(
        displayName = "Guava",
        modelFileName = ModelManager.MODEL_GUAVA,
        drawableResourceName = "Guava"
    ),
    COTTON(
        displayName = "Cotton",
        modelFileName = ModelManager.MODEL_COTTON,
        drawableResourceName = "Cotton"
    );
    
    companion object {
        /**
         * Get all crop types as a list
         */
        fun getAllCrops(): List<CropType> {
            return values().toList()
        }
        
        /**
         * Get default crop type (Tomato as it's commonly used)
         */
        fun getDefault(): CropType = TOMATO
    }
}
