# Gauto 3.0.0 — Living Island

**versionCode:** 3 · **versionName:** 3.0.0 · **пакет:** `com.gauto.app.debug`

## Что нового

| Направление | Реализация |
|---|---|
| Material You | `dynamicDarkColorScheme` на Android 12+, `LocalGautoAccents`, фирменный fallback |
| Анимированный логотип | `GautoAnimatedLogo.kt`: 5 дышащих лепестков, пульсирующее ядро, активный акцент |
| Максимум движения | stagger `Animatable` на каждый лепесток, волна раскрытия, wobble, 42 частицы + хвосты, дыхание, хаптика |
| Карусель статусов | 6 плашек, смена раз в 3 с, `AnimatedContent` slide+fade |
| Управление живостью | `motionLevel` 0/1/2 → `LocalGautoMotion` 0.35/0.7/1.0 |
| ПК | тот же Living Island на PySide6 + системный акцент Windows |

## Детали

- `GautoTheme.kt`: динамическая палитра, `LocalGautoAccents`, `LocalGautoMotion`, типографика.
- `GautoAnimatedLogo.kt`: новый composable, бесконечная фаза 4.2 с, амплитуда зависит от живости.
- `GautoIsland.kt`: `capsuleProgress` + 5 `petalProgress`, `delay(index * 48L)`, обратный порядок при закрытии, `waveTransition`, `GautoAnimatedLogo` в капсуле, карусель `statusIndex`, тактильный отклик.
- `GautoRoot.kt`: сбор статусов из `GautoStore` (правило, уровень, стрик, фокус, синхронизация, событие), `GautoTheme(motion)`, живой логотип в `GautoTopBar`.
- `GautoScreens.kt`: три чипа «живость островка» в настройках.
- `GautoModels.kt`: `motionLevel`, версия 3.0.0, `Gauto Living Island 3.0`.

## Gauto Desktop 3.0

`windows/gauto-desktop/ui/theme.py` читает Windows `ColorizationColor` и строит 5 акцентов.
`ui/island.py` получил stagger (`_petal`), волну раскрытия, анимированный логотип
(`_draw_logo`), карусель (`set_statuses`) и 42 частицы. `ui/main_window.py` передаёт
статусы из синхронизированного состояния. Версия в `app.py` — 3.0.0.

## Совместимость

- Портрет, русский UI, minSdk 30, targetSdk 36, `com.gauto.app.debug`.
- Все правила, привычки, фокус-сессии, LAN-синхронизация и OTA сохранены.
- Имя APK-ассета по-прежнему `GAUTO-v…` ради бесшовного OTA с 1.0.0/2.0.0.

## Полная проверка

- Android: `clean testDebugUnitTest lintDebug assembleDebug` → **BUILD SUCCESSFUL** (первый проход нашёл один устаревший source-guard, исправлен; финальный проход зелёный).
- Unit-тесты Android: **26 / 26**, failures `0`, errors `0`, skipped `0`.
- Android Lint: **27 Warning / 0 Error / 0 Fatal**.
- Debug APK: `GAUTO-v3.0.0-debug.apk`, **61 112 694 байта**, SHA-256 `5839631d91c72cbdafaafa9ab6a46d9278742abd1452a7d775aee4b2336d1a5f`.
- AAPT: `package='com.gauto.app.debug'`, `versionCode='3'`, `versionName='3.0.0'`, `minSdk='30'`, `targetSdk='36'`.
- Gauto Desktop: `python tests/test_sync.py` — **8 / 8 OK**, `py_compile` всех модулей — OK, Windows CI workflow для `Gauto.exe` сохранён.

## Публикация

- **Android latest:** https://github.com/GrapeGG-developer/GAUTO/releases/tag/v3.0.0 — Release `441914830`, asset `GAUTO-v3.0.0-debug.apk` (`736076564`), `prerelease=false`, `draft=false`.
- **Gauto Desktop prerelease:** https://github.com/GrapeGG-developer/GAUTO/releases/tag/gauto-desktop-v3.0.0 — Release `441914889`, ассеты `Gauto-Desktop-3.0.0.zip` и `Gauto-Desktop-win64.zip` (`736084017`, 51 240 960 байт, SHA-256 `a29de7c6228e4df2aa1129262a76e7494cdb6297f825f9d7ed4b48c7d769ca0f`).
- Windows CI: **success**, run `43055190594`.
- Публичный API: latest = `v3.0.0`, prerelease ПК не сместил latest.
