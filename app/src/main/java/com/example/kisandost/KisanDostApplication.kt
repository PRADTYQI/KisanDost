package com.example.kisandost

import android.app.Application
import com.google.android.gms.ads.MobileAds

class KisanDostApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google AdMob SDK
        MobileAds.initialize(this) { initializationStatus ->
            // AdMob initialization complete
            // You can check initializationStatus.adapterStatusMap for adapter statuses
        }
    }
}


