package com.example.ui.screens.feedback

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FeedbackCategory
import com.example.data.model.UserFeedbackEntity
import com.example.service.feedback.FeedbackAttachmentUtility
import com.example.service.feedback.FeedbackManager
import com.example.service.feedback.ProcessedAttachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackFormUiState(
    val selectedCategory: FeedbackCategory = FeedbackCategory.BUG_REPORT,
    val affectedFeature: String = "General",
    val titleText: String = "",
    val descriptionText: String = "",
    val isHighPriority: Boolean = false,
    val relatedErrorId: String? = null,
    val selectedUris: List<Uri> = emptyList(),
    val processedAttachments: List<ProcessedAttachment> = emptyList(),
    val isProcessingAttachments: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionSuccessEntity: UserFeedbackEntity? = null,
    val errorMessage: String? = null,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
)

class FeedbackViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackFormUiState())
    val uiState: StateFlow<FeedbackFormUiState> = _uiState.asStateFlow()

    private val _feedbackHistory = MutableStateFlow<List<UserFeedbackEntity>>(emptyList())
    val feedbackHistory: StateFlow<List<UserFeedbackEntity>> = _feedbackHistory.asStateFlow()

    fun setInitialContext(feature: String?, errorId: String?) {
        _uiState.value = _uiState.value.copy(
            affectedFeature = feature?.ifBlank { "General" } ?: "General",
            relatedErrorId = errorId
        )
    }

    fun setCategory(category: FeedbackCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setAffectedFeature(feature: String) {
        _uiState.value = _uiState.value.copy(affectedFeature = feature)
    }

    fun setTitle(title: String) {
        _uiState.value = _uiState.value.copy(titleText = title)
    }

    fun setDescription(desc: String) {
        _uiState.value = _uiState.value.copy(descriptionText = desc)
    }

    fun setHighPriority(isHigh: Boolean) {
        _uiState.value = _uiState.value.copy(isHighPriority = isHigh)
    }

    fun addUris(context: Context, newUris: List<Uri>) {
        val currentUris = _uiState.value.selectedUris.toMutableList()
        currentUris.addAll(newUris)
        _uiState.value = _uiState.value.copy(selectedUris = currentUris, isProcessingAttachments = true)

        viewModelScope.launch {
            val res = FeedbackAttachmentUtility.processAndValidateUris(context, currentUris)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    processedAttachments = res.getOrDefault(emptyList()),
                    isProcessingAttachments = false,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessingAttachments = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Attachment validation failed."
                )
            }
        }
    }

    fun removeUri(uri: Uri) {
        val updatedUris = _uiState.value.selectedUris.filterNot { it == uri }
        val updatedProcessed = _uiState.value.processedAttachments.filterNot { 
            it.file.name.contains(uri.lastPathSegment ?: "")
        }
        _uiState.value = _uiState.value.copy(
            selectedUris = updatedUris,
            processedAttachments = updatedProcessed
        )
    }

    fun removeAttachment(attachment: ProcessedAttachment) {
        attachment.file.delete()
        val updatedProcessed = _uiState.value.processedAttachments.filterNot { it == attachment }
        _uiState.value = _uiState.value.copy(processedAttachments = updatedProcessed)
    }

    fun submitFeedback(context: Context) {
        val currentState = _uiState.value
        if (currentState.descriptionText.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "Kripya description me detail likhein.")
            return
        }

        if (currentState.isSubmitting) return

        _uiState.value = currentState.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val res = FeedbackManager.submitFeedback(
                context = context,
                category = currentState.selectedCategory,
                title = currentState.titleText,
                description = currentState.descriptionText,
                affectedFeature = currentState.affectedFeature,
                isHighPriority = currentState.isHighPriority,
                relatedErrorId = currentState.relatedErrorId,
                attachments = currentState.processedAttachments
            )

            if (res.isSuccess) {
                val entity = res.getOrNull()
                _uiState.value = FeedbackFormUiState(
                    submissionSuccessEntity = entity,
                    isSubmitting = false
                )
                // Reload history list
                loadFeedbackHistory(context)
            } else {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Feedback submit karne me error aaya."
                )
            }
        }
    }

    fun clearSuccessState() {
        _uiState.value = _uiState.value.copy(submissionSuccessEntity = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun loadFeedbackHistory(context: Context) {
        viewModelScope.launch {
            FeedbackManager.getAllFeedbackFlow(context).collect { list ->
                _feedbackHistory.value = list
            }
        }
    }
}
