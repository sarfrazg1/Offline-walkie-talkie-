package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceEndpoint
import com.example.model.RadioState
import com.example.ui.LocalizationManager
import com.example.ui.WalkieTalkieViewModel
import com.example.ui.theme.PttGreen
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCardSurface
import com.example.ui.theme.RadioDarkBackground
import com.example.ui.theme.RadioDarkSurface
import com.example.ui.theme.RadioReceiveCyan
import com.example.ui.theme.RadioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConnectedDevicesGroupScreen(
    viewModel: WalkieTalkieViewModel,
    modifier: Modifier = Modifier
) {
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val radioState by viewModel.radioState.collectAsState()
    val currentSpeaker by viewModel.currentSpeaker.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val lang = appSettings.language

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioDarkBackground)
            .padding(16.dp)
    ) {
        // Group Header Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RadioDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = PttGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = LocalizationManager.getString("group_title", lang),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${connectedDevices.size} Active Member(s) in Cluster",
                                color = if (connectedDevices.isNotEmpty()) PttGreen else RadioAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (connectedDevices.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.disconnectAllEndpoints() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RadioBusyRed),
                            modifier = Modifier.testTag("disconnect_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect All", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // One-to-Many broadcast banner
                Surface(
                    color = RadioCardSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = RadioReceiveCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizationManager.getString("group_broadcast_info", lang),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Speaker Status Card
        if (radioState == RadioState.RECEIVING || radioState == RadioState.TRANSMITTING) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (radioState == RadioState.TRANSMITTING) PttGreen.copy(alpha = 0.15f) else RadioReceiveCyan.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (radioState == RadioState.TRANSMITTING) PttGreen else RadioReceiveCyan
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (radioState == RadioState.TRANSMITTING) Icons.Default.Mic else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (radioState == RadioState.TRANSMITTING) PttGreen else RadioReceiveCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CURRENT SPEAKER",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (radioState == RadioState.TRANSMITTING) "You (${appSettings.callsign})" else (currentSpeaker ?: "Peer"),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // CONNECTED PEERS LIST
        Text(
            text = "CONNECTED PEERS",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (connectedDevices.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(RadioDarkSurface, RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Connected Peers in Group",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Go to the Radar tab to discover and connect with nearby devices.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(connectedDevices, key = { it.id }) { device ->
                    ConnectedPeerItem(
                        device = device,
                        isSpeaking = currentSpeaker == device.name,
                        onCall = { viewModel.startCall(device.id, device.name) },
                        onPing = { viewModel.sendPing(device.id) },
                        onDisconnect = { viewModel.disconnectEndpoint(device.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MESH RELAY ARCHITECTURE STATUS BADGE
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = RadioDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = RadioAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Multi-Hop Mesh Relay Architecture",
                        color = RadioAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Cluster topology designed to support intermediate hop packet forwarding without internet or cellular towers.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedPeerItem(
    device: DeviceEndpoint,
    isSpeaking: Boolean,
    onCall: () -> Unit,
    onPing: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RadioCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSpeaking) RadioReceiveCyan else RadioSurfaceHighlight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isSpeaking) RadioReceiveCyan else PttGreen)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isSpeaking) "Speaking now…" else "Connected • Active",
                        color = if (isSpeaking) RadioReceiveCyan else TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCall,
                    modifier = Modifier.testTag("call_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Offline Voice Call",
                        tint = PttGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onPing,
                    modifier = Modifier.testTag("ping_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkPing,
                        contentDescription = "Ping Device",
                        tint = RadioReceiveCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier.testTag("disconnect_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Disconnect Peer",
                        tint = RadioBusyRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
