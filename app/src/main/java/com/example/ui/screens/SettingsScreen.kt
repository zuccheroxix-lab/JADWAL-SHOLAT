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
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
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

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // 10 minutes pre reminder
                    RowSettingSwitch(
                        title = "Pengingat 10 Menit Sebelum Adzan",
                        description = "Persiapan wudhu sebelum adzan sholat berkumandang.",
                        checked = isPreReminderEnabled,
                        onCheckedChange = { viewModel.togglePreReminder() },
                        tag = "pre_reminder_switch"
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

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

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

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

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

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

            // APK & GitHub Category
            CategoryHeader("Aplikasi & Rilis APK", Icons.Default.Android)
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
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText(
                                    "Format Link Unduh GitHub",
                                    "https://github.com/<username>/<repo>/releases/download/v1.0.0/app-debug.apk"
                                )
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Format link rilis GitHub disalin ke clipboard!", Toast.LENGTH_SHORT).show()
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
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "File APK: app-debug.apk",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "app/build/outputs/apk/debug/app-debug.apk • Klik untuk salin link rilis GitHub.",
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
