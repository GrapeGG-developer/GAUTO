package com.gauto.app.ui

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gauto.app.core.AlarmLogic
import com.gauto.app.core.AlarmRules
import com.gauto.app.core.AlarmScheduler
import com.gauto.app.core.GautoAlarm
import com.gauto.app.core.GautoReminder
import com.gauto.app.core.GautoStore
import com.gauto.app.core.GautoTimer
import com.gauto.app.core.RuleLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ——————————————————————————— Будильник ———————————————————————————

@Composable
fun AlarmScreen(store: GautoStore, revision: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val alarms = remember(revision) { store.alarms() }
    var editor by remember { mutableStateOf<GautoAlarm?>(null) }
    var editorOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (alarms.isEmpty()) {
                item {
                    GautoCard {
                        Text("Будильников пока нет", style = MaterialTheme.typography.titleLarge)
                        Text("Создай первый через кнопку +. На сигнале не будет «Отложить».", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                GautoCard {
                    SectionTitle("настоящий будильник")
                    Text(
                        "Настройки круче системного: мелодия, нарастание громкости, рассвет, " +
                            "вибрация, яркость пробуждения и снятие блокировок.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Главное: на оверлее нет откладывания. Только «Я встал».",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        context.startActivity(
                            Intent(context, AlarmOverlayActivity::class.java)
                                .putExtra(AlarmOverlayActivity.EXTRA_MODE, AlarmOverlayActivity.MODE_ALARM)
                                .putExtra(AlarmOverlayActivity.EXTRA_LABEL, "Подъём")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("посмотреть оверлей", fontSize = 11.sp)
                    }
                }
            }
            items(alarms, key = { it.id }) { alarm ->
                GautoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(alarm.timeLabel, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Text(alarm.label, style = MaterialTheme.typography.titleSmall)
                            Text(alarm.daysLabel(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${AlarmRules.soundLabel(alarm.sound)} · ${alarm.volume}% · рассвет ${alarm.sunriseMinutes} мин · ${alarm.rampSec} с",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(checked = alarm.enabled, onCheckedChange = {
                            store.saveAlarms(store.alarms().map { a -> if (a.id == alarm.id) a.copy(enabled = it) else a })
                            AlarmScheduler.scheduleAlarm(context, alarm.copy(enabled = it))
                            onChanged()
                        })
                        IconButton(onClick = { store.deleteAlarm(alarm.id); AlarmScheduler.cancel(context, AlarmScheduler.ACTION_ALARM_FIRE, alarm.id); onChanged() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "удалить")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { editor = alarm; editorOpen = true }) { Text("настроить", fontSize = 11.sp) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        FloatingActionButton(
            onClick = { editor = null; editorOpen = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 196.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "новый будильник") }
    }

    if (editorOpen) {
        AlarmEditorDialog(editor, { editorOpen = false }) { alarm ->
            store.upsertAlarm(alarm)
            AlarmScheduler.scheduleAlarm(context, alarm)
            store.event("Будильник сохранён: ${alarm.label} · ${alarm.timeLabel}")
            onChanged()
            editorOpen = false
        }
    }
}

@Composable
private fun AlarmEditorDialog(initial: GautoAlarm?, onDismiss: () -> Unit, onSave: (GautoAlarm) -> Unit) {
    val source = initial ?: GautoAlarm()
    var label by remember(source.id) { mutableStateOf(source.label) }
    var hour by remember(source.id) { mutableIntStateOf(source.hour) }
    var minute by remember(source.id) { mutableIntStateOf(source.minute) }
    var days by remember(source.id) { mutableStateOf(source.days.toSet()) }
    var sound by remember(source.id) { mutableStateOf(source.sound) }
    var volume by remember(source.id) { mutableIntStateOf(source.volume) }
    var ramp by remember(source.id) { mutableIntStateOf(source.rampSec) }
    var sunrise by remember(source.id) { mutableIntStateOf(source.sunriseMinutes) }
    var autoStop by remember(source.id) { mutableIntStateOf(source.autoStopMinutes) }
    var vibration by remember(source.id) { mutableStateOf(source.vibration) }
    var brightness by remember(source.id) { mutableIntStateOf(source.wakeBrightness) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "новый будильник" else "будильник") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hour.toString(), onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour }, label = { Text("час") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minute.toString(), onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute }, label = { Text("минуты") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Text("дни", fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RuleLogic.DAY_SHORT.forEachIndexed { i, d ->
                        val day = i + 1
                        OutlinedButton(onClick = { days = if (day in days) days - day else days + day }, modifier = Modifier.weight(1f)) {
                            Text(if (day in days) "●$d" else d, fontSize = 8.sp, maxLines = 1)
                        }
                    }
                }
                Text("мелодия", fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlarmRules.SOUND_LABELS.forEachIndexed { i, l ->
                        GautoChip(l, sound == AlarmRules.SOUNDS[i]) { sound = AlarmRules.SOUNDS[i] }
                    }
                }
                Text("громкость $volume% · нарастание $ramp с", fontSize = 11.sp)
                Slider(value = volume.toFloat(), onValueChange = { volume = it.toInt() }, valueRange = 1f..100f)
                Slider(value = ramp.toFloat(), onValueChange = { ramp = it.toInt().coerceIn(5, 120) }, valueRange = 5f..120f)
                Text("рассвет за $sunrise мин · яркость $brightness%", fontSize = 11.sp)
                Slider(value = sunrise.toFloat(), onValueChange = { sunrise = it.toInt() }, valueRange = 0f..30f)
                Slider(value = brightness.toFloat(), onValueChange = { brightness = it.toInt() }, valueRange = 1f..100f)
                Text("авто-стоп через $autoStop мин (не перенос)", fontSize = 11.sp)
                Slider(value = autoStop.toFloat(), onValueChange = { autoStop = it.toInt().coerceIn(1, 60) }, valueRange = 1f..60f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("вибрация", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(checked = vibration, onCheckedChange = { vibration = it })
                }
                Text("Строгий режим: подтверждение «Я встал», без откладывания.", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(AlarmLogic.validateAlarm(source.copy(
                    label = label.ifBlank { "Подъём" }, hour = hour, minute = minute,
                    days = days.sorted(), sound = sound, volume = volume, rampSec = ramp,
                    sunriseMinutes = sunrise, autoStopMinutes = autoStop, vibration = vibration,
                    wakeBrightness = brightness, strict = true
                )))
            }) { Text("сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("отмена") } }
    )
}

// ——————————————————————————— Таймеры ———————————————————————————

@Composable
fun TimersScreen(store: GautoStore, revision: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (isActive) { delay(500L); now = System.currentTimeMillis() } }
    val timers = remember(revision, now) { store.timers() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GautoCard {
                SectionTitle("таймеры")
                Text("Обратный отсчёт, звук окончания и блокировка на время. Без откладывания.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(5 to "5 мин", 10 to "10 мин", 25 to "25 мин", 50 to "50 мин").forEach { (min, title) ->
                        Button(onClick = {
                            val timer = GautoTimer(label = title, durationSec = min * 60, running = true, endsAt = System.currentTimeMillis() + min * 60_000L)
                            store.upsertTimer(timer)
                            AlarmScheduler.scheduleTimer(context, timer)
                            onChanged()
                        }, modifier = Modifier.weight(1f)) { Text(title, fontSize = 9.sp, maxLines = 1) }
                    }
                }
            }
        }
        items(timers, key = { it.id }) { timer ->
            GautoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(timer.label, style = MaterialTheme.typography.titleSmall)
                        Text(AlarmLogic.formatTimer(timer.remaining(now)), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text(if (timer.running) "идёт · ${timer.blockMode.title}" else "остановлен", fontSize = 10.sp)
                    }
                    Button(onClick = {
                        val next = timer.copy(running = false, endsAt = 0L)
                        store.upsertTimer(next)
                        AlarmScheduler.cancel(context, AlarmScheduler.ACTION_TIMER_FIRE, timer.id)
                        onChanged()
                    }) { Text("стоп", fontSize = 10.sp) }
                    IconButton(onClick = { store.deleteTimer(timer.id); onChanged() }) { Icon(Icons.Filled.Delete, contentDescription = "удалить") }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ——————————————————————————— Напоминания ———————————————————————————

@Composable
fun RemindersScreen(store: GautoStore, revision: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val reminders = remember(revision) { store.reminders() }
    var editor by remember { mutableStateOf<GautoReminder?>(null) }
    var editorOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 212.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                GautoCard {
                    SectionTitle("напоминания")
                    Text("По времени и дням, со звуком и вибрацией, в едином оверлее Gauto.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(reminders, key = { it.id }) { reminder ->
                GautoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${reminder.timeLabel} · ${reminder.text}", style = MaterialTheme.typography.titleSmall)
                            Text(if (reminder.once) "однократное" else "повторяется", fontSize = 10.sp)
                        }
                        Switch(checked = reminder.enabled, onCheckedChange = {
                            store.saveReminders(store.reminders().map { r -> if (r.id == reminder.id) r.copy(enabled = it) else r })
                            AlarmScheduler.scheduleReminder(context, reminder.copy(enabled = it))
                            onChanged()
                        })
                        IconButton(onClick = { store.deleteReminder(reminder.id); onChanged() }) { Icon(Icons.Filled.Delete, contentDescription = "удалить") }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        FloatingActionButton(
            onClick = { editor = null; editorOpen = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 196.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "новое напоминание") }
    }

    if (editorOpen) {
        ReminderEditorDialog(editor, { editorOpen = false }) { reminder ->
            store.upsertReminder(reminder)
            AlarmScheduler.scheduleReminder(context, reminder)
            onChanged()
            editorOpen = false
        }
    }
}

@Composable
private fun ReminderEditorDialog(initial: GautoReminder?, onDismiss: () -> Unit, onSave: (GautoReminder) -> Unit) {
    val source = initial ?: GautoReminder()
    var text by remember(source.id) { mutableStateOf(source.text) }
    var hour by remember(source.id) { mutableIntStateOf(source.hour) }
    var minute by remember(source.id) { mutableIntStateOf(source.minute) }
    var once by remember(source.id) { mutableStateOf(source.once) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("напоминание") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("текст") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hour.toString(), onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour }, label = { Text("час") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minute.toString(), onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute }, label = { Text("минуты") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("однократное", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(checked = once, onCheckedChange = { once = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(AlarmLogic.validateReminder(source.copy(text = text.ifBlank { "Напоминание" }, hour = hour, minute = minute, once = once))) }) { Text("сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("отмена") } }
    )
}
