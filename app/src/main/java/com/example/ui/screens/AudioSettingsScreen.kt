package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import com.example.config.AppConfig
import com.example.ui.LocalizationManager
import com.example.ui.WalkieTalkieViewModel
import com.example.ui.theme.PttGreen
import com.example.ui.theme.RadioCardSurface
import com.example.ui.theme.RadioDarkBackground
import com.example.ui.theme.RadioDarkSurface
import com.example.ui.theme.RadioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AudioSettingsScreen(
    viewModel: WalkieTalkieViewModel,
    modifier: Modifier = Modifier
) {
    val audioConfig by viewModel.audioConfig.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val lang = appSettings.language

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = LocalizationManager.getString("audio_settings_title", lang),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Fine-tune radio audio processing, echo immunity, and bandwidth",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SAMPLE RATE SELECTION
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizationManager.getString("sample_rate", lang),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Lower sample rates use less bandwidth, providing maximum packet delivery at long distances (up to 500m).",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rates = listOf(
                        Triple(AppConfig.SAMPLE_RATE_8KHZ, "8 kHz", "Max Range"),
                        Triple(AppConfig.SAMPLE_RATE_16KHZ, "16 kHz", "Standard"),
                        Triple(AppConfig.SAMPLE_RATE_44KHZ, "44.1 kHz", "HD")
                    )

                    rates.forEach { (rate, label, sub) ->
                        FilterChip(
                            selected = audioConfig.sampleRate == rate,
                            onClick = { viewModel.updateAudioConfig(audioConfig.copy(sampleRate = rate)) },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(sub, fontSize = 10.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PttGreen,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rate_$rate")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ACOUSTIC ECHO CANCELLATION (AEC)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RadioDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SurroundSound,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = LocalizationManager.getString("echo_cancellation", lang),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Suppresses loudspeaker feedback and echo loops automatically.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = audioConfig.enableEchoCancellation,
                    onCheckedChange = { viewModel.updateAudioConfig(audioConfig.copy(enableEchoCancellation = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PttGreen),
                    modifier = Modifier.testTag("switch_aec")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // NOISE SUPPRESSOR (NS)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RadioDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = LocalizationManager.getString("noise_suppression", lang),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Filters out ambient outdoor wind, vehicle engine, and background noise.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = audioConfig.enableNoiseSuppression,
                    onCheckedChange = { viewModel.updateAudioConfig(audioConfig.copy(enableNoiseSuppression = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PttGreen),
                    modifier = Modifier.testTag("switch_ns")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ROGER BEEP CHIME
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RadioDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = LocalizationManager.getString("roger_beep", lang),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Audible radio beep played when PTT button is released.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = audioConfig.enableRogerBeep,
                    onCheckedChange = { viewModel.updateAudioConfig(audioConfig.copy(enableRogerBeep = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PttGreen),
                    modifier = Modifier.testTag("switch_roger_beep")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // MICROPHONE GAIN BOOST SLIDER
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
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = PttGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizationManager.getString("mic_gain", lang),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", audioConfig.micGainMultiplier)}x",
                        color = PttGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = audioConfig.micGainMultiplier,
                    onValueChange = { viewModel.updateAudioConfig(audioConfig.copy(micGainMultiplier = it)) },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = PttGreen,
                        activeTrackColor = PttGreen
                    ),
                    modifier = Modifier.testTag("slider_mic_gain")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TEST AUDIO FX BUTTON
        Button(
            onClick = { SoundEffects.playRogerBeep() },
            colors = ButtonDefaults.buttonColors(containerColor = RadioCardSurface, contentColor = PttGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_roger_beep_button")
        ) {
            Text("Test Roger Beep Chime", fontWeight = FontWeight.Bold)
        }
    }
}
