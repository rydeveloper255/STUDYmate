package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ResourceType(val displayName: String) {
    ALL("All Resources"),
    NOTES("Notes"),
    PDF("PDF Document"),
    CURRENT_AFFAIRS("Current Affairs"),
    REVISION("Revision Sheet"),
    QUESTION_SET("Question Set"),
    MOCK_TEST("Mock Test"),
    SAVED_RESOURCE("Saved Resources")
}

enum class ResourceSource(val displayName: String) {
    OFFICIAL("Official"),
    STUDYMATE("StudyMate Verified"),
    USER_UPLOADED("User Uploaded"),
    AI_GENERATED("AI Generated"),
    EXTERNAL("External Source")
}

enum class ReadingStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

enum class ResourceStatus {
    ACTIVE,
    ARCHIVED,
    EXPIRED
}

@Entity(tableName = "study_resources")
data class StudyResourceEntity(
    @PrimaryKey val resourceId: String = UUID.randomUUID().toString(),
    val ownerScope: String = "GLOBAL", // "GLOBAL" or userId
    val title: String,
    val description: String = "",
    val resourceType: String = ResourceType.PDF.name, // NOTES, PDF, CURRENT_AFFAIRS, REVISION, QUESTION_SET, MOCK_TEST, SAVED_RESOURCE
    val examId: String = "",
    val examName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val topicId: String = "",
    val topicName: String = "",
    val language: String = "English", // English, Hindi, Hinglish
    val source: String = ResourceSource.STUDYMATE.name, // OFFICIAL, STUDYMATE, USER_UPLOADED, AI_GENERATED, EXTERNAL
    val fileUrl: String = "", // URI / local file path / asset path
    val thumbnail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = ResourceStatus.ACTIVE.name, // ACTIVE, ARCHIVED, EXPIRED
    // Progress & state
    val readingStatus: String = ReadingStatus.NOT_STARTED.name,
    val lastViewedPage: Int = 1,
    val totalPages: Int = 1,
    val isSaved: Boolean = false,
    val isBookmarked: Boolean = false,
    val isDownloadedOffline: Boolean = false,
    val offlineFilePath: String = "",
    val fileSizeBytes: Long = 0L,
    val aiSummaryCache: String = "",
    val version: Int = 1,
    val contentText: String = "", // Source extracted text for search / document Q&A / notes
    val aiRecommendationReason: String = ""
)

@Entity(tableName = "resource_bookmarks")
data class ResourceBookmarkEntity(
    @PrimaryKey val bookmarkId: String = UUID.randomUUID().toString(),
    val resourceId: String,
    val userId: String = "current_user",
    val pageNumber: Int = 1,
    val noteSnippet: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ResourceSearchResult(
    val resource: StudyResourceEntity,
    val relevanceScore: Int,
    val matchReason: String
)
