package com.gauto.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gauto.app.core.Backup
import com.gauto.app.core.GautoRepo
import com.gauto.app.core.GautoStore

private enum class MorePage { MENU, TIMERS, REMINDERS, HABITS, FOCUS, SETTINGS, BACKUP, ABOUT }

/** Один простой список вместо восьми лепестков. Крупные строки, короткие подписи. */
@Composable
fun MoreScreen(store: GautoStore, revision: Int, onChanged: () -> Unit, onOpenPermissions: () -> Unit) {
    var page by remember { mutableStateOf(MorePage.MENU) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(Backup.exportText(store).toByteArray()) }
            Toast.makeText(context, "Копия сохранена", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val ok = Backup.importText(store, text)
            Toast.makeText(context, if (ok) "Восстановлено" else "Файл не подошёл", Toast.LENGTH_SHORT).show()
            if (ok) onChanged()
        }
    }

    if (page != MorePage.MENU) {
        Column(Modifier.fillMaxSize()) {
            OutlinedButton(
                onClick = { page = MorePage.MENU },
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp)
            ) { Text("← Ещё", fontSize = 13.sp) }
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                when (page) {
                    MorePage.TIMERS -> TimersScreen(store, revision, onChanged)
                    MorePage.REMINDERS -> RemindersScreen(store, revision, onChanged)
                    MorePage.HABITS -> HabitsScreen(store, revision, onChanged)
                    MorePage.FOCUS -> FocusScreen(store, revision, onChanged)
                    MorePage.SETTINGS -> SettingsScreen(store, revision, onChanged, onOpenPermissions)
                    MorePage.BACKUP -> BackupScreen({ exportLauncher.launch(Backup.FILE_NAME) }, { importLauncher.launch(arrayOf("application/json", "text/plain")) })
                    MorePage.ABOUT -> AboutScreen()
                    MorePage.MENU -> Unit
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 210.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Ещё", style = MaterialTheme.typography.headlineSmall)
            Text("Всё остальное в одном месте", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }
        item { MoreButton("Таймеры", "Обратный отсчёт и звук", MorePage.TIMERS) { page = it } }
        item { MoreButton("Напоминания", "По времени и дням", MorePage.REMINDERS) { page = it } }
        item { MoreButton("Привычки", "Стрики и очки", MorePage.HABITS) { page = it } }
        item { MoreButton("Фокус-сессии", "Работа без отвлечений", MorePage.FOCUS) { page = it } }
        item { MoreButton("Настройки", "Разрешения, обновления, ПК", MorePage.SETTINGS) { page = it } }
        item { MoreButton("Резервная копия", "Сохранить или восстановить одним файлом", MorePage.BACKUP) { page = it } }
        item { MoreButton("О приложении", "Версия и коротко о Gauto", MorePage.ABOUT) { page = it } }
    }
}

@Composable
private fun MoreButton(title: String, hint: String, page: MorePage, onClick: (MorePage) -> Unit) {
    Button(
        onClick = { onClick(page) },
        modifier = Modifier.fillMaxWidth().height(68.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, fontSize = 15.sp)
            Text(hint, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BackupScreen(onExport: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Резервная копия", style = MaterialTheme.typography.headlineSmall)
        Text("Один JSON-файл: правила, будильники, таймеры, привычки и настройки.", fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Сохранить", fontSize = 14.sp) }
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Восстановить", fontSize = 14.sp) }
    }
}

@Composable
private fun AboutScreen() {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Gauto", style = MaterialTheme.typography.headlineSmall)
        Text("Версия ${GautoRepo.VERSION} · ${GautoRepo.UI_VERSION}", fontSize = 14.sp)
        Text("Подъём без отложить. Правила убирают отвлечения. Всё важное — на экране «Сегодня».", fontSize = 14.sp)
        Text("Данные только на телефоне. Синхронизация с ПК — по локальной сети.", fontSize = 13.sp)
    }
}
