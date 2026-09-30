package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MainApp
import com.example.utils.AlarmScheduler

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Ensure automatic local prayer adzan alarms are registered correctly
    AlarmScheduler.scheduleAlarms(this)

    setContent {
      MainApp()
    }
  }
}

