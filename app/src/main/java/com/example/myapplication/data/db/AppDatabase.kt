package com.example.myapplication.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.db.dao.BudgetDao
import com.example.myapplication.data.db.dao.BudgetItemDao
import com.example.myapplication.data.db.dao.ClientDao
import com.example.myapplication.data.db.dao.SettingsDao
import com.example.myapplication.data.db.entity.BudgetEntity
import com.example.myapplication.data.db.entity.BudgetItemEntity
import com.example.myapplication.data.db.entity.ClientEntity
import com.example.myapplication.data.db.entity.SettingsEntity

@Database(
    entities = [
        BudgetEntity::class,
        ClientEntity::class,
        BudgetItemEntity::class,
        SettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun clientDao(): ClientDao
    abstract fun budgetItemDao(): BudgetItemDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_items ADD COLUMN widthMm INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE budget_items ADD COLUMN heightMm INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE budget_items ADD COLUMN panelCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_items ADD COLUMN panelTypes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN businessType TEXT NOT NULL DEFAULT 'CERRAMIENTOS'")
            }
        }
    }
}
