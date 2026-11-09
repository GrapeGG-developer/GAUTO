# Gauto 2.0.0 — Island UI и синхронизация с ПК

**versionCode:** 2 · **versionName:** 2.0.0 · **пакет:** `com.gauto.app.debug`

## Что изменилось

| Было | Стало |
|---|---|
| `GAUTO` | **`Gauto`** |
| галочка в иконке | новый логотип: остров-капсула с расходящимися категориями |
| Bottom Navigation Bar 80 dp | живой **островок** с веером категорий |
| почти без анимаций | пружины, morph, stagger, дыхание, параллакс, `AnimatedContent` |
| только телефон | **Gauto Desktop** на PySide6 с LAN-синхронизацией |

## Island UI

`ui/GautoIsland.kt` и `ui/GautoRoot.kt`:

- плавающая капсула 208→246 dp с активным правилом, пульсирующим индикатором и «дыханием»;
- `Animatable` + `spring(DampingRatioMediumBouncy)` раскрывает веер из 5 лепестков по дуге −64°…+64°;
- каждый лепесток: свой цвет, `graphicsLayer` (translation / alpha / scale / rotationZ), собственный Canvas-глиф;
- фон: вертикальный градиент, радиальное свечение и 30 дрейфующих частиц, усиливающихся при раскрытии;
- смена экрана — `AnimatedContent` с `fadeIn + scaleIn + slideInVertically`;
- Top App Bar: новый логотип, `Gauto · <вкладка>`, поиск и уведомления с бейджем;
- `NavigationBar`, `NavigationBarItem` и `height(80.dp)` полностью удалены.

## Синхронизация «телефон ⇄ ПК»

Без облака, аккаунта и токена:

- `SyncProtocol.kt`: схема 1, порты 8765/8766, `GAUTO_DISCOVER_v1` / `GAUTO_HERE_v1`, `X-Gauto-Pin`, `GautoState`, слияние по `updatedAt`;
- `GautoSyncService.kt`: foreground-сервис, `ServerSocket`, `DatagramSocket`, `GET /api/ping`, `GET /api/state`, `POST /api/state`, CORS, защита PIN;
- `GautoStore`: `snapshot()`, `applyRemote()`, `ensurePin()`, `saveSessions()`, `saveEvents()`;
- настройки телефона показывают IP, порт, PIN и переключатель доступа с ПК;
- `RECEIVE_BOOT_COMPLETED` / `ACCESS_WIFI_STATE` и foreground-сервис объявлены в манифесте.

## Gauto Desktop

`windows/gauto-desktop/`: PySide6, `ui/island.py` (тот же островок на QPainter + QTimer),
`ui/main_window.py` (5 страниц, трей, автозапуск через реестр, синхронизация каждые 6 с),
`gautosync/` (протокол, HTTP-клиент, UDP-поиск), `resources/gauto.png` / `.ico`,
`tests/test_sync.py`, `build_exe.py` и Windows CI workflow.

## Совместимость

- Портрет, русский UI, minSdk 30, targetSdk 36, `com.gauto.app.debug`.
- Репозиторий и имя APK-ассета оставлены `GAUTO-v…` намеренно: уже установленный 1.0.0
  должен находить обновление по OTA. Видимое имя — Gauto.
- Все прежние правила, привычки, фокус и OTA-логика сохранены; формат `GautoState`
  имеет значения по умолчанию для всех новых полей.
