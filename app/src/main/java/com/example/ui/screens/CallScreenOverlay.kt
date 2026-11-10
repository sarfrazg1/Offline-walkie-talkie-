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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallState
import com.example.ui.LocalizationManager
import com.example.ui.WalkieTalkieViewModel
import com.example.ui.components.AudioVisualizer
import com.example.ui.theme.PttGreen
import com.example.ui.theme.PttGreenDark
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCardSurface
import com.example.ui.theme.RadioDarkBackground
import com.example.ui.theme.RadioDarkSurface
import com.example.ui.theme.RadioReceiveCyan
import com.example.ui.theme.RadioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Full-screen or modal overlay displayed during Outgoing Ringing, Incoming Ringing,
 * and Active Two-Way Offline Calls.
 */
@Composable
fun CallScreenOverlay(
    viewModel: WalkieTalkieViewModel,
    modifier: Modifier = Modifier
) {
    val activeCall by viewModel.activeCall.collectAsState()
    val visualizerLevel by viewModel.visualizerLevel.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val lang = appSettings.language

    val call = activeCall ?: return

    val infiniteTransition = rememberInfiniteTransition(label = "call_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(RadioDarkBackground.copy(alpha = 0.96f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RadioDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (call.state == CallState.ACTIVE_CALL) PttGreen else RadioReceiveCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (call.state) {
                                CallState.OUTGOING_RINGING -> "CALLING PEER • ZERO INTERNET"
                                CallState.INCOMING_RINGING -> "INCOMING OFFLINE CALL"
                                CallState.ACTIVE_CALL -> "FULL-DUPLEX 2-WAY CALL"
                                else -> "OFFLINE CALL"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Center Caller/Callee Avatar & Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        if (call.state == CallState.INCOMING_RINGING || call.state == CallState.OUTGOING_RINGING) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (call.state == CallState.INCOMING_RINGING) PttGreen.copy(alpha = 0.2f) else RadioReceiveCyan.copy(alpha = 0.2f)
                                    )
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(RadioCardSurface)
                                .border(
                                    3.dp,
                                    if (call.state == CallState.ACTIVE_CALL) PttGreen else RadioReceiveCyan,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (call.state == CallState.ACTIVE_CALL) Icons.Default.PhoneInTalk else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (call.state == CallState.ACTIVE_CALL) PttGreen else RadioReceiveCyan,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = call.peerName,
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (call.state) {
                            CallState.OUTGOING_RINGING -> LocalizationManager.getString("ringing", lang)
                            CallState.INCOMING_RINGING -> LocalizationManager.getString("incoming_call", lang)
                            CallState.ACTIVE_CALL -> String.format(
                                "%02d:%02d",
                                call.durationSeconds / 60,
                                call.durationSeconds % 60
                            )
                            else -> ""
                        },
                        color = if (call.state == CallState.ACTIVE_CALL) PttGreen else RadioReceiveCyan,
                        fontSize = if (call.state == CallState.ACTIVE_CALL) 24.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (call.state == CallState.ACTIVE_CALL) FontFamily.Monospace else FontFamily.Default
                    )

                    if (call.state == CallState.ACTIVE_CALL) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AudioVisualizer(
                            level = visualizerLevel,
                            isActive = true,
                            isReceiving = true,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(36.dp)
                        )
                    }
                }

                // Bottom Call Action Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (call.state) {
                        CallState.INCOMING_RINGING -> {
                            // ACCEPT (Green) and REJECT (Red) buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // REJECT
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FilledIconButton(
                                        onClick = { viewModel.rejectCall() },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = RadioBusyRed,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .size(72.dp)
                                            .testTag("reject_call_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CallEnd,
                                            contentDescription = "Reject Call",
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = LocalizationManager.getString("reject", lang),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                // ACCEPT
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FilledIconButton(
                                        onClick = { viewModel.acceptCall() },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = PttGreen,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier
                                            .size(72.dp)
                                            .testTag("accept_call_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Accept Call",
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = LocalizationManager.getString("accept", lang),
                                        color = PttGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        CallState.OUTGOING_RINGING -> {
                            // CANCEL CALL BUTTON
                            FilledIconButton(
                                onClick = { viewModel.endCall() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = RadioBusyRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("cancel_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Cancel Call",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cancel",
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        CallState.ACTIVE_CALL -> {
                            // ACTIVE CALL CONTROLS: MUTE, SPEAKER, END CALL
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mute Mic Toggle
                                FilledIconButton(
                                    onClick = { viewModel.toggleMuteMic() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (call.isMuted) RadioBusyRed.copy(alpha = 0.3f) else RadioCardSurface,
                                        contentColor = if (call.isMuted) RadioBusyRed else TextPrimary
                                    ),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("toggle_mute_button")
                                ) {
                                    Icon(
                                        imageVector = if (call.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute Microphone",
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Big Red End Call Button
                                FilledIconButton(
                                    onClick = { viewModel.endCall() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = RadioBusyRed,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .size(72.dp)
                                        .testTag("end_active_call_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "End Call",
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                // Speakerphone Toggle
                                FilledIconButton(
                                    onClick = { viewModel.toggleSpeakerphone() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (call.isSpeakerphone) PttGreen.copy(alpha = 0.25f) else RadioCardSurface,
                                        contentColor = if (call.isSpeakerphone) PttGreen else TextPrimary
                                    ),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("toggle_speaker_button")
                                ) {
                                    Icon(
                                        imageVector = if (call.isSpeakerphone) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                        contentDescription = "Speakerphone",
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
