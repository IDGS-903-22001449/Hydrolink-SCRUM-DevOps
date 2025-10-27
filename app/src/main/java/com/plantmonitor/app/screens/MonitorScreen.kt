package com.plantmonitor.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantmonitor.app.mqtt.MqttManager

@Composable
fun MonitorScreen(mqttManager: MqttManager) {
    val sensorData by mqttManager.sensorData.collectAsState()
    val connectionStatus by mqttManager.connectionStatus.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "📊 Monitor de Sensores",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            // Control de bomba
            PumpControlCard(
                pumpStatus = sensorData.pumpStatus,
                onPumpCommand = { command ->
                    mqttManager.sendPumpCommand(command)
                }
            )
        }

        item {
            // Control de techo
            RoofControlCard(
                roofStatus = sensorData.roofStatus,
                onRoofCommand = { command ->
                    mqttManager.sendRoofCommand(command)
                }
            )
        }

        item {
            // Sensores principales
            SensorDetailCard(
                title = "💧 TDS (Sólidos Disueltos)",
                value = "${sensorData.tds} PPM",
                icon = Icons.Default.Water,
                status = getTdsStatus(sensorData.tds)
            )
        }

        item {
            SensorDetailCard(
                title = "🌱 Humedad del Suelo",
                value = "${sensorData.soilMoisture}% (${sensorData.soilStatus})",
                icon = Icons.Default.Grass,
                status = getSoilMoistureStatus(sensorData.soilMoisture)
            )
        }

        item {
            SensorDetailCard(
                title = "☀ Intensidad de Luz",
                value = "${sensorData.light} lx",
                icon = Icons.Default.WbSunny,
                status = getLightStatus(sensorData.light)
            )
        }

        item {
            SensorDetailCard(
                title = "🌡 Temperatura",
                value = "${sensorData.temperature}°C",
                icon = Icons.Default.Thermostat,
                status = getTemperatureStatus(sensorData.temperature)
            )
        }

        item {
            SensorDetailCard(
                title = "💨 Humedad Ambiental",
                value = "${sensorData.humidity}%",
                icon = Icons.Default.Air,
                status = getHumidityStatus(sensorData.humidity)
            )
        }

        item {
            SensorDetailCard(
                title = "🚰 Nivel de Agua",
                value = sensorData.waterLevel,
                icon = Icons.Default.LocalDrink,
                status = getWaterLevelStatus(sensorData.waterLevel)
            )
        }
    }
}

@Composable
fun PumpControlCard(
    pumpStatus: String,
    onPumpCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (pumpStatus == "ON")
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "💧 Control Bomba de Agua",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Estado: $pumpStatus",
                        color = if (pumpStatus == "ON")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = if (pumpStatus == "ON") Icons.Default.Water else Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (pumpStatus == "ON")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPumpCommand("ON") },
                    enabled = pumpStatus != "ON",
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Encender")
                }

                OutlinedButton(
                    onClick = { onPumpCommand("OFF") },
                    enabled = pumpStatus != "OFF",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apagar")
                }
            }
        }
    }
}

@Composable
fun RoofControlCard(
    roofStatus: String,
    onRoofCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (roofStatus == "ABIERTO")
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⛅ Control Techo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Estado: $roofStatus",
                        color = if (roofStatus == "ABIERTO")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = if (roofStatus == "ABIERTO") Icons.Default.WbSunny else Icons.Default.Cloud,
                    contentDescription = null,
                    tint = if (roofStatus == "ABIERTO")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onRoofCommand("ABRIR") },
                    enabled = roofStatus != "ABIERTO",
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir")
                }

                OutlinedButton(
                    onClick = { onRoofCommand("CERRAR") },
                    enabled = roofStatus != "CERRADO",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloseFullscreen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun SensorDetailCard(
    title: String,
    value: String,
    icon: ImageVector,
    status: SensorStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = status.color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = status.color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = status.description,
                    fontSize = 12.sp,
                    color = status.color
                )
            }

            Icon(
                imageVector = status.icon,
                contentDescription = null,
                tint = status.color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

data class SensorStatus(
    val description: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
fun getTdsStatus(tds: String): SensorStatus {
    val value = tds.toFloatOrNull() ?: 0f
    return when {
        value < 300 -> SensorStatus("Excelente", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        value < 600 -> SensorStatus("Bueno", Color(0xFFFF9800), Icons.Default.Warning)
        else -> SensorStatus("Alto", Color(0xFFF44336), Icons.Default.Error)
    }
}

@Composable
fun getSoilMoistureStatus(moisture: String): SensorStatus {
    val value = moisture.toIntOrNull() ?: 0
    return when {
        value >= 60 -> SensorStatus("Muy húmedo", Color(0xFF2196F3), Icons.Default.CheckCircle)
        value >= 40 -> SensorStatus("Óptimo", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        value >= 20 -> SensorStatus("Seco", Color(0xFFFF9800), Icons.Default.Warning)
        else -> SensorStatus("Muy seco", Color(0xFFF44336), Icons.Default.Error)
    }
}

@Composable
fun getLightStatus(light: String): SensorStatus {
    val value = light.toFloatOrNull() ?: 0f
    return when {
        value >= 800 -> SensorStatus("Muy brillante", Color(0xFFFF9800), Icons.Default.Warning)
        value >= 200 -> SensorStatus("Óptimo", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        value >= 50 -> SensorStatus("Bajo", Color(0xFFFF9800), Icons.Default.Warning)
        else -> SensorStatus("Muy bajo", Color(0xFFF44336), Icons.Default.Error)
    }
}

@Composable
fun getTemperatureStatus(temperature: String): SensorStatus {
    val value = temperature.toFloatOrNull() ?: 0f
    return when {
        value >= 30 -> SensorStatus("Calor", Color(0xFFFF9800), Icons.Default.Warning)
        value >= 18 -> SensorStatus("Óptimo", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        value >= 10 -> SensorStatus("Frío", Color(0xFF2196F3), Icons.Default.Warning)
        else -> SensorStatus("Muy frío", Color(0xFFF44336), Icons.Default.Error)
    }
}

@Composable
fun getHumidityStatus(humidity: String): SensorStatus {
    val value = humidity.toFloatOrNull() ?: 0f
    return when {
        value >= 70 -> SensorStatus("Alta", Color(0xFF2196F3), Icons.Default.CheckCircle)
        value >= 40 -> SensorStatus("Óptimo", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        value >= 20 -> SensorStatus("Baja", Color(0xFFFF9800), Icons.Default.Warning)
        else -> SensorStatus("Muy baja", Color(0xFFF44336), Icons.Default.Error)
    }
}

@Composable
fun getWaterLevelStatus(waterLevel: String): SensorStatus {
    return when (waterLevel) {
        "ALTO" -> SensorStatus("Nivel bueno", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        "BAJO" -> SensorStatus("Rellenar pronto", Color(0xFFF44336), Icons.Default.Error)
        else -> SensorStatus("Desconocido", Color(0xFF9E9E9E), Icons.Default.Help)
    }
}