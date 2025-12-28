package com.example.kisandost.diagnosis

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class DiagnosisEngine(
    context: Context,
    modelFileName: String,
    private val inputImageWidth: Int = 224,
    private val inputImageHeight: Int = 224
) {

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context, modelFileName))
    }

    private fun loadModelFile(context: Context, fileName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun diagnose(bitmap: Bitmap): FloatArray {
        // Resize bitmap to model input size
        val resizedBitmap =
            Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)

        // Convert bitmap to ByteBuffer
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // Determine number of output classes dynamically
        val outputShape = interpreter.getOutputTensor(0).shape()
        val numClasses = if (outputShape.size > 1) outputShape[1] else 1

        // Prepare output buffer
        val outputBuffer = Array(1) { FloatArray(numClasses) }

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Return prediction probabilities
        return outputBuffer[0]
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer =
            ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(
            intValues,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )

        var pixelIndex = 0
        for (y in 0 until inputImageHeight) {
            for (x in 0 until inputImageWidth) {
                val pixelValue = intValues[pixelIndex++]

                // Normalize RGB to [0,1]
                byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    fun close() {
        interpreter.close()
    }
}
