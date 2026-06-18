package com.example.myapplication.data.repository

import com.example.myapplication.data.db.dao.SettingsDao
import com.example.myapplication.data.db.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDao: SettingsDao) {

    fun getSettings(): Flow<SettingsEntity> = settingsDao.getSettings()

    suspend fun updateSettings(settings: SettingsEntity) = settingsDao.update(settings)

    suspend fun initializeSettings(): SettingsEntity {
        val existing = settingsDao.getSettingsSync()
        return if (existing == null) {
            val defaultSettings = SettingsEntity(
                id = 1,
                companyName = "MB Cerramientos",
                companyCuit = "",
                companyAddress = "",
                companyPhone = "",
                companyEmail = "",
                companyCity = "",
                logoPath = "",
                primaryColor = "#7CB342",
                secondaryColor = "#333333",
                termsConditions = "",
                businessType = "CERRAMIENTOS"
            )
            settingsDao.insert(defaultSettings)
            defaultSettings
        } else {
            existing
        }
    }
}
