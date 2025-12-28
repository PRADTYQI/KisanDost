package com.example.kisandost.diagnosis

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Manages multiple TensorFlow Lite models with memory-safe switching.
 * Ensures previous interpreter is closed before loading a new one.
 */
class ModelManager(private val context: Context) {
    private var currentInterpreter: Interpreter? = null
    private var currentModelPath: String? = null
    
    companion object {
        private const val TAG = "ModelManager"
        
        // Model file mappings
        const val MODEL_COTTON = "Cotton_model_unquant.tflite"
        const val MODEL_GUAVA = "Guava_model_unquant.tflite"
        const val MODEL_MANGO = "Mango_model_unquant.tflite"
        const val MODEL_POTATO = "Potato_model_unquant.tflite"
        const val MODEL_TOMATO = "Tomato_model_unquant.tflite"
        const val MODEL_APPLE = "Apple_model_unquant.tflite"
    }
    
    /**
     * Loads a model by file path. Closes previous interpreter if one exists.
     * @param modelPath Path to the model file in assets folder
     * @return true if model loaded successfully, false otherwise
     */
    fun loadModel(modelPath: String): Boolean {
        return try {
            // Close previous interpreter to prevent memory leaks
            closeCurrentModel()
            
            val modelBuffer = loadModelFile(context, modelPath)
            currentInterpreter = Interpreter(modelBuffer)
            currentModelPath = modelPath
            
            Log.d(TAG, "Model loaded successfully: $modelPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: $modelPath", e)
            currentInterpreter = null
            currentModelPath = null
            false
        }
    }
    
    /**
     * Gets the current interpreter if a model is loaded.
     * @return Interpreter instance or null if no model is loaded
     */
    fun getInterpreter(): Interpreter? {
        return currentInterpreter
    }
    
    /**
     * Gets the path of the currently loaded model.
     * @return Model path or null if no model is loaded
     */
    fun getCurrentModelPath(): String? {
        return currentModelPath
    }
    
    /**
     * Checks if a model is currently loaded.
     * @return true if a model is loaded, false otherwise
     */
    fun isModelLoaded(): Boolean {
        return currentInterpreter != null
    }
    
    /**
     * Closes the current model and frees memory.
     */
    fun closeCurrentModel() {
        currentInterpreter?.close()
        currentInterpreter = null
        currentModelPath = null
        Log.d(TAG, "Model closed")
    }
    
    /**
     * Closes the model manager and releases all resources.
     */
    fun close() {
        closeCurrentModel()
    }
    
    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
