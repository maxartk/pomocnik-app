package cz.kovmak.pomocnik.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_object_part TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_object_part_catalog TEXT NOT NULL DEFAULT 'MGLC'")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_desc TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_desc_catalog TEXT NOT NULL DEFAULT 'MCZ001'")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_text TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause_catalog TEXT NOT NULL DEFAULT 'MGLO'")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause_text TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_impact TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_notification_date TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_notification_author TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_technical_location TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_notification_text TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_priority TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE work_entries ADD COLUMN sap_failure_end_date TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [WorkEntry::class],
    version = 3,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}