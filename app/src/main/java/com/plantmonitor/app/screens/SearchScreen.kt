package com.plantmonitor.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TrefleResponse(
    val data: List<TreflePlant>,
    val links: PaginationLinks? = null,
    val meta: MetaData? = null
)

@Serializable
data class PaginationLinks(val first: String?, val last: String?, val self: String?)

@Serializable
data class MetaData(val total: Int?)

@Serializable
data class TreflePlant(
    val id: Int? = null,
    val common_name: String? = null,
    val scientific_name: String? = null,
    val family: String? = null,
    val genus: String? = null,
    val image_url: String? = null,
    val year: Int? = null,
    val bibliography: String? = null,
    val observations: String? = null,
    val edible: Boolean? = null
)

data class PlantFilters(
    val family: String? = null,
    val genus: String? = null,
    val year: Int? = null,
    val edible: Boolean? = null
)

val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}

const val TREFLE_TOKEN = "aVhq4nwW80xoH8mSUPVw83ZlHUekSm_sYiSm67dPm7g"

suspend fun searchPlants(
    query: String = "",
    filters: PlantFilters = PlantFilters(),
    page: Int = 1
): Pair<List<TreflePlant>, Int> {
    return try {
        val response: TrefleResponse = client.get("https://trefle.io/api/v1/plants") {  // Cambiado de /plants/search a /plants
            contentType(ContentType.Application.Json)
            parameter("token", TREFLE_TOKEN)
            parameter("page", page)

            if (query.isNotEmpty()) {
                parameter("q", query)  // Parámetro de búsqueda
            }

            // Filtros (opcionales)
            filters.family?.let { parameter("filter[family]", it) }
            filters.genus?.let { parameter("filter[genus]", it) }
            filters.edible?.let { parameter("filter[edible]", it) }
        }.body()

        Pair(response.data, response.meta?.total ?: 0)
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(emptyList(), 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TreflePlant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedPlant by remember { mutableStateOf<TreflePlant?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var totalItems by remember { mutableStateOf(0) }

    var filters by remember {
        mutableStateOf(PlantFilters())
    }

    val itemsPerPage = 20
    val totalPages = (totalItems + itemsPerPage - 1) / itemsPerPage

    LaunchedEffect(searchQuery, filters, currentPage) {
        if (searchQuery.isNotEmpty() || filters != PlantFilters()) {
            isLoading = true
            delay(500) // Pequeño delay para evitar múltiples llamadas rápidas
            val (results, total) = searchPlants(searchQuery, filters, currentPage)
            searchResults = results
            totalItems = total
            isLoading = false
        } else {
            searchResults = emptyList()
            totalItems = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra de búsqueda
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar planta...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showFilters = !showFilters }
            ) {
                Icon(
                    if (showFilters) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                    contentDescription = "Filtros",
                    tint = if (filters != PlantFilters()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Panel de filtros
        if (showFilters) {
            FilterPanel(
                filters = filters,
                onFiltersChanged = { newFilters ->
                    filters = newFilters
                    currentPage = 1
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resultados
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            searchResults.isEmpty() -> {
                EmptyResults(
                    isEmptySearch = searchQuery.isEmpty() && filters == PlantFilters(),
                    onExploreClick = { searchQuery = "rose" }
                )
            }

            else -> {
                PlantResultsList(
                    plants = searchResults,
                    onPlantClick = { selectedPlant = it },
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageChange = { page -> currentPage = page }
                )
            }
        }
    }

    // Diálogo de detalle
    selectedPlant?.let { plant ->
        PlantDetailDialog(
            plant = plant,
            onDismiss = { selectedPlant = null }
        )
    }
}

@Composable
fun FilterPanel(
    filters: PlantFilters,
    onFiltersChanged: (PlantFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Filtros avanzados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = filters.family ?: "",
                onValueChange = { onFiltersChanged(filters.copy(family = it.takeIf { it.isNotEmpty() })) },
                label = { Text("Familia") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = filters.genus ?: "",
                onValueChange = { onFiltersChanged(filters.copy(genus = it.takeIf { it.isNotEmpty() })) },
                label = { Text("Género") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = filters.edible ?: false,
                    onCheckedChange = { checked ->
                        onFiltersChanged(filters.copy(edible = checked.takeIf { it }))
                    },
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Solo plantas comestibles",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { onFiltersChanged(PlantFilters()) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Limpiar")
                }

                Button(
                    onClick = { /* Filtros se aplican automáticamente */ }
                ) {
                    Text("Aplicar")
                }
            }
        }
    }
}

@Composable
fun EmptyResults(
    isEmptySearch: Boolean,
    onExploreClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isEmptySearch) {
            Image(
                imageVector = Icons.Default.Nature,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "Busca plantas en Trefle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Usa la barra de búsqueda o aplica filtros",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Button(
                onClick = onExploreClick,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Ejemplo: rosas")
            }
        } else {
            Image(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "No se encontraron plantas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Prueba con otros términos o ajusta los filtros",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PlantResultsList(
    plants: List<TreflePlant>,
    onPlantClick: (TreflePlant) -> Unit,
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(plants) { plant ->
            PlantItem(plant = plant, onClick = { onPlantClick(plant) })
        }

        item {
            if (totalPages > 1) {
                PaginationControls(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageChange = onPageChange,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
fun PlantItem(plant: TreflePlant, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = plant.image_url ?: "https://via.placeholder.com/100?text=🌱",
                contentDescription = plant.common_name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.common_name ?: "Nombre no disponible",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = plant.scientific_name ?: "Nombre científico no disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                plant.family?.let { family ->
                    Text(
                        text = "Familia: $family",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }

                plant.edible?.let { edible ->
                    if (edible) {
                        Text(
                            text = "🌱 Comestible",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF388E3C),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Ver detalles",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onPageChange(maxOf(1, currentPage - 1)) },
            enabled = currentPage > 1
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Página anterior"
            )
        }

        Text(
            text = "$currentPage / $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        IconButton(
            onClick = { onPageChange(minOf(totalPages, currentPage + 1)) },
            enabled = currentPage < totalPages
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Página siguiente"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailDialog(
    plant: TreflePlant,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = {
            Column {
                Text(
                    text = plant.common_name ?: "Planta sin nombre común",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = plant.scientific_name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            LazyColumn {
                item {
                    AsyncImage(
                        model = plant.image_url ?: "https://via.placeholder.com/600x400?text=🌿",
                        contentDescription = plant.common_name,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = 16.dp)
                    )
                }

                plant.family?.let { family ->
                    item {
                        DetailItem("Familia", family)
                    }
                }

                plant.genus?.let { genus ->
                    item {
                        DetailItem("Género", genus)
                    }
                }

                plant.year?.let { year ->
                    item {
                        DetailItem("Año", year.toString())
                    }
                }

                plant.edible?.let { edible ->
                    item {
                        DetailItem(
                            "Comestible",
                            if (edible) "Sí" else "No",
                            if (edible) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                plant.bibliography?.let { bibliography ->
                    if (bibliography.isNotBlank()) {
                        item {
                            DetailItem("Referencia", bibliography)
                        }
                    }
                }

                plant.observations?.let { observations ->
                    if (observations.isNotBlank()) {
                        item {
                            DetailItem("Observaciones", observations)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun DetailItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(top = 2.dp)
        )
        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}