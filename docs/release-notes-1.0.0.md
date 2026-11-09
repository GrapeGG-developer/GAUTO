# Gauto 1.0.0 — первый релиз

**versionCode:** 1 · **versionName:** 1.0.0 · **пакет:** `com.gauto.app.debug`

## Главное

Gauto превращает «не отвлекайся» в автоматические правила: домашние дела закрывают
YouTube, сон делает экран монохромным, фокус-сессия держит блокировки, а выполненные
дела повышают уровень разработчика.

## Устройство

- `core/GautoModels.kt` — модели, 16 наборов / 64 правила, математика правил, привычек, фокуса и обновлений.
- `core/GautoStore.kt` — локальное хранилище правил, привычек, сессий, очков и событий.
- `core/DeviceActions.kt`, `GrayscaleOverlay.kt` — яркость, громкость, DND, системный далтонизатор и оверлей.
- `core/GautoRuntime.kt` — AccessibilityService, будильники, boot-restore, сервис применения, OTA и JobScheduler.
- `ui/GautoRoot.kt` — Bottom Navigation / Top App Bar.
- `ui/GautoScreens.kt`, `ui/GautoComponents.kt` — экраны, редактор правила, панель доступов.

## Совместимость

- Только портрет, русский интерфейс, minSdk 30, targetSdk 36.
- Сеть — только проверка обновлений; токенов в APK нет.
- Расписание яркости/громкости и автоматизации жизни удалены из GSYNK и живут теперь здесь.

## Полная проверка

- `clean testDebugUnitTest lintDebug assembleDebug` — **BUILD SUCCESSFUL**.
- Unit-тесты: **20 / 20**, failures `0`, errors `0`, skipped `0`.
- Android Lint: **25 Warning / 0 Error / 0 Fatal**.
- Debug APK: `GAUTO-v1.0.0-debug.apk`, **60 980 474 байта**, SHA-256 `a8c82d645de881fd079305390e6653cff9ff2ca534452eb1872d974eef403d52`.
- AAPT: `package='com.gauto.app.debug'`, `versionCode='1'`, `versionName='1.0.0'`, `minSdk='30'`, `targetSdk='36'`.
