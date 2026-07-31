package cz.kovmak.pomocnik.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "work_entries")
data class WorkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "order_id")
    val orderId: String = "",

    @ColumnInfo(name = "work_type")
    val workType: String = "E", // E = Elektrická, M = Mechanická

    @ColumnInfo(name = "description_ua")
    val descriptionUa: String = "",

    @ColumnInfo(name = "description_cz")
    val descriptionCz: String = "",

    @ColumnInfo(name = "technical_report")
    val technicalReport: String = "",

    @ColumnInfo(name = "materials")
    val materials: String = "",

    @ColumnInfo(name = "start_time")
    val startTime: String = "",

    @ColumnInfo(name = "end_time")
    val endTime: String = "",

    @ColumnInfo(name = "sap_failure_end_date", defaultValue = "''")
    val sapFailureEndDate: String = "",

    @ColumnInfo(name = "hours")
    val hours: Double = 0.0,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_sent")
    val isSent: Boolean = false,

    @ColumnInfo(name = "user_name")
    val userName: String = "",

    @ColumnInfo(name = "user_email")
    val userEmail: String = "",

    // SAP fields
    @ColumnInfo(name = "sap_object_part", defaultValue = "''")
    val sapObjectPart: String = "",
    
    @ColumnInfo(name = "sap_object_part_catalog", defaultValue = "'MGLC'")
    val sapObjectPartCatalog: String = "MGLC",
    
    @ColumnInfo(name = "sap_damage_desc", defaultValue = "''")
    val sapDamageDesc: String = "",
    
    @ColumnInfo(name = "sap_damage_desc_catalog", defaultValue = "'MCZ001'")
    val sapDamageDescCatalog: String = "MCZ001",
    
    @ColumnInfo(name = "sap_damage_text", defaultValue = "''")
    val sapDamageText: String = "",
    
    @ColumnInfo(name = "sap_cause", defaultValue = "''")
    val sapCause: String = "",
    
    @ColumnInfo(name = "sap_cause_catalog", defaultValue = "'MGLO'")
    val sapCauseCatalog: String = "MGLO",
    
    @ColumnInfo(name = "sap_cause_text", defaultValue = "''")
    val sapCauseText: String = "",
    
    @ColumnInfo(name = "sap_impact", defaultValue = "''")
    val sapImpact: String = "",

    @ColumnInfo(name = "sap_notification_date", defaultValue = "''")
    val sapNotificationDate: String = "",

    @ColumnInfo(name = "sap_notification_author", defaultValue = "''")
    val sapNotificationAuthor: String = "",

    @ColumnInfo(name = "sap_technical_location", defaultValue = "''")
    val sapTechnicalLocation: String = "",

    @ColumnInfo(name = "sap_notification_text", defaultValue = "''")
    val sapNotificationText: String = "",

    @ColumnInfo(name = "sap_priority", defaultValue = "''")
    val sapPriority: String = ""
)