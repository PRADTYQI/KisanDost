package com.example.kisandost.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kisandost.R
import com.example.kisandost.ads.BannerAdView
import com.example.kisandost.diagnosis.CropType
import com.example.kisandost.diagnosis.DiagnosisEngine
import com.example.kisandost.ui.theme.KisanGreen
import com.example.kisandost.ui.theme.KisanWhite
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA)
    )
    
    var diagnosisEngine by remember { mutableStateOf<DiagnosisEngine?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    val isCapturing by viewModel.isCapturing.collectAsState()
    val selectedCrop by viewModel.selectedCrop.collectAsState()
    
    // Initialize diagnosis engine and load initial model
    LaunchedEffect(Unit) {
        diagnosisEngine = DiagnosisEngine(context)
        diagnosisEngine?.switchCropModel(selectedCrop)
    }
    
    // Switch model when crop changes
    LaunchedEffect(selectedCrop) {
        diagnosisEngine?.switchCropModel(selectedCrop)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            diagnosisEngine?.close()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = KisanGreen,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (!cameraPermissionState.allPermissionsGranted) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.camera_permission_required),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cameraPermissionState.launchMultiplePermissionRequest() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KisanGreen
                )
            ) {
                Text(
                    text = stringResource(R.string.grant_camera_permission),
                    color = KisanWhite
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Full-screen Camera Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RealTimeCameraPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    diagnosisEngine = diagnosisEngine,
                    selectedCrop = selectedCrop,
                    onDiagnosisComplete = { result ->
                        viewModel.setDiagnosisResult(result)
                        viewModel.resetCaptureState()
                        navController.navigate("results")
                        isScanning = false
                    },
                    onScanningStateChanged = { scanning ->
                        isScanning = scanning
                    },
                    isCapturing = isCapturing
                )
                
                // Crop Selector at the bottom of viewfinder
                CropSelector(
                    selectedCrop = selectedCrop,
                    onCropSelected = { cropType ->
                        viewModel.setSelectedCrop(cropType)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                
                // Scanning indicator
                if (isScanning) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = KisanGreen
                            )
                            Text(
                                text = stringResource(R.string.scanning_plant),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Capture Button
            Button(
                onClick = {
                    viewModel.captureImage()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KisanGreen
                ),
                enabled = !isScanning
            ) {
                Text(
                    text = stringResource(R.string.capture_diagnose),
                    color = KisanWhite
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Banner Ad at the bottom
        BannerAdView()
    }
}

@Composable
fun CropSelector(
    selectedCrop: CropType,
    onCropSelected: (CropType) -> Unit,
    modifier: Modifier = Modifier
) {
    val crops = CropType.getAllCrops()
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            crops.forEach { crop ->
                CropIconButton(
                    crop = crop,
                    isSelected = crop == selectedCrop,
                    onClick = { onCropSelected(crop) }
                )
            }
        }
    }
}

@Composable
fun CropIconButton(
    crop: CropType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val drawableId = context.resources.getIdentifier(
        crop.drawableResourceName, // Use exact case as file name
        "drawable",
        context.packageName
    )
    
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) KisanGreen else MaterialTheme.colorScheme.surface,
                CircleShape
            )
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) KisanWhite else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (drawableId != 0) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = crop.displayName,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Fallback text if drawable not found
            Text(
                text = crop.displayName.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = if (isSelected) KisanWhite else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RealTimeCameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    diagnosisEngine: DiagnosisEngine?,
    selectedCrop: CropType,
    onDiagnosisComplete: (com.example.kisandost.diagnosis.DiagnosisResult) -> Unit,
    onScanningStateChanged: (Boolean) -> Unit,
    isCapturing: Boolean
) {
    val previewView = remember { PreviewView(context) }
    var imageAnalyzer by remember { mutableStateOf<ImageAnalysis?>(null) }
    var lastAnalysisTime by remember { mutableLongStateOf(0L) }
    val analysisInterval = 333L // ~3 times per second (1000ms / 3)
    
    LaunchedEffect(previewView, isCapturing) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image Analyzer for real-time processing (3 fps) - only when capturing
        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
        
        if (isCapturing) {
            analyzer.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                ImageAnalysis.Analyzer { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastAnalysisTime >= analysisInterval) {
                        lastAnalysisTime = currentTime
                        
                        // Convert ImageProxy to Bitmap
                        val bitmap = imageProxyToBitmap(imageProxy)
                        bitmap?.let { bmp ->
                            diagnosisEngine?.let { engine ->
                                val result = engine.diagnose(bmp, selectedCrop)
                                result?.let {
                                    onScanningStateChanged(true)
                                    onDiagnosisComplete(it)
                                }
                            }
                        }
                    }
                    imageProxy.close()
                }
            )
        } else {
            analyzer.clearAnalyzer()
        }
        
        imageAnalyzer = analyzer
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analyzer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            imageAnalyzer?.clearAnalyzer()
        }
    }
    
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = android.graphics.YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
            90,
            out
        )
        val imageBytes = out.toByteArray()
        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(this)
        )
    }
}
