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
import androidx.compose.ui.unit.dp
import com.gauto.app.core.GautoPermissions
import com.gauto.app.core.GautoStore
import com.gauto.app.core.QuickActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class GautoTab(val title: String, val accent: Color) {
    HOME("Сегодня", Color(0xFF8B5CF6)),
    ALARM("Будильник", Color(0xFFF87171)),
    RULES("Правила", Color(0xFF22D3EE)),
    MORE("Ещё", Color(0xFF34D399))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GautoTopBar(
    title: String,
    activeIndex: Int,
    unread: Int,
    onSearch: () -> Unit,
    onNotifications: () -> Unit
) {
    CenterAlignedTopAppBar(
        navigationIcon = { IconButton(onClick = {}) { GautoAnimatedLogo(Modifier.size(32.dp), activeIndex) } },
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        actions = {
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "поиск") }
            IconButton(onClick = onNotifications) {
                Box {
                    Icon(Icons.Filled.Notifications, contentDescription = "события")
                    if (unread > 0) Box(
                        Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error).align(Alignment.TopEnd)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
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
    fun markChanged() {
        revision++
        runCatching { com.gauto.app.core.GautoWidget.refresh(context) }
    }
    var gate by remember { mutableStateOf(!GautoPermissions.allRequiredGranted(context)) }
    var onboarding by remember { mutableStateOf(!context.getSharedPreferences("gauto_store", 0).getBoolean("onboarding_done", false)) }
    val settings = remember(revision) { store.settings() }
    val motion = 0.35f
    val accents = LocalGautoAccents.current
    val statuses = remember(revision) {
        buildList {
            add(QuickActions.nextAlarmLabel(store))
            add(QuickActions.shortStatus(store))
            add("${settings.xp} очков · уровень ${com.gauto.app.core.HabitsLogic.level(settings.xp)}")
            store.events().lastOrNull()?.let { add(it.take(42)) }
        }
    }
    val unread = remember(revision) { store.events().size % 7 }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1_500L)
            gate = !GautoPermissions.allRequiredGranted(context)
        }
    }

    GautoTheme(motion = motion) {
        if (onboarding) {
            GautoOnboarding {
                context.getSharedPreferences("gauto_store", 0).edit().putBoolean("onboarding_done", true).apply()
                onboarding = false
            }
            return@GautoTheme
        }
        if (gate) {
            PermissionGate(onReady = { gate = false })
            return@GautoTheme
        }
        Box(Modifier.fillMaxSize()) {
            GautoIslandBackground(accents.getOrElse(tab.ordinal) { accents.first() }, expanded)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    GautoTopBar(tab.title, tab.ordinal, unread, { showSearch = true }, { showNotifications = true })
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(spring(stiffness = Spring.StiffnessMedium), initialScale = 0.985f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(targetScale = 1.01f))
                        },
                        label = "mainPage"
                    ) { target ->
                        when (target) {
                            GautoTab.HOME -> TodayScreen(store, revision, { markChanged() }) { tab = GautoTab.RULES }
                            GautoTab.ALARM -> AlarmScreen(store, revision, { markChanged() })
                            GautoTab.RULES -> RulesScreen(store, revision, { markChanged() }, query)
                            GautoTab.MORE -> MoreScreen(store, revision, { markChanged() }) { gate = true }
                        }
                    }
                }
            }

            if (expanded) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)).clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = false }
                )
            }
            GautoIsland(
                current = tab,
                status = statuses.firstOrNull() ?: "Gauto",
                expanded = expanded,
                onToggle = { expanded = !expanded },
                onSelect = { tab = it; expanded = false; revision++ },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (showSearch) SearchDialog(query, { query = it }, store) { showSearch = false }
        if (showNotifications) NotificationsDialog(store) { showNotifications = false }
    }
}
