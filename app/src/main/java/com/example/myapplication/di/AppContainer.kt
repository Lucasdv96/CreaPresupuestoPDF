package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.repository.BudgetRepository
import com.example.myapplication.data.repository.BudgetItemRepository
import com.example.myapplication.data.repository.ClientRepository
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.data.service.PdfGeneratorService
import com.example.myapplication.data.service.SharingService

interface AppContainer {
    val budgetRepository: BudgetRepository
    val clientRepository: ClientRepository
    val settingsRepository: SettingsRepository
    val budgetItemRepository: BudgetItemRepository
    val pdfGeneratorService: PdfGeneratorService
    val sharingService: SharingService
}

class AppDataContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "budget_app_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .enableMultiInstanceInvalidation()
            .build()
    }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.budgetDao())
    }

    override val clientRepository: ClientRepository by lazy {
        ClientRepository(database.clientDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }

    override val budgetItemRepository: BudgetItemRepository by lazy {
        BudgetItemRepository(database.budgetItemDao())
    }

    override val pdfGeneratorService: PdfGeneratorService by lazy {
        PdfGeneratorService(context)
    }

    override val sharingService: SharingService by lazy {
        SharingService(context)
    }
}
