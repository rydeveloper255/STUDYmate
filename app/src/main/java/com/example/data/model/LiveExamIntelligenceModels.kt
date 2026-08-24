package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LiveExamCategory(
    val label: String,
    val hindiLabel: String,
    val icon: String,
    val colorHex: String
) {
    OFFICIAL_NOTIFICATION("Official Notification", "आधिकारिक अधिसूचना", "🏛️", "#10B981"),
    EXAM_UPDATE("Exam Update", "परीक्षा अपडेट", "📢", "#3B82F6"),
    CURRENT_AFFAIRS("Current Affairs", "समसामयिकी / करेंट अफेयर्स", "📰", "#F59E0B"),
    SYLLABUS_PATTERN("Syllabus & Pattern", "पाठ्यक्रम व पैटर्न", "🟣", "#8B5CF6"),
    URGENT_UPDATE("Urgent Notice", "अति महत्वपूर्ण सूचना", "🔴", "#EF4444")
}

enum class LiveSourceType(val label: String, val priority: Int, val isOfficial: Boolean) {
    OFFICIAL("Official Source", 1, true),
    REPUTED_NEWS("Reputed News", 2, false),
    EDUCATIONAL("Educational Source", 3, false),
    WEB("Web Update", 4, false)
}

enum class ExamRelevanceLevel(val label: String, val badgeText: String) {
    HIGH("High", "Exam relevance: High"),
    MEDIUM("Medium", "Exam relevance: Medium"),
    LOW("General", "Exam relevance: Low")
}

@Entity(tableName = "live_exam_updates")
data class LiveExamUpdateEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val examName: String,
    val title: String,
    val summary: String,
    val category: String = LiveExamCategory.EXAM_UPDATE.name,
    val sourceName: String,
    val sourceUrl: String,
    val sourceType: String = LiveSourceType.REPUTED_NEWS.name,
    val isVerifiedOfficial: Boolean = false,
    val publishedAt: String = "",
    val retrievedAt: Long = System.currentTimeMillis(),
    val relevance: String = ExamRelevanceLevel.HIGH.name,
    val importanceScore: Int = 85,
    val whyItMatters: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val isSaved: Boolean = false,
    val isRead: Boolean = false,
    val contentHash: String = ""
)

@Entity(tableName = "trending_exam_topics")
data class TrendingExamTopicEntity(
    @PrimaryKey val id: String,
    val examName: String,
    val title: String,
    val summary: String,
    val whyItMatters: String,
    val relevance: String = ExamRelevanceLevel.HIGH.name,
    val category: String = "National Schemes & Policies",
    val sourceName: String = "Official Release",
    val sourceUrl: String = "",
    val isSaved: Boolean = false,
    val practiceQuestions: List<Question> = emptyList(),
    val retrievedAt: Long = System.currentTimeMillis()
)

data class LiveExamFeedState(
    val examName: String = "RRB Group D",
    val liveNews: List<LiveExamUpdateEntity> = emptyList(),
    val whatsNewList: List<LiveExamUpdateEntity> = emptyList(),
    val officialNotices: List<LiveExamUpdateEntity> = emptyList(),
    val radarUpdates: List<LiveExamUpdateEntity> = emptyList(),
    val trendingTopics: List<TrendingExamTopicEntity> = emptyList(),
    val savedUpdates: List<LiveExamUpdateEntity> = emptyList(),
    val isLoading: Boolean = false,
    val lastUpdatedMillis: Long = 0L,
    val statusMessage: String = "✓ Up to date",
    val errorMessage: String? = null
)
