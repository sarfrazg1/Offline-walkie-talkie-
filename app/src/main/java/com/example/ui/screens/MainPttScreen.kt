package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.AppConfig
import com.example.model.RadioState
import com.example.ui.LocalizationManager
import com.example.ui.WalkieTalkieViewModel
import com.example.ui.components.AudioVisualizer
import com.example.ui.theme.PttGreen
import com.example.ui.theme.PttGreenDark
import com.example.ui.theme.PttGreenGlow
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
fun MainPttScreen(
    viewModel: WalkieTalkieViewModel,
    onNavigateToRadar: () -> Unit,
    onRequestPermissions: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {},
    onOpenWifiSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val radioState by viewModel.radioState.collectAsState()
    val currentSpeaker by viewModel.currentSpeaker.collectAsState()
    val talkDuration by viewModel.talkDurationSeconds.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val visualizerLevel by viewModel.visualizerLevel.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()
    val isBtEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val radioError by viewModel.radioErrorMessage.collectAsState()

    val lang = appSettings.language
    val isTransmitting = radioState == RadioState.TRANSMITTING
    val isReceiving = radioState == RadioState.RECEIVING
    val isInCall = radioState == RadioState.IN_CALL
    val isBusy = radioState == RadioState.BUSY || (isReceiving && !isTransmitting) || isInCall

    // Pulse animation for PTT transmitting ring
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioDarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP TACTICAL FREQUENCY & CHASSIS HEADER
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radio Callsign Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RadioDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (connectedDevices.isNotEmpty()) PttGreen else RadioAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = appSettings.callsign,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Bilingual Toggle Switch
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = lang == "en",
                        onClick = { viewModel.updateLanguage("en") },
                        label = { Text("EN", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PttGreen,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                    FilterChip(
                        selected = lang == "ur",
                        onClick = { viewModel.updateLanguage("ur") },
                        label = { Text("اردو", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PttGreen,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tactical Radio Display Screen (Simulated LCD/OLED panel)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = RadioDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, RadioSurfaceHighlight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Channel & Frequency
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = PttGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${AppConfig.DEFAULT_CHANNEL_NAME} • ${AppConfig.DEFAULT_FREQUENCY_MHZ}",
                                color = PttGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "OFFLINE P2P",
                            color = RadioAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Channel Status Banner & WHO IS SPEAKING
                    val statusColor = when (radioState) {
                        RadioState.TRANSMITTING -> PttGreen
                        RadioState.RECEIVING -> RadioReceiveCyan
                        RadioState.BUSY -> RadioBusyRed
                        RadioState.IN_CALL -> PttGreen
                        RadioState.IDLE -> TextSecondary
                    }

                    val bannerText = when (radioState) {
                        RadioState.TRANSMITTING -> LocalizationManager.getString("channel_transmitting", lang)
                        RadioState.RECEIVING -> LocalizationManager.getString("channel_receiving", lang, currentSpeaker ?: "Peer")
                        RadioState.BUSY -> LocalizationManager.getString("channel_busy", lang)
                        RadioState.IN_CALL -> LocalizationManager.getString("in_call", lang, currentSpeaker ?: "Peer")
                        RadioState.IDLE -> LocalizationManager.getString("channel_idle", lang)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (radioState) {
                                RadioState.TRANSMITTING -> Icons.Default.Mic
                                RadioState.RECEIVING -> Icons.Default.VolumeUp
                                RadioState.IN_CALL -> Icons.Default.Call
                                else -> Icons.Default.Podcasts
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (isTransmitting) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = String.format("%02d:%02ds", talkDuration / 60, talkDuration % 60),
                                color = PttGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio Visualizer waveform inside the tactical panel
                    AudioVisualizer(
                        level = visualizerLevel,
                        isActive = isTransmitting || isReceiving,
                        isReceiving = isReceiving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // CENTRAL PUSH-TO-TALK BUTTON SECTION
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Animated Glowing Pulse Ring when Transmitting
                if (isTransmitting) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(PttGreenGlow.copy(alpha = pulseAlpha))
                    )
                }

                // Outer Bezel / Chassis Rim
                Box(
                    modifier = Modifier
                        .size(216.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(RadioCardSurface, RadioDarkSurface)
                            )
                        )
                        .border(
                            width = 4.dp,
                            color = when {
                                isTransmitting -> PttGreen
                                isReceiving -> RadioReceiveCyan
                                else -> RadioSurfaceHighlight
                            },
                            shape = CircleShape
                        )
                        .shadow(elevation = 16.dp, shape = CircleShape)
                )

                // Large Central PTT Button Core
                val pttGradient = when {
                    isTransmitting -> listOf(PttGreen, PttGreenDark)
                    isReceiving -> listOf(Color(0xFF00838F), Color(0xFF004D40))
                    else -> listOf(Color(0xFF1B5E20), Color(0xFF0E3812))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(176.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(pttGradient))
                        .border(
                            width = 3.dp,
                            color = if (isTransmitting) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                            shape = CircleShape
                        )
                        .pointerInput(isBusy) {
                            if (isBusy) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                viewModel.onPttPressed()
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                }
                                viewModel.onPttReleased()
                            }
                        }
                        .testTag("ptt_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isInCall -> Icons.Default.Call
                                isTransmitting -> Icons.Default.Mic
                                isReceiving -> Icons.Default.VolumeUp
                                else -> Icons.Default.Mic
                            },
                            contentDescription = "Push to Talk Microphone",
                            tint = if (isTransmitting) Color.Black else TextPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isInCall) "IN CALL" else "PTT",
                            fontWeight = FontWeight.Black,
                            fontSize = if (isInCall) 18.sp else 24.sp,
                            letterSpacing = 2.sp,
                            color = if (isTransmitting) Color.Black else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instructional text under PTT button
            Text(
                text = when {
                    isInCall -> LocalizationManager.getString("active_call", lang)
                    isTransmitting -> LocalizationManager.getString("ptt_release", lang)
                    isReceiving -> LocalizationManager.getString("ptt_locked", lang)
                    else -> LocalizationManager.getString("ptt_hold", lang)
                },
                color = when {
                    isInCall -> PttGreen
                    isTransmitting -> PttGreen
                    isReceiving -> RadioReceiveCyan
                    else -> TextSecondary
                },
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }

        // BOTTOM TELEMETRY & RADAR SHORTCUT DOCK
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Peer Connectivity Status Card
            Card(
                onClick = onNavigateToRadar,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radar_shortcut_card"),
                colors = CardDefaults.cardColors(containerColor = RadioCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = null,
                            tint = if (connectedDevices.isNotEmpty()) PttGreen else RadioAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (connectedDevices.isNotEmpty()) {
                                    LocalizationManager.getString("connected_peers", lang, connectedDevices.size)
                                } else {
                                    LocalizationManager.getString("no_peers", lang)
                                },
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Zero Internet / SIM Required • Offline Mesh Ready",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connectedDevices.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val peer = connectedDevices.first()
                                    viewModel.startCall(peer.id, peer.name)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PttGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 2.dp
                                ),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("quick_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = LocalizationManager.getString("call", lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RadioDarkSurface
                        ) {
                            Text(
                                text = "RADAR →",
                                color = PttGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Radio warnings if Wi-Fi or Bluetooth is disabled or radio error occurred
            AnimatedVisibility(
                visible = !isWifiEnabled || !isBtEnabled || radioError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(RadioBusyRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = RadioBusyRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                !isBtEnabled -> "Bluetooth is OFF. Turn ON for radio."
                                !isWifiEnabled -> "Wi-Fi is OFF. Enable for high-bandwidth audio."
                                else -> radioError ?: "Radio error. Check Bluetooth & Wi-Fi."
                            },
                            color = RadioBusyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!isBtEnabled) {
                        Button(
                            onClick = onEnableBluetooth,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadioBusyRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("turn_on_bt_button")
                        ) {
                            Text("Turn On", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (!isWifiEnabled) {
                        Button(
                            onClick = onOpenWifiSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadioBusyRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("open_wifi_button")
                        ) {
                            Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (radioError != null) {
                        Button(
                            onClick = {
                                viewModel.checkHardwareRadios()
                                viewModel.restartDiscovery()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadioBusyRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("retry_radio_button")
                        ) {
                            Text("Retry", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Permissions warning if Nearby permissions are not granted
            AnimatedVisibility(
                visible = !hasPermissions,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(RadioAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = RadioAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nearby permissions needed for radio",
                            color = RadioAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    androidx.compose.material3.Button(
                        onClick = onRequestPermissions,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = RadioAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
