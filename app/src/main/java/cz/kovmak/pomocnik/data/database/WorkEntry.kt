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

    @ColumnInfo(name = "materials")
    val materials: String = "",

    @ColumnInfo(name = "start_time")
    val startTime: String = "",

    @ColumnInfo(name = "end_time")
    val endTime: String = "",

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
    val userEmail: String = ""
)
