package com.example.myapplication.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.myapplication.utils.formatCurrency
import com.example.myapplication.utils.parseAmount
import com.example.myapplication.utils.toInputString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.presentation.viewmodel.AddItemViewModel

private val CERRAMIENTOS_TYPES = listOf("WINDOW", "DOOR", "RAILING", "OTHER")
private val HERRERIA_TYPES = listOf(
    "FENCE", "FENCE_DOOR", "RAILING", "GATE", "STAIR",
    "GRILL", "GRILL_FRONT", "UNDER_COUNTER", "INDUSTRIAL_FURNITURE",
    "TABLE", "CHAIR", "TRAILER", "STORAGE", "TRASH_CAN", "OTHER"
)
private val TYPES_WITH_DIMENSIONS = listOf(
    "WINDOW", "DOOR", "RAILING",
    "FENCE", "FENCE_DOOR", "GATE", "STAIR", "GRILL", "GRILL_FRONT",
    "UNDER_COUNTER", "INDUSTRIAL_FURNITURE", "TRAILER", "STORAGE"
)
private val TYPES_WITH_PANELS = listOf("WINDOW", "DOOR")

fun itemTypeLabel(type: String): String = when (type) {
    "WINDOW" -> "Ventana"
    "DOOR" -> "Puerta"
    "RAILING" -> "Baranda"
    "FENCE" -> "Reja"
    "FENCE_DOOR" -> "Puerta Reja"
    "GATE" -> "Portón"
    "STAIR" -> "Escalera"
    "GRILL" -> "Parrilla"
    "GRILL_FRONT" -> "Frente de Parrilla"
    "UNDER_COUNTER" -> "Bajo Mesada"
    "INDUSTRIAL_FURNITURE" -> "Mueble Industrial"
    "TABLE" -> "Mesa"
    "CHAIR" -> "Silla"
    "TRAILER" -> "Trailer"
    "STORAGE" -> "Baulera"
    "TRASH_CAN" -> "Tacho de Basura"
    else -> "Otro"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel,
    businessType: String = "CERRAMIENTOS",
    onNavigateBack: () -> Unit,
    onItemAdded: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTypeMenu by remember { mutableStateOf(false) }

    if (uiState.itemSaved) {
        onItemAdded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar Item" else "Agregar Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("TIPO DE ITEM")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tipo:", modifier = Modifier.weight(0.3f))
                Button(
                    onClick = { showTypeMenu = true },
                    modifier = Modifier.weight(0.7f)
                ) {
                    Text(itemTypeLabel(uiState.type))
                    val availableTypes = if (businessType == "HERRERIA") HERRERIA_TYPES else CERRAMIENTOS_TYPES
                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        availableTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(itemTypeLabel(type)) },
                                onClick = {
                                    viewModel.updateType(type)
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }
            }

            SectionTitle("INFORMACIÓN DEL ITEM")

            FormTextField(
                label = "Descripción",
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                enabled = !uiState.isSaving
            )

            FormTextField(
                label = "Especificaciones",
                value = uiState.specifications,
                onValueChange = viewModel::updateSpecifications,
                enabled = !uiState.isSaving
            )

            SectionTitle("CÁLCULO DE PRECIO")

            FormTextField(
                label = "Cantidad",
                value = if (uiState.quantity == 0) "" else uiState.quantity.toString(),
                onValueChange = { value ->
                    val intValue = value.toIntOrNull() ?: 1
                    viewModel.updateQuantity(intValue)
                },
                enabled = !uiState.isSaving
            )

            FormTextField(
                label = "Precio Unitario",
                value = uiState.unitPrice.toInputString(),
                onValueChange = { viewModel.updateUnitPrice(parseAmount(it)) },
                enabled = !uiState.isSaving
            )

            if (uiState.quantity > 0 && uiState.unitPrice > 0.0) {
                Text(
                    text = "Subtotal: ${formatCurrency(uiState.quantity * uiState.unitPrice)}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (uiState.type in TYPES_WITH_DIMENSIONS) {
                SectionTitle("DIMENSIONES (para el plano técnico)")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormTextField(
                        label = "Ancho (mm)",
                        value = if (uiState.widthMm == 0) "" else uiState.widthMm.toString(),
                        onValueChange = { viewModel.updateWidthMm(it.toIntOrNull() ?: 0) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f)
                    )
                    FormTextField(
                        label = "Alto (mm)",
                        value = if (uiState.heightMm == 0) "" else uiState.heightMm.toString(),
                        onValueChange = { viewModel.updateHeightMm(it.toIntOrNull() ?: 0) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.type in TYPES_WITH_PANELS) {
                    FormTextField(
                        label = "Cantidad de hojas",
                        value = if (uiState.panelCount == 0) "" else uiState.panelCount.toString(),
                        onValueChange = { viewModel.updatePanelCount(it.toIntOrNull() ?: 0) },
                        enabled = !uiState.isSaving
                    )

                    if (uiState.type == "WINDOW" && uiState.panelCount > 0) {
                        val typeList = uiState.panelTypes.split(",")
                        Text(
                            text = "Tipo por hoja:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (i in 0 until uiState.panelCount) {
                                val isFijo = typeList.getOrElse(i) { "M" } == "F"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "H${i + 1}", fontSize = 11.sp)
                                    Button(
                                        onClick = { viewModel.togglePanelType(i) },
                                        enabled = !uiState.isSaving,
                                        modifier = Modifier.size(width = 56.dp, height = 36.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFijo)
                                                MaterialTheme.colorScheme.secondaryContainer
                                            else
                                                MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = if (isFijo)
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Text(
                                            text = if (isFijo) "FIJO" else "MÓV",
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SectionTitle("NOTAS")

            FormTextField(
                label = "Notas",
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                enabled = !uiState.isSaving
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    enabled = !uiState.isSaving
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = viewModel::saveItem,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Text(if (uiState.isEditMode) "Guardar Cambios" else "Guardar Item")
                }
            }
        }
    }
}
