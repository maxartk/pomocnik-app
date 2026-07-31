package cz.kovmak.pomocnik.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.round

object SapDurationCalculator {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    fun isValidDate(value: String): Boolean = try {
        LocalDate.parse(value.trim(), dateFormatter)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    fun isValidTime(value: String): Boolean = try {
        LocalTime.parse(value.trim(), timeFormatter)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    fun suggestEndDate(notificationDate: String, notificationTime: String, failureEndTime: String): String {
        return try {
            val startDate = LocalDate.parse(notificationDate.trim(), dateFormatter)
            val startTime = LocalTime.parse(notificationTime.trim(), timeFormatter)
            val endTime = LocalTime.parse(failureEndTime.trim(), timeFormatter)
            val endDate = if (endTime.isBefore(startTime)) startDate.plusDays(1) else startDate
            endDate.format(dateFormatter)
        } catch (_: DateTimeParseException) {
            ""
        }
    }

    fun resolveEndDate(
        notificationDate: String,
        notificationTime: String,
        failureEndTime: String,
        currentEndDate: String,
        endDateManuallyEdited: Boolean
    ): String {
        if (endDateManuallyEdited) return currentEndDate
        return suggestEndDate(notificationDate, notificationTime, failureEndTime)
            .ifBlank { currentEndDate }
    }

    fun hours(
        notificationDate: String,
        notificationTime: String,
        failureEndDate: String,
        failureEndTime: String
    ): Double {
        return try {
            val start = LocalDateTime.of(
                LocalDate.parse(notificationDate.trim(), dateFormatter),
                LocalTime.parse(notificationTime.trim(), timeFormatter)
            )
            val end = LocalDateTime.of(
                LocalDate.parse(failureEndDate.trim(), dateFormatter),
                LocalTime.parse(failureEndTime.trim(), timeFormatter)
            )
            if (end.isBefore(start)) return 0.0
            val minutes = ChronoUnit.MINUTES.between(start, end)
            round((minutes / 60.0) * 100) / 100
        } catch (_: DateTimeParseException) {
            0.0
        }
    }
}
