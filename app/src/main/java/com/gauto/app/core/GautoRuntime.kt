package com.gauto.app.core

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gauto.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.ZonedDateTime

/** Планировщик точных будильников правил. */
object GautoScheduler {
    const val ACTION_FIRE = "com.gauto.app.action.FIRE_RULE"
    const val EXTRA_RULE_ID = "rule_id"

    fun pendingIntent(context: Context, ruleId: String): PendingIntent {
        val intent = Intent(context, ApplyReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_RULE_ID, ruleId)
        return PendingIntent.getBroadcast(
            context,
            ruleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canScheduleExact(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
            }.getOrDefault(false)
        } else true

    fun schedule(context: Context, rule: GautoRule): ZonedDateTime? {
        val at = RuleLogic.nextOccurrence(rule) ?: return null
        val am = context.getSystemService(AlarmManager::class.java) ?: return null
        runCatching {
            val trigger = at.toInstant().toEpochMilli()
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context, rule.id))
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context, rule.id))
            }
        }
        return at
    }

    fun cancel(context: Context, ruleId: String) {
        runCatching {
            context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context, ruleId))
        }
    }

    fun scheduleAll(context: Context, rules: List<GautoRule>): ZonedDateTime? {
        var earliest: ZonedDateTime? = null
        rules.forEach { rule ->
            if (rule.enabled) {
                val at = schedule(context, rule)
                val current = earliest
                if (at != null && (current == null || at.isBefore(current))) earliest = at
            } else {
                cancel(context, rule.id)
            }
        }
        return earliest
    }
}

/** Применяет активное правило целиком: яркость, громкость, тишина, ч/б и блокировки. */
internal fun applyRuleNow(context: Context, ruleId: String?) {
    val store = GautoStore(context)
    val rule = ruleId?.let { id -> store.rules().firstOrNull { it.id == id } } ?: store.activeRule()
    val settings = store.settings()
    if (rule != null) {
        if (rule.useBrightness) {
            if (!settings.autoBrightnessSaved) {
                store.saveSettings(
                    settings.copy(
                        autoBrightnessSaved = true,
                        autoBrightnessWasOn = DeviceActions.isAutoBrightnessOn(context.contentResolver)
                    )
                )
            }
            DeviceActions.setBrightnessPercent(context, rule.brightness)
        } else if (settings.autoBrightnessSaved && settings.autoBrightnessWasOn) {
            DeviceActions.setAutoBrightness(context, true)
            store.saveSettings(store.settings().copy(autoBrightnessSaved = false))
        }
        if (rule.useVolume) DeviceActions.setVolumePercent(context, rule.volume)
        if (rule.silentMode != -1) DeviceActions.setDnd(context, rule.silentMode == 1)
        GrayscaleController.apply(context, rule.grayscale)
        store.event("Применено «${rule.name}» · ${rule.timeLabel}")
    } else {
        GrayscaleController.apply(context, false)
    }
    GautoScheduler.scheduleAll(context, store.rules())
}

class ApplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val ruleId = intent?.getStringExtra(GautoScheduler.EXTRA_RULE_ID)
        val service = Intent(context, ApplyService::class.java).putExtra(GautoScheduler.EXTRA_RULE_ID, ruleId)
        runCatching { context.startForegroundService(service) }
            .onFailure { applyRuleNow(context.applicationContext, ruleId) }
    }
}

class ApplyService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, notification())
        val ruleId = intent?.getStringExtra(GautoScheduler.EXTRA_RULE_ID)
        Thread {
            runCatching { applyRuleNow(applicationContext, ruleId) }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }.apply { name = "gauto-apply" }.start()
        return START_NOT_STICKY
    }

    private fun notification(): Notification = Notification.Builder(this, GAUTOApp.CHANNEL_APPLY)
        .setSmallIcon(R.drawable.ic_gauto_foreground)
        .setContentTitle("GAUTO")
        .setContentText("Применяю правило…")
        .setAutoCancel(true)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val store = GautoStore(app)
        GautoScheduler.scheduleAll(app, store.rules())
        GautoUpdateJobs.schedule(app)
        if (store.settings().syncAfterBoot) {
            runCatching { app.startForegroundService(Intent(app, ApplyService::class.java)) }
                .onFailure { applyRuleNow(app, null) }
        }
    }
}

/**
 * Сервис доступности: замечает открытие заблокированного приложения и уводит
 * на главный экран. Без root, без VPN, без ADB. Список блокируемых пакетов
 * всегда берётся из активного правила / активной фокус-сессии.
 */
class GautoAccessibilityService : AccessibilityService() {

