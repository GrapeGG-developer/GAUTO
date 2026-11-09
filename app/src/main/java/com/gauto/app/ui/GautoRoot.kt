package com.gauto.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gauto.app.core.GautoPermissions
import com.gauto.app.core.GautoStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class GautoTab(val title: String, val icon: ImageVector) {
    HOME("Главная", Icons.Filled.Home),
    RULES("Правила", Icons.Filled.List),
    HABITS("Привычки", Icons.Filled.CheckCircle),
    FOCUS("Фокус", Icons.Filled.Timer),
    SETTINGS("Настройки", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GautoTopBar(title: String, onSearch: () -> Unit, onNotifications: () -> Unit) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Person, contentDescription = "профиль")
            }
        },
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "поиск")
            }
            IconButton(onClick = onNotifications) {
                Icon(Icons.Filled.Notifications, contentDescription = "уведомления")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun GautoBottomBar(current: GautoTab, onSelect: (GautoTab) -> Unit) {
    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        GautoTab.entries.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.title,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                label = { Text(tab.title, fontSize = 11.sp, maxLines = 1) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun GautoRoot() {
    val context = LocalContext.current
    val store = remember { GautoStore(context) }
    var tab by remember { mutableStateOf(GautoTab.HOME) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var gate by remember { mutableStateOf(!GautoPermissions.allRequiredGranted(context)) }

    // Полноэкранная панель доступов не закрывается, пока не выданы все обязательные доступы.
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(800L)
            gate = !GautoPermissions.allRequiredGranted(context)
        }
    }

    GautoTheme {
        if (gate) {
            PermissionGate(onReady = { gate = false })
            return@GautoTheme
        }

        Scaffold(
            topBar = {
                GautoTopBar(
                    title = tab.title,
                    onSearch = { showSearch = true },
                    onNotifications = { showNotifications = true }
                )
            },
            bottomBar = { GautoBottomBar(tab) { tab = it } }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                when (tab) {
                    GautoTab.HOME -> HomeScreen(store, revision, { revision++ }) { tab = it }
                    GautoTab.RULES -> RulesScreen(store, revision, { revision++ }, query)
                    GautoTab.HABITS -> HabitsScreen(store, revision, { revision++ })
                    GautoTab.FOCUS -> FocusScreen(store, revision, { revision++ })
                    GautoTab.SETTINGS -> SettingsScreen(store, revision, { revision++ }) { gate = true }
                }
            }
        }

        if (showSearch) {
            SearchDialog(
                query = query,
                onQueryChange = { query = it },
                store = store,
                onDismiss = { showSearch = false }
            )
        }
        if (showNotifications) {
            NotificationsDialog(store = store, onDismiss = { showNotifications = false })
        }
    }
}
