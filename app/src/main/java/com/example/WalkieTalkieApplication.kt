package com.example

import android.app.Application
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Base Application class for Offline Walkie-Talkie.
 */
class WalkieTalkieApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        checkGooglePlayServices()
    }

    fun isGooglePlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun checkGooglePlayServices() {
        val isAvailable = isGooglePlayServicesAvailable()
        Log.d("WalkieTalkieApp", "Google Play Services availability: $isAvailable")
    }
}
