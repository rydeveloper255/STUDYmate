package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PlanPriority
import com.example.data.model.RevisionCategory

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(";;;") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(";;;").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromPlanPriority(value: PlanPriority): String {
        return value.name
    }

    @TypeConverter
    fun toPlanPriority(value: String): PlanPriority {
        return try {
            PlanPriority.valueOf(value)
        } catch (e: Exception) {
            PlanPriority.MEDIUM
        }
    }

    @TypeConverter
    fun fromRevisionCategory(value: RevisionCategory): String {
        return value.name
    }

    @TypeConverter
    fun toRevisionCategory(value: String): RevisionCategory {
        return try {
            RevisionCategory.valueOf(value)
        } catch (e: Exception) {
            RevisionCategory.PRACTICE_SOON
        }
    }
}
