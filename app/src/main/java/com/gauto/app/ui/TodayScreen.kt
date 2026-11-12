package com.gauto.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gauto.app.core.GautoStore
import com.gauto.app.core.QuickActions
import com.gauto.app.core.WakeEffects
import com.gauto.app.core.AlarmRinger

/** Один понятный экран: всё важное и четыре действия. Без лишних слов. */
@Composable
fun TodayScreen(store: GautoStore, revision: Int, onChanged: () -> Unit, onOpenRules: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(revision) { store.settings() }
    val alarmLabel = remember(revision) { QuickActions.nextAlarmLabel(store) }
    val status = remember(revision) { QuickActions.shortStatus(store) }
    val habit = remember(revision) { QuickActions.todayHabit(store) }
    val timer = remember(revision) { store.timers().firstOrNull { it.running } }
    val lastEvent = remember(revision) { store.events().lastOrNull().orEmpty() }
    val alarmActive = settings.activeAlarmId.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 190.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Сегодня", style = MaterialTheme.typography.headlineSmall)
            Text(status, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            GautoCard {
                Text("Ближайший подъём", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(3.dp))
                Text(alarmLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Будильник без откладывания", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (alarmActive) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (settings.activeAlarmId == "quick-sleep") QuickActions.wake(context, store) else {
                            AlarmRinger.stop()
                            WakeEffects.confirm(context, null)
                        }
                        onChanged()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (settings.activeAlarmId == "quick-sleep") "Выключить режим сна" else "Я встал", fontSize = 14.sp)
                    }
                }
            }
        }
        item {
            Text("Быстрые действия", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { QuickActions.startFocus(context, store); onChanged() }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Фокус 25 мин", fontSize = 12.sp)
                    }
                    Button(onClick = { QuickActions.quiet(context, store); onChanged() }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Тишина 30 мин", fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { QuickActions.sleep(context, store); onChanged() }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Сон", fontSize = 12.sp)
                    }
                    Button(onClick = onOpenRules, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Дела", fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            GautoCard {
                val rule = remember(revision) { store.activeRule() }
                            Text("Сейчас", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(rule?.let { "${it.timeLabel} · ${it.name}" } ?: "Нет активного правила", style = MaterialTheme.typography.titleMedium)
                habit?.let { h ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("На сегодня: ${h.title}", style = MaterialTheme.typography.bodyMedium)
                            Text("стрик ${com.gauto.app.core.HabitsLogic.streak(h.doneDates)} · +${h.xp} xp", fontSize = 11.sp)
                        }
                        Button(onClick = { store.markHabitDone(h.id); onChanged() }) { Text("Готово", fontSize = 12.sp) }
                    }
                }
            }
        }
        if (timer != null) {
            item {
                GautoCard {
                    Text("Таймер идёт", style = MaterialTheme.typography.titleSmall)
                    Text(timer.label, fontSize = 12.sp)
                }
            }
        }
        if (lastEvent.isNotBlank()) {
            item { Text("Последнее: $lastEvent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
