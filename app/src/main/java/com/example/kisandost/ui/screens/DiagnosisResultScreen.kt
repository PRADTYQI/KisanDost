package com.example.kisandost.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
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
import com.example.kisandost.diagnosis.DiagnosisResult
import com.example.kisandost.safety.SafetyGuard
import com.example.kisandost.ui.theme.KisanGreen
import com.example.kisandost.ui.theme.KisanWhite
import com.example.kisandost.utils.NetworkUtils
import com.example.kisandost.utils.TTSManager
import java.util.Locale

@Composable
fun DiagnosisResultScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val diagnosisResult by viewModel.diagnosisResult.collectAsState()
    val ttsManager = remember { TTSManager(context) }
    val isSpeaking by ttsManager.isSpeaking.collectAsState()
    val isOnline = remember { NetworkUtils.isOnline(context) }
    val isHighSpeed = remember { NetworkUtils.isHighSpeedConnection(context) }
    
    val safetyGuard = remember { SafetyGuard.getInstance() }
    val rewardedAdManager = remember { RewardedAdManager.getInstance() }
    
    // Load rewarded ad on screen entry
    LaunchedEffect(Unit) {
        rewardedAdManager.loadRewardedAd(context)
    }
    
    // Cleanup TTS on exit
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }
    
    Scaffold(
        bottomBar = {
            BannerAdView()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Network Status Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isOnline) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = KisanWhite
                    ) {
                        Text(
                            text = stringResource(R.string.cached_result),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isHighSpeed) {
                    Badge(
                        containerColor = KisanGreen,
                        contentColor = KisanWhite
                    ) {
                        Text(
                            text = stringResource(R.string.online_mode),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Title
            Text(
                text = stringResource(R.string.diagnosis_result),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = KisanGreen,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            diagnosisResult?.let { result ->
                // Disease Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = KisanGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.disease_detected),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = KisanGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = result.disease,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.confidence,
                                        (result.confidence * 100).toInt()
                                    ),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            // Speaker Icon for TTS
                            IconButton(
                                onClick = {
                                    val ttsText = if (result.disease == "Healthy") {
                                        context.getString(R.string.tts_healthy_plant)
                                    } else {
                                        context.getString(
                                            R.string.tts_disease_detected,
                                            result.disease,
                                            (result.confidence * 100).toInt()
                                        )
                                    }
                                    ttsManager.speak(ttsText, "hi")
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = stringResource(R.string.listen_instructions),
                                    tint = if (isSpeaking) KisanGreen else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
                
                // Remedy Cards: Chemical, Organic, Traditional
                val remedies = result.remedies
                
                // Chemical Remedies Card
                if (remedies.chemical.isNotEmpty()) {
                    RemedyCard(
                        title = stringResource(R.string.remedy_chemical),
                        remedies = remedies.chemical,
                        cardColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        titleColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // Organic Remedies Card
                if (remedies.organic.isNotEmpty()) {
                    RemedyCard(
                        title = stringResource(R.string.remedy_organic),
                        remedies = remedies.organic,
                        cardColor = KisanGreen.copy(alpha = 0.1f),
                        titleColor = KisanGreen,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // Traditional Remedies Card
                if (remedies.traditional.isNotEmpty()) {
                    RemedyCard(
                        title = stringResource(R.string.remedy_traditional),
                        remedies = remedies.traditional,
                        cardColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                        titleColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // Show message if no remedies available
                if (remedies.chemical.isEmpty() && remedies.organic.isEmpty() && remedies.traditional.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_remedies_available),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
                
                // Upload for Expert Review Button (if online and high speed)
                if (isOnline && isHighSpeed) {
                    Button(
                        onClick = {
                            // TODO: Implement cloud upload
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KisanGreen
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.upload_for_expert_review),
                            color = KisanWhite
                        )
                    }
                }
                
                // Get Expert Remedy Button (Triggers Rewarded Ad)
                Button(
                    onClick = {
                        if (context is Activity) {
                            rewardedAdManager.showRewardedAd(
                                activity = context,
                                onAdRewarded = {
                                    // Premium remedy already shown in remedy cards
                                    // Could expand with additional premium content here
                                },
                                onAdClosed = {},
                                onAdFailedToShow = {}
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.get_expert_remedy),
                        color = KisanWhite
                    )
                }
            } ?: run {
                // No result state
                Text(
                    text = stringResource(R.string.no_disease_detected),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun RemedyCard(
    title: String,
    remedies: List<String>,
    cardColor: Color,
    titleColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            remedies.forEachIndexed { index, remedy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "${index + 1}. ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Text(
                        text = remedy,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

