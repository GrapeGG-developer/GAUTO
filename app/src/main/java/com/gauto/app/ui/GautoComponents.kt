package com.gauto.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gauto.app.core.GautoPermissions
import com.gauto.app.core.GautoRepo
import com.gauto.app.core.GautoRule
import com.gauto.app.core.RuleLogic
import com.gauto.app.core.RuleSort
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun GautoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun StatPill(label: String, value: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GautoChip(text: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(if (selected) "● $text" else text, fontSize = 11.sp)
    }
}

@Composable
fun PermissionGate(onReady: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(GautoPermissions.snapshot(context)) }
    val allGranted = items.filterNot { it.optional }.all { it.granted }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(800L)
            items = GautoPermissions.snapshot(context)
            if (items.filterNot { it.optional }.all { it.granted }) {
                onReady()
                break
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Gauto · доступы", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Приложение полностью локальное. Эти доступы нужны, чтобы правила реально работали: " +
                    "закрывать YouTube, делать экран монохромным и срабатывать вовремя. " +
                    "Панель закроется сама, когда всё будет включено.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { items.count { it.granted }.toFloat() / items.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth()
            )
            items.forEach { item ->
                GautoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(item.hint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.optional) Text("необязательно", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(if (item.granted) "вкл" else "выкл", fontSize = 11.sp, color = if (item.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { GautoPermissions.open(context, item.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (item.granted) "открыть настройки" else "включить")
                    }
                }
            }
            Button(onClick = onReady, enabled = allGranted, modifier = Modifier.fillMaxWidth()) {
                Text(if (allGranted) "продолжить" else "выдайте все обязательные доступы")
            }
        }
    }
}

@Composable
fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    store: com.gauto.app.core.GautoStore,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("поиск") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("правило, привычка, приложение") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                val rules = store.rules().filter { RuleLogic.matches(query, it) }
                val habits = store.habits().filter { it.title.contains(query, ignoreCase = true) }
                Text("правил: ${rules.size} · привычек: ${habits.size}", fontSize = 11.sp)
                rules.take(6).forEach {
                    Text("• ${it.timeLabel}  ${it.name}", fontSize = 12.sp)
                }
                habits.take(6).forEach {
                    Text("• ${it.title}", fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("готово") } }
    )
}

@Composable
fun NotificationsDialog(store: com.gauto.app.core.GautoStore, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("уведомления") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val events = store.events().takeLast(15).reversed()
                if (events.isEmpty()) {
                    Text("Событий пока нет", fontSize = 12.sp)
                } else {
                    events.forEach { Text("• $it", fontSize = 12.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("закрыть") } }
    )
}

private val KNOWN_APPS = listOf(
    "YouTube" to "com.google.android.youtube",
    "YouTube Music" to "com.google.android.apps.youtube.music",
    "TikTok" to "com.ss.android.ugc.trill",
    "Instagram" to "com.instagram.android",
    "Chrome" to "com.android.chrome"
)

@Composable
fun RuleEditorDialog(
    initial: GautoRule?,
    onDismiss: () -> Unit,
    onSave: (GautoRule) -> Unit
) {
    val source = initial ?: GautoRule()
    var name by remember(source.id) { mutableStateOf(source.name) }
    var hour by remember(source.id) { mutableIntStateOf(source.hour) }
    var minute by remember(source.id) { mutableIntStateOf(source.minute) }
    var days by remember(source.id) { mutableStateOf(source.days.toSet()) }
    var enabled by remember(source.id) { mutableStateOf(source.enabled) }
    var useBrightness by remember(source.id) { mutableStateOf(source.useBrightness) }
    var brightness by remember(source.id) { mutableIntStateOf(source.brightness) }
    var useVolume by remember(source.id) { mutableStateOf(source.useVolume) }
    var volume by remember(source.id) { mutableIntStateOf(source.volume) }
    var silentMode by remember(source.id) { mutableIntStateOf(source.silentMode) }
    var grayscale by remember(source.id) { mutableStateOf(source.grayscale) }
    var blocked by remember(source.id) { mutableStateOf(source.blockPackages.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "новое правило" else "правило") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                        label = { Text("час") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minute.toString(),
                        onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                        label = { Text("минуты") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("дни недели", fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    RuleLogic.DAY_SHORT.forEachIndexed { index, label ->
                        val day = index + 1
                        OutlinedButton(
                            onClick = {
                                days = if (day in days) days - day else days + day
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (day in days) "●$label" else label, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("включено", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("яркость", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(checked = useBrightness, onCheckedChange = { useBrightness = it })
                }
                if (useBrightness) {
                    Text("яркость $brightness%", fontSize = 11.sp)
                    Slider(
                        value = brightness.toFloat(),
                        onValueChange = { brightness = it.roundToInt() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("громкость", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(checked = useVolume, onCheckedChange = { useVolume = it })
                }
                if (useVolume) {
                    Text("громкость $volume%", fontSize = 11.sp)
                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { volume = it.roundToInt() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("Не беспокоить", fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GautoChip("не трогать", silentMode == -1) { silentMode = -1 }
                    GautoChip("выкл", silentMode == 0) { silentMode = 0 }
                    GautoChip("вкл", silentMode == 1) { silentMode = 1 }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ч/б экран (монохромный слой)", Modifier.weight(1f), fontSize = 12.sp)
                    Switch(checked = grayscale, onCheckedChange = { grayscale = it })
                }
                Text("блокировать приложения", fontSize = 11.sp)
                KNOWN_APPS.forEach { (label, pkg) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = pkg in blocked,
                            onCheckedChange = { checked ->
                                blocked = if (checked) blocked + pkg else blocked - pkg
                            }
                        )
                        Text(label, fontSize = 12.sp)
                    }
                }
                Text(
                    "Без ADB и root: сначала системный далтонизатор, затем оверлей. " +
                        "Приложение целиком локальное, репозиторий обновлений — ${GautoRepo.REPO}.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    RuleLogic.validate(
                        source.copy(
                            name = name.ifBlank { "Правило Gauto" },
                            hour = hour,
                            minute = minute,
                            days = days.sorted(),
                            enabled = enabled,
                            useBrightness = useBrightness,
                            brightness = brightness,
                            useVolume = useVolume,
                            volume = volume,
                            silentMode = silentMode,
                            grayscale = grayscale,
                            blockPackages = blocked.toList()
                        )
                    )
                )
            }) { Text("сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("отмена") } }
    )
}
