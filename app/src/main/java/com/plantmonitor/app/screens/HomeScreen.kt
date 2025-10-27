package com.plantmonitor.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantmonitor.app.mqtt.MqttManager

@Composable
fun HomeScreen(mqttManager: MqttManager) {
    val connectionStatus by mqttManager.connectionStatus.collectAsState()
    val sensorData by mqttManager.sensorData.collectAsState()
    val autoWateringEnabled by mqttManager.autoWateringEnabled.collectAsState()
    val notificationsEnabled by mqttManager.notificationsEnabled.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌱 PlantMonitor Pro",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sistema Inteligente de Cuidado",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        item {
            // Estado de conexión
            ConnectionStatusCard(connectionStatus)
        }

        item {
            // Estado del sistema automático
            AutoSystemStatusCard(
                autoWateringEnabled = autoWateringEnabled,
                notificationsEnabled = notificationsEnabled,
                autoWateringStatus = mqttManager.getAutoWateringStatus(),
                lastWateringInfo = mqttManager.getLastWateringInfo()
            )
        }

        item {
            // Controles rápidos
            QuickControlsCard(
                pumpStatus = sensorData.pumpStatus,
                autoWateringEnabled = autoWateringEnabled,
                onPumpToggle = { status ->
                    mqttManager.sendPumpCommand(if (status == "ON") "OFF" else "ON")
                },
                onAutoWateringToggle = { mqttManager.toggleAutoWatering() }
            )
        }

        item {
            // Resumen de sensores
            SensorSummaryCard(sensorData)
        }

        item {
            // Estado de la planta con alertas
            PlantHealthCard(sensorData)
        }

        item {
            // Alertas críticas
            CriticalAlertsCard(sensorData)
        }
    }
}

@Composable
fun ConnectionStatusCard(status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                "Conectado" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                "Conectando..." -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status) {
                    "Conectado" -> Icons.Default.CheckCircle
                    "Conectando..." -> Icons.Default.Refresh
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (status) {
                    "Conectado" -> MaterialTheme.colorScheme.primary
                    "Conectando..." -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Estado: $status",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (status == "Conectado") {
                    Text(
                        text = "Recibiendo datos en tiempo real",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun AutoSystemStatusCard(
    autoWateringEnabled: Boolean,
    notificationsEnabled: Boolean,
    autoWateringStatus: String,
    lastWateringInfo: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🤖 Sistema Automático",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Riego automático
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (autoWateringEnabled) Icons.Default.AutoMode else Icons.Default.PanTool,
                            contentDescription = null,
                            tint = if (autoWateringEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Riego Auto",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = if (autoWateringEnabled) autoWateringStatus else "Desactivado",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                // Notificaciones
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Alertas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = if (notificationsEnabled) "Activas" else "Pausadas",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            if (autoWateringEnabled && lastWateringInfo != "Sin riegos registrados") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lastWateringInfo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun QuickControlsCard(
    pumpStatus: String,
    autoWateringEnabled: Boolean,
    onPumpToggle: (String) -> Unit,
    onAutoWateringToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎮 Controles Rápidos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onPumpToggle(pumpStatus) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pumpStatus == "ON")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (pumpStatus == "ON") Icons.Default.Stop else Icons.Default.Water,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (pumpStatus == "ON") "Parar Riego" else "Regar Ahora")
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = onAutoWateringToggle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (autoWateringEnabled) Icons.Default.AutoMode else Icons.Default.PanTool,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (autoWateringEnabled) "Auto: ON" else "Auto: OFF")
                }
            }
        }
    }
}

@Composable
fun SensorSummaryCard(sensorData: com.plantmonitor.app.mqtt.SensorData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Sensores en Tiempo Real",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorMiniCard("💧", "${sensorData.soilMoisture}%", "Humedad", getSensorColor(sensorData.soilMoisture.toIntOrNull(), 25, 80))
                SensorMiniCard("🌡️", "${sensorData.temperature}°C", "Temp.", getTemperatureColor(sensorData.temperature.toFloatOrNull()))
                SensorMiniCard("☀️", "${sensorData.light} lx", "Luz", getLightColor(sensorData.light.toFloatOrNull()))
                SensorMiniCard("🚰", sensorData.waterLevel, "Agua", getWaterLevelColor(sensorData.waterLevel))
            }
        }
    }
}

