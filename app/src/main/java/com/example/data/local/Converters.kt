package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PlanPriority
import com.example.data.model.Question
import com.example.data.model.RevisionCategory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val questionListType = Types.newParameterizedType(List::class.java, Question::class.java)
    private val questionListAdapter = moshi.adapter<List<Question>>(questionListType)

    @TypeConverter
    fun fromQuestionList(value: List<Question>?): String {
        if (value == null) return ""
        return try {
            questionListAdapter.toJson(value)
        } catch (e: Exception) {
            ""
        }
    }

    @TypeConverter
    fun toQuestionList(value: String?): List<Question> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            questionListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

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

    @TypeConverter
    fun fromNovaMemoryCategory(value: com.example.data.model.NovaMemoryCategory): String {
        return value.name
    }

    @TypeConverter
    fun toNovaMemoryCategory(value: String): com.example.data.model.NovaMemoryCategory {
        return try {
            com.example.data.model.NovaMemoryCategory.valueOf(value)
        } catch (e: Exception) {
            com.example.data.model.NovaMemoryCategory.ACADEMIC
        }
    }

    @TypeConverter
    fun fromVoiceNoteType(value: com.example.data.model.VoiceNoteType): String {
        return value.name
    }

    @TypeConverter
    fun toVoiceNoteType(value: String): com.example.data.model.VoiceNoteType {
        return try {
            com.example.data.model.VoiceNoteType.valueOf(value)
        } catch (e: Exception) {
            com.example.data.model.VoiceNoteType.LECTURE
        }
    }
}
