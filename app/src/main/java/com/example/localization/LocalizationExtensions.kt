package com.example.localization

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
val LocalLanguageManager = staticCompositionLocalOf<LanguageManager?> { null }

@Composable
fun appString(key: String, vararg args: Any): String {
    val language = LocalAppLanguage.current
    return AppStrings.get(key, language, *args)
}

/**
 * Universal Global Language Switcher Pill.
 * Shows "EN | हिन्दी" with active highlight and instant reactive update.
 */
@Composable
fun GlobalLanguageSwitcher(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    compact: Boolean = false,
    onLanguageChanged: ((AppLanguage) -> Unit)? = null
) {
    val languageManager = LocalLanguageManager.current
    val currentLang = LocalAppLanguage.current

    val activeBgColor = Color(0xFF00E5FF)
    val activeTextColor = Color(0xFF0A0F1D)
    val inactiveTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val containerBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFE2E8F0).copy(alpha = 0.85f)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .testTag("global_language_switcher"),
        color = containerBg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // English Option
            val isEnSelected = currentLang == AppLanguage.ENGLISH
            val enBg by animateColorAsState(
                targetValue = if (isEnSelected) activeBgColor else Color.Transparent,
                animationSpec = tween(200),
                label = "en_bg"
            )
            val enText by animateColorAsState(
                targetValue = if (isEnSelected) activeTextColor else inactiveTextColor,
                animationSpec = tween(200),
                label = "en_text"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(enBg)
                    .clickable {
                        languageManager?.setLanguage(AppLanguage.ENGLISH)
                        onLanguageChanged?.invoke(AppLanguage.ENGLISH)
                    }
                    .padding(
                        horizontal = if (compact) 8.dp else 10.dp,
                        vertical = if (compact) 3.dp else 4.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EN",
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = if (isEnSelected) FontWeight.Bold else FontWeight.Medium,
                    color = enText
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Hindi Option
            val isHiSelected = currentLang == AppLanguage.HINDI
            val hiBg by animateColorAsState(
                targetValue = if (isHiSelected) activeBgColor else Color.Transparent,
                animationSpec = tween(200),
                label = "hi_bg"
            )
            val hiText by animateColorAsState(
                targetValue = if (isHiSelected) activeTextColor else inactiveTextColor,
                animationSpec = tween(200),
                label = "hi_text"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(hiBg)
                    .clickable {
                        languageManager?.setLanguage(AppLanguage.HINDI)
                        onLanguageChanged?.invoke(AppLanguage.HINDI)
                    }
                    .padding(
                        horizontal = if (compact) 8.dp else 10.dp,
                        vertical = if (compact) 3.dp else 4.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "हिन्दी",
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = if (isHiSelected) FontWeight.Bold else FontWeight.Medium,
                    color = hiText
                )
            }
        }
    }
}

/**
 * Dialog for selecting app language with full descriptive details.
 */
@Composable
fun LanguageSelectionModal(
    onDismiss: () -> Unit,
    isDark: Boolean = true
) {
    val languageManager = LocalLanguageManager.current
    val currentLang = LocalAppLanguage.current

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val cyanAccent = Color(0xFF00E5FF)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("language_selection_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(cyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = null,
                                tint = cyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = appString("settings_language_section"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                            Text(
                                text = "Select App Display Language",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryText
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = secondaryText)
                    }
                }

                HorizontalDivider(color = cardBorder)

                // Language Choices
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = currentLang == lang
                        Surface(
                            onClick = {
                                languageManager?.setLanguage(lang)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) cyanAccent.copy(alpha = 0.12f) else if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) cyanAccent else cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("language_option_${lang.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) cyanAccent else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang.shortCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFF0A0F1D) else primaryText
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = lang.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                        Text(
                                            text = lang.nativeName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) cyanAccent else secondaryText
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = cyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "App applies changes immediately without restarting. Content translation is cached locally.",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}