@Composable
fun SensorMiniCard(icon: String, value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun CriticalAlertsCard(sensorData: com.plantmonitor.app.mqtt.SensorData) {
    val alerts = getCriticalAlerts(sensorData)

    if (alerts.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚠️ Alertas Críticas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                alerts.forEach { alert ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alert,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlantHealthCard(sensorData: com.plantmonitor.app.mqtt.SensorData) {
    val healthScore = calculateHealthScore(sensorData)
    val healthColor = when {
        healthScore >= 80 -> Color(0xFF4CAF50)
        healthScore >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🌿 Estado de la Planta",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = healthScore / 100f,
                    modifier = Modifier.size(80.dp),
                    color = healthColor,
                    strokeWidth = 8.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Salud: ${healthScore.toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                    Text(
                        text = getHealthStatus(healthScore),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = getHealthRecommendation(sensorData),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// Funciones auxiliares para colores y alertas
@Composable
fun getSensorColor(value: Int?, min: Int, max: Int): Color {
    return when {
        value == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        value < min -> Color(0xFFF44336)
        value > max -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
}

@Composable
fun getTemperatureColor(temp: Float?): Color {
    return when {
        temp == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        temp < 15f || temp > 32f -> Color(0xFFF44336)
        temp < 18f || temp > 28f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
}

@Composable
fun getLightColor(light: Float?): Color {
    return when {
        light == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        light < 100f || light > 1200f -> Color(0xFFF44336)
        light < 200f || light > 800f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
}

@Composable
fun getWaterLevelColor(level: String): Color {
    return when (level) {
        "ALTO" -> Color(0xFF4CAF50)
        "BAJO" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
}

fun getCriticalAlerts(sensorData: com.plantmonitor.app.mqtt.SensorData): List<String> {
    val alerts = mutableListOf<String>()

    sensorData.soilMoisture.toIntOrNull()?.let { moisture ->
        if (moisture < 25) alerts.add("Humedad crítica: ${moisture}%")
    }

    sensorData.temperature.toFloatOrNull()?.let { temp ->
        if (temp < 15f) alerts.add("Temperatura muy baja: ${temp}°C")
        if (temp > 32f) alerts.add("Temperatura muy alta: ${temp}°C")
    }

    if (sensorData.waterLevel == "BAJO") {
        alerts.add("Depósito de agua vacío")
    }

    sensorData.tds.toFloatOrNull()?.let { tds ->
        if (tds > 800f) alerts.add("TDS muy alto: ${tds.toInt()} PPM")
    }

    return alerts
}

fun getHealthRecommendation(sensorData: com.plantmonitor.app.mqtt.SensorData): String {
    val moisture = sensorData.soilMoisture.toIntOrNull()
    val temp = sensorData.temperature.toFloatOrNull()
    val light = sensorData.light.toFloatOrNull()

    return when {
        moisture != null && moisture < 30 -> "💧 Necesita más agua"
        temp != null && temp > 28f -> "🌡️ Buscar lugar más fresco"
        temp != null && temp < 18f -> "🔥 Necesita más calor"
        light != null && light < 200f -> "☀️ Necesita más luz"
        light != null && light > 800f -> "🌳 Necesita sombra parcial"
        else -> "✅ Condiciones óptimas"
    }
}

fun calculateHealthScore(sensorData: com.plantmonitor.app.mqtt.SensorData): Float {
    var score = 0f
    var factors = 0

    // Humedad del suelo (30% del score)
    sensorData.soilMoisture.toIntOrNull()?.let { moisture ->
        score += when {
            moisture in 40..70 -> 30f
            moisture in 30..80 -> 20f
            else -> 10f
        }
        factors++
    }

    // Temperatura (25% del score)
    sensorData.temperature.toFloatOrNull()?.let { temp ->
        score += when {
            temp in 18f..28f -> 25f
            temp in 15f..32f -> 15f
            else -> 5f
        }
        factors++
    }

    // Luz (25% del score)
    sensorData.light.toFloatOrNull()?.let { light ->
        score += when {
            light in 200f..800f -> 25f
            light in 100f..1000f -> 15f
            else -> 5f
        }
        factors++
    }

    // Nivel de agua (20% del score)
    score += when (sensorData.waterLevel) {
        "ALTO" -> 20f
        "BAJO" -> 5f
        else -> 10f
    }
    factors++

    return if (factors > 0) score else 0f
}

fun getHealthStatus(score: Float): String {
    return when {
        score >= 80 -> "Excelente estado"
        score >= 60 -> "Buen estado"
        score >= 40 -> "Necesita atención"
        else -> "Estado crítico"
    }
}