package com.example.nearby

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.config.AppConfig
import com.example.model.ControlMessage
import com.example.model.DeviceEndpoint
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream

/**
 * High-reliability peer-to-peer wireless offline transport layer using Google Nearby Connections API.
 * Uses Strategy.P2P_CLUSTER for true multi-device mesh-like group walkie-talkie communication.
 */
class NearbyConnectionsManager(private val context: Context) {
    private val TAG = "NearbyConnections"
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    
    // Discovered and Connected Endpoints State
    private val _discoveredDevices = MutableStateFlow<List<DeviceEndpoint>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceEndpoint>> = _discoveredDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<DeviceEndpoint>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceEndpoint>> = _connectedDevices.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Radio Hardware & Stack Error Notification Flow
    private val _radioErrorMessage = MutableStateFlow<String?>(null)
    val radioErrorMessage: StateFlow<String?> = _radioErrorMessage.asStateFlow()

    // Pending Connection Request for Auth confirmation
    private val _pendingAuthRequest = MutableStateFlow<DeviceEndpoint?>(null)
    val pendingAuthRequest: StateFlow<DeviceEndpoint?> = _pendingAuthRequest.asStateFlow()

    // Callbacks for UI/Audio engine
    var onStreamReceived: ((endpointId: String, inputStream: InputStream) -> Unit)? = null
    var onControlMessageReceived: ((endpointId: String, message: ControlMessage) -> Unit)? = null
    var onPeerDisconnected: ((endpointId: String) -> Unit)? = null

    // Track active stream transfers
    private val activeStreams = mutableMapOf<Long, InputStream>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.STREAM -> {
                    val stream = payload.asStream()?.asInputStream()
                    if (stream != null) {
                        activeStreams[payload.id] = stream
                        onStreamReceived?.invoke(endpointId, stream)
                    }
                }
                Payload.Type.BYTES -> {
                    val bytes = payload.asBytes()
                    if (bytes != null) {
                        val message = ControlMessage.parse(bytes)
                        if (message != null) {
                            onControlMessageReceived?.invoke(endpointId, message)
                        }
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS ||
                update.status == PayloadTransferUpdate.Status.CANCELED ||
                update.status == PayloadTransferUpdate.Status.FAILURE
            ) {
                activeStreams.remove(update.payloadId)
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with $endpointId (${connectionInfo.endpointName}), auth: ${connectionInfo.authenticationDigits}")
            
            val device = DeviceEndpoint(
                id = endpointId,
                name = connectionInfo.endpointName,
                isConnecting = true,
                authDigits = connectionInfo.authenticationDigits
            )
            _pendingAuthRequest.value = device
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            _pendingAuthRequest.value = null
            if (result.status.isSuccess) {
                Log.d(TAG, "Connection SUCCESS with $endpointId")
                
                // Move from discovered or update connected
                val devName = _discoveredDevices.value.find { it.id == endpointId }?.name ?: "Peer"
                val connectedDevice = DeviceEndpoint(
                    id = endpointId,
                    name = devName,
                    isConnected = true,
                    isConnecting = false
                )
                
                _connectedDevices.value = _connectedDevices.value.filter { it.id != endpointId } + connectedDevice
                _discoveredDevices.value = _discoveredDevices.value.filter { it.id != endpointId }
            } else {
                Log.w(TAG, "Connection failed or rejected for $endpointId: ${result.status.statusCode}")
                _discoveredDevices.value = _discoveredDevices.value.map {
                    if (it.id == endpointId) it.copy(isConnecting = false) else it
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Peer disconnected: $endpointId")
            _connectedDevices.value = _connectedDevices.value.filter { it.id != endpointId }
            onPeerDisconnected?.invoke(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            if (_connectedDevices.value.none { it.id == endpointId }) {
                val newEndpoint = DeviceEndpoint(
                    id = endpointId,
                    name = info.endpointName,
                    isConnected = false,
                    isConnecting = false
                )
                _discoveredDevices.value = _discoveredDevices.value.filter { it.id != endpointId } + newEndpoint
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            _discoveredDevices.value = _discoveredDevices.value.filter { it.id != endpointId }
        }
    }

    fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBtScan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val hasBtAdv = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            val hasBtConn = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (!hasBtScan || !hasBtAdv || !hasBtConn) return false
        } else {
            val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasLoc) return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasWifi = ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
            if (!hasWifi) return false
        }
        return true
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            btAdapter?.isEnabled == true
        } catch (e: Exception) {
            false
        }
    }

