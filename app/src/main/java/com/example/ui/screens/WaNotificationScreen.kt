package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PrayerNotificationLog
import com.example.ui.viewmodel.WaNotificationViewModel
import com.example.utils.WhatsAppExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaNotificationScreen(viewModel: WaNotificationViewModel) {
    val context = LocalContext.current
    val prayerLogs by viewModel.prayerLogs.collectAsState()
    val prayerFilter by viewModel.prayerLogFilter.collectAsState()

    var showClearConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Log Sholat & Ekspor WA",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (prayerLogs.isNotEmpty()) {
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
            PrayerNotificationLogTab(
                logs = prayerLogs,
                currentFilter = prayerFilter,
                onSelectFilter = { viewModel.setPrayerLogFilter(it) },
                onDeleteLog = { viewModel.deletePrayerLog(it) },
                onAddSampleLog = { prayer, isPre -> viewModel.addSamplePrayerLog(prayer, isPre) }
            )
        }
    }

    // Confirmation dialog before clearing
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = {
                Text(
                    text = "Hapus Semua Log Sholat?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin membersihkan seluruh riwayat notifikasi waktu sholat yang tersimpan?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllPrayerLogs()
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
    val context = LocalContext.current
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (log.isPreReminder) 
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                else 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.isPreReminder) Icons.Default.Alarm else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (log.isPreReminder) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = log.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Waktu Sholat: ${log.prayerTime} • ${log.locationName}",
                            fontSize = 11.sp,
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
