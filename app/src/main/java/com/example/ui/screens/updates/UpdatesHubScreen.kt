package com.example.ui.screens.updates

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.model.updates.CategoryFeedState
import com.example.data.model.updates.LatestUpdateItem
import com.example.data.model.updates.UpdateCategory
import com.example.ui.theme.*

/**
 * Sub-destinations for Latest Updates module.
 * Provides genuinely separate screens/routes for all 5 features:
 * 1. Vacancy
 * 2. Admit Card
 * 3. Result
 * 4. Answer Key
 * 5. Admission
 * Plus Detail screen and Main Updates Launcher Home.
 */
sealed class UpdatesSubDestination {
    object MainHub : UpdatesSubDestination()
    object Vacancy : UpdatesSubDestination()
    object AdmitCard : UpdatesSubDestination()
    object Result : UpdatesSubDestination()
    object AnswerKey : UpdatesSubDestination()
    object Admission : UpdatesSubDestination()
    data class Detail(val item: LatestUpdateItem) : UpdatesSubDestination()
}

@Composable
fun UpdatesHubScreen(
    vacancyState: CategoryFeedState,
    admitCardState: CategoryFeedState,
    resultState: CategoryFeedState,
    answerKeyState: CategoryFeedState,
    admissionState: CategoryFeedState,
    onLoadCategory: (UpdateCategory, Boolean) -> Unit,
    onSearchChange: (UpdateCategory, String) -> Unit,
    onOrgFilterChange: (UpdateCategory, String) -> Unit,
    onExamFilterChange: (UpdateCategory, String) -> Unit,
    onSortChange: (UpdateCategory, String) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onSetReminder: (String, Boolean, Int) -> Unit,
    onOpenBookmarks: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    initialCategory: UpdateCategory? = null,
    initialDetailItem: LatestUpdateItem? = null,
    onBackToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    // Determine initial destination
    val initialDest = remember(initialCategory, initialDetailItem) {
        when {
            initialDetailItem != null -> UpdatesSubDestination.Detail(initialDetailItem)
            initialCategory == UpdateCategory.VACANCY -> UpdatesSubDestination.Vacancy
            initialCategory == UpdateCategory.ADMIT_CARD -> UpdatesSubDestination.AdmitCard
            initialCategory == UpdateCategory.RESULT -> UpdatesSubDestination.Result
            initialCategory == UpdateCategory.ANSWER_KEY -> UpdatesSubDestination.AnswerKey
            initialCategory == UpdateCategory.ADMISSION -> UpdatesSubDestination.Admission
            else -> UpdatesSubDestination.MainHub
        }
    }

    // Hierarchical back stack for Latest Updates module
    val updatesStack = remember {
        mutableStateListOf<UpdatesSubDestination>().apply {
            if (initialDest !is UpdatesSubDestination.MainHub) {
                add(UpdatesSubDestination.MainHub)
            }
            add(initialDest)
        }
    }

    val currentDestination = updatesStack.lastOrNull() ?: UpdatesSubDestination.MainHub

    fun navigateUpdates(dest: UpdatesSubDestination) {
        if (updatesStack.lastOrNull() != dest) {
            updatesStack.add(dest)
        }
    }

    fun popUpdates() {
        if (updatesStack.size > 1) {
            updatesStack.removeAt(updatesStack.size - 1)
        } else {
            onBackToHome()
        }
    }

    // BackHandler manages internal sub-destination navigation stack
    BackHandler(enabled = true) {
        popUpdates()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundGradient(isDark))
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "updates_sub_destination"
        ) { destination ->
            when (destination) {
                is UpdatesSubDestination.MainHub -> {
                    UpdatesLauncherHomeScreen(
                        onNavigateToCategory = { category ->
                            onLoadCategory(category, false)
                            when (category) {
                                UpdateCategory.VACANCY -> navigateUpdates(UpdatesSubDestination.Vacancy)
                                UpdateCategory.ADMIT_CARD -> navigateUpdates(UpdatesSubDestination.AdmitCard)
                                UpdateCategory.RESULT -> navigateUpdates(UpdatesSubDestination.Result)
                                UpdateCategory.ANSWER_KEY -> navigateUpdates(UpdatesSubDestination.AnswerKey)
                                UpdateCategory.ADMISSION -> navigateUpdates(UpdatesSubDestination.Admission)
                            }
                        },
                        onOpenSearch = {
                            onLoadCategory(UpdateCategory.VACANCY, false)
                            navigateUpdates(UpdatesSubDestination.Vacancy)
                        },
                        onOpenBookmarks = onOpenBookmarks,
                        onOpenNotifications = onOpenNotifications
                    )
                }

                is UpdatesSubDestination.Vacancy -> {
                    LaunchedEffect(Unit) {
                        onLoadCategory(UpdateCategory.VACANCY, false)
                    }
                    VacancyListScreen(
                        items = vacancyState.items,
                        isLoading = vacancyState.isLoading,
                        errorMessage = vacancyState.errorMessage,
                        searchQuery = vacancyState.searchQuery,
                        selectedOrg = vacancyState.selectedOrg,
                        selectedExam = vacancyState.selectedExam,
                        selectedSort = vacancyState.selectedSort,
                        onSearchChange = { onSearchChange(UpdateCategory.VACANCY, it) },
                        onOrgFilterChange = { onOrgFilterChange(UpdateCategory.VACANCY, it) },
                        onExamFilterChange = { onExamFilterChange(UpdateCategory.VACANCY, it) },
                        onSortChange = { onSortChange(UpdateCategory.VACANCY, it) },
                        onRefresh = { onLoadCategory(UpdateCategory.VACANCY, true) },
                        onSelectDetail = { item ->
                            navigateUpdates(UpdatesSubDestination.Detail(item))
                        },
                        onToggleSave = onToggleSave,
                        onBackToLauncher = { popUpdates() }
                    )
                }

                is UpdatesSubDestination.AdmitCard -> {
                    LaunchedEffect(Unit) {
                        onLoadCategory(UpdateCategory.ADMIT_CARD, false)
                    }
                    AdmitCardListScreen(
                        items = admitCardState.items,
                        isLoading = admitCardState.isLoading,
                        errorMessage = admitCardState.errorMessage,
                        searchQuery = admitCardState.searchQuery,
                        selectedOrg = admitCardState.selectedOrg,
                        selectedExam = admitCardState.selectedExam,
                        selectedSort = admitCardState.selectedSort,
                        onSearchChange = { onSearchChange(UpdateCategory.ADMIT_CARD, it) },
                        onOrgFilterChange = { onOrgFilterChange(UpdateCategory.ADMIT_CARD, it) },
                        onExamFilterChange = { onExamFilterChange(UpdateCategory.ADMIT_CARD, it) },
                        onSortChange = { onSortChange(UpdateCategory.ADMIT_CARD, it) },
                        onRefresh = { onLoadCategory(UpdateCategory.ADMIT_CARD, true) },
                        onSelectDetail = { item ->
                            navigateUpdates(UpdatesSubDestination.Detail(item))
                        },
                        onToggleSave = onToggleSave,
                        onBackToLauncher = { popUpdates() }
                    )
                }

                is UpdatesSubDestination.Result -> {
                    LaunchedEffect(Unit) {
                        onLoadCategory(UpdateCategory.RESULT, false)
                    }
                    ResultListScreen(
                        items = resultState.items,
                        isLoading = resultState.isLoading,
                        errorMessage = resultState.errorMessage,
                        searchQuery = resultState.searchQuery,
                        selectedOrg = resultState.selectedOrg,
                        selectedExam = resultState.selectedExam,
                        selectedSort = resultState.selectedSort,
                        onSearchChange = { onSearchChange(UpdateCategory.RESULT, it) },
                        onOrgFilterChange = { onOrgFilterChange(UpdateCategory.RESULT, it) },
                        onExamFilterChange = { onExamFilterChange(UpdateCategory.RESULT, it) },
                        onSortChange = { onSortChange(UpdateCategory.RESULT, it) },
                        onRefresh = { onLoadCategory(UpdateCategory.RESULT, true) },
                        onSelectDetail = { item ->
                            navigateUpdates(UpdatesSubDestination.Detail(item))
                        },
                        onToggleSave = onToggleSave,
                        onBackToLauncher = { popUpdates() }
                    )
                }

                is UpdatesSubDestination.AnswerKey -> {
                    LaunchedEffect(Unit) {
                        onLoadCategory(UpdateCategory.ANSWER_KEY, false)
                    }
                    AnswerKeyListScreen(
                        items = answerKeyState.items,
                        isLoading = answerKeyState.isLoading,
                        errorMessage = answerKeyState.errorMessage,
                        searchQuery = answerKeyState.searchQuery,
                        selectedOrg = answerKeyState.selectedOrg,
                        selectedExam = answerKeyState.selectedExam,
                        selectedSort = answerKeyState.selectedSort,
                        onSearchChange = { onSearchChange(UpdateCategory.ANSWER_KEY, it) },
                        onOrgFilterChange = { onOrgFilterChange(UpdateCategory.ANSWER_KEY, it) },
                        onExamFilterChange = { onExamFilterChange(UpdateCategory.ANSWER_KEY, it) },
                        onSortChange = { onSortChange(UpdateCategory.ANSWER_KEY, it) },
                        onRefresh = { onLoadCategory(UpdateCategory.ANSWER_KEY, true) },
                        onSelectDetail = { item ->
                            navigateUpdates(UpdatesSubDestination.Detail(item))
                        },
                        onToggleSave = onToggleSave,
                        onBackToLauncher = { popUpdates() }
                    )
                }

                is UpdatesSubDestination.Admission -> {
                    LaunchedEffect(Unit) {
                        onLoadCategory(UpdateCategory.ADMISSION, false)
                    }
                    AdmissionListScreen(
                        items = admissionState.items,
                        isLoading = admissionState.isLoading,
                        errorMessage = admissionState.errorMessage,
                        searchQuery = admissionState.searchQuery,
                        selectedOrg = admissionState.selectedOrg,
                        selectedExam = admissionState.selectedExam,
                        selectedSort = admissionState.selectedSort,
                        onSearchChange = { onSearchChange(UpdateCategory.ADMISSION, it) },
                        onOrgFilterChange = { onOrgFilterChange(UpdateCategory.ADMISSION, it) },
                        onExamFilterChange = { onExamFilterChange(UpdateCategory.ADMISSION, it) },
                        onSortChange = { onSortChange(UpdateCategory.ADMISSION, it) },
                        onRefresh = { onLoadCategory(UpdateCategory.ADMISSION, true) },
                        onSelectDetail = { item ->
                            navigateUpdates(UpdatesSubDestination.Detail(item))
                        },
                        onToggleSave = onToggleSave,
                        onBackToLauncher = { popUpdates() }
                    )
                }

                is UpdatesSubDestination.Detail -> {
                    UpdateDetailScreen(
                        item = destination.item,
                        onBack = { popUpdates() },
                        onToggleSave = onToggleSave,
                        onSetReminder = onSetReminder
                    )
                }
            }
        }
    }
}