    @Volatile private var lastBlockedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val now = System.currentTimeMillis()
        if (now - lastBlockedAt < 1_500L) return
        val store = GautoStore(this)
        if (!store.isPackageBlocked(pkg, now)) return
        lastBlockedAt = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        DeviceActions.vibrate(this, 120L)
        val label = store.activeBlockingLabel(now)
        store.event("Заблокирован $pkg · $label")
        Handler(Looper.getMainLooper()).post {
            runCatching {
                Toast.makeText(this, "GAUTO: «$label» — $pkg закрыт", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInterrupt() = Unit
}

/** Публичный GitHub API без единого токена: ETag, digest, автозагрузка и установка. */
object UpdateChecker {

    fun check(context: Context): GautoUpdate? {
        val store = GautoStore(context)
        val settings = store.settings()
        val url = "https://api.github.com/repos/${GautoRepo.REPO}/releases/latest"
        return runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "GAUTO-Updater")
            if (settings.etag.isNotBlank()) conn.setRequestProperty("If-None-Match", settings.etag)
            val code = conn.responseCode
            if (code == 304) {
                store.updateState(settings.etag, System.currentTimeMillis(), settings.seenVersion)
                conn.disconnect()
                return@runCatching null
            }
            if (code !in 200..299) {
                conn.disconnect()
                return@runCatching checkByPage(context)
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val etag = conn.getHeaderField("ETag").orEmpty()
            conn.disconnect()
            val update = UpdateLogic.parseLatest(body) ?: return@runCatching checkByPage(context)
            store.updateState(etag, System.currentTimeMillis(), settings.seenVersion)
            update
        }.getOrNull()
    }

    /** Запасной путь без API: страница последнего релиза. */
    fun checkByPage(context: Context): GautoUpdate? = runCatching {
        val page = URL("https://github.com/${GautoRepo.REPO}/releases/latest").readText()
        val version = Regex("tag/v([0-9]+\\.[0-9]+\\.[0-9]+)").find(page)?.groupValues?.get(1)
            ?: return@runCatching null
        val tag = "v$version"
        GautoUpdate(
            tag = tag,
            name = "GAUTO $tag",
            apkUrl = UpdateLogic.apkUrl(GautoRepo.REPO, tag),
            size = 0L,
            sha256 = ""
        )
    }.getOrNull()

    fun download(context: Context, update: GautoUpdate): File? = runCatching {
        val file = File(context.filesDir, "gauto-update.apk")
        val conn = URL(update.apkUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        conn.instanceFollowRedirects = true
        conn.inputStream.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
        conn.disconnect()
        file
    }.getOrNull()

    fun verify(file: File, expectedSha: String): Boolean {
        if (expectedSha.isBlank()) return file.length() > 0L
        val actual = runCatching {
            val bytes = file.readBytes()
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
        return actual.equals(expectedSha, ignoreCase = true)
    }

    fun downloadAndInstall(context: Context, update: GautoUpdate): Boolean {
        val file = download(context, update) ?: return false
        if (!verify(file, update.sha256)) {
            file.delete()
            return false
        }
        notify(context, update)
        install(context, file)
        return true
    }

    private fun notify(context: Context, update: GautoUpdate) {
        runCatching {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val notification = Notification.Builder(context, GAUTOApp.CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_gauto_foreground)
                .setContentTitle("GAUTO ${update.tag} готов")
                .setContentText("Обновление скачано, подтвердите установку")
                .setAutoCancel(true)
                .build()
            nm.notify(2, notification)
        }
    }

    private fun install(context: Context, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /** Временный APK удаляется при первом запуске уже обновлённой версии. */
    fun cleanupDownloadedUpdates(context: Context) {
        runCatching { File(context.filesDir, "gauto-update.apk").delete() }
    }
}

object GautoUpdateJobs {
    private const val JOB_ID = 610

    fun schedule(context: Context) {
        runCatching {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val info = JobInfo.Builder(JOB_ID, ComponentName(context, GautoUpdateJobService::class.java))
                .setPeriodic(15 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()
            scheduler.schedule(info)
        }
    }
}

class GautoUpdateJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Thread {
            runCatching {
                val store = GautoStore(this)
                val update = UpdateChecker.check(this)
                if (update != null && store.settings().updateAuto && UpdateLogic.isNewer(update.tag)) {
                    UpdateChecker.downloadAndInstall(this, update)
                }
            }
            jobFinished(params, false)
        }.apply { name = "gauto-update" }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}

/** Сторож обновлений: опрос каждые 200 мс, сеть — не чаще раза в минуту. */
object UpdateWatcher {
    @Volatile private var started = false

    fun start(app: Application) {
        if (started) return
        started = true
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            while (isActive) {
                delay(200L)
                val store = GautoStore(app)
                val settings = store.settings()
                val now = System.currentTimeMillis()
                if (settings.updateAuto && now - settings.lastUpdateCheck >= 60_000L) {
                    store.saveSettings(settings.copy(lastUpdateCheck = now))
                    val update = runCatching { UpdateChecker.check(app) }.getOrNull()
                    if (update != null && UpdateLogic.isNewer(update.tag)) {
                        runCatching { UpdateChecker.downloadAndInstall(app, update) }
                    }
                }
            }
        }
    }
}

class GAUTOApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createChannels()
        runCatching { UpdateChecker.cleanupDownloadedUpdates(this) }
        runCatching { GautoScheduler.scheduleAll(this, GautoStore(this).rules()) }
        GautoUpdateJobs.schedule(this)
        UpdateWatcher.start(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_APPLY, "Применение правил", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_UPDATES, "Обновления GAUTO", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setShowBadge(true)
                enableVibration(true)
                setSound(null, null)
            }
        )
    }

    companion object {
        const val CHANNEL_APPLY = "gauto_apply"
        const val CHANNEL_UPDATES = "gauto_updates"
    }
}
