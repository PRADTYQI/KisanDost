package com.example.kisandost.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kisandost.R
import com.example.kisandost.admob.RewardedAdManager
import com.example.kisandost.ads.BannerAdView
import com.example.kisandost.ui.theme.KisanGreen
import com.example.kisandost.ui.theme.KisanWhite
import com.example.kisandost.utils.TTSManager

@Composable
fun DiagnosisResultScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val diagnosisResult by viewModel.diagnosisResult.collectAsState()
    val ttsManager = remember { TTSManager(context) }
    val isSpeaking by ttsManager.isSpeaking.collectAsState()
    val rewardedAdManager = remember { RewardedAdManager.getInstance() }
    
    LaunchedEffect(Unit) { rewardedAdManager.loadRewardedAd(context) }
    
    DisposableEffect(Unit) { onDispose { ttsManager.shutdown() } }
    
    Scaffold(bottomBar = { BannerAdView() }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.diagnosis_result),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = KisanGreen,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            diagnosisResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KisanGreen.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.disease_detected), style = MaterialTheme.typography.titleMedium, color = KisanGreen)
                                Text(text = result.disease, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(text = stringResource(R.string.confidence, (result.confidence * 100).toInt()), style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = { ttsManager.speak(result.disease, "hi") }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = if (isSpeaking) KisanGreen else Color.Gray)
                            }
                        }
                    }
                }
                
                // Render remedies safely
                RemedySection(stringResource(R.string.remedy_chemical), result.remedies.chemical, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                RemedySection(stringResource(R.string.remedy_organic), result.remedies.organic, KisanGreen.copy(alpha = 0.1f), KisanGreen)
                Spacer(modifier = Modifier.height(8.dp))
                RemedySection(stringResource(R.string.remedy_traditional), result.remedies.traditional, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.tertiary)

                Button(
                    onClick = { if (context is Activity) rewardedAdManager.showRewardedAd(context, {}, {}, {}) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.get_expert_remedy), color = KisanWhite)
                }
            }
        }
    }
}

@Composable
fun RemedySection(title: String, remedies: List<String>, cardColor: Color, titleColor: Color) {
    if (remedies.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = titleColor)
            remedies.forEachIndexed { i, remedy ->
                Text(text = "${i + 1}. $remedy", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}