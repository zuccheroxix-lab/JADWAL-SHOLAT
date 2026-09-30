package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PrayerNotificationLog
import com.example.data.database.WaNotification
import com.example.ui.viewmodel.WaNotificationViewModel
import com.example.utils.WhatsAppExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaNotificationScreen(viewModel: WaNotificationViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterImportant by viewModel.filterOnlyImportant.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val prayerLogs by viewModel.prayerLogs.collectAsState()
    val prayerFilter by viewModel.prayerLogFilter.collectAsState()

    var isPermissionAllowed by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }

    // Check notification listener package status on resume/reload
    fun checkPermission() {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        isPermissionAllowed = flat != null && flat.contains(context.packageName)
    }

    LaunchedEffect(Unit) {
        checkPermission()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "Notifikasi WhatsApp" else "Log Sholat & Ekspor WA",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (selectedTab == 0 && notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmationDialog = true },
                            modifier = Modifier.testTag("clear_notifications_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Hapus Semua Pesan")
                        }
                    } else if (selectedTab == 1 && prayerLogs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val report = WhatsAppExporter.formatMultipleLogs(prayerLogs)
                                WhatsAppExporter.shareToWhatsApp(context, report)
                            },
                            modifier = Modifier.testTag("export_all_wa_button")
                        ) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = "Bagikan Semua ke WA")
                        }
                        IconButton(
                            onClick = { showClearConfirmationDialog = true },
                            modifier = Modifier.testTag("clear_prayer_logs_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Hapus Log Sholat")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Elegant Primary Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pesan WA Masuk", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_wa_messages")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Sholat & WA", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_prayer_logs")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body depending on selected tab
            if (selectedTab == 0) {
                WhatsAppListenerTab(
                    isPermissionAllowed = isPermissionAllowed,
                    onOpenPermissionSettings = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (e: Exception) {
                            // ignore fallback
                        }
                    },
                    onRefreshPermission = { checkPermission() },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    filterImportant = filterImportant,
                    onToggleFilterImportant = { viewModel.toggleFilterImportant() },
                    notifications = notifications,
                    onDeleteNotification = { viewModel.deleteNotification(it) }
                )
            } else {
                PrayerNotificationLogTab(
                    logs = prayerLogs,
                    currentFilter = prayerFilter,
                    onSelectFilter = { viewModel.setPrayerLogFilter(it) },
                    onDeleteLog = { viewModel.deletePrayerLog(it) },
                    onAddSampleLog = { prayer, isPre -> viewModel.addSamplePrayerLog(prayer, isPre) }
                )
            }
        }
    }

    // Confirmation dialog before clearing
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = {
                Text(
                    text = if (selectedTab == 0) "Hapus Semua Pesan?" else "Hapus Semua Log Sholat?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (selectedTab == 0) {
                        "Apakah Anda yakin ingin menghapus seluruh riwayat pesan WhatsApp yang tersimpan secara lokal?"
                    } else {
                        "Apakah Anda yakin ingin membersihkan seluruh riwayat notifikasi waktu sholat tersimpan?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            viewModel.clearAll()
                        } else {
                            viewModel.clearAllPrayerLogs()
                        }
                        showClearConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmationDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun PrayerNotificationLogTab(
    logs: List<PrayerNotificationLog>,
    currentFilter: String,
    onSelectFilter: (String) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onAddSampleLog: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val filterOptions = listOf("Semua", "Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya", "Pengingat")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Summary & Export WhatsApp Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ekspor WhatsApp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${logs.size} log notifikasi tersimpan",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Test Log Button
                    FilledTonalButton(
                        onClick = { onAddSampleLog("Dzuhur", false) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Catat Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Riwayat alarm sholat dicatat secara otomatis dan dapat diekspor langsung ke kontak atau grup WhatsApp dengan format Islami rapi.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons for Batch Export & Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val formatted = WhatsAppExporter.formatMultipleLogs(logs)
                            WhatsAppExporter.shareToWhatsApp(context, formatted)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366) // WhatsApp Brand Green
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_export_all_whatsapp"),
                        enabled = logs.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim ke WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val formatted = WhatsAppExporter.formatMultipleLogs(logs)
                            WhatsAppExporter.copyToClipboard(context, formatted, "Laporan Log Sholat WA")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_copy_all_whatsapp"),
                        enabled = logs.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Teks WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Horizontal filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        // List of logs
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AlarmOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada Riwayat Notifikasi Sholat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Setiap notifikasi waktu sholat yang masuk akan disimpan otomatis di database lokal ini dan siap dibagikan ke WhatsApp.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAddSampleLog("Dzuhur", false) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambahkan Contoh Log")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    PrayerLogCard(
                        log = log,
                        onDelete = { onDeleteLog(log.id) },
                        onShareWa = {
                            val formatted = WhatsAppExporter.formatSingleLog(log)
                            WhatsAppExporter.shareToWhatsApp(context, formatted)
                        },
                        onCopyWa = {
                            val formatted = WhatsAppExporter.formatSingleLog(log)
                            WhatsAppExporter.copyToClipboard(context, formatted, "Log ${log.prayerName}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerLogCard(
    log: PrayerNotificationLog,
    onDelete: () -> Unit,
    onShareWa: () -> Unit,
    onCopyWa: () -> Unit
) {
    val dateSdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("id", "ID"))
    val recordedTimeStr = dateSdf.format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (log.isPreReminder) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.isPreReminder) Icons.Default.HourglassBottom else Icons.Default.Mosque,
                            contentDescription = null,
                            tint = if (log.isPreReminder) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = log.prayerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (log.isPreReminder) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (log.isPreReminder) "10 Menit Sebelum" else "Waktu Sholat",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.isPreReminder) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Jadwal: ${log.prayerTime} WIB" + if (log.locationName.isNotEmpty()) " • ${log.locationName}" else "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = log.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tercatat pada: $recordedTimeStr",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row for export to WhatsApp and copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCopyWa,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onShareWa,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kirim WA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WhatsAppListenerTab(
    isPermissionAllowed: Boolean,
    onOpenPermissionSettings: () -> Unit,
    onRefreshPermission: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterImportant: Boolean,
    onToggleFilterImportant: () -> Unit,
    notifications: List<WaNotification>,
    onDeleteNotification: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Permission Alert prompt if not allowed
        if (!isPermissionAllowed) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Izin Pembaca Pesan WhatsApp Dibutuhkan",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aplikasi memerlukan izin Notification Listener agar dapat mencatat pengingat penting dari WhatsApp secara lokal tanpa server pihak ketiga.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenPermissionSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Buka Pengaturan Izin")
                        }
                        OutlinedButton(onClick = onRefreshPermission) {
                            Text("Cek Status")
                        }
                    }
                }
            }
        }

        // Search and Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            placeholder = { Text("Cari pesan atau kontak WA...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterImportant,
                onClick = onToggleFilterImportant,
                label = { Text("Hanya Pesan Penting / Sholat") },
                leadingIcon = {
                    if (filterImportant) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
                }
            )

            Text(
                text = "${notifications.size} pesan",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MarkChatUnread,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada Pesan WhatsApp Masuk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ketika WhatsApp menerima pesan di latar belakang, pesan akan otomatis terdeteksi dan tercatat rapi di sini.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    WaMessageItemCard(item = item, onDelete = { onDeleteNotification(item.id) })
                }
            }
        }
    }
}

@Composable
fun WaMessageItemCard(item: WaNotification, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale("id", "ID"))
    val formattedDate = sdf.format(Date(item.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.sender.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.sender,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isImportant) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("Penting", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus item",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
