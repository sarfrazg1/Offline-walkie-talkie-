package com.example.ui

/**
 * In-app dynamic bilingual localization strings dictionary.
 * Supports switching between English and Urdu instantly within the application.
 */
object LocalizationManager {

    fun getString(key: String, language: String, vararg args: Any): String {
        val isUrdu = language == "ur"
        val raw = when (key) {
            "app_title" -> if (isUrdu) "آف لائن واکی ٹاکی" else "OFFLINE WALKIE TALKIE"
            "nav_radio" -> if (isUrdu) "ریڈیو" else "Radio"
            "nav_calls" -> if (isUrdu) "کالز" else "Calls"
            "nav_radar" -> if (isUrdu) "رڈار" else "Radar"
            "nav_group" -> if (isUrdu) "گروپ" else "Group"
            "nav_audio" -> if (isUrdu) "آڈیو" else "Audio"
            "nav_settings" -> if (isUrdu) "سیٹنگز" else "Settings"
            "nav_about" -> if (isUrdu) "معلومات" else "About"

            "channel_idle" -> if (isUrdu) "چینل خالی ہے • بات کرنے کے لیے تیار" else "Channel Clear • Ready to Transmit"
            "channel_transmitting" -> if (isUrdu) "براہ راست آواز نشر ہو رہی ہے" else "TRANSMITTING AUDIO LIVE"
            "channel_receiving" -> if (isUrdu) "%s بول رہے ہیں" else "%s IS SPEAKING"
            "channel_busy" -> if (isUrdu) "چینل مصروف ہے • برائے مہربانی انتظار کریں" else "Channel Busy • Please Wait"
            "in_call" -> if (isUrdu) "%s کے ساتھ کال جاری ہے" else "IN CALL WITH %s"
            
            "ptt_hold" -> if (isUrdu) "بولنے کے لیے دبا کر رکھیں" else "PRESS & HOLD TO TALK"
            "ptt_release" -> if (isUrdu) "سننے کے لیے چھوڑ دیں" else "RELEASE TO LISTEN"
            "ptt_locked" -> if (isUrdu) "چینل مقفل ہے" else "CHANNEL LOCKED"

            "connected_peers" -> if (isUrdu) "%d منسلک ڈیوائسز" else "%d Connected Peer(s)"
            "no_peers" -> if (isUrdu) "کوئی ڈیوائس منسلک نہیں۔ جوڑنے کے لیے رڈار کھولیں۔" else "No Peers Connected • Open Radar to Connect"

            // Calling
            "call" -> if (isUrdu) "کال کریں" else "Call"
            "calling" -> if (isUrdu) "کال ہو رہی ہے…" else "Calling…"
            "ringing" -> if (isUrdu) "گھنٹی بج رہی ہے…" else "Ringing…"
            "incoming_call" -> if (isUrdu) "آنے والی آف لائن کال" else "Incoming Offline Call"
            "incoming_call_sub" -> if (isUrdu) "%s آپ کو کال کر رہے ہیں…" else "%s is calling you…"
            "active_call" -> if (isUrdu) "فعال آف لائن کال" else "Active Offline Call"
            "end_call" -> if (isUrdu) "کال ختم کریں" else "End Call"
            "mute" -> if (isUrdu) "میوٹ" else "Mute"
            "unmute" -> if (isUrdu) "آواز کھولیں" else "Unmute"
            "speaker" -> if (isUrdu) "اسپیکر" else "Speaker"
            "call_history" -> if (isUrdu) "کال ہسٹری" else "Call History"
            "no_call_history" -> if (isUrdu) "کوئی پچھلی کال نہیں ہے" else "No Call History"
            "clear_history" -> if (isUrdu) "ہسٹری صاف کریں" else "Clear History"
            "call_back" -> if (isUrdu) "دوبارہ کال کریں" else "Call Back"
            "missed_call" -> if (isUrdu) "مسڈ کال" else "Missed Call"
            "declined_call" -> if (isUrdu) "مسترد شدہ" else "Declined Call"
            "outgoing_call" -> if (isUrdu) "جانے والی کال" else "Outgoing Call"
            "incoming_call_label" -> if (isUrdu) "موصول شدہ کال" else "Incoming Call"
            "offline_calling_desc" -> if (isUrdu) "براہ راست فون سے فون کال۔ انٹرنیٹ، سم کارڈ، یا سرور کی ضرورت نہیں۔" else "Direct phone-to-phone voice call. Zero Internet, SIM, or servers needed."

            "radar_title" -> if (isUrdu) "ڈیوائس ڈسکوری رڈار" else "Device Discovery Radar"
            "radar_scanning" -> if (isUrdu) "قریبی واکی ٹاکیز تلاش کی جا رہی ہیں…" else "Scanning for Nearby Walkie-Talkies…"
            "radar_advertising" -> if (isUrdu) "اپنی موجودگی نشر ہو رہی ہے" else "Broadcasting Presence (Advertising)"
            "connect" -> if (isUrdu) "رابطہ کریں" else "Connect"
            "disconnect" -> if (isUrdu) "منقطع کریں" else "Disconnect"
            "connecting" -> if (isUrdu) "رابطہ ہو رہا ہے…" else "Connecting…"
            "connected" -> if (isUrdu) "منسلک" else "Connected"

            "auth_title" -> if (isUrdu) "رابطے کی سیکیورٹی تصدیق" else "Security Connection Auth"
            "auth_desc" -> if (isUrdu) "تصدیق کریں کہ دونوں فونز پر یہ 4 ہندسوں کا کوڈ یکساں ہے:" else "Confirm this 4-digit authentication code matches on both devices:"
            "accept" -> if (isUrdu) "قبول کریں" else "Accept"
            "reject" -> if (isUrdu) "رد کریں" else "Reject"

            "group_title" -> if (isUrdu) "فعال گروپ کمیونیکیشن" else "Active Group Communication"
            "group_broadcast_info" -> if (isUrdu) "آواز ایک وقت میں تمام منسلک آلات پر براہ راست نشر ہوتی ہے۔" else "Voice streams to all connected peers simultaneously in half-duplex mode."
            "disconnect_all" -> if (isUrdu) "سب منقطع کریں" else "Disconnect All"
            "ping" -> if (isUrdu) "پنگ" else "Ping"

            "audio_settings_title" -> if (isUrdu) "آڈیو اور ریڈیو سیٹنگز" else "Audio & Radio Settings"
            "sample_rate" -> if (isUrdu) "آڈیو سیمپل ریٹ" else "Audio Sample Rate"
            "echo_cancellation" -> if (isUrdu) "ایکو کینسلیشن (AEC)" else "Acoustic Echo Cancellation (AEC)"
            "noise_suppression" -> if (isUrdu) "شور کو کم کرنے کا نظام" else "Hardware Noise Suppression"
            "roger_beep" -> if (isUrdu) "راجر بیپ (آواز کا اختتامی سگنل)" else "Roger Beep (PTT Release Chime)"
            "mic_gain" -> if (isUrdu) "مائیکروفون گین بوسٹ" else "Microphone Gain Boost"

            "app_settings_title" -> if (isUrdu) "ایپلی کیشن سیٹنگز" else "Application Settings"
            "callsign" -> if (isUrdu) "میرا نام / کال سائن" else "My Callsign / Device Name"
            "language" -> if (isUrdu) "زبان منتخب کریں" else "App Language"
            "keep_screen_on" -> if (isUrdu) "ریڈیو کے دوران اسکرین آن رکھیں" else "Keep Screen Awake During Radio"
            "radios_status" -> if (isUrdu) "ریڈیو ہارڈویئر اسٹیٹس" else "Radio Hardware Health"
            "wifi_enabled" -> if (isUrdu) "وائی فائی فعال ہے" else "Wi-Fi is Active"
            "wifi_disabled" -> if (isUrdu) "وائی فائی بند ہے - برائے مہربانی آن کریں" else "Wi-Fi Disabled • Please Enable"
            "bt_enabled" -> if (isUrdu) "بلوٹوتھ فعال ہے" else "Bluetooth is Active"
            "bt_disabled" -> if (isUrdu) "بلوٹوتھ بند ہے - برائے مہربانی آن کریں" else "Bluetooth Disabled • Please Enable"

            "about_title" -> if (isUrdu) "معلومات اور ڈویلپر رابطہ" else "About & Developer Contact"
            "developer_label" -> if (isUrdu) "ڈویلپر" else "Developed by"
            "whatsapp_contact" -> if (isUrdu) "واٹس ایپ پر رابطہ کریں" else "Chat on WhatsApp"
            "field_test_guide" -> if (isUrdu) "2 فونز پر حقیقی ٹیسٹنگ کا طریقہ کار" else "2-Phone Real Testing Procedure"
            else -> key
        }
        return if (args.isNotEmpty()) String.format(raw, *args) else raw
    }
}
