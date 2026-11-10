package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LocalizationManager
import com.example.ui.WalkieTalkieViewModel
import com.example.ui.theme.PttGreen
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioDarkBackground
import com.example.ui.theme.RadioDarkSurface
import com.example.ui.theme.RadioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AppSettingsScreen(
    viewModel: WalkieTalkieViewModel,
    modifier: Modifier = Modifier
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()
    val isBtEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val isGpsOk by viewModel.isGooglePlayServicesOk.collectAsState()
    val lang = appSettings.language
    val context = LocalContext.current

    var callsignInput by remember(appSettings.callsign) { mutableStateOf(appSettings.callsign) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Screen Title
        Text(
            text = LocalizationManager.getString("app_settings_title", lang),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure device identity, language, and wireless hardware controls",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CALLSIGN / DEVICE NAME
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizationManager.getString("callsign", lang),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = callsignInput,
                        onValueChange = { callsignInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PttGreen,
                            unfocusedBorderColor = RadioSurfaceHighlight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_callsign")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.updateCallsign(callsignInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = PttGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_callsign_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // LANGUAGE SELECTOR
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
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizationManager.getString("language", lang),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = lang == "en",
                        onClick = { viewModel.updateLanguage("en") },
                        label = { Text("English (EN)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PttGreen,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_en")
                    )
                    FilterChip(
                        selected = lang == "ur",
                        onClick = { viewModel.updateLanguage("ur") },
                        label = { Text("اردو (Urdu)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PttGreen,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_ur")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // KEEP SCREEN AWAKE
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
                        imageVector = Icons.Default.StayCurrentPortrait,
                        contentDescription = null,
                        tint = PttGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = LocalizationManager.getString("keep_screen_on", lang),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Prevents device from sleeping during walkie-talkie sessions.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = appSettings.keepScreenOn,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PttGreen),
                    modifier = Modifier.testTag("switch_keep_awake")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RADIO HARDWARE HEALTH & SYSTEM SHORTCUTS
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
                Text(
                    text = LocalizationManager.getString("radios_status", lang),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Wi-Fi Radio Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isWifiEnabled) PttGreen else RadioBusyRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWifiEnabled) "Wi-Fi: Enabled" else "Wi-Fi: Disabled (Turn ON)",
                            color = if (isWifiEnabled) TextPrimary else RadioBusyRed,
                            fontSize = 13.sp
                        )
                    }
                    if (!isWifiEnabled) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                } catch (ignored: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RadioBusyRed),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Turn On", fontSize = 11.sp)
                        }
                    }
                }

                // Bluetooth Radio Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (isBtEnabled) PttGreen else RadioBusyRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBtEnabled) "Bluetooth: Enabled" else "Bluetooth: Disabled (Turn ON)",
                            color = if (isBtEnabled) TextPrimary else RadioBusyRed,
                            fontSize = 13.sp
                        )
                    }
                    if (!isBtEnabled) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (ignored: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RadioBusyRed),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Turn On", fontSize = 11.sp)
                        }
                    }
                }

                // Google Play Services status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isGpsOk) PttGreen else RadioBusyRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGpsOk) "Google Play Services: Ready (Nearby Connections)" else "Google Play Services: Unavailable",
                        color = if (isGpsOk) TextPrimary else RadioBusyRed,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
