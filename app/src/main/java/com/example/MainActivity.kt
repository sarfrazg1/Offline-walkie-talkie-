package com.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CallType
import com.example.service.WalkieTalkieService
import com.example.ui.LocalizationManager
import com.example.ui.WalkieTalkieViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.AudioSettingsScreen
import com.example.ui.screens.CallHistoryScreen
import com.example.ui.screens.CallScreenOverlay
import com.example.ui.screens.ConnectedDevicesGroupScreen
import com.example.ui.screens.MainPttScreen
import com.example.ui.screens.RadarDiscoveryScreen
import com.example.ui.theme.PttGreen
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioDarkBackground
import com.example.ui.theme.RadioDarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalkieTalkieTheme

enum class ScreenTab {
    RADIO,
    CALLS,
    RADAR,
    GROUP,
    AUDIO,
    SETTINGS,
    ABOUT
}

class MainActivity : ComponentActivity() {

    private val viewModel: WalkieTalkieViewModel by viewModels()

    private var walkieTalkieService: WalkieTalkieService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? WalkieTalkieService.LocalBinder
            walkieTalkieService = binder?.getService()
            viewModel.walkieTalkieService = walkieTalkieService
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            walkieTalkieService = null
            viewModel.walkieTalkieService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming call notification actions
        handleCallNotificationIntent(intent)

        // Start and bind Foreground Service for background audio support
        try {
            val serviceIntent = Intent(this, WalkieTalkieService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            // Service start exception safety
        }

        setContent {
            WalkieTalkieTheme {
                val appSettings by viewModel.appSettings.collectAsState()

                // Keep screen awake setting synchronization
                DisposableEffect(appSettings.keepScreenOn) {
                    if (appSettings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {}
                }

                WalkieTalkieAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallNotificationIntent(intent)
    }

    private fun handleCallNotificationIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            WalkieTalkieService.ACTION_ACCEPT_CALL -> {
                viewModel.acceptCall()
            }
            WalkieTalkieService.ACTION_REJECT_CALL -> {
                viewModel.rejectCall()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkHardwareRadios()
        if (viewModel.hasPermissions.value && viewModel.isBluetoothEnabled.value && !viewModel.isAdvertising.value) {
            viewModel.startRadioEngine()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

@Composable
fun WalkieTalkieAppRoot(viewModel: WalkieTalkieViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(ScreenTab.RADIO) }
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val callHistory by viewModel.callHistory.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val lang = appSettings.language
    val missedCallsCount = callHistory.count { it.type == CallType.MISSED }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkHardwareRadios()
        viewModel.restartDiscovery()
    }

    val onEnableBluetooth: () -> Unit = {
        try {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            } catch (ignored: Exception) {}
        }
    }

    val onOpenWifiSettings: () -> Unit = {
        try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (ignored: Exception) {}
    }

    // Check & request permissions on launch
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkHardwareRadios()
        viewModel.restartDiscovery()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    // Android back button handling: Navigate back to Radio if on another tab
    if (currentTab != ScreenTab.RADIO) {
        BackHandler {
            currentTab = ScreenTab.RADIO
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioDarkBackground),
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            NavigationBar(
                containerColor = RadioDarkSurface,
                contentColor = TextPrimary,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                // Radio Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.RADIO,
                    onClick = { currentTab = ScreenTab.RADIO },
                    icon = {
                        Icon(Icons.Default.Radio, contentDescription = "Radio")
                    },
                    label = { Text(LocalizationManager.getString("nav_radio", lang), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PttGreen,
                        indicatorColor = PttGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_radio")
                )

                // Calls History Tab with Missed Badge
                NavigationBarItem(
                    selected = currentTab == ScreenTab.CALLS,
                    onClick = { currentTab = ScreenTab.CALLS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (missedCallsCount > 0) {
                                    Badge(containerColor = RadioBusyRed, contentColor = Color.White) {
                                        Text(
                                            text = missedCallsCount.toString(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Calls")
                        }
                    },
                    label = { Text(LocalizationManager.getString("nav_calls", lang), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PttGreen,
                        indicatorColor = PttGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_calls")
                )

                // Radar Discovery Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.RADAR,
                    onClick = { currentTab = ScreenTab.RADAR },
                    icon = {
                        Icon(Icons.Default.Radar, contentDescription = "Radar")
                    },
                    label = { Text(LocalizationManager.getString("nav_radar", lang), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PttGreen,
                        indicatorColor = PttGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_radar")
                )

                // Group Tab with Badge
                NavigationBarItem(
                    selected = currentTab == ScreenTab.GROUP,
                    onClick = { currentTab = ScreenTab.GROUP },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (connectedDevices.isNotEmpty()) {
                                    Badge(containerColor = PttGreen, contentColor = Color.Black) {
                                        Text(
                                            text = connectedDevices.size.toString(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Group, contentDescription = "Group")
                        }
                    },
                    label = { Text(LocalizationManager.getString("nav_group", lang), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PttGreen,
                        indicatorColor = PttGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_group")
                )

                // Audio Settings Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.AUDIO,
                    onClick = { currentTab = ScreenTab.AUDIO },
                    icon = {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Audio")
                    },
                    label = { Text(LocalizationManager.getString("nav_audio", lang), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PttGreen,
                        indicatorColor = PttGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_audio")
                )

                // App Settings Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.SETTINGS,
                    onClick = { currentTab = ScreenTab.SETTINGS },
                    icon = {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    },
                    label = { Text(LocalizationManager.getString("nav_settings", lang), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PttGreen,
                        indicatorColor = PttGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )

                // About & Developer Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.ABOUT,
                    onClick = { currentTab = ScreenTab.ABOUT },
                    icon = {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    },
                    label = { Text(LocalizationManager.getString("nav_about", lang), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PttGreen,
                        indicatorColor = PttGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_about")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.RADIO -> MainPttScreen(
                    viewModel = viewModel,
                    onNavigateToRadar = { currentTab = ScreenTab.RADAR },
                    onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
                    onEnableBluetooth = onEnableBluetooth,
                    onOpenWifiSettings = onOpenWifiSettings
                )
                ScreenTab.CALLS -> CallHistoryScreen(viewModel = viewModel)
                ScreenTab.RADAR -> RadarDiscoveryScreen(
                    viewModel = viewModel,
                    onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
                    onEnableBluetooth = onEnableBluetooth,
                    onOpenWifiSettings = onOpenWifiSettings
                )
                ScreenTab.GROUP -> ConnectedDevicesGroupScreen(viewModel = viewModel)
                ScreenTab.AUDIO -> AudioSettingsScreen(viewModel = viewModel)
                ScreenTab.SETTINGS -> AppSettingsScreen(viewModel = viewModel)
                ScreenTab.ABOUT -> AboutScreen()
            }

            // Global Full-Screen Overlay for Outgoing, Incoming, and Active Offline Calls
            CallScreenOverlay(viewModel = viewModel)
        }
    }
}