    fun isWifiEnabled(): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.isWifiEnabled == true
        } catch (e: Exception) {
            false
        }
    }

    fun startAdvertising(localCallsign: String) {
        if (!hasRequiredPermissions()) {
            Log.d(TAG, "Cannot start advertising: required permissions not yet granted")
            return
        }

        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth radio is disabled; skipping advertising start")
            _radioErrorMessage.value = "Bluetooth is turned off. Please turn on Bluetooth in Settings to discover nearby walkie-talkies."
            _isAdvertising.value = false
            return
        }

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            localCallsign,
            AppConfig.SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started as $localCallsign")
            _isAdvertising.value = true
            _radioErrorMessage.value = null
        }.addOnFailureListener { e ->
            if (e is ApiException) {
                when (e.statusCode) {
                    ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING -> {
                        Log.d(TAG, "NearbyConnections already advertising as $localCallsign")
                        _isAdvertising.value = true
                        _radioErrorMessage.value = null
                        return@addOnFailureListener
                    }
                    ConnectionsStatusCodes.STATUS_RADIO_ERROR -> {
                        Log.w(TAG, "NearbyConnections STATUS_RADIO_ERROR (8007): Radio hardware disabled or unavailable")
                        _radioErrorMessage.value = "Radio Error: Bluetooth or Wi-Fi radio is unavailable or disabled. Please enable Bluetooth and Wi-Fi."
                        try {
                            connectionsClient.stopAdvertising()
                        } catch (ignored: Exception) {}
                    }
                    else -> {
                        Log.e(TAG, "Failed to start advertising: ${e.statusCode} ${e.message}")
                        _radioErrorMessage.value = "Advertising failed (Code ${e.statusCode})"
                    }
                }
            } else {
                Log.e(TAG, "Failed to start advertising: ${e.message}")
                _radioErrorMessage.value = e.message
            }
            _isAdvertising.value = false
        }
    }

    fun stopAdvertising() {
        try {
            connectionsClient.stopAdvertising()
        } catch (ignored: Exception) {}
        _isAdvertising.value = false
    }

    fun startDiscovery() {
        if (!hasRequiredPermissions()) {
            Log.d(TAG, "Cannot start discovery: required permissions not yet granted")
            return
        }

        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth radio is disabled; skipping discovery start")
            _isDiscovering.value = false
            return
        }

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            AppConfig.SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started")
            _isDiscovering.value = true
            _radioErrorMessage.value = null
        }.addOnFailureListener { e ->
            if (e is ApiException) {
                when (e.statusCode) {
                    ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING -> {
                        Log.d(TAG, "NearbyConnections already discovering")
                        _isDiscovering.value = true
                        _radioErrorMessage.value = null
                        return@addOnFailureListener
                    }
                    ConnectionsStatusCodes.STATUS_RADIO_ERROR -> {
                        Log.w(TAG, "NearbyConnections STATUS_RADIO_ERROR (8007) during discovery")
                        _radioErrorMessage.value = "Radio Error: Bluetooth or Wi-Fi radio is unavailable or disabled. Please enable Bluetooth and Wi-Fi."
                        try {
                            connectionsClient.stopDiscovery()
                        } catch (ignored: Exception) {}
                    }
                    else -> {
                        Log.e(TAG, "Failed to start discovery: ${e.statusCode} ${e.message}")
                    }
                }
            } else {
                Log.e(TAG, "Failed to start discovery: ${e.message}")
            }
            _isDiscovering.value = false
        }
    }

    fun stopDiscovery() {
        try {
            connectionsClient.stopDiscovery()
        } catch (ignored: Exception) {}
        _isDiscovering.value = false
    }

    fun requestConnection(localCallsign: String, endpointId: String) {
        if (!hasRequiredPermissions()) {
            Log.d(TAG, "Cannot request connection: required permissions not yet granted")
            return
        }

        _discoveredDevices.value = _discoveredDevices.value.map {
            if (it.id == endpointId) it.copy(isConnecting = true) else it
        }
        
        connectionsClient.requestConnection(
            localCallsign,
            endpointId,
            connectionLifecycleCallback
        ).addOnFailureListener { e ->
            Log.e(TAG, "Failed to request connection to $endpointId: ${e.message}")
            _discoveredDevices.value = _discoveredDevices.value.map {
                if (it.id == endpointId) it.copy(isConnecting = false) else it
            }
        }
    }

    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Accepted connection from $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to accept connection: ${e.message}")
            }
        _pendingAuthRequest.value = null
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
        _pendingAuthRequest.value = null
    }

    fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        _connectedDevices.value = _connectedDevices.value.filter { it.id != endpointId }
    }

    fun disconnectAll() {
        connectionsClient.stopAllEndpoints()
        _connectedDevices.value = emptyList()
    }

    /**
     * Broadcasts a control message (e.g. TALK_START, TALK_END) to all connected peers.
     */
    fun broadcastControlMessage(message: ControlMessage) {
        val connected = _connectedDevices.value.map { it.id }
        if (connected.isEmpty()) return

        val bytes = ControlMessage.serialize(message)
        val payload = Payload.fromBytes(bytes)
        connectionsClient.sendPayload(connected, payload)
    }

    /**
     * Sends a control message to a specific peer endpoint.
     */
    fun sendDirectControlMessage(targetEndpointId: String, message: ControlMessage) {
        val bytes = ControlMessage.serialize(message)
        val payload = Payload.fromBytes(bytes)
        connectionsClient.sendPayload(targetEndpointId, payload)
    }

    /**
     * Creates an outgoing audio stream pipe and transmits live microphone audio to a specific peer endpoint.
     */
    fun createDirectAudioStream(targetEndpointId: String): OutputStream? {
        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            val streamPayload = Payload.fromStream(ParcelFileDescriptor.AutoCloseInputStream(readSide))
            connectionsClient.sendPayload(targetEndpointId, streamPayload)
            
            return ParcelFileDescriptor.AutoCloseOutputStream(writeSide)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating direct audio stream pipe: ${e.message}")
            return null
        }
    }

    /**
     * Creates an outgoing audio stream pipe and transmits live microphone audio to all connected peers.
     */
    fun createBroadcastAudioStream(): OutputStream? {
        val connected = _connectedDevices.value.map { it.id }
        if (connected.isEmpty()) return null

        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            val streamPayload = Payload.fromStream(ParcelFileDescriptor.AutoCloseInputStream(readSide))
            connectionsClient.sendPayload(connected, streamPayload)
            
            return ParcelFileDescriptor.AutoCloseOutputStream(writeSide)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating audio stream pipe: ${e.message}")
            return null
        }
    }
}
