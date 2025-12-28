package com.example.kisandost.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
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
import java.io.ByteArrayOutputStream
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
    val cameraPermissionState = rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.CAMERA))
    
    var diagnosisEngine by remember { mutableStateOf<DiagnosisEngine?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    val isCapturing by viewModel.isCapturing.collectAsState()
    val selectedCrop by viewModel.selectedCrop.collectAsState()
    
    LaunchedEffect(Unit) {
        diagnosisEngine = DiagnosisEngine(context)
        diagnosisEngine?.switchCropModel(selectedCrop)
    }
    
    LaunchedEffect(selectedCrop) {
        diagnosisEngine?.switchCropModel(selectedCrop)
    }
    
    DisposableEffect(Unit) {
        onDispose { diagnosisEngine?.close() }
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = KisanGreen,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        Text(text = stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
        
        if (!cameraPermissionState.allPermissionsGranted) {
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { cameraPermissionState.launchMultiplePermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = KisanGreen)) {
                Text(text = stringResource(R.string.grant_camera_permission), color = KisanWhite)
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                    onScanningStateChanged = { isScanning = it },
                    isCapturing = isCapturing
                )
                
                CropSelector(
                    selectedCrop = selectedCrop,
                    onCropSelected = { viewModel.setSelectedCrop(it) },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.captureImage() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KisanGreen),
                enabled = !isScanning
            ) {
                Text(text = stringResource(R.string.capture_diagnose), color = KisanWhite)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        BannerAdView()
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
    var lastAnalysisTime by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(isCapturing) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        
        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        
        if (isCapturing) {
            analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAnalysisTime >= 333L) {
                    lastAnalysisTime = currentTime
                    imageProxyToBitmap(imageProxy)?.let { bmp ->
                        diagnosisEngine?.diagnose(bmp, selectedCrop)?.let {
                            onScanningStateChanged(true)
                            onDiagnosisComplete(it)
                        }
                    }
                }
                imageProxy.close()
            }
        }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

// Helper Functions moved outside scope to fix unresolved reference errors
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        val nv21 = ByteArray(yBuffer.remaining() + uBuffer.remaining() + vBuffer.remaining())
        yBuffer.get(nv21, 0, yBuffer.remaining())
        vBuffer.get(nv21, yBuffer.remaining(), vBuffer.remaining())
        uBuffer.get(nv21, yBuffer.remaining() + vBuffer.remaining(), uBuffer.remaining())
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        android.graphics.BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    } catch (e: Exception) { null }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
}

@Composable
fun CropSelector(selectedCrop: CropType, onCropSelected: (CropType) -> Unit, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CropType.getAllCrops().forEach { crop ->
                CropIconButton(crop = crop, isSelected = crop == selectedCrop, onClick = { onCropSelected(crop) })
            }
        }
    }
}

@Composable
fun CropIconButton(crop: CropType, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val drawableId = context.resources.getIdentifier(crop.drawableResourceName, "drawable", context.packageName)
    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(if (isSelected) KisanGreen else Color.Transparent, CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        if (drawableId != 0) Image(painter = painterResource(id = drawableId), contentDescription = crop.displayName, modifier = Modifier.size(48.dp))
    }
}