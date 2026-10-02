package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.*

const val ROUTE_HOME = "home"
const val ROUTE_PRAYER = "prayer"
const val ROUTE_QIBLA = "qibla"
const val ROUTE_CHAT = "chat"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_SETTINGS = "settings"

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen(ROUTE_HOME, "Beranda", Icons.Default.Mosque)
    object Prayer : Screen(ROUTE_PRAYER, "Jadwal", Icons.Default.CalendarMonth)
    object Qibla : Screen(ROUTE_QIBLA, "Kiblat", Icons.Default.Explore)
    object Chat : Screen(ROUTE_CHAT, "AI", Icons.Default.AutoAwesome)
    object Notifications : Screen(ROUTE_NOTIFICATIONS, "WA Notif", Icons.Default.Message)
    object Settings : Screen(ROUTE_SETTINGS, "Pengaturan", Icons.Default.Settings)
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    
    // ViewModels initialized via standard Compose ViewModel scopes
    val prayerViewModel: PrayerViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val waViewModel: WaNotificationViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val darkModePreference by settingsViewModel.darkMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val darkThemeActive = when (darkModePreference) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    MyApplicationTheme(darkTheme = darkThemeActive) {
        val activeNotif by prayerViewModel.activeBrowserNotification.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        val items = listOf(
                            Screen.Home,
                            Screen.Prayer,
                            Screen.Qibla,
                            Screen.Chat,
                            Screen.Notifications,
                            Screen.Settings
                        )

                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title, fontSize = 10.sp, maxLines = 1) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(ROUTE_HOME) {
                        BerandaScreen(viewModel = prayerViewModel)
                    }
                    composable(ROUTE_PRAYER) {
                        JadwalSholatScreen(viewModel = prayerViewModel)
                    }
                    composable(ROUTE_QIBLA) {
                        KiblatScreen()
                    }
                    composable(ROUTE_CHAT) {
                        AiChatScreen(viewModel = chatViewModel)
                    }
                    composable(ROUTE_NOTIFICATIONS) {
                        WaNotificationScreen(viewModel = waViewModel)
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }

            // Real browser-style sliding top notification banner
            AnimatedVisibility(
                visible = activeNotif != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(100f)
            ) {
                activeNotif?.let { notif ->
                    BrowserNotificationCard(
                        notification = notif,
                        onDismiss = { prayerViewModel.dismissBrowserNotification() }
                    )
                }
            }
        }
    }
}

@Composable
fun BrowserNotificationCard(
    notification: BrowserNotificationAlert,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .widthIn(max = 420.dp)
            .shadow(12.dp, RoundedCornerShape(14.dp))
            .testTag("browser_notification_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Chrome origin simulation representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pengingat Sholat ZUCCHERO:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "sekarang",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Title + message + action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (notification.iconType == "PRE_REMINDER") 
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) 
                            else 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (notification.iconType == "PRE_REMINDER") 
                            Icons.Default.NotificationsPaused 
                        else 
                            Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (notification.iconType == "PRE_REMINDER") 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.message,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Tutup", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (notification.iconType == "PRE_REMINDER") 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK", fontSize = 11.sp, color = if (notification.iconType == "PRE_REMINDER") MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
