package com.plantmonitor.app.mqtt

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.text.Charsets.UTF_8

data class SensorData(
    val tds: String = "--",
    val soilMoisture: String = "--",
    val soilStatus: String = "--",
    val light: String = "--",
    val waterLevel: String = "--",
    val temperature: String = "--",
    val humidity: String = "--",
    val pumpStatus: String = "OFF",
    val roofStatus: String = "CERRADO"   // NUEVO: Control del techo
)

data class NotificationThresholds(
    val soilMoistureMin: Int = 25,
    val soilMoistureMax: Int = 80,
    val temperatureMin: Float = 15f,
    val temperatureMax: Float = 32f,
    val lightMin: Float = 100f,
    val lightMax: Float = 1200f,
    val humidityMin: Float = 30f,
    val humidityMax: Float = 80f,
    val tdsMax: Float = 800f
)

data class AutoWateringConfig(
    val moistureThreshold: Int = 30,
    val stopMoistureLevel: Int = 65,
    val maxWateringDuration: Long = 30000L, // 30 segundos máximo
    val cooldownPeriod: Long = 300000L // 5 minutos entre riegos
)

class MqttManager(private val context: Context) {
    private var mqttClient: Mqtt5BlockingClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Configuración MQTT
    private val host = "mqtt-dashboard.com"
    private val username = "vicente"
    private val password = "hola123456789"

    // Topics MQTT
    private val pumpCommandTopic = "esp32/pump/command"
    private val roofCommandTopic = "esp32/roof/command"    // NUEVO: Topic para comandos del techo

    // Configuración de umbrales y riego automático
    private val thresholds = NotificationThresholds()
    private val autoWateringConfig = AutoWateringConfig()

    // Control de notificaciones para evitar spam
    private val lastNotificationTime = mutableMapOf<String, Long>()
    private val notificationCooldown = 300000L // 5 minutos

    // Control de riego automático
    private var lastWateringTime = 0L
    private var isCurrentlyWatering = false
    private var wateringStartTime = 0L
    private var wateringJob: Job? = null

    // Estados observables
    private val _connectionStatus = MutableStateFlow("Desconectado")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _autoWateringEnabled = MutableStateFlow(true)
    val autoWateringEnabled: StateFlow<Boolean> = _autoWateringEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    init {
        connectAndListen()
    }

