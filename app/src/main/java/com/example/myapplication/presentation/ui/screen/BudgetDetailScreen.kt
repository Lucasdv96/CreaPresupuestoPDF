package com.example.myapplication.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.example.myapplication.utils.formatCurrency
import com.example.myapplication.utils.parseAmount
import com.example.myapplication.utils.toInputString
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.myapplication.data.db.entity.BudgetItemEntity
import com.example.myapplication.presentation.ui.components.ConfirmDeleteDialog
import com.example.myapplication.presentation.ui.components.ShareOptionsDialog
import com.example.myapplication.presentation.viewmodel.BudgetDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    viewModel: BudgetDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddItem: (budgetId: Int) -> Unit,
    onNavigateToEditItem: (budgetId: Int, itemId: Int) -> Unit,
    onNavigateToDuplicatedBudget: (budgetId: Int) -> Unit,
    onBudgetDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    @Suppress("OPT_IN_USAGE")
    val clientSuggestions by viewModel.clientSuggestions.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(uiState.downloadSuccess) {
        if (uiState.downloadSuccess) {
            Toast.makeText(context, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
            delay(2000)
            viewModel.clearDownloadSuccess()
        }
    }

    LaunchedEffect(uiState.duplicatedBudgetId) {
        if (uiState.duplicatedBudgetId != null) {
            viewModel.clearDuplicatedBudgetId()
            onNavigateToDuplicatedBudget(uiState.duplicatedBudgetId!!)
        }
    }

    LaunchedEffect(uiState.budget, uiState.error) {
        if (uiState.budget == null && uiState.error?.contains("no encontrado") == true) {
            onBudgetDeleted()
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClientDropdown by remember { mutableStateOf(false) }
    var showLaborDialog by remember { mutableStateOf(false) }
    var laborCostInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.budget?.budgetNumber ?: "Presupuesto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.isEditing) viewModel.saveChanges() else viewModel.toggleEditMode()
                    }) {
                        Icon(
                            if (uiState.isEditing) Icons.Filled.Done else Icons.Filled.Edit,
                            contentDescription = if (uiState.isEditing) "Guardar" else "Editar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }
            }

            uiState.budget == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Presupuesto no encontrado")
                    Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp)) { Text("Volver") }
                }
            }

            else -> {
                val budget = uiState.budget!!
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionTitle("👤 CLIENTE")
                    if (uiState.isEditing) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.editClientName,
                                onValueChange = {
                                    viewModel.updateEditClientName(it)
                                    showClientDropdown = true
                                },
                                label = { Text("Nombre del cliente") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (showClientDropdown && clientSuggestions.isNotEmpty()) {
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    clientSuggestions.take(5).forEach { client ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(client.name, fontWeight = FontWeight.Medium)
                                                    val location = listOf(client.city, client.province)
                                                        .filter { it.isNotBlank() }.joinToString(", ")
                                                    if (location.isNotBlank()) {
                                                        Text(location, fontSize = 11.sp,
                                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectClientSuggestion(client)
                                                showClientDropdown = false
                                            }
                                        )
                                        androidx.compose.material3.Divider()
                                    }
                                }
                            }
                        }
                        OutlinedTextField(value = uiState.editClientCuit, onValueChange = viewModel::updateEditClientCuit, label = { Text("CUIT") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = uiState.editClientAddress, onValueChange = viewModel::updateEditClientAddress, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = uiState.editClientCity, onValueChange = viewModel::updateEditClientCity, label = { Text("Ciudad") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = uiState.editClientProvince, onValueChange = viewModel::updateEditClientProvince, label = { Text("Provincia") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = uiState.editClientPhone, onValueChange = viewModel::updateEditClientPhone, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = uiState.editClientEmail, onValueChange = viewModel::updateEditClientEmail, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    } else {
                        val client = uiState.client
                        if (client != null) {
                            Text(text = "Nombre: ${client.name}", fontSize = 14.sp)
                            if (client.cuit.isNotBlank()) Text(text = "CUIT: ${client.cuit}", fontSize = 12.sp)
                            if (client.address.isNotBlank()) Text(text = "Dirección: ${client.address}", fontSize = 12.sp)
                            if (client.city.isNotBlank()) Text(text = "Ciudad: ${client.city}", fontSize = 12.sp)
                            if (client.phone.isNotBlank()) Text(text = "Tel: ${client.phone}", fontSize = 12.sp)
                            if (client.email.isNotBlank()) Text(text = "Email: ${client.email}", fontSize = 12.sp)
                        }
                    }

                    SectionTitle("📁 PROYECTO")
                    if (uiState.isEditing) {
                        OutlinedTextField(value = uiState.editProjectName, onValueChange = viewModel::updateEditProjectName, label = { Text("Nombre del proyecto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    } else {
                        Text(text = "Proyecto: ${budget.project}", fontSize = 14.sp)
                        Text(text = "Creado: ${dateFormat.format(Date(budget.createdDate))}", fontSize = 12.sp)
                    }

                    SectionTitle("🛠️ ITEMS (${uiState.items.size})")
                    if (uiState.items.isEmpty()) {
                        Text(text = "Sin items agregados", fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.items.forEach { item ->
                                BudgetItemCard(
                                    item = item,
                                    onEdit = { onNavigateToEditItem(budget.id, item.id) },
                                    onDelete = { viewModel.deleteItem(item) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onNavigateToAddItem(budget.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Agregar Item") }

                    OutlinedButton(
                        onClick = {
                            laborCostInput = if (budget.laborCostPerItem > 0)
                                budget.laborCostPerItem.toInputString() else ""
                            showLaborDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (budget.laborCostPerItem > 0)
                                "Mano de obra: ${formatCurrency(budget.laborCostPerItem)}"
                            else
                                "+ Agregar mano de obra"
                        )
                    }

                    SectionTitle("💰 RESUMEN")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Items:", fontWeight = FontWeight.Bold)
                        Text(formatCurrency(viewModel.getItemsTotal()))
                    }
                    if (budget.laborCostPerItem > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Mano de obra:", fontWeight = FontWeight.Bold)
                            Text(formatCurrency(budget.laborCostPerItem))
                        }
                    }
                    androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(formatCurrency(viewModel.getGrandTotal()), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    SectionTitle("📝 NOTAS")
                    OutlinedTextField(
                        value = uiState.notesInput,
                        onValueChange = { viewModel.updateNotesInput(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .onFocusChanged { if (!it.isFocused) viewModel.saveNotes() },
                        minLines = 3
                    )

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = viewModel::generateAndShare,
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving && !uiState.isGeneratingPdf && !uiState.isDownloadingPdf
                            ) {
                                if (uiState.isGeneratingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                }
                                Text("Compartir")
                            }
                            OutlinedButton(
                                onClick = viewModel::downloadPdf,
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving && !uiState.isGeneratingPdf && !uiState.isDownloadingPdf
                            ) {
                                if (uiState.isDownloadingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.GetApp, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                }
                                Text(if (uiState.downloadSuccess) "¡Guardado!" else "Descargar")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving
                            ) { Text("Eliminar") }
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving
                            ) { Text("Volver") }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteBudget(); showDeleteConfirm = false; onBudgetDeleted() },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showLaborDialog) {
        AlertDialog(
            onDismissRequest = { showLaborDialog = false },
            title = { Text("Mano de obra") },
            text = {
                OutlinedTextField(
                    value = laborCostInput,
                    onValueChange = { laborCostInput = it },
                    label = { Text("Costo de mano de obra") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveLaborCost(parseAmount(laborCostInput))
                    showLaborDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                Button(onClick = { showLaborDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (uiState.showShareOptions) {
        ShareOptionsDialog(
            onWhatsApp = viewModel::shareViaWhatsApp,
            onEmail = viewModel::shareViaEmail,
            onGeneral = viewModel::shareGeneral,
            onDismiss = viewModel::dismissShareOptions
        )
    }
}

@Composable
fun BudgetItemCard(
    item: BudgetItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val itemTypeDisplay = itemTypeLabel(item.type)
    val subtotal = item.quantity * item.unitPrice

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(itemTypeDisplay, fontWeight = FontWeight.Bold)
                if (item.description.isNotEmpty()) Text(item.description, fontSize = 12.sp)
                Text("Cant: ${item.quantity} x ${formatCurrency(item.unitPrice)}", fontSize = 12.sp)
                Text("Subtotal: ${formatCurrency(subtotal)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (item.laborCost > 0) Text("M.O.: ${formatCurrency(item.laborCost)}", fontSize = 12.sp)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
        androidx.compose.material3.Divider()
    }
}
