package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.preferences.SettingsManager
import com.example.ui.viewmodel.PrayerViewModel
import com.example.utils.PrayerTimeCalculator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JadwalSholatScreen(viewModel: PrayerViewModel? = null) {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Hari Ini, 1 = Besok

    val calendarToday = Calendar.getInstance()
    val calendarTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    val vmTimes by viewModel?.prayerTimes?.collectAsState() ?: remember { mutableStateOf(null) }
    val dataSourceInfo by viewModel?.dataSourceInfo?.collectAsState() ?: remember { mutableStateOf("GPS & Kalender Astronomis") }
    val isRefreshing by viewModel?.isRefreshing?.collectAsState() ?: remember { mutableStateOf(false) }
    val currentLocationName by viewModel?.currentLocationName?.collectAsState() ?: remember { mutableStateOf(settings.cityName) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel?.requestGpsLocation {}
        }
    }

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
                actions = {
                    IconButton(
                        onClick = {
                            val fineCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            if (fineCheck == PackageManager.PERMISSION_GRANTED) {
                                viewModel?.requestGpsLocation {}
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Deteksi GPS")
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
                .padding(horizontal = 16.dp)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                divider = { HorizontalDivider() },
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLocationName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mosque,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = pray.first,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = pray.second,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (idx < listPrays.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
                                text = "Lokasi: $currentLocationName (Koordinat GPS: ${String.format(Locale.US, "%.4f, %.4f", settings.latitude, settings.longitude)}).",
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
