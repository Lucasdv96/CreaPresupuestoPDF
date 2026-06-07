package com.example.myapplication.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val companyName: String = "",
    val companyCuit: String = "",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyCity: String = "",
    val logoPath: String = "",
    val primaryColor: String = "",
    val secondaryColor: String = "",
    val termsConditions: String = "",
    val businessType: String = "CERRAMIENTOS"
)
