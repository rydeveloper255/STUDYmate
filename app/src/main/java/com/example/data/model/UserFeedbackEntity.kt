package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Categories of User Feedback / Bug Reports (Step 76)
 */
enum class FeedbackCategory(
    val label: String,
    val hindiLabel: String,
    val iconEmoji: String
) {
    BUG_REPORT("Report a Bug", "बग की शिकायत करें", "🐞"),
    FEATURE_NOT_WORKING("Feature Not Working", "फीचर काम नहीं कर रहा", "⚙️"),
    FEATURE_SUGGESTION("Suggest a Feature", "सुझाव दें", "💡"),
    CONTENT_ISSUE("Content Issue", "सामग्री/प्रश्नों में त्रुटि", "📚"),
    LOGIN_ACCOUNT("Account/Login Issue", "खाता/लॉगिन समस्या", "🔐"),
    PERFORMANCE_LAG("Performance/Lag Issue", "ऐप धीमा/लैग हो रहा है", "🐌"),
    OTHER("Other", "अन्य", "❓");

    companion object {
        fun fromString(value: String?): FeedbackCategory {
            if (value == null) return OTHER
            return values().firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                        it.label.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}

/**
 * Database Entity representing a submitted or pending User Feedback / Bug Report.
 */
@Entity(tableName = "user_feedback")
data class UserFeedbackEntity(
    @PrimaryKey
    val feedbackId: String, // Format: FB-YYYYMMDD-XXXXXX
    val category: String, // FeedbackCategory.name
    val title: String = "",
    val description: String,
    val affectedFeature: String = "General",
    val isHighPriority: Boolean = false,
    val relatedErrorId: String? = null,
    val userId: String = "ANONYMOUS",
    val userName: String = "Student",
    val userEmail: String = "",
    val appVersion: String = "1.0.1",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val status: String = "NEW", // NEW, REVIEWING, RESOLVED
    val createdAtMillis: Long = System.currentTimeMillis(),
    val attachmentPathsJson: String? = null, // JSON list of file paths
    val syncState: String = "PENDING" // PENDING, SENT, FAILED
)
