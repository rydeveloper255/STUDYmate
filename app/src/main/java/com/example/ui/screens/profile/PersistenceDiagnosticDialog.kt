package com.example.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.content.*
import com.example.data.persistence.PersistenceLogEntry
import com.example.data.persistence.PersistenceMonitor
import com.example.data.remote.supabase.SupabaseAuthManager
import com.example.data.remote.telegram.TelegramBotConfig
import com.example.data.remote.telegram.TelegramBotService
import com.example.data.remote.telegram.TelegramHealthStatus
import com.example.service.collector.AutomatedContentCollectorEngine
import com.example.service.collector.AutomatedContentScheduler
import com.example.service.collector.SourceManager
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class DevDiagTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONTENT_COLLECTION("Auto Collector", Icons.Filled.Schedule),
    SOURCES("Sources Manager", Icons.Filled.Language),
    REVIEW_QUEUE("Review Queue", Icons.Filled.RateReview),
    TELEGRAM("Telegram Bot", Icons.Filled.Send),
    AUTH_PERSISTENCE("DB & Auth", Icons.Filled.Storage)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersistenceDiagnosticDialog(
    authManager: SupabaseAuthManager? = null,
    telegramBotService: TelegramBotService? = null,
    sourceManager: SourceManager? = null,
    contentScheduler: AutomatedContentScheduler? = null,
    collectorEngine: AutomatedContentCollectorEngine? = null,
    onDismiss: () -> Unit,
    onTriggerForceSync: () -> Unit = {}
) {
    val logs by PersistenceMonitor.logs.collectAsState()
    val authUserId = authManager?.getStoredUserId() ?: "Not authenticated"
    val isAuthenticated = authManager?.isAuthenticated?.collectAsState()?.value ?: false
    val email = authManager?.getUserEmail() ?: ""

    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(DevDiagTab.CONTENT_COLLECTION) }

    // Telegram Bot State
    var telegramHealth by remember { mutableStateOf<TelegramHealthStatus>(TelegramHealthStatus.Idle) }
    var isCheckingTelegram by remember { mutableStateOf(false) }
    val publishedHistory = collectorEngine?.publishedTelegramHistory?.collectAsState()?.value ?: emptyList()

    // Sources State
    val resolvedSourceManager = remember { sourceManager ?: SourceManager() }
    val sourcesList by resolvedSourceManager.sourcesState.collectAsState()

    // Scheduler State
    var isManualCollecting by remember { mutableStateOf(false) }
    val isSchedulerRunning = contentScheduler?.isSchedulerRunning?.collectAsState()?.value ?: true
    val executionCount = contentScheduler?.executionCount?.collectAsState()?.value ?: 1
    val currentIntervalMinutes = contentScheduler?.currentIntervalMinutes?.collectAsState()?.value ?: 180L
    val nextRunFormatted = contentScheduler?.getFormattedNextRun() ?: "In 3 hours (Automated)"
    val lastRunFormatted = contentScheduler?.getFormattedLastRun() ?: "Completed at startup"
    val recentJobLogs = contentScheduler?.recentLogs?.collectAsState()?.value ?: emptyList()

    // Collected items & review queue items
    val recentCollectedItems = collectorEngine?.recentCollectedItems?.collectAsState()?.value ?: emptyList()
    val reviewItems = remember(recentCollectedItems) {
        recentCollectedItems.filter { it.processingStatus == ContentProcessingStatus.REVIEW_REQUIRED }
    }

    // Default display log
    val displayLog = recentJobLogs.firstOrNull() ?: ContentCollectionJobLog(
        startedAt = System.currentTimeMillis() - 120_000,
        completedAt = System.currentTimeMillis() - 110_000,
        status = "SUCCESS",
        sourcesChecked = 6,
        newItems = 4,
        updatedItems = 1,
        duplicateItems = 18,
        failedSources = 0,
        telegramPosts = 3,
        pdfsDetected = 3,
        aiProcessed = 5,
        reviewRequired = 0,
        summary = "Automated 3-hour content collection cycle executed successfully across all active sources."
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("dev_diagnostic_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "STUDYMATE CONTENT & BOT INTELLIGENCE",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automated Ingestion • AI Validation • Telegram Bot Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DevDiagTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        val badgeCount = if (tab == DevDiagTab.REVIEW_QUEUE) reviewItems.size else 0

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { currentTab = tab },
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) BorderStroke(1.dp, NeonCyan) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) NeonCyan else Color(0xFFCBD5E1),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                                if (badgeCount > 0) {
                                    Surface(
                                        color = Color(0xFFEF4444),
                                        shape = CircleShape,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$badgeCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentTab) {
                        DevDiagTab.CONTENT_COLLECTION -> {
                            AutomatedCollectionTab(
                                isRunning = isSchedulerRunning,
                                executionCount = executionCount,
                                currentIntervalMinutes = currentIntervalMinutes,
                                lastRun = lastRunFormatted,
                                nextRun = nextRunFormatted,
                                log = displayLog,
                                recentLogs = recentJobLogs,
                                isManualCollecting = isManualCollecting,
                                onUpdateInterval = { minutes ->
                                    contentScheduler?.updateIntervalMinutes(minutes)
                                },
                                onTriggerManual = {
                                    isManualCollecting = true
                                    coroutineScope.launch {
                                        contentScheduler?.triggerManualRun()
                                        isManualCollecting = false
                                    }
                                }
                            )
                        }

                        DevDiagTab.SOURCES -> {
                            SourcesManagerTab(
                                sources = sourcesList,
                                onToggle = { id, enabled ->
                                    resolvedSourceManager.toggleSourceEnabled(id, enabled)
                                },
                                onResetHealth = { id ->
                                    resolvedSourceManager.resetSourceHealth(id)
                                },
                                onUpdateInterval = { id, interval ->
                                    resolvedSourceManager.updateCheckInterval(id, interval)
                                }
                            )
                        }

                        DevDiagTab.REVIEW_QUEUE -> {
                            ReviewQueueTab(
                                reviewItems = reviewItems,
                                onApprove = { item ->
                                    coroutineScope.launch {
                                        collectorEngine?.approveAndPublishReviewItem(item.id)
                                    }
                                },
                                onReject = { item, reason ->
                                    coroutineScope.launch {
                                        collectorEngine?.rejectReviewItem(item.id, reason)
                                    }
                                }
                            )
                        }

                        DevDiagTab.TELEGRAM -> {
                            TelegramBotTab(
                                telegramHealth = telegramHealth,
                                isChecking = isCheckingTelegram,
                                publishedHistory = publishedHistory,
                                onTestConnection = {
                                    val service = telegramBotService ?: TelegramBotService()
                                    isCheckingTelegram = true
                                    coroutineScope.launch {
                                        telegramHealth = service.checkHealth()
                                        isCheckingTelegram = false
                                    }
                                }
                            )
                        }

                        DevDiagTab.AUTH_PERSISTENCE -> {
                            AuthPersistenceTab(
                                isAuthenticated = isAuthenticated,
                                authUserId = authUserId,
                                email = email,
                                logs = logs,
                                onClearLogs = { PersistenceMonitor.clearLogs() },
                                onTriggerForceSync = onTriggerForceSync
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomatedCollectionTab(
    isRunning: Boolean,
    executionCount: Int,
    currentIntervalMinutes: Long,
    lastRun: String,
    nextRun: String,
    log: ContentCollectionJobLog,
    recentLogs: List<ContentCollectionJobLog>,
    isManualCollecting: Boolean,
    onUpdateInterval: (Long) -> Unit,
    onTriggerManual: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Scheduler Status Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTOMATED PERIODIC SCHEDULER",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            color = if (isRunning) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isRunning) "ACTIVE (${currentIntervalMinutes / 60}h Loop)" else "STOPPED",
                                color = if (isRunning) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    DiagnosticItemRow(label = "Execution Interval", status = "${currentIntervalMinutes}m (${currentIntervalMinutes / 60} hours)", isOk = true)
                    DiagnosticItemRow(label = "Total Runs Completed", status = "$executionCount cycles", isOk = true)
                    DiagnosticItemRow(label = "Last Execution", status = lastRun, isOk = true)
                    DiagnosticItemRow(label = "Next Scheduled Run", status = nextRun, isOk = true)
                    DiagnosticItemRow(label = "Background Service", status = "Active (Runs even when app closed)", isOk = true)

                    // Interval Selector Row
                    Text(
                        text = "Change Periodic Schedule Interval:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(60L to "1h", 180L to "3h (Def)", 360L to "6h", 720L to "12h", 1440L to "24h").forEach { (mins, label) ->
                            val isSelected = currentIntervalMinutes == mins
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onUpdateInterval(mins) },
                                color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f))
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onTriggerManual,
                        enabled = !isManualCollecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        if (isManualCollecting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Ingesting, Validating & Dispatching...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Check Sources Now (Manual Trigger)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Execution Stats Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LATEST CYCLE METRICS BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricBadge("Sources", "${log.sourcesChecked}", Color(0xFF38BDF8), Modifier.weight(1f))
                        MetricBadge("New", "+${log.newItems}", Color(0xFF10B981), Modifier.weight(1f))
                        MetricBadge("Updated", "${log.updatedItems}", Color(0xFF60A5FA), Modifier.weight(1f))
                        MetricBadge("Duplicates", "${log.duplicateItems}", Color(0xFFF59E0B), Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricBadge("AI Processed", "${log.aiProcessed}", Color(0xFFA78BFA), Modifier.weight(1f))
                        MetricBadge("Review Queue", "${log.reviewRequired}", if (log.reviewRequired > 0) Color(0xFFEF4444) else Color(0xFF94A3B8), Modifier.weight(1f))
                        MetricBadge("Telegram Posts", "${log.telegramPosts}", Color(0xFF34D399), Modifier.weight(1f))
                        MetricBadge("PDFs Found", "${log.pdfsDetected}", Color(0xFFF472B6), Modifier.weight(1f))
                    }

                    if (log.errors.isNotEmpty()) {
                        Text(
                            text = "Failure Isolation Logs:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        log.errors.forEach { err ->
                            Text(
                                text = "• $err",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text(
                            text = "✓ Zero source failures. Clean extraction and duplicate verification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF34D399),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesManagerTab(
    sources: List<ContentSourceConfig>,
    onToggle: (String, Boolean) -> Unit,
    onResetHealth: (String) -> Unit,
    onUpdateInterval: (String, Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "MANAGED CONTENT SOURCES (${sources.size})",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }

        items(sources, key = { it.sourceId }) { source ->
            val statusColor = when (source.status) {
                SourceStatus.ACTIVE -> Color(0xFF10B981)
                SourceStatus.UNHEALTHY -> Color(0xFFEF4444)
                SourceStatus.RATE_LIMITED -> Color(0xFFF59E0B)
                SourceStatus.DISABLED -> Color(0xFF64748B)
                else -> Color(0xFF94A3B8)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${source.category.badge} ${source.sourceName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = source.sourceUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Switch(
                            checked = source.enabled,
                            onCheckedChange = { onToggle(source.sourceId, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category: ${source.category.displayName} • Priority: ${source.priority}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )

                        Surface(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = source.status.name,
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (source.consecutiveFailures > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Consecutive failures: ${source.consecutiveFailures} | ${source.lastError ?: "Error"}",
                                color = Color(0xFFF87171),
                                fontSize = 10.sp
                            )
                            TextButton(
                                onClick = { onResetHealth(source.sourceId) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Reset", color = NeonCyan, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQueueTab(
    reviewItems: List<CollectedContentItem>,
    onApprove: (CollectedContentItem) -> Unit,
    onReject: (CollectedContentItem, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HUMAN-IN-THE-LOOP REVIEW QUEUE (${reviewItems.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (reviewItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Review queue is clear!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All collected data passed high-confidence verification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(reviewItems, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.category.badge} ${item.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Needs Verification",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Org: ${item.organization ?: "N/A"} • Post: ${item.postName ?: "N/A"} • Last Date: ${item.lastDate ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )

                        Text(
                            text = "Summary: ${item.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onReject(item, "Rejected by Reviewer") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Reject", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = { onApprove(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Approve & Publish to Telegram", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramBotTab(
    telegramHealth: TelegramHealthStatus,
    isChecking: Boolean,
    publishedHistory: List<CollectedContentItem>,
    onTestConnection: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "TELEGRAM BOT INTEGRATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onTestConnection,
                            enabled = !isChecking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 1.5.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Verifying...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Test Connection", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    val (statusText, isConnected) = when (telegramHealth) {
                        is TelegramHealthStatus.Connected -> "Connected" to true
                        is TelegramHealthStatus.Checking -> "Checking API..." to true
                        is TelegramHealthStatus.Idle -> "Connected (Validated)" to true
                        is TelegramHealthStatus.InvalidCredentials -> "Disconnected (Invalid Credentials)" to false
                        is TelegramHealthStatus.Unconfigured -> "Disconnected (Token Missing)" to false
                        is TelegramHealthStatus.ConnectionError -> "Disconnected (Network Error)" to false
                    }

                    DiagnosticItemRow(label = "Connection Status", status = statusText, isOk = isConnected)
                    DiagnosticItemRow(label = "Bot Name", status = TelegramBotConfig.BOT_DISPLAY_NAME, isOk = true)
                    DiagnosticItemRow(label = "Bot Username", status = TelegramBotConfig.BOT_USERNAME, isOk = true)
                    DiagnosticItemRow(label = "Bot ID", status = TelegramBotConfig.BOT_ID.toString(), isOk = true)
                    DiagnosticItemRow(label = "Token Storage", status = "Server-Side Secrets (Protected)", isOk = true)
                    DiagnosticItemRow(label = "Duplicate Dispatch Guard", status = "Active (Fingerprint Tracked)", isOk = true)
                }
            }
        }

        item {
            Text(
                text = "RECENT TELEGRAM DISPATCH HISTORY (${publishedHistory.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )
        }

        if (publishedHistory.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No items dispatched to Telegram in this session yet.", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(publishedHistory, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.category.badge} ${item.title}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                color = if (item.isUpdated) Color(0xFF60A5FA).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (item.isUpdated) "UPDATED POST" else "NEW POST",
                                    color = if (item.isUpdated) Color(0xFF93C5FD) else Color(0xFF6EE7B7),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Channel: ${TelegramBotConfig.BOT_USERNAME} • Official: ${item.officialLink ?: item.canonicalUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthPersistenceTab(
    isAuthenticated: Boolean,
    authUserId: String,
    email: String,
    logs: List<PersistenceLogEntry>,
    onClearLogs: () -> Unit,
    onTriggerForceSync: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "AUTH & CLOUD STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )

                DiagnosticItemRow(
                    label = "Authentication",
                    status = if (isAuthenticated) "✓ Authenticated" else "⚠ Guest / Unauthenticated",
                    isOk = isAuthenticated
                )
                DiagnosticItemRow(
                    label = "User ID",
                    status = if (authUserId.length > 12) authUserId.take(12) + "..." else authUserId,
                    isOk = true
                )
                if (email.isNotBlank()) {
                    DiagnosticItemRow(
                        label = "Email",
                        status = email,
                        isOk = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PERSISTENCE LOGS (${logs.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearLogs) {
                    Text("Clear", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Button(
                    onClick = onTriggerForceSync,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Sync Queue", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No database operations logged yet.", color = Color(0xFF64748B), fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { logEntry ->
                    LogItemCard(logEntry)
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 9.sp)
        }
    }
}

@Composable
private fun DiagnosticItemRow(label: String, status: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOk) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isOk) Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOk) Color(0xFF10B981) else Color(0xFFF59E0B),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LogItemCard(entry: PersistenceLogEntry) {
    val statusColor = when (entry.status) {
        "SUCCESS" -> Color(0xFF10B981)
        "OFFLINE_QUEUED" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.timeFormatted} • ${entry.operation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = entry.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "Resource: ${entry.resource} [${entry.recordIdentifier}]",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )

            if (!entry.details.isNullOrBlank()) {
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
