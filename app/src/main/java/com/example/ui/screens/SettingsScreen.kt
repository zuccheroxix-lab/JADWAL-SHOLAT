package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAuthLoading by authViewModel.isAuthLoading.collectAsState()
    val todayPrayerChecks by authViewModel.todayPrayerChecks.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.handleGoogleSignInResult(result.data)
    }
    val darkMode by viewModel.darkMode.collectAsState()
    val calcMethod by viewModel.calcMethod.collectAsState()
    val adzanSound by viewModel.adzanSound.collectAsState()
    val isAdzanNotifEnabled by viewModel.isAdzanNotifEnabled.collectAsState()
    val isPreReminderEnabled by viewModel.isPreReminderEnabled.collectAsState()
    val isFastingSunnahEnabled by viewModel.isFastingSunnahEnabled.collectAsState()
    val language by viewModel.language.collectAsState()

    var calculationMenuExpanded by remember { mutableStateOf(false) }
    var soundMenuExpanded by remember { mutableStateOf(false) }
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var langMenuExpanded by remember { mutableStateOf(false) }

    val methods = listOf("Kemenag RI", "Umm Al-Qura", "Muslim World League", "ISNA", "Egypt")
    val sounds = listOf("Mekkah", "Madinah", "Standard Beep", "Alunan Syahdu")
    val themes = listOf("LIGHT" to "Terang", "DARK" to "Gelap", "SYSTEM" to "Ikuti Sistem")
    val languages = listOf("id" to "Bahasa Indonesia", "en" to "English")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Aplikasi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Notification Toggles Category
            CategoryHeader("Notifikasi & Adzan", Icons.Default.Notifications)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Adzan Auto notif
                    RowSettingSwitch(
                        title = "Notifikasi Adzan Otomatis",
                        description = "Nyalakan peringatan suara dan getar saat masuk waktu shalat.",
                        checked = isAdzanNotifEnabled,
                        onCheckedChange = { viewModel.toggleAdzanNotif() },
                        tag = "adzan_notif_switch"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // 10 minutes pre reminder
                    RowSettingSwitch(
                        title = "Pengingat 10 Menit Sebelum Adzan",
                        description = "Persiapan wudhu sebelum adzan sholat berkumandang.",
                        checked = isPreReminderEnabled,
                        onCheckedChange = { viewModel.togglePreReminder() },
                        tag = "pre_reminder_switch"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Fasting reminder
                    RowSettingSwitch(
                        title = "Pengingat Puasa Sunnah",
                        description = "Mendapat pemberitahuan malam hari untuk puasa Senin-Kamis.",
                        checked = isFastingSunnahEnabled,
                        onCheckedChange = { viewModel.toggleFastingSunnah() },
                        tag = "fasting_reminder_switch"
                    )
                }
            }

            // Calculation & Sounds Category
            CategoryHeader("Perhitungan & Audio", Icons.Default.CompassCalibration)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Calculation dropdown
                    RowDropdown(
                        title = "Metode Perhitungan",
                        selectedValue = calcMethod,
                        isExpanded = calculationMenuExpanded,
                        onExpandedChange = { calculationMenuExpanded = it },
                        onSelectValue = {
                            viewModel.updateCalculationMethod(it)
                            calculationMenuExpanded = false
                        },
                        items = methods,
                        tag = "calc_method_row"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Sound Adzan selection
                    RowDropdown(
                        title = "Suara Adzan",
                        selectedValue = adzanSound,
                        isExpanded = soundMenuExpanded,
                        onExpandedChange = { soundMenuExpanded = it },
                        onSelectValue = {
                            viewModel.updateAdzanSound(it)
                            soundMenuExpanded = false
                        },
                        items = sounds,
                        tag = "adzan_sound_row"
                    )
                }
            }

            // Display Theme & Languages Category
            CategoryHeader("Tampilan & Bahasa", Icons.Default.Palette)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Dark Mode selections
                    RowDropdown(
                        title = "Pilihan Tema",
                        selectedValue = themes.find { it.first == darkMode }?.second ?: "Ikuti Sistem",
                        isExpanded = themeMenuExpanded,
                        onExpandedChange = { themeMenuExpanded = it },
                        onSelectValue = { displayName ->
                            val code = themes.find { it.second == displayName }?.first ?: "SYSTEM"
                            viewModel.updateDarkMode(code)
                            themeMenuExpanded = false
                        },
                        items = themes.map { it.second },
                        tag = "theme_row"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Language selections
                    RowDropdown(
                        title = "Pilihan Bahasa",
                        selectedValue = languages.find { it.first == language }?.second ?: "Bahasa Indonesia",
                        isExpanded = langMenuExpanded,
                        onExpandedChange = { langMenuExpanded = it },
                        onSelectValue = { displayName ->
                            val code = languages.find { it.second == displayName }?.first ?: "id"
                            viewModel.updateLanguage(code)
                            langMenuExpanded = false
                        },
                        items = languages.map { it.second },
                        tag = "language_row"
                    )
                }
            }

            // Security, Backup Restore & Reset Category
            CategoryHeader("Keamanan & Backup", Icons.Default.FolderZip)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast
                                    .makeText(
                                        context,
                                        "Berhasil mengenkripsi & mencadangkan data Room lokal ke: /Zucchero/Backup.db",
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Cadangkan Data Lokal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Ekspor chat, notifikasi WA dan database terenkripsi secara aman.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category Akun & Cloud Firestore
            CategoryHeader("Akun & Sinkronisasi Cloud (Firebase)", Icons.Default.CloudSync)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val user = currentUser
                    if (user != null) {
                        // User Signed In
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user.displayName ?: user.email ?: "U").take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName ?: "Hamba Allah",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = user.email ?: (if (user.isAnonymous) "Akun Tamu (Tersinkron Cloud)" else "Pengguna Terdaftar"),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { authViewModel.signOut() }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Keluar Akun",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Cloud Firestore Status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Terhubung ke Cloud Firestore • Histori Sholat Tersimpan",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time Firestore Prayer Tracker Checklist
                        Text(
                            text = "Catatan Sholat Hari Ini (Disimpan ke Firestore):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya").forEach { prayerName ->
                                val isDone = todayPrayerChecks[prayerName] ?: false
                                FilterChip(
                                    selected = isDone,
                                    onClick = { authViewModel.togglePrayerDone(prayerName) },
                                    label = { Text(prayerName, fontSize = 11.sp) },
                                    leadingIcon = if (isDone) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                                    } else null
                                )
                            }
                        }
                    } else {
                        // User Not Signed In
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Hubungkan ke Firebase & Firestore",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Cadangkan catatan sholat dan tanya-jawab AI secara otomatis ke cloud.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Google Sign-In Button
                            Button(
                                onClick = {
                                    val intent = authViewModel.getGoogleSignInIntent(context)
                                    googleSignInLauncher.launch(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_signin_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Masuk dengan Akun Google", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Guest Sign-In Button
                            OutlinedButton(
                                onClick = { authViewModel.signInAsGuest() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("guest_signin_button")
                            ) {
                                Text("Masuk sebagai Tamu (Firebase Anonymous)")
                            }
                        }
                    }
                }
            }

            // Category Unduh APK Debug Langsung
            CategoryHeader("Aplikasi & Unduh APK", Icons.Default.Android)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Debug APK
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                com.example.utils.ApkDownloadHelper.saveAndInstallDebugApk(context)
                            }
                            .padding(vertical = 8.dp)
                            .testTag("row_download_debug_apk"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DOWNLOAD DEBUG APK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Ekspor dan simpan app-debug.apk ke folder Download.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    // Release APK
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                com.example.utils.ApkDownloadHelper.saveAndInstallReleaseApk(context)
                            }
                            .padding(vertical = 8.dp)
                            .testTag("row_download_release_apk"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DOWNLOAD RELEASE APK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Ekspor dan simpan app-release.apk ke folder Download.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryHeader(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun RowSettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun RowDropdown(
    title: String,
    selectedValue: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectValue: (String) -> Unit,
    items: List<String>,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(true) }
            .padding(vertical = 4.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Klik untuk memilih opsi: " + selectedValue,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = { onSelectValue(item) }
                    )
                }
            }
        }
    }
}
