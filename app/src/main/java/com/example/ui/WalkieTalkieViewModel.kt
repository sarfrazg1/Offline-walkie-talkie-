package com.example.ui

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WalkieTalkieApplication
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.SoundEffects
import com.example.config.AppConfig
import com.example.model.ActiveCallInfo
import com.example.model.AudioConfig
import com.example.model.AppSettings
import com.example.model.CallRecord
import com.example.model.CallState
import com.example.model.CallType
import com.example.model.ControlMessage
import com.example.model.DeviceEndpoint
import com.example.model.RadioState
import com.example.nearby.NearbyConnectionsManager
import com.example.service.WalkieTalkieService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.util.UUID

/**
 * Primary ViewModel orchestrating the offline walkie-talkie and offline two-way calling engine.
 * Handles half-duplex PTT, full-duplex offline phone calls, call signaling, ringtones,
 * background notifications, and persistent call history without internet or SIM cards.
 */
class WalkieTalkieViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WalkieTalkieVM"

    private val nearbyManager = NearbyConnectionsManager(application)
    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer()
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs: SharedPreferences = application.getSharedPreferences("offline_walkie_talkie_prefs", Context.MODE_PRIVATE)

    // Radio & PTT state flows
    private val _radioState = MutableStateFlow(RadioState.IDLE)
    val radioState: StateFlow<RadioState> = _radioState.asStateFlow()

    private val _currentSpeaker = MutableStateFlow<String?>(null)
    val currentSpeaker: StateFlow<String?> = _currentSpeaker.asStateFlow()

    private val _talkDurationSeconds = MutableStateFlow(0)
    val talkDurationSeconds: StateFlow<Int> = _talkDurationSeconds.asStateFlow()

    // Offline Calling state flows
    private val _activeCall = MutableStateFlow<ActiveCallInfo?>(null)
    val activeCall: StateFlow<ActiveCallInfo?> = _activeCall.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    // Audio & App configurations
    private val _audioConfig = MutableStateFlow(AudioConfig())
    val audioConfig: StateFlow<AudioConfig> = _audioConfig.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Radio hardware health
    private val _isBluetoothEnabled = MutableStateFlow(true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(true)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val _isGooglePlayServicesOk = MutableStateFlow(true)
    val isGooglePlayServicesOk: StateFlow<Boolean> = _isGooglePlayServicesOk.asStateFlow()

    // Service reference for foreground notification updates
    var walkieTalkieService: WalkieTalkieService? = null

    // Pass-through flows from NearbyManager
    val discoveredDevices = nearbyManager.discoveredDevices
    val connectedDevices = nearbyManager.connectedDevices
    val isAdvertising = nearbyManager.isAdvertising
    val isDiscovering = nearbyManager.isDiscovering
    val pendingAuthRequest = nearbyManager.pendingAuthRequest
    val radioErrorMessage = nearbyManager.radioErrorMessage

    // Audio Visualizer Wave level: combines micLevel and speakerLevel
    val visualizerLevel: StateFlow<Float> = MutableStateFlow(0f).also { flow ->
        viewModelScope.launch {
            combine(
                audioRecorder.micLevel,
                audioPlayer.speakerLevel,
                _radioState
            ) { mic, speaker, state ->
                when (state) {
                    RadioState.TRANSMITTING -> mic
                    RadioState.RECEIVING -> speaker
                    RadioState.IN_CALL -> if (mic > 0.05f) mic else speaker
                    else -> 0f
                }
            }.collect { level ->
                flow.value = level
            }
        }
    }

    private var activeAudioStreamOut: OutputStream? = null
    private var talkTimerJob: Job? = null
    private var callDurationJob: Job? = null
    private var callTimeoutJob: Job? = null
    private val pingSentTimestamps = mutableMapOf<String, Long>()

    private val _hasPermissions = MutableStateFlow(nearbyManager.hasRequiredPermissions())
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    init {
        loadCallHistory()
        checkHardwareRadios()
        setupNearbyCallbacks()
        if (nearbyManager.hasRequiredPermissions() && _isBluetoothEnabled.value) {
            startRadioEngine()
        }
    }

    private fun setupNearbyCallbacks() {
        nearbyManager.onStreamReceived = { endpointId, inputStream ->
            viewModelScope.launch {
                val peerName = connectedDevices.value.find { it.id == endpointId }?.name ?: "Remote Peer"
                Log.d(TAG, "Audio stream received from $peerName ($endpointId)")

                if (_activeCall.value?.state == CallState.ACTIVE_CALL) {
                    // Full-duplex continuous call audio
                    audioPlayer.playStream(
                        inputStream = inputStream,
                        sampleRate = _audioConfig.value.sampleRate,
                        onFinished = {}
                    )
                } else {
                    // Half-duplex PTT stream
                    _radioState.value = RadioState.RECEIVING
                    _currentSpeaker.value = peerName
                    updateServiceNotification()

                    audioPlayer.playStream(
                        inputStream = inputStream,
                        sampleRate = _audioConfig.value.sampleRate,
                        onFinished = {
                            viewModelScope.launch {
                                if (_radioState.value == RadioState.RECEIVING) {
                                    _radioState.value = RadioState.IDLE
                                    _currentSpeaker.value = null
                                    updateServiceNotification()
                                    if (_audioConfig.value.enableRogerBeep) {
                                        SoundEffects.playRogerBeep()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        nearbyManager.onControlMessageReceived = { endpointId, msg ->
            viewModelScope.launch {
                when (msg.type) {
                    // --- CALL SIGNALING MESSAGES ---
                    ControlMessage.TYPE_CALL_REQUEST -> {
                        handleIncomingCallRequest(endpointId, msg.senderName, msg.payload)
                    }
                    ControlMessage.TYPE_CALL_ACCEPT -> {
                        handleCallAcceptedByPeer(endpointId, msg.payload)
                    }
                    ControlMessage.TYPE_CALL_REJECT -> {
                        handleCallRejectedByPeer(msg.senderName)
                    }
                    ControlMessage.TYPE_CALL_BUSY -> {
                        handleCallBusyByPeer(msg.senderName)
                    }
                    ControlMessage.TYPE_CALL_END -> {
                        handleCallEndedByPeer()
                    }

                    // --- PTT WALKIE TALKIE MESSAGES ---
                    ControlMessage.TYPE_TALK_START -> {
                        if (_radioState.value != RadioState.IN_CALL && _radioState.value != RadioState.TRANSMITTING) {
                            _radioState.value = RadioState.RECEIVING
                            _currentSpeaker.value = msg.senderName
                            updateServiceNotification()
                        } else if (_radioState.value == RadioState.TRANSMITTING) {
                            val busyMsg = ControlMessage(
                                type = ControlMessage.TYPE_TALK_BUSY,
                                senderId = nearbyManager.connectedDevices.value.firstOrNull()?.id ?: "local",
                                senderName = _appSettings.value.callsign
                            )
                            nearbyManager.broadcastControlMessage(busyMsg)
                        }
                    }
                    ControlMessage.TYPE_TALK_END -> {
                        if (_radioState.value == RadioState.RECEIVING && _currentSpeaker.value == msg.senderName) {
                            _radioState.value = RadioState.IDLE
                            _currentSpeaker.value = null
                            updateServiceNotification()
                            if (_audioConfig.value.enableRogerBeep) {
                                SoundEffects.playRogerBeep()
                            }
                        }
                    }
                    ControlMessage.TYPE_TALK_BUSY -> {
                        SoundEffects.playBusyTone()
                        vibratePattern(longArrayOf(0, 100, 80, 100))
                    }
                    ControlMessage.TYPE_PING -> {
                        val pongMsg = ControlMessage(
                            type = ControlMessage.TYPE_PONG,
                            senderId = endpointId,
                            senderName = _appSettings.value.callsign,
                            payload = msg.payload
                        )
                        nearbyManager.broadcastControlMessage(pongMsg)
                    }
                    ControlMessage.TYPE_PONG -> {
                        val sentTime = pingSentTimestamps.remove(msg.payload)
                        if (sentTime != null) {
                            val rtt = SystemClock.elapsedRealtime() - sentTime
                            Log.d(TAG, "Ping response from $endpointId: ${rtt}ms")
                        }
                    }
                }
            }
        }

        nearbyManager.onPeerDisconnected = { endpointId ->
            viewModelScope.launch {
                if (_activeCall.value?.peerId == endpointId) {
                    endCall()
                }
                if (_radioState.value == RadioState.RECEIVING) {
                    _radioState.value = RadioState.IDLE
                    _currentSpeaker.value = null
                }
                updateServiceNotification()
            }
        }
    }

    // =========================================================================
    // FULL OFFLINE CALLING METHODS
    // =========================================================================

    /**
     * Initiates an outgoing offline call to a discovered or connected peer.
     */
    fun startCall(endpointId: String, peerName: String) {
        if (_activeCall.value != null || _radioState.value == RadioState.IN_CALL) {
            return
        }

        val callId = UUID.randomUUID().toString().take(8)
        _activeCall.value = ActiveCallInfo(
            callId = callId,
            peerId = endpointId,
            peerName = peerName,
            state = CallState.OUTGOING_RINGING
        )
        _radioState.value = RadioState.IN_CALL

        // Play audible ringback cadence for caller
        SoundEffects.startRingback()

        // Send CALL_REQ to peer
        val callReqMsg = ControlMessage(
            type = ControlMessage.TYPE_CALL_REQUEST,
            senderId = "local",
            senderName = _appSettings.value.callsign,
            payload = callId
        )
        nearbyManager.sendDirectControlMessage(endpointId, callReqMsg)

        // 30-second call timeout if peer doesn't answer
        callTimeoutJob?.cancel()
        callTimeoutJob = viewModelScope.launch {
            delay(30_000)
            if (_activeCall.value?.state == CallState.OUTGOING_RINGING) {
                SoundEffects.stopRingback()
                SoundEffects.playCallEndTone()
                recordCallHistory(peerName, endpointId, CallType.OUTGOING, 0)
                _activeCall.value = null
                _radioState.value = RadioState.IDLE
                updateServiceNotification()
            }
        }

        updateServiceNotification()
    }

    private fun handleIncomingCallRequest(callerEndpointId: String, callerName: String, callId: String) {
        // If already in a call or transmitting, signal BUSY
        if (_activeCall.value != null || _radioState.value == RadioState.TRANSMITTING) {
            val busyMsg = ControlMessage(
                type = ControlMessage.TYPE_CALL_BUSY,
                senderId = "local",
                senderName = _appSettings.value.callsign,
                payload = callId
            )
            nearbyManager.sendDirectControlMessage(callerEndpointId, busyMsg)
            recordCallHistory(callerName, callerEndpointId, CallType.MISSED, 0)
            return
        }

        _activeCall.value = ActiveCallInfo(
            callId = callId,
            peerId = callerEndpointId,
            peerName = callerName,
            state = CallState.INCOMING_RINGING
        )
        _radioState.value = RadioState.IN_CALL

        // Play incoming call ringtone and vibrate
        SoundEffects.startRingtone()
        vibratePattern(longArrayOf(0, 1000, 1000, 1000, 1000))

        // Display High-Priority Call Notification (with Full-Screen Intent where allowed)
        walkieTalkieService?.showIncomingCallNotification(callerName, callId)

        // 30-second ringing timeout
        callTimeoutJob?.cancel()
        callTimeoutJob = viewModelScope.launch {
            delay(30_000)
            if (_activeCall.value?.state == CallState.INCOMING_RINGING) {
                SoundEffects.stopRingtone()
                walkieTalkieService?.cancelIncomingCallNotification()
                recordCallHistory(callerName, callerEndpointId, CallType.MISSED, 0)
                _activeCall.value = null
                _radioState.value = RadioState.IDLE
                updateServiceNotification()
            }
        }

        updateServiceNotification()
    }

    /**
     * Accepts an incoming call: establishes two-way voice communication.
     */
    fun acceptCall() {
        val current = _activeCall.value ?: return
        if (current.state != CallState.INCOMING_RINGING) return

        callTimeoutJob?.cancel()
        callTimeoutJob = null

        SoundEffects.stopRingtone()
        walkieTalkieService?.cancelIncomingCallNotification()

        // Send CALL_ACCEPT to caller
        val acceptMsg = ControlMessage(
            type = ControlMessage.TYPE_CALL_ACCEPT,
            senderId = "local",
            senderName = _appSettings.value.callsign,
            payload = current.callId
        )
        nearbyManager.sendDirectControlMessage(current.peerId, acceptMsg)

        // Enter active two-way call
        startActiveCallAudio(current)
    }

    private fun handleCallAcceptedByPeer(peerEndpointId: String, callId: String) {
        val current = _activeCall.value ?: return
        if (current.state != CallState.OUTGOING_RINGING) return

        callTimeoutJob?.cancel()
        callTimeoutJob = null

        SoundEffects.stopRingback()

        // Enter active two-way call
        startActiveCallAudio(current)
    }

    private fun startActiveCallAudio(callInfo: ActiveCallInfo) {
        _activeCall.value = callInfo.copy(state = CallState.ACTIVE_CALL, durationSeconds = 0)
        _radioState.value = RadioState.IN_CALL

        // Open outgoing microphone audio stream pipe directly to peer
        val outStream = nearbyManager.createDirectAudioStream(callInfo.peerId)
        activeAudioStreamOut = outStream

        if (outStream != null) {
            audioRecorder.startRecording(
                config = _audioConfig.value,
                outputStream = outStream,
                onStopped = {}
            )
        }

        // Start call duration timer
        callDurationJob?.cancel()
        callDurationJob = viewModelScope.launch {
            while (isActive && _activeCall.value?.state == CallState.ACTIVE_CALL) {
                delay(1000)
                val updatedDuration = (_activeCall.value?.durationSeconds ?: 0) + 1
                _activeCall.value = _activeCall.value?.copy(durationSeconds = updatedDuration)
            }
        }

        updateServiceNotification()
    }

    /**
     * Rejects an incoming call request.
     */
    fun rejectCall() {
        val current = _activeCall.value ?: return
        callTimeoutJob?.cancel()
        callTimeoutJob = null

        SoundEffects.stopRingtone()
        walkieTalkieService?.cancelIncomingCallNotification()

        val rejMsg = ControlMessage(
            type = ControlMessage.TYPE_CALL_REJECT,
            senderId = "local",
            senderName = _appSettings.value.callsign,
            payload = current.callId
        )
        nearbyManager.sendDirectControlMessage(current.peerId, rejMsg)

        recordCallHistory(current.peerName, current.peerId, CallType.REJECTED, 0)
        _activeCall.value = null
        _radioState.value = RadioState.IDLE
        updateServiceNotification()
    }

    private fun handleCallRejectedByPeer(peerName: String) {
        val current = _activeCall.value ?: return
        callTimeoutJob?.cancel()
        callTimeoutJob = null

        SoundEffects.stopRingback()
        SoundEffects.playCallEndTone()

        recordCallHistory(peerName, current.peerId, CallType.OUTGOING, 0)
        _activeCall.value = null
        _radioState.value = RadioState.IDLE
        updateServiceNotification()
    }

    private fun handleCallBusyByPeer(peerName: String) {
        val current = _activeCall.value ?: return
        callTimeoutJob?.cancel()
        callTimeoutJob = null

        SoundEffects.stopRingback()
        SoundEffects.playBusyTone()

        recordCallHistory(peerName, current.peerId, CallType.OUTGOING, 0)
        _activeCall.value = null
        _radioState.value = RadioState.IDLE
        updateServiceNotification()
    }

    /**
     * Terminates an ongoing call or cancels an outgoing call.
     */
    fun endCall() {
        val current = _activeCall.value ?: return
        callTimeoutJob?.cancel()
        callTimeoutJob = null
        callDurationJob?.cancel()
        callDurationJob = null

        SoundEffects.stopRingtone()
        SoundEffects.stopRingback()
        SoundEffects.playCallEndTone()
        walkieTalkieService?.cancelIncomingCallNotification()

        // Send CALL_END to peer
        val endMsg = ControlMessage(
            type = ControlMessage.TYPE_CALL_END,
            senderId = "local",
            senderName = _appSettings.value.callsign,
            payload = current.callId
        )
        nearbyManager.sendDirectControlMessage(current.peerId, endMsg)

        // Stop audio transmission & playback
        audioRecorder.stopRecording()
        audioPlayer.stopPlayback()
        try {
            activeAudioStreamOut?.close()
        } catch (ignored: Exception) {}
        activeAudioStreamOut = null

        val callType = if (current.state == CallState.ACTIVE_CALL) {
            CallType.OUTGOING
        } else {
            CallType.OUTGOING
        }
        recordCallHistory(current.peerName, current.peerId, callType, current.durationSeconds)

        _activeCall.value = null
        _radioState.value = RadioState.IDLE
        updateServiceNotification()
    }

    private fun handleCallEndedByPeer() {
        val current = _activeCall.value ?: return
        callTimeoutJob?.cancel()
        callTimeoutJob = null
        callDurationJob?.cancel()
        callDurationJob = null

        SoundEffects.stopRingtone()
        SoundEffects.stopRingback()
        SoundEffects.playCallEndTone()
        walkieTalkieService?.cancelIncomingCallNotification()

        audioRecorder.stopRecording()
        audioPlayer.stopPlayback()
        try {
            activeAudioStreamOut?.close()
        } catch (ignored: Exception) {}
        activeAudioStreamOut = null

        recordCallHistory(current.peerName, current.peerId, CallType.INCOMING, current.durationSeconds)

        _activeCall.value = null
        _radioState.value = RadioState.IDLE
        updateServiceNotification()
    }

    fun toggleMuteMic() {
        val current = _activeCall.value ?: return
        val newMuted = !current.isMuted
        audioRecorder.setMuted(newMuted)
        _activeCall.value = current.copy(isMuted = newMuted)
    }

    fun toggleSpeakerphone() {
        val current = _activeCall.value ?: return
        val newSpeaker = !current.isSpeakerphone
        audioManager.isSpeakerphoneOn = newSpeaker
        _activeCall.value = current.copy(isSpeakerphone = newSpeaker)
    }

    // =========================================================================
    // CALL HISTORY PERSISTENCE
    // =========================================================================

    private fun recordCallHistory(peerName: String, peerEndpointId: String, type: CallType, duration: Int) {
        val record = CallRecord(
            id = UUID.randomUUID().toString().take(8),
            peerName = peerName,
            peerEndpointId = peerEndpointId,
            type = type,
            timestamp = System.currentTimeMillis(),
            durationSeconds = duration
        )
        val updated = listOf(record) + _callHistory.value
        _callHistory.value = updated
        saveCallHistory(updated)
    }

    fun clearCallHistory() {
        _callHistory.value = emptyList()
        prefs.edit().remove("call_history_json").apply()
    }

    private fun loadCallHistory() {
        val jsonStr = prefs.getString("call_history_json", null) ?: return
        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<CallRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CallRecord(
                        id = obj.getString("id"),
                        peerName = obj.getString("peerName"),
                        peerEndpointId = obj.getString("peerEndpointId"),
                        type = CallType.valueOf(obj.getString("type")),
                        timestamp = obj.getLong("timestamp"),
                        durationSeconds = obj.getInt("durationSeconds")
                    )
                )
            }
            _callHistory.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load call history: ${e.message}")
        }
    }

    private fun saveCallHistory(list: List<CallRecord>) {
        try {
            val jsonArray = JSONArray()
            for (record in list.take(100)) {
                val obj = JSONObject().apply {
                    put("id", record.id)
                    put("peerName", record.peerName)
                    put("peerEndpointId", record.peerEndpointId)
                    put("type", record.type.name)
                    put("timestamp", record.timestamp)
                    put("durationSeconds", record.durationSeconds)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("call_history_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save call history: ${e.message}")
        }
    }

    // =========================================================================
    // HARDWARE & PTT RADIO METHODS
    // =========================================================================

    fun checkHardwareRadios() {
        val app = getApplication<WalkieTalkieApplication>()
        _isGooglePlayServicesOk.value = app.isGooglePlayServicesAvailable()
        _hasPermissions.value = nearbyManager.hasRequiredPermissions()

        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            _isBluetoothEnabled.value = btAdapter?.isEnabled == true
        } catch (e: Exception) {
            _isBluetoothEnabled.value = false
        }

        try {
            val wifiManager = app.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            _isWifiEnabled.value = wifiManager?.isWifiEnabled == true
        } catch (e: Exception) {
            _isWifiEnabled.value = false
        }
    }

    fun startRadioEngine() {
        checkHardwareRadios()
        _hasPermissions.value = nearbyManager.hasRequiredPermissions()
        if (!_hasPermissions.value) {
            Log.d(TAG, "Cannot start radio engine: required permissions not yet granted")
            return
        }
        if (!_isBluetoothEnabled.value) {
            Log.w(TAG, "Cannot start radio engine: Bluetooth is turned off")
            return
        }
        val callsign = _appSettings.value.callsign
        nearbyManager.startAdvertising(callsign)
        nearbyManager.startDiscovery()
        updateServiceNotification()
    }

    fun restartDiscovery() {
        checkHardwareRadios()
        _hasPermissions.value = nearbyManager.hasRequiredPermissions()
        if (!_hasPermissions.value) {
            Log.d(TAG, "Cannot restart discovery: required permissions not yet granted")
            return
        }
        viewModelScope.launch {
            nearbyManager.stopDiscovery()
            nearbyManager.stopAdvertising()
            // Provide a brief pause for the radio stack to teardown before restarting
            delay(700)
            checkHardwareRadios()
            if (_isBluetoothEnabled.value) {
                val callsign = _appSettings.value.callsign
                nearbyManager.startAdvertising(callsign)
                nearbyManager.startDiscovery()
            }
        }
    }

    /**
     * Push-To-Talk Button Pressed (Hold to talk)
     */
    fun onPttPressed(): Boolean {
        if (_activeCall.value != null) {
            // PTT disabled during active offline call
            return false
        }

        // Half-duplex arbitration: Check if channel is already occupied
        if (_radioState.value == RadioState.RECEIVING) {
            SoundEffects.playBusyTone()
            vibratePattern(longArrayOf(0, 80, 50, 80))
            return false
        }

        if (_radioState.value == RadioState.TRANSMITTING) {
            return true
        }

        // Mute playback to prevent local acoustic echo
        audioPlayer.stopPlayback()

        // Acquire talk lock
        _radioState.value = RadioState.TRANSMITTING
        _currentSpeaker.value = _appSettings.value.callsign
        _talkDurationSeconds.value = 0
        vibratePattern(longArrayOf(0, 45))

        // Play tactical click
        SoundEffects.playPttStartChime()

        // Send TALK_START packet to lock channel on peer phones
        val startMsg = ControlMessage(
            type = ControlMessage.TYPE_TALK_START,
            senderId = "local",
            senderName = _appSettings.value.callsign
        )
        nearbyManager.broadcastControlMessage(startMsg)

        // Open audio streaming pipe to all connected endpoints
        val streamOut = nearbyManager.createBroadcastAudioStream()
        activeAudioStreamOut = streamOut

        if (streamOut != null) {
            audioRecorder.startRecording(
                config = _audioConfig.value,
                outputStream = streamOut,
                onStopped = {}
            )
        }

        // Start talk duration timer
        talkTimerJob?.cancel()
        talkTimerJob = viewModelScope.launch {
            while (isActive && _radioState.value == RadioState.TRANSMITTING) {
                delay(1000)
                _talkDurationSeconds.value += 1
            }
        }

        updateServiceNotification()
        return true
    }

    /**
     * Push-To-Talk Button Released (Release to listen)
     */
    fun onPttReleased() {
        if (_radioState.value != RadioState.TRANSMITTING) return

        talkTimerJob?.cancel()
        talkTimerJob = null

        audioRecorder.stopRecording()
        try {
            activeAudioStreamOut?.close()
        } catch (ignored: Exception) {}
        activeAudioStreamOut = null

        // Broadcast TALK_END
        val endMsg = ControlMessage(
            type = ControlMessage.TYPE_TALK_END,
            senderId = "local",
            senderName = _appSettings.value.callsign
        )
        nearbyManager.broadcastControlMessage(endMsg)

        _radioState.value = RadioState.IDLE
        _currentSpeaker.value = null
        _talkDurationSeconds.value = 0

        vibratePattern(longArrayOf(0, 30))

        if (_audioConfig.value.enableRogerBeep) {
            SoundEffects.playRogerBeep()
        }

        updateServiceNotification()
    }

    fun connectToEndpoint(endpointId: String) {
        nearbyManager.requestConnection(_appSettings.value.callsign, endpointId)
    }

    fun acceptPendingAuth() {
        val pending = pendingAuthRequest.value ?: return
        nearbyManager.acceptConnection(pending.id)
        SoundEffects.playConnectedChime()
        vibratePattern(longArrayOf(0, 60, 40, 60))
    }

    fun rejectPendingAuth() {
        val pending = pendingAuthRequest.value ?: return
        nearbyManager.rejectConnection(pending.id)
    }

    fun disconnectEndpoint(endpointId: String) {
        nearbyManager.disconnect(endpointId)
        updateServiceNotification()
    }

    fun disconnectAllEndpoints() {
        nearbyManager.disconnectAll()
        updateServiceNotification()
    }

    fun sendPing(endpointId: String) {
        val pingId = System.currentTimeMillis().toString()
        pingSentTimestamps[pingId] = SystemClock.elapsedRealtime()
        val pingMsg = ControlMessage(
            type = ControlMessage.TYPE_PING,
            senderId = "local",
            senderName = _appSettings.value.callsign,
            payload = pingId
        )
        nearbyManager.broadcastControlMessage(pingMsg)
    }

    fun updateCallsign(newCallsign: String) {
        val trimmed = newCallsign.trim()
        if (trimmed.isNotEmpty()) {
            _appSettings.value = _appSettings.value.copy(callsign = trimmed)
            restartDiscovery()
        }
    }

    fun updateLanguage(langCode: String) {
        _appSettings.value = _appSettings.value.copy(language = langCode)
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        _appSettings.value = _appSettings.value.copy(keepScreenOn = keepOn)
    }

    fun updateAudioConfig(newConfig: AudioConfig) {
        _audioConfig.value = newConfig
    }

    private fun updateServiceNotification() {
        val connectedCount = connectedDevices.value.size
        val statusText = when (connectedCount) {
            0 -> "Ready • 0 Peers"
            1 -> "1 Peer Connected"
            else -> "$connectedCount Peers in Group"
        }
        val isTransmitting = _radioState.value == RadioState.TRANSMITTING || _activeCall.value?.state == CallState.ACTIVE_CALL
        val speakerText = when {
            _activeCall.value?.state == CallState.ACTIVE_CALL -> "In Call with ${_activeCall.value?.peerName}"
            _activeCall.value?.state == CallState.OUTGOING_RINGING -> "Calling ${_activeCall.value?.peerName}…"
            _activeCall.value?.state == CallState.INCOMING_RINGING -> "Incoming Call: ${_activeCall.value?.peerName}"
            _radioState.value == RadioState.TRANSMITTING -> "Transmitting Live Voice"
            _radioState.value == RadioState.RECEIVING -> "${_currentSpeaker.value ?: "Peer"} is speaking"
            _radioState.value == RadioState.BUSY -> "Channel Busy"
            else -> "Channel Clear"
        }
        walkieTalkieService?.updateNotification(statusText, speakerText, isTransmitting)
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Vibrator not available
        }
    }

    override fun onCleared() {
        super.onCleared()
        endCall()
        audioRecorder.stopRecording()
        audioPlayer.stopPlayback()
        nearbyManager.stopDiscovery()
        nearbyManager.stopAdvertising()
        nearbyManager.disconnectAll()
    }
}
