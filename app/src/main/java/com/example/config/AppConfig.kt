package com.example.config

/**
 * Centralized configuration file for the Offline Walkie-Talkie application.
 * All developer credentials, contact information, service IDs, and radio defaults
 * are maintained here for easy editing and maintenance.
 */
object AppConfig {
    const val APP_NAME = "OFFLINE WALKIE TALKIE"
    
    // Official Developer Information
    const val DEVELOPER_NAME = "Rao Muhammad Sarfraz"
    const val WHATSAPP_NUMBER = "+923360074976"
    const val WHATSAPP_LINK = "https://wa.me/923360074976"
    
    // Google Nearby Connections Service ID
    // Devices with the same Service ID discover and communicate with each other offline.
    const val SERVICE_ID = "com.aistudio.offline.walkietalkie"
    
    // Default Radio Audio Settings
    const val DEFAULT_SAMPLE_RATE = 16000 // 16 kHz: optimal balance of voice clarity and low bandwidth
    const val SAMPLE_RATE_8KHZ = 8000     // 8 kHz: ultra-range / low bandwidth
    const val SAMPLE_RATE_16KHZ = 16000   // 16 kHz: standard voice communications
    const val SAMPLE_RATE_44KHZ = 44100   // 44.1 kHz: high fidelity
    
    // Target maximum communication range in favorable outdoor conditions (informative)
    const val TARGET_OUTDOOR_RANGE_METERS = 500
    
    // Virtual radio channel frequency tag (aesthetic display for tactical feel)
    const val DEFAULT_CHANNEL_NAME = "CH-01"
    const val DEFAULT_FREQUENCY_MHZ = "446.006 MHz"
}
