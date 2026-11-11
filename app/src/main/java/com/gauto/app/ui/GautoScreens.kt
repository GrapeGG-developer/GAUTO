package com.gauto.app.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gauto.app.core.FocusLogic
import com.gauto.app.core.FocusSession
import com.gauto.app.core.GautoRepo
import com.gauto.app.core.GautoRule
import com.gauto.app.core.GautoScheduler
import com.gauto.app.core.GautoSyncService
import com.gauto.app.core.SyncProtocol
import com.gauto.app.core.GautoStore
import com.gauto.app.core.Habit
import com.gauto.app.core.HabitsLogic
import com.gauto.app.core.LifePack
import com.gauto.app.core.LifePacks
import com.gauto.app.core.RuleLogic
import com.gauto.app.core.RuleSort
import com.gauto.app.core.UpdateChecker
import com.gauto.app.core.UpdateLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun HomeScreen(
    store: GautoStore,
    revision: Int,
    onChanged: () -> Unit,
    onTab: (GautoTab) -> Unit
) {
    val rules = remember(revision) { store.rules() }
    val habits = remember(revision) { store.habits() }
    val settings = remember(revision) { store.settings() }
    val active = remember(revision) { store.activeRule() }
    val next = remember(revision) {
        rules.mapNotNull { rule -> RuleLogic.nextOccurrence(rule)?.let { it to rule } }
            .minByOrNull { it.first.toInstant().toEpochMilli() }?.second
    }
    val level = HabitsLogic.level(settings.xp)
    val streak = habits.maxOfOrNull { HabitsLogic.streak(it.doneDates) } ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GautoCard {
                Text("Gauto", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("личная жизнь → навык разработки", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("уровень", level.toString())
                    StatPill("стрик", "$streak дн.")
                    StatPill("xp", settings.xp.toString())
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { HabitsLogic.progress(settings.xp) }, modifier = Modifier.fillMaxWidth())
                Text(HabitsLogic.title(settings.xp), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
        item {
            GautoCard {
                SectionTitle("сейчас действует")
                if (active == null) {
                    Text("Правил пока нет — установите готовый набор или создайте своё.", fontSize = 12.sp)
                } else {
                    Text("${active.timeLabel} · ${active.name}", style = MaterialTheme.typography.titleMedium)
                    Text(active.daysLabel(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (active.useBrightness) Text("яркость ${active.brightness}%", fontSize = 11.sp)
                    if (active.useVolume) Text("громкость ${active.volume}%", fontSize = 11.sp)
                    if (active.grayscale) Text("экран монохромный", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    if (active.blockPackages.isNotEmpty()) {
                        Text("заблокировано приложений: ${active.blockPackages.size}", fontSize = 11.sp)
                    }
                }
                next?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("дальше: ${it.timeLabel} · ${it.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            GautoCard {
                SectionTitle("быстрый старт")
                Text("Один тап — и правила уже работают.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { store.installPack("chores"); onChanged() }, modifier = Modifier.weight(1f)) {
                        Text("домашние дела", fontSize = 10.sp)
                    }
                    Button(onClick = { store.installPack("sleep"); onChanged() }, modifier = Modifier.weight(1f)) {
                        Text("сон", fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onTab(GautoTab.FOCUS) }, modifier = Modifier.weight(1f)) {
                        Text("фокус", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { onTab(GautoTab.RULES) }, modifier = Modifier.weight(1f)) {
                        Text("правила", fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            GautoCard {
                SectionTitle("не стопайся")
                Text(
                    "Блокировки, ч/б экран и фокус-сессии снимают мелкие отвлечения. " +
                        "Каждое выполненное дело даёт xp и повышает уровень разработчика.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: GautoRule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    GautoCard(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${rule.timeLabel} · ${rule.name}", style = MaterialTheme.typography.titleSmall)
                Text(rule.daysLabel(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val tags = buildList {
                    if (rule.useBrightness) add("яркость ${rule.brightness}%")
                    if (rule.useVolume) add("громкость ${rule.volume}%")
                    if (rule.grayscale) add("ч/б")
                    if (rule.blockPackages.isNotEmpty()) add("блок ${rule.blockPackages.size}")
                }
                Text(tags.joinToString(" · "), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "удалить") }
        }
    }
}

@Composable
private fun PackCard(pack: LifePack, installed: Boolean, onInstall: () -> Unit) {
    GautoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pack.title, style = MaterialTheme.typography.titleSmall)
                Text(pack.hint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${pack.specs.size} правила", fontSize = 10.sp)
            }
            Button(onClick = onInstall) { Text(if (installed) "обновить" else "установить", fontSize = 10.sp) }
        }
    }
}

@Composable
fun RulesScreen(store: GautoStore, revision: Int, onChanged: () -> Unit, query: String) {
    val context = LocalContext.current
    var editorOpen by remember { mutableStateOf(false) }
    var editorRule by remember { mutableStateOf<GautoRule?>(null) }
    val allRules = remember(revision) { store.rules() }
    val rules = remember(revision, query) {
        RuleSort.apply(allRules.filter { RuleLogic.matches(query, it) }, store.settings().ruleSort)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionTitle("правила")
                Text("${allRules.size} всего · ${allRules.count { it.enabled }} включено", fontSize = 11.sp)
            }
            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onClick = { editorRule = rule; editorOpen = true },
                    onToggle = { store.setRuleEnabled(rule.id, it); GautoScheduler.scheduleAll(context, store.rules()); onChanged() },
                    onDelete = { store.deleteRule(rule.id); GautoScheduler.cancel(context, rule.id); onChanged() }
                )
            }
            item {
                Spacer(Modifier.height(6.dp))
                SectionTitle("готовые наборы")
                Text("${LifePacks.ALL.size} сценариев · ${LifePacks.ALL.sumOf { it.specs.size }} правил", fontSize = 11.sp)
                Text("школа, домашние дела, сон, глубокий код, детокс и день релиза", fontSize = 10.sp)
            }
            items(LifePacks.ALL, key = { it.id }) { pack ->
                PackCard(pack, LifePacks.isInstalled(allRules, pack)) {
                    store.installPack(pack.id)
                    GautoScheduler.scheduleAll(context, store.rules())
                    onChanged()
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        FloatingActionButton(
            onClick = { editorRule = null; editorOpen = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 196.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "новое правило") }
    }

    if (editorOpen) {
        RuleEditorDialog(
            initial = editorRule,
            onDismiss = { editorOpen = false },
            onSave = { rule ->
                store.upsertRule(rule)
                GautoScheduler.scheduleAll(context, store.rules())
                store.event("Сохранено правило «${rule.name}» · ${rule.timeLabel}")
                onChanged()
                editorOpen = false
            }
        )
    }
}

@Composable
private fun HabitCard(habit: Habit, onDone: () -> Unit) {
    val streak = HabitsLogic.streak(habit.doneDates)
    val week = HabitsLogic.weekProgress(habit)
    GautoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.titleSmall)
                Text(habit.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("стрик $streak · неделя $week/${habit.targetPerWeek} · +${habit.xp} xp", fontSize = 10.sp)
            }
            Button(onClick = onDone) { Text("отметить", fontSize = 10.sp) }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { week.toFloat() / habit.targetPerWeek.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun HabitsScreen(store: GautoStore, revision: Int, onChanged: () -> Unit) {
    val habits = remember(revision) { store.habits() }
    val settings = remember(revision) { store.settings() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GautoCard {
                SectionTitle("уровень разработчика")
                Text("${HabitsLogic.level(settings.xp)} · ${HabitsLogic.title(settings.xp)}", style = MaterialTheme.typography.titleMedium)
                Text("${settings.xp} xp · стрик ${settings.streak}", fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { HabitsLogic.progress(settings.xp) }, modifier = Modifier.fillMaxWidth())
            }
        }
        items(habits, key = { it.id }) { habit ->
            HabitCard(habit) { store.markHabitDone(habit.id); onChanged() }
        }
    }
}

@Composable
fun FocusScreen(store: GautoStore, revision: Int, onChanged: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }
    val sessions = remember(revision, now) { store.sessions() }
    val active = sessions.lastOrNull { FocusLogic.isActive(it, now) }
    val remaining = active?.let { FocusLogic.remainingSeconds(it, now) } ?: 0L
    val settings = remember(revision) { store.settings() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GautoCard {
                SectionTitle("фокус-сессия")
                if (active == null) {
                    Text("Нет активной сессии. Во время сессии YouTube и соцсети закрываются.", fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            store.addSession(FocusSession(startedAt = System.currentTimeMillis(), minutes = settings.focusMinutes, label = "глубокий труд"))
                            onChanged()
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.PlayArrow, contentDescription = null); Text("25 мин", fontSize = 10.sp) }
                        Button(onClick = {
                            store.addSession(FocusSession(startedAt = System.currentTimeMillis(), minutes = 50, label = "глубокий труд 50"))
                            onChanged()
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.PlayArrow, contentDescription = null); Text("50 мин", fontSize = 10.sp) }
                    }
                } else {
                    Text(FocusLogic.label(remaining), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(active.label, fontSize = 12.sp)
                    Text("заблокировано: ${active.blockedPackages.size} приложений", fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { store.completeSession(active.id); onChanged() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Text("завершить и получить xp", fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            GautoCard {
                SectionTitle("зачем это")
                Text(
                    "Один таймер = один блок без переключений. Пока идёт сессия, " +
                        "сервис доступности закрывает отвлекающие приложения, а завершение " +
                        "даёт очки к уровню разработчика.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            GautoCard {
                SectionTitle("последние сессии")
                val done = sessions.takeLast(6).reversed()
                if (done.isEmpty()) Text("Сессий пока нет", fontSize = 11.sp)
                else done.forEach {
                    Text("• ${it.label} · ${it.minutes} мин · ${if (it.completed) "завершена" else "в процессе"}", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun StatsScreen(store: GautoStore, revision: Int) {
    val settings = remember(revision) { store.settings() }
    val habits = remember(revision) { store.habits() }
    val sessions = remember(revision) { store.sessions() }
    val events = remember(revision) { store.events() }
    val doneTotal = habits.sumOf { it.doneDates.size }
    val blocked = events.count { it.startsWith("Заблокирован") }
    val focusDone = sessions.count { it.completed }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GautoCard {
                SectionTitle("прогресс")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("уровень", HabitsLogic.level(settings.xp).toString())
                    StatPill("xp", settings.xp.toString())
                    StatPill("стрик", settings.streak.toString())
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { HabitsLogic.progress(settings.xp) }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            GautoCard {
                SectionTitle("что уже сделано")
                Text("привычек отмечено: $doneTotal", fontSize = 12.sp)
                Text("фокус-сессий завершено: $focusDone", fontSize = 12.sp)
                Text("блокировок отвлечений: $blocked", fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            GautoCard {
                SectionTitle("последние события")
                val last = events.takeLast(10).reversed()
                if (last.isEmpty()) Text("Событий пока нет", fontSize = 11.sp)
                else last.forEach { Text("• $it", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    store: GautoStore,
    revision: Int,
    onChanged: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember(revision) { store.settings() }
    var updateText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GautoCard {
                SectionTitle("доступы")
                Text("Панель не закроется, пока не выданы все обязательные разрешения.", fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenPermissions, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Security, contentDescription = null)
                    Text("открыть панель доступов", fontSize = 11.sp)
                }
            }
        }
        item {
            GautoCard {
                SectionTitle("синхронизация с ПК")
                Text("Gauto Desktop находит телефон сам по локальной сети. Без облака, аккаунта и токена.", fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text("адрес: ${GautoSyncService.localIpAddress().ifBlank { "нет Wi-Fi" }}:${SyncProtocol.PORT}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                Text("UDP-поиск: порт ${SyncProtocol.DISCOVERY_PORT} · PIN: ${settings.syncPin.ifBlank { "не задан" }}", fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("доступ с ПК", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(
                        checked = settings.syncEnabled,
                        onCheckedChange = { enabled ->
                            store.ensurePin()
                            store.saveSettings(store.settings().copy(syncEnabled = enabled))
                            if (enabled) GautoSyncService.start(context) else GautoSyncService.stop(context)
                            onChanged()
                        }
                    )
                }
            }
        }
        item {
            GautoCard {
                SectionTitle("внутреннее автообновление")
                Text("источник: ${GautoRepo.REPO}", fontSize = 11.sp)
                Text("версия: ${GautoRepo.VERSION} (vc ${GautoRepo.VERSION_CODE})", fontSize = 11.sp)
                Text("публичный API + ETag, без токена в APK; сторож 200 мс", fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("проверять автоматически", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(
                        checked = settings.updateAuto,
                        onCheckedChange = { store.saveSettings(settings.copy(updateAuto = it)); onChanged() }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        Handler(Looper.getMainLooper()).post { updateText = "проверяю…" }
                        Thread {
                            val update = runCatching { UpdateChecker.check(context) }.getOrNull()
                            val text = when {
                                update == null -> "обновлений нет"
                                UpdateLogic.isNewer(update.tag) -> {
                                    runCatching { UpdateChecker.downloadAndInstall(context, update) }
                                    "найден ${update.tag} — запрашиваю установку"
                                }
                                else -> "уже актуально: ${update.tag}"
                            }
                            Handler(Looper.getMainLooper()).post { updateText = text }
                        }.apply { name = "gauto-manual-update" }.start()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("проверить обновление", fontSize = 11.sp)
                }
                if (updateText.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(updateText, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        item {
            GautoCard {
                SectionTitle("поведение")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("автопилот правил", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(
                        checked = settings.autopilot,
                        onCheckedChange = { store.saveSettings(settings.copy(autopilot = it)); onChanged() }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("вибрация при блокировке", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(
                        checked = settings.haptics,
                        onCheckedChange = { store.saveSettings(settings.copy(haptics = it)); onChanged() }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("после перезагрузки применять последнее правило", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(
                        checked = settings.syncAfterBoot,
                        onCheckedChange = { store.saveSettings(settings.copy(syncAfterBoot = it)); onChanged() }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("4D-сцена (глубина + время)", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(
                        checked = settings.fourDEnabled,
                        onCheckedChange = { store.saveSettings(settings.copy(fourDEnabled = it)); onChanged() }
                    )
                }
                Text("Блокировки: приложения · сон · фокус · таймер · до подъёма", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("живость островка", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("спокойно" to 0, "обычно" to 1, "максимум" to 2).forEach { (label, level) ->
                        GautoChip(label, settings.motionLevel == level) {
                            store.saveSettings(store.settings().copy(motionLevel = level))
                            onChanged()
                        }
                    }
                }
            }
        }
        item {
            GautoCard {
                SectionTitle("о приложении")
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Gauto — полностью независимое приложение. Все правила, привычки и " +
                        "статистика хранятся только на телефоне; интернет нужен лишь для " +
                        "проверки обновлений. ${GautoRepo.UI_VERSION}.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
