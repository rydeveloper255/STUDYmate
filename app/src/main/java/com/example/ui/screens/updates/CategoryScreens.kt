package com.example.ui.screens.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.updates.LatestUpdateItem
import com.example.data.model.updates.UpdateCategory

@Composable
fun VacancyListScreen(
    items: List<LatestUpdateItem>,
    isLoading: Boolean,
    errorMessage: String?,
    searchQuery: String,
    selectedOrg: String,
    selectedExam: String,
    selectedSort: String,
    onSearchChange: (String) -> Unit,
    onOrgFilterChange: (String) -> Unit,
    onExamFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDetail: (LatestUpdateItem) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onBackToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryUpdateListScreen(
        category = UpdateCategory.VACANCY,
        items = items,
        isLoading = isLoading,
        errorMessage = errorMessage,
        searchQuery = searchQuery,
        selectedOrg = selectedOrg,
        selectedExam = selectedExam,
        selectedSort = selectedSort,
        onSearchChange = onSearchChange,
        onOrgFilterChange = onOrgFilterChange,
        onExamFilterChange = onExamFilterChange,
        onSortChange = onSortChange,
        onRefresh = onRefresh,
        onSelectDetail = onSelectDetail,
        onToggleSave = onToggleSave,
        onBackToLauncher = onBackToLauncher,
        modifier = modifier
    )
}

@Composable
fun AdmitCardListScreen(
    items: List<LatestUpdateItem>,
    isLoading: Boolean,
    errorMessage: String?,
    searchQuery: String,
    selectedOrg: String,
    selectedExam: String,
    selectedSort: String,
    onSearchChange: (String) -> Unit,
    onOrgFilterChange: (String) -> Unit,
    onExamFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDetail: (LatestUpdateItem) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onBackToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryUpdateListScreen(
        category = UpdateCategory.ADMIT_CARD,
        items = items,
        isLoading = isLoading,
        errorMessage = errorMessage,
        searchQuery = searchQuery,
        selectedOrg = selectedOrg,
        selectedExam = selectedExam,
        selectedSort = selectedSort,
        onSearchChange = onSearchChange,
        onOrgFilterChange = onOrgFilterChange,
        onExamFilterChange = onExamFilterChange,
        onSortChange = onSortChange,
        onRefresh = onRefresh,
        onSelectDetail = onSelectDetail,
        onToggleSave = onToggleSave,
        onBackToLauncher = onBackToLauncher,
        modifier = modifier
    )
}

@Composable
fun ResultListScreen(
    items: List<LatestUpdateItem>,
    isLoading: Boolean,
    errorMessage: String?,
    searchQuery: String,
    selectedOrg: String,
    selectedExam: String,
    selectedSort: String,
    onSearchChange: (String) -> Unit,
    onOrgFilterChange: (String) -> Unit,
    onExamFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDetail: (LatestUpdateItem) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onBackToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryUpdateListScreen(
        category = UpdateCategory.RESULT,
        items = items,
        isLoading = isLoading,
        errorMessage = errorMessage,
        searchQuery = searchQuery,
        selectedOrg = selectedOrg,
        selectedExam = selectedExam,
        selectedSort = selectedSort,
        onSearchChange = onSearchChange,
        onOrgFilterChange = onOrgFilterChange,
        onExamFilterChange = onExamFilterChange,
        onSortChange = onSortChange,
        onRefresh = onRefresh,
        onSelectDetail = onSelectDetail,
        onToggleSave = onToggleSave,
        onBackToLauncher = onBackToLauncher,
        modifier = modifier
    )
}

@Composable
fun AnswerKeyListScreen(
    items: List<LatestUpdateItem>,
    isLoading: Boolean,
    errorMessage: String?,
    searchQuery: String,
    selectedOrg: String,
    selectedExam: String,
    selectedSort: String,
    onSearchChange: (String) -> Unit,
    onOrgFilterChange: (String) -> Unit,
    onExamFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDetail: (LatestUpdateItem) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onBackToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryUpdateListScreen(
        category = UpdateCategory.ANSWER_KEY,
        items = items,
        isLoading = isLoading,
        errorMessage = errorMessage,
        searchQuery = searchQuery,
        selectedOrg = selectedOrg,
        selectedExam = selectedExam,
        selectedSort = selectedSort,
        onSearchChange = onSearchChange,
        onOrgFilterChange = onOrgFilterChange,
        onExamFilterChange = onExamFilterChange,
        onSortChange = onSortChange,
        onRefresh = onRefresh,
        onSelectDetail = onSelectDetail,
        onToggleSave = onToggleSave,
        onBackToLauncher = onBackToLauncher,
        modifier = modifier
    )
}

@Composable
fun AdmissionListScreen(
    items: List<LatestUpdateItem>,
    isLoading: Boolean,
    errorMessage: String?,
    searchQuery: String,
    selectedOrg: String,
    selectedExam: String,
    selectedSort: String,
    onSearchChange: (String) -> Unit,
    onOrgFilterChange: (String) -> Unit,
    onExamFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDetail: (LatestUpdateItem) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onBackToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryUpdateListScreen(
        category = UpdateCategory.ADMISSION,
        items = items,
        isLoading = isLoading,
        errorMessage = errorMessage,
        searchQuery = searchQuery,
        selectedOrg = selectedOrg,
        selectedExam = selectedExam,
        selectedSort = selectedSort,
        onSearchChange = onSearchChange,
        onOrgFilterChange = onOrgFilterChange,
        onExamFilterChange = onExamFilterChange,
        onSortChange = onSortChange,
        onRefresh = onRefresh,
        onSelectDetail = onSelectDetail,
        onToggleSave = onToggleSave,
        onBackToLauncher = onBackToLauncher,
        modifier = modifier
    )
}
