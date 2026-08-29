package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.ui.components.AppNavTab

class AppBackStackManager(
    initialDestination: AppDestination = AppDestination.MainTab(AppNavTab.HOME)
) {
    val stack = mutableStateListOf<AppDestination>(initialDestination)

    val currentDestination: AppDestination
        get() = stack.lastOrNull() ?: AppDestination.MainTab(AppNavTab.HOME)

    val currentTab: AppNavTab
        get() {
            // Find the most recent tab in stack or active destination
            return when (val current = currentDestination) {
                is AppDestination.MainTab -> current.tab
                else -> {
                    val lastTab = stack.filterIsInstance<AppDestination.MainTab>().lastOrNull()
                    lastTab?.tab ?: AppNavTab.HOME
                }
            }
        }

    fun navigateTo(destination: AppDestination, clearTop: Boolean = false) {
        if (clearTop) {
            stack.clear()
            stack.add(destination)
            return
        }

        // Avoid adding duplicate identical consecutive destinations
        if (stack.isNotEmpty() && stack.last() == destination) {
            return
        }

        // If navigating to a MainTab from another MainTab
        if (destination is AppDestination.MainTab) {
            // If already at root tab and switching tab, we push it so Back returns to previous tab
            stack.add(destination)
        } else {
            stack.add(destination)
        }
    }

    fun selectTab(tab: AppNavTab) {
        val dest = AppDestination.MainTab(tab)
        // If current top is already this tab, do nothing
        if (currentDestination == dest) return
        
        // Push the tab onto the navigation stack
        stack.add(dest)
    }

    /**
     * Pops the top screen from the navigation stack.
     * Returns true if back was handled internally, false if already at root.
     */
    fun popBackStack(): Boolean {
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1)
            return true
        } else if (stack.size == 1) {
            val root = stack.first()
            if (root is AppDestination.MainTab && root.tab == AppNavTab.HOME) {
                // At absolute root (Home tab)
                return false
            } else {
                // Not on Home tab, redirect to Home tab
                stack.clear()
                stack.add(AppDestination.MainTab(AppNavTab.HOME))
                return true
            }
        }
        return false
    }

    fun popToRoot() {
        stack.clear()
        stack.add(AppDestination.MainTab(AppNavTab.HOME))
    }
}

val LocalBackStackManager = staticCompositionLocalOf<AppBackStackManager?> { null }

@Composable
fun rememberAppBackStackManager(
    initialDestination: AppDestination = AppDestination.MainTab(AppNavTab.HOME)
): AppBackStackManager {
    return remember { AppBackStackManager(initialDestination) }
}

