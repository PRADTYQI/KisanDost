package com.example.kisandost.admob

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager private constructor() {
    private var rewardedAd: RewardedAd? = null
    
    companion object {
        @Volatile
        private var INSTANCE: RewardedAdManager? = null
        
        fun getInstance(): RewardedAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RewardedAdManager().also { INSTANCE = it }
            }
        }
        
        // Use test ad unit ID during development
        // Replace with your actual ad unit ID for production
        // Test ID: ca-app-pub-3940256099942544/5224354917
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
    
    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                }
                
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
            }
        )
    }
    
    fun showRewardedAd(
        activity: Activity,
        onAdRewarded: () -> Unit,
        onAdClosed: () -> Unit,
        onAdFailedToShow: () -> Unit
    ) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    onAdClosed()
                    // Load the next rewarded ad
                    loadRewardedAd(activity)
                }
                
                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                    rewardedAd = null
                    onAdFailedToShow()
                }
            }
            
            ad.show(activity) { rewardItem ->
                // User earned reward
                onAdRewarded()
            }
        } ?: run {
            onAdFailedToShow()
            // Try to load a new ad
            loadRewardedAd(activity)
        }
    }
    
    fun isAdLoaded(): Boolean = rewardedAd != null
}


