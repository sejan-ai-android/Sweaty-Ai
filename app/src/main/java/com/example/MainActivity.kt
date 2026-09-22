package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.NavScreen
import com.example.ui.SweatyViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DeviceControlScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.SweatyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SweatyViewModel by viewModels {
        SweatyViewModel.provideFactory(application as SweatyApp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SweatyTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: SweatyViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isAlwaysListening by viewModel.isAlwaysListening.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmationResult.collectAsState()

    // Permission launcher for Mic and Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] ?: false
        if (micGranted) {
            viewModel.onPermissionsChecked(true)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            viewModel.onPermissionsChecked(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Accessibility Status Indicator dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAccessibilityActive) EmeraldGlow else CoralAlert)
                        )
                    }
                },
                actions = {
                    // Always listening quick toggle in TopBar
                    IconButton(
                        onClick = { viewModel.toggleAlwaysListening() },
                        modifier = Modifier.testTag("topbar_always_listening_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Always Listening",
                            tint = if (isAlwaysListening) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    // Panic stop quick action in TopBar
                    IconButton(
                        onClick = { viewModel.panicStop() },
                        modifier = Modifier.testTag("topbar_panic_stop")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = stringResource(R.string.action_panic_stop),
                            tint = CoralAlert
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == NavScreen.HOME,
                    onClick = { viewModel.navigateTo(NavScreen.HOME) },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanNeon,
                        selectedTextColor = CyanNeon,
                        indicatorColor = CyanNeon.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_home")
                )
                NavigationBarItem(
                    selected = currentScreen == NavScreen.CHAT,
                    onClick = { viewModel.navigateTo(NavScreen.CHAT) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_chat), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanNeon,
                        selectedTextColor = CyanNeon,
                        indicatorColor = CyanNeon.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_chat")
                )
                NavigationBarItem(
                    selected = currentScreen == NavScreen.MEMORY,
                    onClick = { viewModel.navigateTo(NavScreen.MEMORY) },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_memory), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanNeon,
                        selectedTextColor = CyanNeon,
                        indicatorColor = CyanNeon.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_memory")
                )
                NavigationBarItem(
                    selected = currentScreen == NavScreen.REMINDERS,
                    onClick = { viewModel.navigateTo(NavScreen.REMINDERS) },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_reminders), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanNeon,
                        selectedTextColor = CyanNeon,
                        indicatorColor = CyanNeon.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_reminders")
                )
                NavigationBarItem(
                    selected = currentScreen == NavScreen.CONTROL,
                    onClick = { viewModel.navigateTo(NavScreen.CONTROL) },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_control), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanNeon,
                        selectedTextColor = CyanNeon,
                        indicatorColor = CyanNeon.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_control")
                )
                NavigationBarItem(
                    selected = currentScreen == NavScreen.SETTINGS,
                    onClick = { viewModel.navigateTo(NavScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanNeon,
                        selectedTextColor = CyanNeon,
                        indicatorColor = CyanNeon.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavScreen.HOME -> HomeScreen(viewModel = viewModel)
                NavScreen.CHAT -> ChatScreen(viewModel = viewModel)
                NavScreen.MEMORY -> MemoryScreen(viewModel = viewModel)
                NavScreen.REMINDERS -> RemindersScreen(viewModel = viewModel)
                NavScreen.CONTROL -> DeviceControlScreen(viewModel = viewModel)
                NavScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Safety Confirmation Dialog for sensitive actions
            if (pendingConfirmation != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelPendingAction() },
                    title = {
                        Text(
                            text = stringResource(R.string.dialog_confirm_action_title),
                            fontWeight = FontWeight.Bold,
                            color = CoralAlert
                        )
                    },
                    text = {
                        Text(
                            text = pendingConfirmation?.message ?: "This action may perform sensitive operations.",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmPendingAction() },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAlert)
                        ) {
                            Text(stringResource(R.string.dialog_confirm_btn))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelPendingAction() }) {
                            Text(stringResource(R.string.dialog_cancel_btn))
                        }
                    }
                )
            }
        }
    }
}
