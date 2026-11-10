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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord
import com.example.model.CallType
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallHistoryScreen(
    viewModel: WalkieTalkieViewModel,
    modifier: Modifier = Modifier
) {
    val callHistory by viewModel.callHistory.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val lang = appSettings.language

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioDarkBackground)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = LocalizationManager.getString("call_history", lang),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline Peer-to-Peer Calls",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (callHistory.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearCallHistory() },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Offline calling technical explanation banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = RadioDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = RadioAmber,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Calls are 100% offline via direct Wi-Fi/Bluetooth signals. Incoming calls require the other device to be powered on with Wi-Fi & Bluetooth active and this app running in foreground or background.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (callHistory.isEmpty()) {
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
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = LocalizationManager.getString("no_call_history", lang),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Make calls to any discovered peer from the Radar or Group screen.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
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
                items(callHistory, key = { it.id }) { record ->
                    CallHistoryItem(
                        record = record,
                        lang = lang,
                        onCallBack = { viewModel.startCall(record.peerEndpointId, record.peerName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CallHistoryItem(
    record: CallRecord,
    lang: String,
    onCallBack: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val formattedTime = formatter.format(Date(record.timestamp))

    val icon = when (record.type) {
        CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
        CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
        CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
        CallType.REJECTED -> Icons.Default.CallEnd
    }

    val iconColor = when (record.type) {
        CallType.OUTGOING -> PttGreen
        CallType.INCOMING -> RadioReceiveCyan
        CallType.MISSED -> RadioBusyRed
        CallType.REJECTED -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RadioCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RadioSurfaceHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.peerName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (record.type) {
                                CallType.MISSED -> LocalizationManager.getString("missed_call", lang)
                                CallType.REJECTED -> LocalizationManager.getString("declined_call", lang)
                                CallType.OUTGOING -> "Outgoing"
                                CallType.INCOMING -> "Incoming"
                            },
                            color = iconColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (record.durationSeconds > 0) {
                            Text(
                                text = " • ${String.format("%02d:%02d", record.durationSeconds / 60, record.durationSeconds % 60)}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = " • $formattedTime",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick Call Back Button
            Button(
                onClick = onCallBack,
                colors = ButtonDefaults.buttonColors(containerColor = PttGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("callback_${record.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call Back",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = LocalizationManager.getString("call", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
