package com.example.kisandost.diagnosis

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Data class for remedy recommendations categorized by type
 */
data class RemedyRecommendations(
    val chemical: List<String>,
    val organic: List<String>,
    val traditional: List<String>
)

data class DiagnosisResult(
    val disease: String,
    val confidence: Float,
    val recommendations: List<String>,
    val cropType: CropType,
    val remedies: RemedyRecommendations = RemedyRecommendations(
        chemical = emptyList(),
        organic = emptyList(),
        traditional = emptyList()
    )
)

class DiagnosisEngine(private val context: Context) {
    private val modelManager: ModelManager = ModelManager(context)
    
    // Expected input image dimensions (adjust based on your model)
    private val inputImageWidth = 224
    private val inputImageHeight = 224
    private val pixelSize = 3 // RGB
    private val imageMean = 128.0f
    private val imageStd = 128.0f
    
    companion object {
        private const val TAG = "DiagnosisEngine"
        // Default number of output classes (adjust based on your models)
        private const val DEFAULT_OUTPUT_CLASSES = 10
    }
    
    /**
     * Switches to a different crop model.
     * @param cropType The crop type to switch to
     * @return true if model loaded successfully, false otherwise
     */
    fun switchCropModel(cropType: CropType): Boolean {
        return modelManager.loadModel(cropType.modelFileName)
    }
    
    /**
     * Gets the currently loaded model path.
     */
    fun getCurrentModelPath(): String? = modelManager.getCurrentModelPath()
    
    /**
     * Checks if a model is currently loaded.
     */
    fun isModelLoaded(): Boolean = modelManager.isModelLoaded()
    
    /**
     * Diagnoses a plant image using the currently loaded model.
     * @param bitmap The image to diagnose
     * @param cropType The crop type being diagnosed (used for recommendations)
     * @return DiagnosisResult or null if diagnosis fails
     */
    fun diagnose(bitmap: Bitmap, cropType: CropType): DiagnosisResult? {
        try {
            // Ensure correct model is loaded for this crop
            if (modelManager.getCurrentModelPath() != cropType.modelFileName) {
                if (!switchCropModel(cropType)) {
                    Log.e(TAG, "Failed to load model for crop: ${cropType.displayName}")
                    return null
                }
            }
            
            val interpreter = modelManager.getInterpreter() ?: run {
                Log.e(TAG, "No model loaded. Please switch to a crop model first.")
                return null
            }
            
            // Preprocess the bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
            val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)
            
            // Get output shape from interpreter (defaults to DEFAULT_OUTPUT_CLASSES if unavailable)
            val outputShape = interpreter.outputTensor(0).shape()
            val numClasses = if (outputShape.isNotEmpty()) outputShape[1] else DEFAULT_OUTPUT_CLASSES
            
            // Run inference
            val outputBuffer = ByteBuffer.allocateDirect(4 * numClasses)
            outputBuffer.order(ByteOrder.nativeOrder())
            
            interpreter.run(byteBuffer, outputBuffer)
            
            // Process output
            outputBuffer.rewind()
            val predictions = FloatArray(numClasses)
            outputBuffer.asFloatBuffer().get(predictions)
            
            // Find the class with highest confidence
            var maxIndex = 0
            var maxConfidence = predictions[0]
            for (i in 1 until predictions.size) {
                if (predictions[i] > maxConfidence) {
                    maxConfidence = predictions[i]
                    maxIndex = i
                }
            }
            
            // Normalize confidence (assuming softmax output, if sigmoid adjust accordingly)
            val normalizedConfidence = maxConfidence.coerceIn(0f, 1f)
            
            // Map index to disease name (generic - models should have consistent labeling)
            val diseaseName = getDiseaseName(maxIndex, cropType)
            
            // Generate recommendations based on disease and crop type
            val recommendations = generateRecommendations(diseaseName, cropType)
            val remedies = generateRemedies(diseaseName, cropType)
            
            return DiagnosisResult(
                disease = diseaseName,
                confidence = normalizedConfidence,
                recommendations = recommendations,
                cropType = cropType,
                remedies = remedies
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during diagnosis", e)
            return null
        }
    }
    
