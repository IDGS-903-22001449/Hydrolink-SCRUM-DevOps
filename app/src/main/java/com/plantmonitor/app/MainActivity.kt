// MainActivity.kt
package com.plantmonitor.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.plantmonitor.app.ui.theme.PlantMonitorTheme
import com.plantmonitor.app.navigation.PlantMonitorNavigation
import com.plantmonitor.app.mqtt.MqttManager

class MainActivity : ComponentActivity() {
    private lateinit var mqttManager: MqttManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Crear canal de notificaciones
        createNotificationChannel()

        // Inicializar MQTT Manager
        mqttManager = MqttManager(this)

        setContent {
            PlantMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlantMonitorNavigation(mqttManager)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Plant Monitoring"
            val descriptionText = "Notifications for plant care"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("PLANT_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }
}