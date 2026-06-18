package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.db.entity.SettingsEntity
import com.example.myapplication.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val companyName: String = "",
    val companyCuit: String = "",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyCity: String = "",
    val termsConditions: String = "",
    val logoPath: String = "",
    val businessType: String = "CERRAMIENTOS",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.initializeSettings()
                settingsRepository.getSettings().collect { settings ->
                    _uiState.value = _uiState.value.copy(
                        companyName = settings.companyName,
                        companyCuit = settings.companyCuit,
                        companyAddress = settings.companyAddress,
                        companyPhone = settings.companyPhone,
                        companyEmail = settings.companyEmail,
                        companyCity = settings.companyCity,
                        termsConditions = settings.termsConditions,
                        logoPath = settings.logoPath,
                        businessType = settings.businessType
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al cargar configuración: ${e.message}")
            }
        }
    }

    fun updateCompanyName(value: String) { _uiState.value = _uiState.value.copy(companyName = value, isSaved = false) }
    fun updateCompanyCuit(value: String) { _uiState.value = _uiState.value.copy(companyCuit = value, isSaved = false) }
    fun updateCompanyAddress(value: String) { _uiState.value = _uiState.value.copy(companyAddress = value, isSaved = false) }
    fun updateCompanyPhone(value: String) { _uiState.value = _uiState.value.copy(companyPhone = value, isSaved = false) }
    fun updateCompanyEmail(value: String) { _uiState.value = _uiState.value.copy(companyEmail = value, isSaved = false) }
    fun updateCompanyCity(value: String) { _uiState.value = _uiState.value.copy(companyCity = value, isSaved = false) }
    fun updateTermsConditions(value: String) { _uiState.value = _uiState.value.copy(termsConditions = value, isSaved = false) }
    fun updateLogoPath(path: String) { _uiState.value = _uiState.value.copy(logoPath = path, isSaved = false) }
    fun updateBusinessType(value: String) { _uiState.value = _uiState.value.copy(businessType = value, isSaved = false) }

    fun saveSettings() {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                _uiState.value = s.copy(isSaving = true, error = null)
                val settings = SettingsEntity(
                    id = 1,
                    companyName = s.companyName,
                    companyCuit = s.companyCuit,
                    companyAddress = s.companyAddress,
                    companyPhone = s.companyPhone,
                    companyEmail = s.companyEmail,
                    companyCity = s.companyCity,
                    termsConditions = s.termsConditions,
                    logoPath = s.logoPath,
                    businessType = s.businessType
                )
                settingsRepository.updateSettings(settings)
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Error al guardar: ${e.message}")
            }
        }
    }

    companion object {
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(settingsRepository) as T
            }
    }
}
