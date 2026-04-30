package cz.kovmak.pomocnik.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_object_part TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_object_part_catalog TEXT NOT NULL DEFAULT 'MGLC002'")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_desc TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_desc_catalog TEXT NOT NULL DEFAULT 'MCZ001'")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_text TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause_catalog TEXT NOT NULL DEFAULT 'MGL0003'")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause_text TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_impact TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [WorkEntry::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workEntryDao(): WorkEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pomocnik_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}