    private fun connectAndListen() {
        coroutineScope.launch {
            try {
                _connectionStatus.value = "Conectando..."

                mqttClient = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost(host)
                    .serverPort(1883)
                    .buildBlocking()

                val connection = mqttClient?.connectWith()
                    ?.simpleAuth()
                    ?.username(username)
                    ?.password(UTF_8.encode(password))
                    ?.applySimpleAuth()
                    ?.send()

                if (connection?.reasonCode?.isError == false) {
                    _connectionStatus.value = "Conectado"

                    // Suscribirse a todos los topics de sensores
                    val topics = listOf(
                        "esp32/tds",
                        "esp32/soil/moisture",
                        "esp32/soil/status",
                        "esp32/light",
                        "esp32/water/level",
                        "esp32/temperature",
                        "esp32/humidity",
                        "esp32/pump/status",
                        "esp32/roof/status"  // NUEVO: Suscripción al estado del techo
                    )

                    topics.forEach { topic ->
                        mqttClient?.subscribeWith()
                            ?.topicFilter(topic)
                            ?.qos(MqttQos.AT_LEAST_ONCE)
                            ?.send()
                    }

                    // Escuchar mensajes
                    launch {
                        val received = mqttClient?.publishes(ALL, true)
                        while (true) {
                            val publish = received?.receive()
                            if (publish != null) {
                                val topic = publish.topic.toString()
                                val payload = String(publish.payloadAsBytes, UTF_8)
                                handleMqttMessage(topic, payload)
                            }
                        }
                    }
                } else {
                    _connectionStatus.value = "Error de conexión"
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Error: ${e.message}"
                Log.e("MQTT", "Error de conexión", e)
            }
        }
    }

    private fun handleMqttMessage(topic: String, payload: String) {
        val currentData = _sensorData.value
        val newData = when (topic) {
            "esp32/tds" -> {
                val newData = currentData.copy(tds = payload)
                checkTdsLevels(payload)
                newData
            }
            "esp32/soil/moisture" -> {
                val newData = currentData.copy(soilMoisture = payload)
                checkSoilMoisture(payload)
                newData
            }
            "esp32/soil/status" -> currentData.copy(soilStatus = payload)
            "esp32/light" -> {
                val newData = currentData.copy(light = payload)
                checkLightLevels(payload)
                newData
            }
            "esp32/water/level" -> {
                val newData = currentData.copy(waterLevel = payload)
                checkWaterLevel(payload)
                newData
            }
            "esp32/temperature" -> {
                val newData = currentData.copy(temperature = payload)
                checkTemperature(payload)
                newData
            }
            "esp32/humidity" -> {
                val newData = currentData.copy(humidity = payload)
                checkHumidity(payload)
                newData
            }
            "esp32/pump/status" -> {
                val newData = currentData.copy(pumpStatus = payload)
                handlePumpStatusChange(payload)
                newData
            }
            "esp32/roof/status" -> {
                val newData = currentData.copy(roofStatus = payload)
                handleRoofStatusChange(payload)  // NUEVO: Manejar cambios del techo
                newData
            }
            else -> currentData
        }
        _sensorData.value = newData

        // Evaluar condiciones generales después de actualizar datos
        evaluateAutoWatering(newData)
    }

    private fun checkTdsLevels(tdsValue: String) {
        try {
            val tds = tdsValue.toFloatOrNull() ?: return

            when {
                tds > thresholds.tdsMax -> {
                    sendThrottledNotification(
                        "tds_high",
                        "⚠️ TDS Muy Alto",
                        "Los sólidos disueltos están en ${tds.toInt()} PPM. Esto puede dañar las raíces.",
                        NotificationCompat.PRIORITY_HIGH
                    )
                }
                tds < 100 -> {
                    sendThrottledNotification(
                        "tds_low",
                        "📉 TDS Bajo",
                        "Los nutrientes están bajos (${tds.toInt()} PPM). Considera fertilizar.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error procesando TDS", e)
        }
    }

    private fun checkSoilMoisture(moistureValue: String) {
        try {
            val moisture = moistureValue.toIntOrNull() ?: return

            when {
                moisture < thresholds.soilMoistureMin -> {
                    sendThrottledNotification(
                        "soil_dry",
                        "🏜️ Suelo Muy Seco",
                        "La humedad del suelo es crítica: ${moisture}%. ¡La planta necesita agua urgente!",
                        NotificationCompat.PRIORITY_HIGH
                    )
                }
                moisture > thresholds.soilMoistureMax -> {
                    sendThrottledNotification(
                        "soil_wet",
                        "💧 Suelo Muy Húmedo",
                        "Exceso de humedad: ${moisture}%. Reduce el riego para evitar pudrición de raíces.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error procesando humedad", e)
        }
    }

    private fun checkTemperature(temperatureValue: String) {
        try {
            val temp = temperatureValue.toFloatOrNull() ?: return

            when {
                temp < thresholds.temperatureMin -> {
                    sendThrottledNotification(
                        "temp_low",
                        "🥶 Temperatura Baja",
                        "Hace frío para la planta: ${temp}°C. Considera moverla a un lugar más cálido.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
                temp > thresholds.temperatureMax -> {
                    sendThrottledNotification(
                        "temp_high",
                        "🔥 Temperatura Alta",
                        "Hace mucho calor: ${temp}°C. La planta puede estresarse. Busca un lugar más fresco.",
                        NotificationCompat.PRIORITY_HIGH
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error procesando temperatura", e)
        }
    }

    private fun checkLightLevels(lightValue: String) {
        try {
            val light = lightValue.toFloatOrNull() ?: return

            when {
                light < thresholds.lightMin -> {
                    sendThrottledNotification(
                        "light_low",
                        "🌑 Poca Luz",
                        "La luz es insuficiente: ${light.toInt()} lx. La planta necesita más iluminación.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
                light > thresholds.lightMax -> {
                    sendThrottledNotification(
                        "light_high",
                        "☀️ Luz Intensa",
                        "Luz muy intensa: ${light.toInt()} lx. Puede quemar las hojas, considera sombra parcial.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error procesando luz", e)
        }
    }

    private fun checkHumidity(humidityValue: String) {
        try {
            val humidity = humidityValue.toFloatOrNull() ?: return

            when {
                humidity < thresholds.humidityMin -> {
                    sendThrottledNotification(
                        "humidity_low",
                        "🏜️ Aire Muy Seco",
                        "Humedad ambiental baja: ${humidity.toInt()}%. Usa un humidificador o pulveriza las hojas.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
                humidity > thresholds.humidityMax -> {
                    sendThrottledNotification(
                        "humidity_high",
                        "💨 Aire Muy Húmedo",
                        "Humedad ambiental alta: ${humidity.toInt()}%. Mejora la ventilación para evitar hongos.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error procesando humedad ambiental", e)
        }
    }

    private fun checkWaterLevel(waterLevel: String) {
        when (waterLevel) {
            "BAJO" -> {
                sendThrottledNotification(
                    "water_low",
                    "🚰 Depósito Vacío",
                    "El depósito de agua está bajo. Rellénalo pronto o el riego automático no funcionará.",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
        }
    }

    private fun handlePumpStatusChange(pumpStatus: String) {
        if (pumpStatus == "ON") {
            isCurrentlyWatering = true
            wateringStartTime = System.currentTimeMillis()

            // Programar parada automática por seguridad
            wateringJob?.cancel()
            wateringJob = coroutineScope.launch {
                delay(autoWateringConfig.maxWateringDuration)
                if (isCurrentlyWatering) {
                    sendPumpCommand("OFF")
                    sendNotification(
                        "⏰ Riego Detenido por Seguridad",
                        "Se alcanzó el tiempo máximo de riego (${autoWateringConfig.maxWateringDuration/1000}s).",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }
            }
        } else {
            isCurrentlyWatering = false
            wateringJob?.cancel()

            if (wateringStartTime > 0) {
                val duration = (System.currentTimeMillis() - wateringStartTime) / 1000
                sendNotification(
                    "✅ Riego Completado",
                    "La bomba se detuvo después de ${duration}s. Monitoreando humedad del suelo.",
                    NotificationCompat.PRIORITY_LOW
                )
            }
        }
    }

    // NUEVO: Manejar cambios en el estado del techo
    private fun handleRoofStatusChange(roofStatus: String) {
        when (roofStatus) {
            "ABIERTO" -> {
                sendNotification(
                    "🏠 Techo Abierto",
                    "El techo se ha abierto exitosamente. La planta recibe más luz natural.",
                    NotificationCompat.PRIORITY_LOW
                )
            }
            "CERRADO" -> {
                sendNotification(
                    "🏠 Techo Cerrado",
                    "El techo se ha cerrado para proteger la planta.",
                    NotificationCompat.PRIORITY_LOW
                )
            }
            "ERROR" -> {
                sendNotification(
                    "⚠️ Error en el Techo",
                    "Hubo un problema con el mecanismo del techo. Revisa el sistema.",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
        }
    }

    private fun evaluateAutoWatering(sensorData: SensorData) {
        if (!_autoWateringEnabled.value) return
        if (isCurrentlyWatering) return

        // Verificar cooldown
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastWateringTime < autoWateringConfig.cooldownPeriod) return

        try {
            val soilMoisture = sensorData.soilMoisture.toIntOrNull()
            val waterLevel = sensorData.waterLevel

            // Condiciones para riego automático
            val needsWater = soilMoisture != null && soilMoisture < autoWateringConfig.moistureThreshold
            val hasWater = waterLevel == "ALTO"
            val shouldStop = soilMoisture != null && soilMoisture >= autoWateringConfig.stopMoistureLevel

            when {
                // Detener riego si ya está húmedo
                sensorData.pumpStatus == "ON" && shouldStop -> {
                    sendPumpCommand("OFF")
                    sendNotification(
                        "🌱 Riego Automático Completado",
                        "Humedad óptima alcanzada: ${soilMoisture}%. Deteniendo bomba.",
                        NotificationCompat.PRIORITY_LOW
                    )
                }

                // Iniciar riego automático
                needsWater && hasWater && sensorData.pumpStatus == "OFF" -> {
                    lastWateringTime = currentTime
                    sendPumpCommand("ON")
                    sendNotification(
                        "🤖 Riego Automático Iniciado",
                        "Humedad baja detectada (${soilMoisture}%). Activando bomba automáticamente.",
                        NotificationCompat.PRIORITY_DEFAULT
                    )
                }

                // Alerta si necesita agua pero no hay en el depósito
                needsWater && waterLevel == "BAJO" -> {
                    sendThrottledNotification(
                        "auto_water_blocked",
                        "⚠️ Riego Automático Bloqueado",
                        "La planta necesita agua (${soilMoisture}%) pero el depósito está vacío.",
                        NotificationCompat.PRIORITY_HIGH
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("MQTT", "Error evaluando riego automático", e)
        }
    }

    private fun sendThrottledNotification(
        key: String,
        title: String,
        content: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        if (!_notificationsEnabled.value) return

        val currentTime = System.currentTimeMillis()
        val lastTime = lastNotificationTime[key] ?: 0

        if (currentTime - lastTime > notificationCooldown) {
            sendNotification(title, content, priority)
            lastNotificationTime[key] = currentTime
        }
    }

    fun sendPumpCommand(command: String) {
        coroutineScope.launch {
            try {
                mqttClient?.publishWith()
                    ?.topic(pumpCommandTopic)
                    ?.payload(UTF_8.encode(command))
                    ?.send()

                Log.d("MQTT", "Comando bomba enviado: $command")
            } catch (e: Exception) {
                Log.e("MQTT", "Error enviando comando bomba", e)
            }
        }
    }

    // NUEVO: Función para enviar comandos al techo
    fun sendRoofCommand(command: String) {
        coroutineScope.launch {
            try {
                mqttClient?.publishWith()
                    ?.topic(roofCommandTopic)
                    ?.payload(UTF_8.encode(command))
                    ?.send()
                Log.d("MQTT", "Comando de techo enviado: $command")
            } catch (e: Exception) {
                Log.e("MQTT", "Error enviando comando de techo", e)
            }
        }
    }

    fun toggleAutoWatering() {
        val newValue = !_autoWateringEnabled.value
        _autoWateringEnabled.value = newValue

        sendNotification(
            if (newValue) "🤖 Riego Automático Activado" else "⏸️ Riego Automático Desactivado",
            if (newValue) "El sistema monitoreará y regará automáticamente cuando sea necesario."
            else "El riego automático está deshabilitado. Solo funcionará el control manual.",
            NotificationCompat.PRIORITY_LOW
        )
    }

    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
    }

    private fun sendNotification(
        title: String,
        content: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        if (!_notificationsEnabled.value) return

        try {
            val builder = NotificationCompat.Builder(context, "PLANT_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(priority)
                .setAutoCancel(true)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())

            Log.d("Notification", "Enviada: $title")
        } catch (e: Exception) {
            Log.e("Notification", "Error enviando notificación", e)
        }
    }

    fun getAutoWateringStatus(): String {
        return when {
            !_autoWateringEnabled.value -> "Desactivado"
            isCurrentlyWatering -> "Regando..."
            else -> "Monitoreando"
        }
    }

    fun getLastWateringInfo(): String {
        return if (lastWateringTime > 0) {
            val minutesAgo = (System.currentTimeMillis() - lastWateringTime) / (1000 * 60)
            "Último riego hace ${minutesAgo}m"
        } else {
            "Sin riegos registrados"
        }
    }

    fun disconnect() {
        try {
            wateringJob?.cancel()
            coroutineScope.cancel()
            mqttClient?.disconnect()
        } catch (e: Exception) {
            Log.e("MQTT", "Error desconectando", e)
        }
    }
}