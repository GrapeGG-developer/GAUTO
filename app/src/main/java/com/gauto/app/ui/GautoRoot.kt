package com.gauto.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gauto.app.R
import com.gauto.app.core.GautoPermissions
import com.gauto.app.core.GautoStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class GautoTab(val title: String, val accent: Color) {
    HOME("Главная", Color(0xFF8B5CF6)),
    RULES("Правила", Color(0xFF22D3EE)),
    HABITS("Привычки", Color(0xFF34D399)),
    FOCUS("Фокус", Color(0xFFFBBF24)),
    SETTINGS("Настройки", Color(0xFFF472B6))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GautoTopBar(
    title: String,
    unread: Int,
    onSearch: () -> Unit,
    onNotifications: () -> Unit
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(R.drawable.ic_gauto_logo),
                    contentDescription = "Gauto",
                    tint = Color.Unspecified
                )
            }
        },
        title = {
            Text("Gauto · $title", style = MaterialTheme.typography.titleMedium)
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "поиск")
            }
            IconButton(onClick = onNotifications) {
                Box {
                    Icon(Icons.Filled.Notifications, contentDescription = "уведомления")
                    if (unread > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun GautoRoot() {
    val context = LocalContext.current
    val store = remember { GautoStore(context) }
    var tab by remember { mutableStateOf(GautoTab.HOME) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var gate by remember { mutableStateOf(!GautoPermissions.allRequiredGranted(context)) }
    val activeLabel = remember(revision) {
        store.activeRule()?.let { "${it.timeLabel} · ${it.name}" } ?: "правил пока нет"
    }
    val unread = remember(revision) { store.events().size % 7 }

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

        Box(Modifier.fillMaxSize()) {
            GautoIslandBackground(accent = tab.accent, expanded = expanded)

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    GautoTopBar(
                        title = tab.title,
                        unread = unread,
                        onSearch = { showSearch = true },
                        onNotifications = { showNotifications = true }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(260)) +
                                scaleIn(spring(stiffness = Spring.StiffnessMedium), initialScale = 0.96f) +
                                slideInVertically { it / 18 }) togetherWith
                                (fadeOut(tween(180)) + scaleOut(targetScale = 1.02f))
                        },
                        label = "gautoScreen"
                    ) { target ->
                        when (target) {
                            GautoTab.HOME -> HomeScreen(store, revision, { revision++ }) { tab = it }
                            GautoTab.RULES -> RulesScreen(store, revision, { revision++ }, query)
                            GautoTab.HABITS -> HabitsScreen(store, revision, { revision++ })
                            GautoTab.FOCUS -> FocusScreen(store, revision, { revision++ })
                            GautoTab.SETTINGS -> SettingsScreen(store, revision, { revision++ }) { gate = true }
                        }
                    }
                    // Место для острова, чтобы контент не уходил под капсулу.
                    Box(Modifier.fillMaxSize())
                }
            }

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { expanded = false }
                )
            }

            GautoIsland(
                current = tab,
                activeLabel = activeLabel,
                expanded = expanded,
                onToggle = { expanded = !expanded },
                onSelect = { selected ->
                    tab = selected
                    expanded = false
                    revision++
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
