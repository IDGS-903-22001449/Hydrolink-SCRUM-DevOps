package com.plantmonitor.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantmonitor.app.mqtt.MqttManager

@Composable
fun SettingsScreen(mqttManager: MqttManager) {
    val connectionStatus by mqttManager.connectionStatus.collectAsState()
    val autoWateringEnabled by mqttManager.autoWateringEnabled.collectAsState()
    val notificationsEnabled by mqttManager.notificationsEnabled.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⚙️ Configuración",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            // Estado de conexión
            SettingsCard(
                title = "Estado de Conexión",
                subtitle = connectionStatus,
                icon = when (connectionStatus) {
                    "Conectado" -> Icons.Default.CheckCircle
                    "Conectando..." -> Icons.Default.Refresh
                    else -> Icons.Default.Error
                },
                content = {
                    if (connectionStatus != "Conectado") {
                        TextButton(
                            onClick = { /* Reconnect logic */ }
                        ) {
                            Text("Reconectar")
                        }
                    }
                }
            )
        }

        item {
            // Configuración de riego automático
            SettingsCard(
                title = "Riego Automático",
                subtitle = "${if (autoWateringEnabled) "Activado" else "Desactivado"} • ${mqttManager.getAutoWateringStatus()}",
                icon = Icons.Default.AutoMode,
                content = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Activar riego inteligente")
                            Switch(
                                checked = autoWateringEnabled,
                                onCheckedChange = { mqttManager.toggleAutoWatering() }
                            )
                        }

                        if (autoWateringEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Se activa cuando humedad < 30%\n• Se detiene cuando humedad ≥ 65%\n• ${mqttManager.getLastWateringInfo()}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            )
        }

        item {
            // Configuración de notificaciones
            SettingsCard(
                title = "Notificaciones Inteligentes",
                subtitle = if (notificationsEnabled) "Activadas - Monitoreo completo" else "Desactivadas",
                icon = Icons.Default.Notifications,
                content = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recibir alertas")
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { mqttManager.toggleNotifications() }
                            )
                        }

                        if (notificationsEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Alertas por:\n• Humedad crítica (< 25% o > 80%)\n• Temperatura extrema (< 15°C o > 32°C)\n• Luz inadecuada (< 100 lx o > 1200 lx)\n• Nivel de agua bajo\n• TDS alto (> 800 PPM)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            )
        }

        item {
            // Umbrales de sensores
            SettingsCard(
                title = "Umbrales de Sensores",
                subtitle = "Valores para alertas y riego automático",
                icon = Icons.Default.Tune,
                content = {
                    Column {
                        ThresholdInfo("💧 Humedad Suelo", "25% - 80%", "Crítico si está fuera del rango")
                        ThresholdInfo("🌡️ Temperatura", "15°C - 32°C", "Óptimo: 18-28°C")
                        ThresholdInfo("☀️ Luz", "100 - 1200 lx", "Óptimo: 200-800 lx")
                        ThresholdInfo("🚰 Riego Auto", "< 30%", "Se detiene en 65%")
                    }
                }
            )
        }

        item {
            // Información del dispositivo
            SettingsCard(
                title = "Información del Dispositivo",
                subtitle = "ESP32 Plant Monitor",
                icon = Icons.Default.DeviceHub,
                content = {
                    Column {
                        InfoRow("Broker MQTT", "mqtt-dashboard.com")
                        InfoRow("Usuario", "vicente")
                        InfoRow("Puerto", "1883")
                        InfoRow("Frecuencia", "Tiempo real")
                    }
                }
            )
        }

        item {
            // Sistema de seguridad
            SettingsCard(
                title = "Sistema de Seguridad",
                subtitle = "Protecciones automáticas",
                icon = Icons.Default.Security,
                content = {
                    Column {
                        Text(
                            text = "🛡️ Protecciones Activas:",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "• Máximo 30s de riego continuo\n• Pausa de 5 min entre riegos\n• Verificación de nivel de agua\n• Notificaciones con cooldown\n• Detección de sensores desconectados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            )
        }

        item {
            // Acerca de
            SettingsCard(
                title = "Acerca de PlantMonitor",
                subtitle = "Versión 2.0.0 - Sistema Inteligente",
                icon = Icons.Default.Info,
                content = {
                    Text(
                        text = "Monitoreo inteligente de plantas con IA.\n\n✨ Nuevas características:\n• Riego automático inteligente\n• Notificaciones contextuales\n• Protecciones de seguridad\n• Análisis de salud de planta",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            )
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ThresholdInfo(label: String, range: String, description: String) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = range,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}