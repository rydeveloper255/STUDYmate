package com.example.navigation

import com.example.ui.components.AppNavTab

sealed class AppDestination {
    data class MainTab(val tab: AppNavTab) : AppDestination()
    data class LearnHub(val initialSubModule: String? = null) : AppDestination()
    data class PracticeHub(val initialSubModule: String? = null) : AppDestination()
    object DailyCurrentAffairs : AppDestination()
    data class UpdatesCategory(val category: com.example.data.model.updates.UpdateCategory, val detailId: String? = null) : AppDestination()
    data class UpdateDetail(val item: com.example.data.model.updates.LatestUpdateItem) : AppDestination()
    data class SmartVacancy(val initialTab: String = "VACANCY", val detailId: String? = null) : AppDestination()
    object LiveExamIntelligence : AppDestination()
    object NotificationCenter : AppDestination()
    object DailyBriefing : AppDestination()
    object ProfileSettings : AppDestination()
    object ExamReadinessCenter : AppDestination()
    object StudySchedule : AppDestination()
    object SmartPlanner : AppDestination()
    object DocumentSummarizer : AppDestination()
    object RevisionHub : AppDestination()
    object ActiveMockTest : AppDestination()
}

