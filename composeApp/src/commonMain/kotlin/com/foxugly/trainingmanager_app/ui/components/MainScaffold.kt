package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.i18n.Strings

/** The five top-level destinations shown in the bottom navigation bar. */
enum class MainTab { DASHBOARD, EVENTS, TEAMS, NOTIFICATIONS, PROFILE }

/**
 * Chrome shared by every top-level screen: a [TopAppBar] with the screen title
 * (+ optional [actions]) and a Material 3 [NavigationBar] to switch between the
 * five main destinations. Screens stay navigation-agnostic — they declare which
 * tab they are and delegate switching to [onSelectTab].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    title: String,
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val s = LocalStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = actions,
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == currentTab,
                        onClick = { if (tab != currentTab) onSelectTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label(s)) },
                        label = { Text(tab.label(s), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        // Only the selected destination shows its (sometimes long,
                        // French) label; the rest stay icon-only so 5 items fit.
                        alwaysShowLabel = false,
                    )
                }
            }
        },
        content = content,
    )
}

private val MainTab.icon: ImageVector
    get() = when (this) {
        MainTab.DASHBOARD -> Icons.Filled.Home
        MainTab.EVENTS -> Icons.Filled.CalendarMonth
        MainTab.TEAMS -> Icons.Filled.Groups
        MainTab.NOTIFICATIONS -> Icons.Filled.Notifications
        MainTab.PROFILE -> Icons.Filled.Person
    }

private fun MainTab.label(s: Strings): String = when (this) {
    MainTab.DASHBOARD -> s.dashboardTitle
    MainTab.EVENTS -> s.eventsEntry
    MainTab.TEAMS -> s.teamsEntry
    MainTab.NOTIFICATIONS -> s.notificationsEntry
    MainTab.PROFILE -> s.profileTitle
}
