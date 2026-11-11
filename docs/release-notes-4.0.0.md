# Gauto 4.0.0 — 4D Living Island + настоящий будильник

**versionCode:** 4 · **versionName:** 4.0.0 · **пакет:** `com.gauto.app.debug`

## Будильник без «Отложить на 10 минут»

| Было (системный) | Стало (Gauto) |
|---|---|
| «Отложить на 10 минут» на главном экране | **нет вообще**: только долгое подтверждение «Я встал» |
| один рингтон | 5 мелодий, собственный синтезатор, нарастание громкости |
| резкий свет | «рассвет» за 0…30 мин, плавный подъём яркости, снятие ч/б |
| звонок живёт сам | DND выключается, блокировки снимаются после подтверждения |
| можно уйти кнопкой Назад | `BackHandler` блокирует выход до подтверждения |

Авто-стоп через 1…60 мин — это завершение сигнала с отметкой пропуска, а не перенос.

## Архитектура

- `core/AlarmModels.kt` — `GautoAlarm`, `GautoTimer`, `GautoReminder`, `BlockMode`, `AlarmLogic` (чистая математика).
- `core/AlarmRinger.kt` — `AudioTrack` `USAGE_ALARM`, gradual volume, вибро `USAGE_ALARM`, авто-стоп, `WakeEffects`.
- `core/AlarmRuntime.kt` — `AlarmScheduler` (exact alarms), `AlarmReceiver`, full-screen intent, пересбор после boot.
- `ui/AlarmOverlayActivity.kt` — единый оверлей для подъёма / таймера / напоминания, 4D-сцена, рассвет, без snooze.
- `ui/AlarmScreens.kt` — редактор будильника, таймеры, напоминания в стиле `GautoCard` / `GautoChip`.
- `ui/Gauto4D.kt` — глубина + время как четвёртая ось.
- `GautoRoot.kt` / `GautoIsland.kt` — 8 лепестков, карусель статусов с ближайшим подъёмом.
- `SyncProtocol.kt` — schema 2, новые поля в `GautoState`.
- Manifest — `AlarmOverlayActivity`, `AlarmReceiver`, `USE_FULL_SCREEN_INTENT`.

## Единый UI и блокировки

Веер: Главная · Будильник · Таймеры · Правила · Привычки · Фокус · Напоминания · Настройки.
`BlockMode`: NONE / APP / SLEEP / FOCUS / ALARM / TIMER / REMINDER. Пока `activeAlarmId`
не пуст, сервис доступности закрывает отвлекающие приложения целиком; системные
экраны (`systemui`, телефон) остаются доступны.

## ПК

Gauto Desktop 4.0: 8 страниц, 4D-кольца (глубина + время), таблицы будильников,
таймеров и напоминаний, карусель статусов, `GautoState` schema 2.

## Совместимость

- Портрет, русский UI, minSdk 30, targetSdk 36.
- Все функции 3.0.0 сохранены; OTA-имя ассета `GAUTO-v…` не менялось.
- Пользовательские правила/привычки/фокус читаются старым кодеком (`ignoreUnknownKeys`).

## Полная проверка

- `testDebugUnitTest lintDebug assembleDebug` — **BUILD SUCCESSFUL**.
- Unit-тесты Android: **36 / 36**, failures `0`, errors `0`, skipped `0`.
- Android Lint: **32 Warning / 0 Error / 0 Fatal**.
- Debug APK: `GAUTO-v4.0.0-debug.apk`, **61 407 734 байта**, SHA-256 `06783edd121abbd51db37d3102f763544a89daa6ff711c2dc0c9f266a7824dab`.
- AAPT: `package='com.gauto.app.debug'`, `versionCode='4'`, `versionName='4.0.0'`, `minSdk='30'`, `targetSdk='36'`.
- Gauto Desktop: `python tests/test_sync.py` — **8 / 8 OK**, `py_compile` всех модулей — OK.
