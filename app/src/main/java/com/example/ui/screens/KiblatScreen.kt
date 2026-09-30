package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import java.util.Locale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.SettingsManager
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiblatScreen() {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }

    // Let's load the user's lat/lng to calculate exact Qibla degrees from North
    val userLat = settings.latitude.toDouble()
    val userLng = settings.longitude.toDouble()

    // Kaaba coordinates
    val kaabaLat = 21.4225
    val kaabaLng = 39.8262

    // Great circle direction calculation
    val latRad = Math.toRadians(userLat)
    val kaabaLatRad = Math.toRadians(kaabaLat)
    val lngDiffRad = Math.toRadians(kaabaLng - userLng)

    val y = sin(lngDiffRad)
    val x = cos(latRad) * tan(kaabaLatRad) - sin(latRad) * cos(lngDiffRad)
    var qiblaAngle = Math.toDegrees(atan2(y, x))
    if (qiblaAngle < 0) qiblaAngle += 360.0

    // Magnetic Heading state
    var azimuth by remember { mutableStateOf(0f) }

    // Sensor registration
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val sensorEventListener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    val yaw = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                    // Normalize azimuth (yaw is -180 to 180)
                    azimuth = (yaw + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // Smooth animator for compass transition
    val compassRotation = ( -azimuth )
    val qiblaRelativeAngle = (qiblaAngle - azimuth + 360.0) % 360.0

    val primaryGreen = MaterialTheme.colorScheme.primary
    val compassGold = MaterialTheme.colorScheme.tertiary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arah Kiblat AI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ZUCCHERO Compass",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = primaryGreen
            )
            Text(
                text = "Putar perangkat Anda untuk menyelaraskan dengan Ka'bah",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Dynamic Compass Drawing Component
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(140.dp))
                    .padding(16.dp)
            ) {
                // Interactive Canvas drawing Compass Rose
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(compassRotation)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2

                    // Draw outer ring
                    drawCircle(
                        color = primaryGreen.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4f)
                    )

                    // Draw ticks
                    for (angle in 0 until 360 step 30) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val start = Offset(
                            (center.x + (radius - 12) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 12) * sin(angleRad)).toFloat()
                        )
                        val end = Offset(
                            (center.x + radius * cos(angleRad)).toFloat(),
                            (center.y + radius * sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = primaryGreen.copy(alpha = 0.4f),
                            start = start,
                            end = end,
                            strokeWidth = 2f
                        )
                    }

                    // Draw cardinal letters
                    // Just basic representations relative to rotation
                }

                // Draw Qibla pointer Arrow on top relative to rotation
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = compassGold,
                    modifier = Modifier
                        .size(72.dp)
                        .rotate(qiblaRelativeAngle.toFloat())
                )

                // Golden Kaaba indicator point in outer ring
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate((qiblaAngle - azimuth).toFloat())
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2
                    
                    // Draw mini circle representing Kaaba at exact Qibla degrees
                    drawCircle(
                        color = compassGold,
                        radius = 16f,
                        center = Offset(center.x, center.y - radius)
                    )
                }

                // Center core point
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(primaryGreen, RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Information display cards
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sudut Kiblat",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f° dari Utara", qiblaAngle),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = primaryGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Suhu / Sensor",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "Heading: %.1f°", azimuth),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = compassGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guidance hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = primaryGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lakukan gerakan '8' untuk mengkalibrasi kompas",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
