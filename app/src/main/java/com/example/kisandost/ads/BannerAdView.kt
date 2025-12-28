package com.example.kisandost.ads

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kisandost.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adUnitId = stringResource(R.string.admob_banner_test_id)
    
    // Get adaptive ad size based on screen width
    val adSize = remember {
        getAdaptiveAdSize(context)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Gets the appropriate adaptive banner ad size for the current context.
 * Falls back to standard banner if adaptive sizing is not available.
 */
private fun getAdaptiveAdSize(context: Context): AdSize {
    return try {
        if (context is Activity) {
            // Calculate screen width in dp
            val displayMetrics = context.resources.displayMetrics
            val widthPixels = displayMetrics.widthPixels
            val density = displayMetrics.density
            val widthDp = (widthPixels / density).toInt()
            
            // Use adaptive banner size (minimum width 320dp)
            AdSize.getAnchoredAdaptiveBannerAdSize(context, widthDp.coerceAtLeast(320))
        } else {
            AdSize.BANNER
        }
    } catch (e: Exception) {
        // Fallback to standard banner if adaptive size fails
        AdSize.BANNER
    }
}


