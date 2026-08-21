package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.persistence.PersistenceStatus

@Composable
fun PersistenceStatusIndicator(
    status: PersistenceStatus,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    testTagPrefix: String = "persistence"
) {
    AnimatedVisibility(
        visible = status !is PersistenceStatus.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val (bgColor, borderColor, iconColor, icon, text) = when (status) {
            is PersistenceStatus.Saving -> Triple5(
                Color(0xFF0F172A),
                Color(0xFF38BDF8).copy(alpha = 0.4f),
                Color(0xFF38BDF8),
                null,
                "Saving..."
            )
            is PersistenceStatus.Syncing -> Triple5(
                Color(0xFF0F172A),
                Color(0xFF38BDF8).copy(alpha = 0.4f),
                Color(0xFF38BDF8),
                Icons.Filled.Refresh,
                "↻ Syncing with cloud..."
            )
            is PersistenceStatus.Saved -> Triple5(
                Color(0xFF064E3B).copy(alpha = 0.3f),
                Color(0xFF10B981).copy(alpha = 0.6f),
                Color(0xFF10B981),
                Icons.Filled.CheckCircle,
                status.message
            )
            is PersistenceStatus.Offline -> Triple5(
                Color(0xFF451A03).copy(alpha = 0.3f),
                Color(0xFFF59E0B).copy(alpha = 0.6f),
                Color(0xFFF59E0B),
                Icons.Filled.CloudOff,
                status.message
            )
            is PersistenceStatus.Failed -> Triple5(
                Color(0xFF4C1D95).copy(alpha = 0.3f),
                Color(0xFFEF4444).copy(alpha = 0.6f),
                Color(0xFFEF4444),
                Icons.Filled.Warning,
                status.message
            )
            PersistenceStatus.Idle -> Triple5(
                Color.Transparent, Color.Transparent, Color.Transparent, null, ""
            )
        }

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_status_indicator"),
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (status is PersistenceStatus.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = iconColor,
                            strokeWidth = 2.dp
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (status is PersistenceStatus.Failed && status.canRetry && onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class Triple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
