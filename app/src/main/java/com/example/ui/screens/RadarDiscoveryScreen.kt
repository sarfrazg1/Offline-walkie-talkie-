package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceEndpoint
import com.example.ui.LocalizationManager
import com.example.ui.WalkieTalkieViewModel
import com.example.ui.theme.PttGreen
import com.example.ui.theme.PttGreenDark
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCardSurface
import com.example.ui.theme.RadioDarkBackground
import com.example.ui.theme.RadioDarkSurface
import com.example.ui.theme.RadioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarDiscoveryScreen(
    viewModel: WalkieTalkieViewModel,
    onRequestPermissions: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {},
    onOpenWifiSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val pendingAuth by viewModel.pendingAuthRequest.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val isBtEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()
    val radioError by viewModel.radioErrorMessage.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val lang = appSettings.language

    var showEditCallsignDialog by remember { mutableStateOf(false) }
    var tempCallsign by remember(appSettings.callsign) { mutableStateOf(appSettings.callsign) }

    // Radar Beam Sweep Rotation Angle
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioDarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Callsign & Radar Status Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RadioDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = LocalizationManager.getString("callsign", lang),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = appSettings.callsign,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PttGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showEditCallsignDialog = true },
                        modifier = Modifier.testTag("edit_callsign_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Callsign",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.restartDiscovery() },
                        modifier = Modifier.testTag("refresh_radar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Radar Scan",
                            tint = PttGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TACTICAL RADAR CANVAS DISPLAY
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(RadioDarkSurface)
                .border(2.dp, RadioSurfaceHighlight, CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Concentric range rings: 25%, 50%, 75%, 100%
                val ringPaints = listOf(0.25f, 0.5f, 0.75f, 0.98f)
                ringPaints.forEach { factor ->
                    drawCircle(
                        color = RadioSurfaceHighlight,
                        radius = radius * factor,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Crosshair axes
                drawLine(
                    color = RadioSurfaceHighlight.copy(alpha = 0.6f),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = RadioSurfaceHighlight.copy(alpha = 0.6f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1.dp.toPx()
                )

                // Rotating radar beam
                val rad = Math.toRadians(sweepAngle.toDouble())
                val beamEnd = Offset(
                    (center.x + radius * cos(rad)).toFloat(),
                    (center.y + radius * sin(rad)).toFloat()
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, PttGreen.copy(alpha = 0.8f)),
                        start = center,
                        end = beamEnd
                    ),
                    start = center,
                    end = beamEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Radar center ping indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PttGreen)
            )

            // Overlaid distance markings
            Text(
                text = "500m RANGE",
                color = PttGreen.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // DISCOVERED DEVICES HEADER & LIST
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DISCOVERED DEVICES (${discoveredDevices.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDiscovering || isAdvertising) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = PttGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SCANNING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PttGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (discoveredDevices.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(RadioDarkSurface, RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val icon = when {
                        !hasPermissions -> Icons.Default.Security
                        !isBtEnabled || radioError != null -> Icons.Default.WifiOff
                        else -> Icons.Default.PhoneAndroid
                    }
                    val iconTint = when {
                        !hasPermissions -> RadioAmber
                        !isBtEnabled || radioError != null -> RadioBusyRed
                        else -> TextSecondary.copy(alpha = 0.5f)
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val title = when {
                        !hasPermissions -> "Nearby Permissions Required"
                        !isBtEnabled -> "Bluetooth is Turned Off"
                        radioError != null -> "Radio Error (8007)"
                        else -> LocalizationManager.getString("radar_scanning", lang)
                    }
                    Text(
                        text = title,
                        color = when {
                            !hasPermissions -> RadioAmber
                            !isBtEnabled || radioError != null -> RadioBusyRed
                            else -> TextPrimary
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val subtitle = when {
                        !hasPermissions -> "Bluetooth and Nearby Wi-Fi permissions are needed to scan and connect with nearby walkie-talkies offline."
                        !isBtEnabled -> "Bluetooth must be turned on to broadcast and discover nearby devices."
                        radioError != null -> radioError ?: "Bluetooth or Wi-Fi radio is in an error state. Please verify both are active."
                        else -> "Make sure the other phone has OFFLINE WALKIE TALKIE open with Wi-Fi & Bluetooth enabled."
                    }
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!hasPermissions) {
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = RadioAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant Permissions", fontWeight = FontWeight.Bold)
                        }
                    } else if (!isBtEnabled) {
                        Button(
                            onClick = onEnableBluetooth,
                            colors = ButtonDefaults.buttonColors(containerColor = PttGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("radar_enable_bt_button")
                        ) {
                            Text("Turn On Bluetooth", fontWeight = FontWeight.Bold)
                        }
                    } else if (radioError != null) {
                        Button(
                            onClick = {
                                viewModel.checkHardwareRadios()
                                viewModel.restartDiscovery()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RadioAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("radar_retry_button")
                        ) {
                            Text("Retry Radar Scan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(discoveredDevices, key = { it.id }) { device ->
                    DiscoveredDeviceItem(
                        device = device,
                        lang = lang,
                        onConnect = { viewModel.connectToEndpoint(device.id) }
                    )
                }
            }
        }
    }

    // PENDING AUTHENTICATION DIALOG (Pairing confirmation with 4-digit code)
    if (pendingAuth != null) {
        AlertDialog(
            onDismissRequest = { viewModel.rejectPendingAuth() },
            containerColor = RadioDarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizationManager.getString("auth_title", lang),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Device \"${pendingAuth?.name}\" wants to pair. Verify authentication digits match on both phones:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Digit Security Code Banner
                    Surface(
                        color = RadioCardSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, PttGreen)
                    ) {
                        Text(
                            text = pendingAuth?.authDigits ?: "••••",
                            color = PttGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptPendingAuth() },
                    colors = ButtonDefaults.buttonColors(containerColor = PttGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("accept_auth_button")
                ) {
                    Text(LocalizationManager.getString("accept", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.rejectPendingAuth() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RadioBusyRed),
                    modifier = Modifier.testTag("reject_auth_button")
                ) {
                    Text(LocalizationManager.getString("reject", lang))
                }
            }
        )
    }

    // EDIT CALLSIGN DIALOG
    if (showEditCallsignDialog) {
        AlertDialog(
            onDismissRequest = { showEditCallsignDialog = false },
            containerColor = RadioDarkSurface,
            title = { Text(LocalizationManager.getString("callsign", lang), color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = tempCallsign,
                    onValueChange = { tempCallsign = it },
                    singleLine = true,
                    label = { Text("Callsign / Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCallsign(tempCallsign)
                        showEditCallsignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PttGreen, contentColor = Color.Black)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCallsignDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DiscoveredDeviceItem(
    device: DeviceEndpoint,
    lang: String,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RadioCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RadioDarkSurface,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = PttGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "ID: ${device.id.take(8)} • Offline Ready",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (device.isConnecting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PttGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LocalizationManager.getString("connecting", lang),
                        color = PttGreen,
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = PttGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("connect_${device.id}")
                ) {
                    Text(
                        text = LocalizationManager.getString("connect", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
