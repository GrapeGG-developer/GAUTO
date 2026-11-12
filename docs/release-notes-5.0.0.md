# Gauto 5.0.0 — просто и по делу

**versionCode:** 5 · **versionName:** 5.0.0 · **пакет:** `com.gauto.app.debug`

## Что изменилось

Вместо восьми категорий — четыре понятных раздела: **Сегодня · Будильник · Правила · Ещё**.
«Сегодня» показывает только следующий подъём, текущее состояние, одну привычку и четыре действия.
Таймеры, напоминания, привычки, фокус, настройки и backup теперь в «Ещё».

- Короткий онбординг объясняет три действия, затем открывает разрешения.
- Готовые планы правил свёрнуты до нажатия на кнопку.
- Home-screen виджет: следующий подъём, состояние, Фокус / Сон / Тишина.
- Резервная копия: экспорт/восстановление одним JSON-файлом.
- Quick «Тишина 30 мин» сам выключает DND; повторяющиеся сигналы перепланируются.
- Desktop-навигация тоже сведена к четырём разделам.
- Убраны повторяющиеся карточки и визуальные эффекты с обычных экранов; 4D оставлен только в оверлее рассвета.

## Сохранено

Будильник по-прежнему без «Отложить» — только долгое подтверждение «Я встал».
Сохранены рассвет, нарастание звука, блокировки, привычки, фокус, LAN-синхронизация и OTA.

## Полная проверка

- `testDebugUnitTest lintDebug assembleDebug` — **BUILD SUCCESSFUL**.
- Android unit-тесты: **40 / 40**, failures/errors/skipped `0`.
- Android Lint: **39 Warning / 0 Error / 0 Fatal**.
- APK `GAUTO-v5.0.0-debug.apk`, 61 443 978 байта, SHA-256 `9c812af3c5631ec6317436718a8753ba5507b5067ba6a258d71c1914ba09ca83`.
- AAPT: `versionCode=5`, `versionName=5.0.0`, `minSdk=30`, `targetSdk=36`.
- Gauto Desktop: **8 / 8** протокольных тестов; `py_compile` всех модулей — OK.

## Публикация

- **Android latest:** https://github.com/GrapeGG-developer/GAUTO/releases/tag/v5.0.0 — Release `443114818`, asset `GAUTO-v5.0.0-debug.apk`, SHA-256 `9c812af3c5631ec6317436718a8753ba5507b5067ba6a258d71c1914ba09ca83`.
- **ПК prerelease:** https://github.com/GrapeGG-developer/GAUTO/releases/tag/gauto-desktop-v5.0.0 — Release `443114937`, исходники `Gauto-Desktop-5.0.0.zip` и готовый `Gauto-Desktop-win64.zip` с `Gauto.exe` (51 241 605 байт, SHA-256 `9df87046c7f1900b12da9e56ca8a34f23a878966f6e0fd5056aee1197a631127`).
- Windows CI: **success**, run `43245075410`.
- Публичный API подтвердил latest `v5.0.0`, `prerelease=false`, `draft=false`; Desktop prerelease latest не смещает.
