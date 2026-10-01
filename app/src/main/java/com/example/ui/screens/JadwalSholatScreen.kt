package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.SettingsManager
import com.example.utils.PrayerTimeCalculator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JadwalSholatScreen(viewModel: com.example.ui.viewmodel.PrayerViewModel? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { SettingsManager(context) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Hari Ini, 1 = Besok

    val calendarToday = Calendar.getInstance()
    val calendarTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    val vmTimes by viewModel?.prayerTimes?.collectAsState() ?: remember { mutableStateOf(null) }
    val dataSourceInfo by viewModel?.dataSourceInfo?.collectAsState() ?: remember { mutableStateOf("Aladhan API") }

    val timesToday = vmTimes ?: PrayerTimeCalculator.calculateTimes(
        settings.latitude.toDouble(),
        settings.longitude.toDouble(),
        settings.calculationMethod,
        calendarToday
    )

    val timesTomorrow = PrayerTimeCalculator.calculateTimes(
        settings.latitude.toDouble(),
        settings.longitude.toDouble(),
        settings.calculationMethod,
        calendarTomorrow
    )

    val activeTimes = if (selectedTab == 0) timesToday else timesTomorrow
    val activeCalendar = if (selectedTab == 0) calendarToday else calendarTomorrow

    val dfMasehi = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    val outputDateStr = dfMasehi.format(activeCalendar.time)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Sholat AI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Elegant Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                divider = { Divider() },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Hari Ini", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Besok", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Date Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = outputDateStr,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeTimes.hijriDate,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Grid of detailed prayer items
                val listPrays = listOf(
                    "Imsak" to activeTimes.imsak,
                    "Subuh" to activeTimes.subuh,
                    "Terbit" to activeTimes.terbit,
                    "Dzuhur" to activeTimes.dzuhur,
                    "Ashar" to activeTimes.ashar,
                    "Maghrib" to activeTimes.maghrib,
                    "Isya" to activeTimes.isya
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        listPrays.forEachIndexed { idx, pray ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pray.first,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pray.second,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (idx < listPrays.size - 1) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }

                // Calculation details info card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Sumber Data: $dataSourceInfo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Metode perhitungan: ${settings.calculationMethod} (Koordinat GPS: ${settings.latitude}, ${settings.longitude}).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