    /**
     * Maps prediction index to disease name based on crop type.
     * This is a generic implementation - adjust based on your model's label files.
     */
    private fun getDiseaseName(index: Int, cropType: CropType): String {
        // Generic disease labels - in production, load from labels.txt per crop
        val genericLabels = arrayOf(
            "Healthy",
            "Blight",
            "Rust",
            "Mosaic",
            "Leaf Curl",
            "Scab",
            "Rot",
            "Wilt",
            "Spot",
            "Mildew"
        )
        
        return if (index < genericLabels.size) {
            "${cropType.displayName} ${genericLabels[index]}"
        } else {
            "Unknown Disease"
        }
    }
    
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * pixelSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var pixel = 0
        for (i in 0 until inputImageHeight) {
            for (j in 0 until inputImageWidth) {
                val `val` = intValues[pixel++]
                
                // Normalize pixel values to [-1, 1]
                byteBuffer.putFloat(((`val` shr 16 and 0xFF) - imageMean) / imageStd)
                byteBuffer.putFloat(((`val` shr 8 and 0xFF) - imageMean) / imageStd)
                byteBuffer.putFloat(((`val` and 0xFF) - imageMean) / imageStd)
            }
        }
        
        return byteBuffer
    }
    
    private fun generateRecommendations(disease: String, cropType: CropType): List<String> {
        // Combine all remedy types for general recommendations
        val remedies = generateRemedies(disease, cropType)
        return remedies.chemical + remedies.organic + remedies.traditional
    }
    
    /**
     * Generates categorized remedy recommendations based on disease and crop type.
     */
    private fun generateRemedies(disease: String, cropType: CropType): RemedyRecommendations {
        // Check if healthy
        if (disease.contains("Healthy", ignoreCase = true)) {
            return RemedyRecommendations(
                chemical = emptyList(),
                organic = listOf(
                    "Continue monitoring plant health",
                    "Maintain proper watering schedule",
                    "Apply compost for nutrition"
                ),
                traditional = listOf(
                    "Continue traditional care practices",
                    "Use neem-based preventive sprays monthly"
                )
            )
        }
        
        // Generate crop-specific remedies
        return when (cropType) {
            CropType.TOMATO -> generateTomatoRemedies(disease)
            CropType.POTATO -> generatePotatoRemedies(disease)
            CropType.APPLE -> generateAppleRemedies(disease)
            CropType.MANGO -> generateMangoRemedies(disease)
            CropType.GUAVA -> generateGuavaRemedies(disease)
            CropType.COTTON -> generateCottonRemedies(disease)
        }
    }
    
    private fun generateTomatoRemedies(disease: String): RemedyRecommendations {
        val isBlight = disease.contains("Blight", ignoreCase = true)
        val isMosaic = disease.contains("Mosaic", ignoreCase = true)
        val isLeafCurl = disease.contains("Leaf Curl", ignoreCase = true)
        
        return RemedyRecommendations(
            chemical = if (isBlight) {
                listOf(
                    "Apply mancozeb 75% WP @ 2g/L (CIBRC approved)",
                    "Use copper oxychloride 50% WP @ 3g/L",
                    "Apply chlorothalonil 75% WP @ 2g/L as preventive"
                )
            } else if (isMosaic) {
                listOf(
                    "Apply systemic insecticides for vector control",
                    "Use imidacloprid 17.8% SL @ 0.3ml/L"
                )
            } else {
                listOf("Consult KVK for CIBRC-approved fungicides")
            },
            organic = listOf(
                "Apply neem oil 5ml + soap solution 1L",
                "Use Trichoderma viride @ 10g/L",
                "Spray cow urine solution (1:10 ratio)",
                "Apply garlic-chilli extract (50g each in 1L water)"
            ),
            traditional = listOf(
                "Apply buttermilk solution (1:5 ratio)",
                "Use ash + turmeric powder mix",
                "Spray neem leaf extract (50g leaves in 1L water)",
                "Apply panchagavya (traditional bio-formulation)"
            )
        )
    }
    
    private fun generatePotatoRemedies(disease: String): RemedyRecommendations {
        val isBlight = disease.contains("Blight", ignoreCase = true)
        val isScab = disease.contains("Scab", ignoreCase = true)
        
        return RemedyRecommendations(
            chemical = if (isBlight) {
                listOf(
                    "Apply mancozeb 75% WP @ 2g/L",
                    "Use metalaxyl 8% + mancozeb 64% WP @ 2.5g/L",
                    "Apply propineb 70% WP @ 2g/L"
                )
            } else if (isScab) {
                listOf(
                    "Treat seed tubers with carbendazim 50% WP @ 2g/kg",
                    "Apply pentachloronitrobenzene (PCNB) as soil treatment"
                )
            } else {
                listOf("Consult KVK for CIBRC-approved fungicides")
            },
            organic = listOf(
                "Apply Trichoderma harzianum @ 10g/L",
                "Use neem cake in soil @ 250kg/acre",
                "Spray Pseudomonas fluorescens @ 10g/L",
                "Apply compost tea for soil health"
            ),
            traditional = listOf(
                "Use ash treatment for seed tubers",
                "Apply cow dung solution (1:10 ratio)",
                "Use neem leaf mulch",
                "Practice 3-year crop rotation with non-solanaceous crops")
        )
    }
    
    private fun generateAppleRemedies(disease: String): RemedyRecommendations {
        return RemedyRecommendations(
            chemical = listOf(
                "Apply carbendazim 50% WP @ 1g/L for fungal diseases",
                "Use mancozeb 75% WP @ 2g/L as preventive spray",
                "Apply hexaconazole 5% EC @ 2ml/L"
            ),
            organic = listOf(
                "Apply neem oil 5ml + soap solution",
                "Use Trichoderma @ 10g/L",
                "Spray baking soda solution (5g/L)",
                "Apply compost tea"
            ),
            traditional = listOf(
                "Use cow urine + neem extract",
                "Apply wood ash around tree base",
                "Spray garlic extract",
                "Use fermented buttermilk solution"
            )
        )
    }
    
    private fun generateMangoRemedies(disease: String): RemedyRecommendations {
        return RemedyRecommendations(
            chemical = listOf(
                "Apply carbendazim 50% WP @ 1g/L for anthracnose",
                "Use copper oxychloride 50% WP @ 3g/L",
                "Apply propiconazole 25% EC @ 1ml/L"
            ),
            organic = listOf(
                "Apply neem oil 5ml + soap solution",
                "Use Trichoderma @ 10g/L",
                "Spray Pseudomonas fluorescens",
                "Apply compost tea"
            ),
            traditional = listOf(
                "Use neem leaf extract",
                "Apply cow dung solution",
                "Spray fermented buttermilk",
                "Use wood ash treatment"
            )
        )
    }
    
    private fun generateGuavaRemedies(disease: String): RemedyRecommendations {
        return RemedyRecommendations(
            chemical = listOf(
                "Apply carbendazim 50% WP @ 1g/L",
                "Use mancozeb 75% WP @ 2g/L",
                "Apply hexaconazole 5% EC @ 2ml/L"
            ),
            organic = listOf(
                "Apply neem oil spray",
                "Use Trichoderma @ 10g/L",
                "Spray baking soda solution",
                "Apply compost tea"
            ),
            traditional = listOf(
                "Use neem leaf extract",
                "Apply cow urine solution",
                "Spray garlic-chilli extract",
                "Use ash treatment"
            )
        )
    }
    
    private fun generateCottonRemedies(disease: String): RemedyRecommendations {
        return RemedyRecommendations(
            chemical = listOf(
                "Apply mancozeb 75% WP @ 2g/L for bacterial blight",
                "Use carbendazim 50% WP @ 1g/L for fungal diseases",
                "Apply propiconazole 25% EC @ 1ml/L"
            ),
            organic = listOf(
                "Apply neem oil spray",
                "Use Trichoderma @ 10g/L",
                "Spray Pseudomonas fluorescens",
                "Apply compost tea"
            ),
            traditional = listOf(
                "Use neem leaf extract",
                "Apply cow dung solution",
                "Spray fermented buttermilk",
                "Use ash + turmeric mix"
            )
        )
    }
    
    fun close() {
        modelManager.close()
    }
}


