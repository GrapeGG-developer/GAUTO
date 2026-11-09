package com.gauto.app.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** Панель доступов GAUTO: каждая карточка ведёт на свой системный экран. */
object GautoPermissions {

    const val ID_ACCESSIBILITY = "accessibility"
    const val ID_OVERLAY = "overlay"
    const val ID_WRITE = "write"
    const val ID_EXACT = "exact"
    const val ID_NOTIF = "notifications"
    const val ID_DND = "dnd"
    const val ID_BATTERY = "battery"
    const val ID_USAGE = "usage"

    data class Item(
        val id: String,
        val title: String,
        val hint: String,
        val granted: Boolean,
        val optional: Boolean = false
    )

    fun accessibilityEnabled(context: Context): Boolean {
        val cr = context.contentResolver
        val enabled = runCatching {
            Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        }.getOrDefault(false)
        if (!enabled) return false
        val services = runCatching {
            Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        }.getOrDefault("")
        return services.contains(context.packageName)
    }

    fun usageGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        return runCatching {
            appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName) ==
                AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    fun batteryExempt(context: Context): Boolean = runCatching {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)

    fun exactGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
            }.getOrDefault(false)
        } else true

    fun notificationsGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

    fun snapshot(context: Context): List<Item> = listOf(
        Item(ID_ACCESSIBILITY, "Спецвозможности GAUTO", "ловит открытие YouTube и закрывает его", accessibilityEnabled(context)),
        Item(ID_OVERLAY, "Слой поверх приложений", "монохромный слой перед сном и заглушка блокировки", Settings.canDrawOverlays(context)),
        Item(ID_WRITE, "Изменение системных настроек", "системная яркость и автояркость", DeviceActions.canWriteSettings(context)),
        Item(ID_EXACT, "Точные будильники", "правила срабатывают в назначенную минуту", exactGranted(context)),
        Item(ID_NOTIF, "Уведомления", "отчёты о правилах и обновлениях", notificationsGranted(context)),
        Item(ID_DND, "Не беспокоить", "тишина во время сна и фокуса", DeviceActions.dndGranted(context)),
        Item(ID_BATTERY, "Без ограничений батареи", "HyperOS не усыпляет будильники", batteryExempt(context), optional = true),
        Item(ID_USAGE, "Статистика использования", "честная статистика фокуса", usageGranted(context), optional = true)
    )

    fun allRequiredGranted(context: Context): Boolean = snapshot(context).filterNot { it.optional }.all { it.granted }

    @SuppressLint("BatteryLife")
    fun open(context: Context, id: String) {
        val uri = Uri.parse("package:${context.packageName}")
        val intent: Intent = when (id) {
            ID_ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            ID_OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            ID_WRITE -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, uri)
            ID_EXACT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, uri)
            } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            ID_NOTIF -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            ID_DND -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            ID_BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri)
            ID_USAGE -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        }
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
