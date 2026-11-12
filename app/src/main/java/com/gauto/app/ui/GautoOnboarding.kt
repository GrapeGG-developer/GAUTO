package com.gauto.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Первое знакомство — короткое и пропускаемое, объясняет три главных действия. */
@Composable
fun GautoOnboarding(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.Center
    ) {
        GautoAnimatedLogo(Modifier.fillMaxWidth().height(90.dp), 0)
        Spacer(Modifier.height(24.dp))
        Text("Начни с главного", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        OnboardingLine("1", "Поставь подъём", "Будильник без кнопки «отложить». Чтобы встать, нужно подтвердить.")
        Spacer(Modifier.height(12.dp))
        OnboardingLine("2", "Убери отвлечения", "На главной — Сон и Фокус. Всё остальное — во вкладке «Ещё».")
        Spacer(Modifier.height(12.dp))
        OnboardingLine("3", "Одним файлом на ПК", "Будильник — без кнопки «Отложить». Синхронизация с ПК — в «Ещё».")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text("Понятно", fontSize = 15.sp)
        }
    }
}

@Composable
private fun OnboardingLine(number: String, title: String, description: String) {
    Column {
        Text("$number · $title", style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
