package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.db.entity.BudgetEntity
import com.example.myapplication.data.db.entity.BudgetItemEntity
import com.example.myapplication.data.repository.BudgetRepository
import com.example.myapplication.data.repository.BudgetItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetDetailUiState(
    val budget: BudgetEntity? = null,
    val items: List<BudgetItemEntity> = emptyList(),
    val client: com.example.myapplication.data.db.entity.ClientEntity? = null,
    val settings: com.example.myapplication.data.db.entity.SettingsEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    val pdfPath: String? = null,
    val showShareOptions: Boolean = false,
    val lastGeneratedPdfPath: String? = null,
    val duplicatedBudgetId: Int? = null,
    val isDownloadingPdf: Boolean = false,
    val downloadSuccess: Boolean = false,
    val editProjectName: String = "",
    val editLaborCost: String = "",
    val editClientName: String = "",
    val editClientCuit: String = "",
    val editClientAddress: String = "",
    val editClientCity: String = "",
    val editClientProvince: String = "",
    val editClientPhone: String = "",
    val editClientEmail: String = "",
    val notesInput: String = ""
)

class BudgetDetailViewModel(
    private val budgetRepository: BudgetRepository,
    private val budgetItemRepository: BudgetItemRepository,
    private val clientRepository: com.example.myapplication.data.repository.ClientRepository,
    private val settingsRepository: com.example.myapplication.data.repository.SettingsRepository,
    private val pdfGeneratorService: com.example.myapplication.data.service.PdfGeneratorService,
    private val sharingService: com.example.myapplication.data.service.SharingService,
    private val budgetId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetDetailUiState>(
        BudgetDetailUiState(isLoading = true)
    )
    val uiState: StateFlow<BudgetDetailUiState> = _uiState.asStateFlow()

    private val _clientSearchQuery = MutableStateFlow("")

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val clientSuggestions: StateFlow<List<com.example.myapplication.data.db.entity.ClientEntity>> =
        _clientSearchQuery
            .flatMapLatest { query ->
                if (query.length >= 2) clientRepository.searchClients(query)
                else flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadBudget()
        loadItems()
        loadSettings()
    }

    private fun loadBudget() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val budget = budgetRepository.getBudgetById(budgetId)
                val client = if (budget != null) clientRepository.getClientById(budget.clientId) else null
                _uiState.value = _uiState.value.copy(
                    budget = budget,
                    client = client,
                    isLoading = false,
                    notesInput = budget?.notes ?: "",
                    error = if (budget == null) "Presupuesto no encontrado" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cargar presupuesto: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            try {
                budgetItemRepository.getItemsByBudget(budgetId).collect { items ->
                    _uiState.value = _uiState.value.copy(items = items)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cargar items: ${e.message}"
                )
            }
        }
    }

    fun updateBudgetStatus(newStatus: String) {
        val currentBudget = _uiState.value.budget ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                val updatedBudget = currentBudget.copy(
                    status = newStatus,
                    modifiedDate = System.currentTimeMillis()
                )
                budgetRepository.updateBudget(updatedBudget)
                _uiState.value = _uiState.value.copy(
                    budget = updatedBudget,
                    isSaving = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al actualizar estado: ${e.message}",
                    isSaving = false
                )
            }
        }
    }

    fun updateNotesInput(notes: String) {
        _uiState.value = _uiState.value.copy(notesInput = notes)
    }

    fun saveNotes() {
        val currentBudget = _uiState.value.budget ?: return
        val notes = _uiState.value.notesInput
        if (notes == currentBudget.notes) return
        viewModelScope.launch {
            try {
                val updatedBudget = currentBudget.copy(
                    notes = notes,
                    modifiedDate = System.currentTimeMillis()
                )
                budgetRepository.updateBudget(updatedBudget)
                _uiState.value = _uiState.value.copy(budget = updatedBudget, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al guardar notas: ${e.message}",
                    isSaving = false
                )
            }
        }
    }

    fun toggleEditMode() {
        val budget = _uiState.value.budget ?: return
        val client = _uiState.value.client
        if (!_uiState.value.isEditing) {
            _uiState.value = _uiState.value.copy(
                isEditing = true,
                editProjectName = budget.project,
                editLaborCost = if (budget.laborCostPerItem > 0) budget.laborCostPerItem.toString() else "",
                editClientName = client?.name ?: "",
                editClientCuit = client?.cuit ?: "",
                editClientAddress = client?.address ?: "",
                editClientCity = client?.city ?: "",
                editClientProvince = client?.province ?: "",
                editClientPhone = client?.phone ?: "",
                editClientEmail = client?.email ?: ""
            )
        } else {
            _uiState.value = _uiState.value.copy(isEditing = false)
        }
    }

    fun updateEditProjectName(name: String) { _uiState.value = _uiState.value.copy(editProjectName = name) }
    fun updateEditLaborCost(cost: String) { _uiState.value = _uiState.value.copy(editLaborCost = cost) }
    fun updateEditClientName(name: String) {
        _uiState.value = _uiState.value.copy(editClientName = name)
        _clientSearchQuery.value = name
    }
    fun selectClientSuggestion(client: com.example.myapplication.data.db.entity.ClientEntity) {
        _clientSearchQuery.value = ""
        _uiState.value = _uiState.value.copy(
            editClientName = client.name,
            editClientCuit = client.cuit,
            editClientAddress = client.address,
            editClientCity = client.city,
            editClientProvince = client.province,
            editClientPhone = client.phone,
            editClientEmail = client.email,
            client = client
        )
    }
    fun updateEditClientCuit(cuit: String) { _uiState.value = _uiState.value.copy(editClientCuit = cuit) }
    fun updateEditClientAddress(address: String) { _uiState.value = _uiState.value.copy(editClientAddress = address) }
    fun updateEditClientCity(city: String) { _uiState.value = _uiState.value.copy(editClientCity = city) }
    fun updateEditClientProvince(province: String) { _uiState.value = _uiState.value.copy(editClientProvince = province) }
    fun updateEditClientPhone(phone: String) { _uiState.value = _uiState.value.copy(editClientPhone = phone) }
    fun updateEditClientEmail(email: String) { _uiState.value = _uiState.value.copy(editClientEmail = email) }

    fun saveChanges() {
        val budget = _uiState.value.budget ?: return
        val client = _uiState.value.client
        val s = _uiState.value
        viewModelScope.launch {
            try {
                _uiState.value = s.copy(isSaving = true)
                val updatedBudget = budget.copy(
                    project = s.editProjectName,
                    laborCostPerItem = s.editLaborCost.toDoubleOrNull() ?: 0.0,
                    modifiedDate = System.currentTimeMillis()
                )
                budgetRepository.updateBudget(updatedBudget)
                val updatedClient = client?.copy(
                    name = s.editClientName,
                    cuit = s.editClientCuit,
                    address = s.editClientAddress,
                    city = s.editClientCity,
                    province = s.editClientProvince,
                    phone = s.editClientPhone,
                    email = s.editClientEmail
                )
                if (updatedClient != null) clientRepository.updateClient(updatedClient)
                _uiState.value = _uiState.value.copy(
                    budget = updatedBudget,
                    client = updatedClient ?: client,
                    isEditing = false,
                    isSaving = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al guardar: ${e.message}",
                    isSaving = false
                )
            }
        }
    }

    fun deleteBudget() {
        val currentBudget = _uiState.value.budget ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                budgetRepository.deleteBudgetById(currentBudget.id)
                _uiState.value = _uiState.value.copy(isSaving = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar: ${e.message}",
                    isSaving = false
                )
            }
        }
    }

    fun deleteItem(item: BudgetItemEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                budgetItemRepository.deleteItem(item)
                _uiState.value = _uiState.value.copy(isSaving = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar item: ${e.message}",
                    isSaving = false
                )
            }
        }
    }

    fun saveLaborCost(cost: Double) {
        val currentBudget = _uiState.value.budget ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                val updatedBudget = currentBudget.copy(
                    laborCostPerItem = cost,
                    modifiedDate = System.currentTimeMillis()
                )
                budgetRepository.updateBudget(updatedBudget)
                _uiState.value = _uiState.value.copy(budget = updatedBudget, isSaving = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al guardar mano de obra: ${e.message}",
                    isSaving = false
                )
            }
        }
    }

    fun getItemsTotal(): Double = _uiState.value.items.sumOf { it.quantity * it.unitPrice }
    fun getLaborTotal(): Double = _uiState.value.items.sumOf { it.laborCost }
    fun getGrandTotal(): Double = getItemsTotal() + getLaborTotal() + (_uiState.value.budget?.laborCostPerItem ?: 0.0)

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.initializeSettings()
                settingsRepository.getSettings().collect { settings ->
                    _uiState.value = _uiState.value.copy(settings = settings)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cargar configuración: ${e.message}"
                )
            }
        }
    }

    fun generatePdf() {
        val budget = _uiState.value.budget ?: return
        val items = _uiState.value.items
        val settings = _uiState.value.settings ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isGeneratingPdf = true, error = null)
                val client = clientRepository.getClientById(budget.clientId)
                val pdfPath = pdfGeneratorService.generateBudgetPdf(budget, items, client, settings)
                _uiState.value = _uiState.value.copy(
                    isGeneratingPdf = false,
                    pdfPath = pdfPath,
                    lastGeneratedPdfPath = pdfPath,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingPdf = false,
                    error = "Error al generar PDF: ${e.message}"
                )
            }
        }
    }

    fun shareGeneratedPdf() {
        val budget = _uiState.value.budget ?: return
        val pdfPath = _uiState.value.lastGeneratedPdfPath
        if (pdfPath != null) sharingService.sharePdf(pdfPath, budget.budgetNumber)
        else _uiState.value = _uiState.value.copy(showShareOptions = true)
    }

    fun generateAndShare() {
        val budget = _uiState.value.budget ?: return
        val items = _uiState.value.items
        val settings = _uiState.value.settings ?: run {
            _uiState.value = _uiState.value.copy(error = "Error: configuración no disponible")
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isGeneratingPdf = true, error = null)
                val client = clientRepository.getClientById(budget.clientId)
                val pdfPath = pdfGeneratorService.generateBudgetPdf(budget, items, client, settings)
                _uiState.value = _uiState.value.copy(
                    isGeneratingPdf = false,
                    lastGeneratedPdfPath = pdfPath,
                    showShareOptions = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingPdf = false,
                    error = "Error al generar PDF: ${e.message}"
                )
            }
        }
    }

    fun shareViaWhatsApp() {
        val budget = _uiState.value.budget ?: return
        val pdfPath = _uiState.value.lastGeneratedPdfPath ?: return
        sharingService.shareViaWhatsApp(pdfPath, budget.budgetNumber)
        _uiState.value = _uiState.value.copy(showShareOptions = false)
    }

    fun shareViaEmail() {
        val budget = _uiState.value.budget ?: return
        val pdfPath = _uiState.value.lastGeneratedPdfPath ?: return
        sharingService.shareViaEmail(pdfPath, budget.budgetNumber)
        _uiState.value = _uiState.value.copy(showShareOptions = false)
    }

    fun shareGeneral() {
        val budget = _uiState.value.budget ?: return
        val pdfPath = _uiState.value.lastGeneratedPdfPath ?: return
        sharingService.sharePdf(pdfPath, budget.budgetNumber)
        _uiState.value = _uiState.value.copy(showShareOptions = false)
    }

    fun duplicateBudget() {
        val budget = _uiState.value.budget ?: return
        val items = _uiState.value.items
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                val newNumber = budgetRepository.generateBudgetNumber(budget.project)
                val newBudget = budget.copy(
                    id = 0,
                    budgetNumber = newNumber,
                    status = "DRAFT",
                    createdDate = System.currentTimeMillis(),
                    modifiedDate = System.currentTimeMillis()
                )
                val newBudgetId = budgetRepository.createBudget(newBudget).toInt()
                items.forEach { item ->
                    budgetItemRepository.createItem(item.copy(id = 0, budgetId = newBudgetId))
                }
                _uiState.value = _uiState.value.copy(isSaving = false, duplicatedBudgetId = newBudgetId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Error al duplicar: ${e.message}"
                )
            }
        }
    }

    fun downloadPdf() {
        val budget = _uiState.value.budget ?: return
        val items = _uiState.value.items
        val settings = _uiState.value.settings ?: run {
            _uiState.value = _uiState.value.copy(error = "Error: configuración no disponible")
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDownloadingPdf = true, error = null, downloadSuccess = false)
                val client = clientRepository.getClientById(budget.clientId)
                val pdfPath = pdfGeneratorService.generateBudgetPdf(budget, items, client, settings)
                val ok = sharingService.downloadPdfToPublicStorage(pdfPath, budget.budgetNumber)
                _uiState.value = _uiState.value.copy(
                    isDownloadingPdf = false,
                    downloadSuccess = ok,
                    error = if (!ok) "No se pudo guardar en Descargas" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloadingPdf = false,
                    error = "Error al descargar PDF: ${e.message}"
                )
            }
        }
    }

    fun clearDownloadSuccess() { _uiState.value = _uiState.value.copy(downloadSuccess = false) }
    fun clearDuplicatedBudgetId() { _uiState.value = _uiState.value.copy(duplicatedBudgetId = null) }
    fun dismissShareOptions() { _uiState.value = _uiState.value.copy(showShareOptions = false) }
    fun clearPdfPath() { _uiState.value = _uiState.value.copy(pdfPath = null) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    companion object {
        fun provideFactory(
            budgetRepository: BudgetRepository,
            budgetItemRepository: BudgetItemRepository,
            clientRepository: com.example.myapplication.data.repository.ClientRepository,
            settingsRepository: com.example.myapplication.data.repository.SettingsRepository,
            pdfGeneratorService: com.example.myapplication.data.service.PdfGeneratorService,
            sharingService: com.example.myapplication.data.service.SharingService,
            budgetId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetDetailViewModel(
                    budgetRepository, budgetItemRepository, clientRepository,
                    settingsRepository, pdfGeneratorService, sharingService, budgetId
                ) as T
            }
        }
    }
}
