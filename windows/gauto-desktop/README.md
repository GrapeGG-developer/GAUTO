# Gauto Desktop 3.0.0

Нативное приложение **Gauto** для Windows на PySide6: тот же **Living Island 3.0** —
stagger-веер категорий, волна раскрытия, анимированный логотип, живая карусель
статусов, системный акцент Windows, новая иконка, трей и автозапуск.

## Синхронизация

Полностью локальная, без облака, аккаунта и токена:

1. На телефоне: **Настройки → синхронизация с ПК → включить «доступ с ПК»**.
   Там же显示在 IP, порт `8765` и PIN.
2. На ПК: `Настройки → найти телефон` (UDP-поиск на порту `8766`) или введите IP вручную.
3. Введите PIN и нажмите `подключиться`. Состояние подтягивается каждые 6 секунд;
   кнопка `отправить на телефон` пушит изменения обратно.

Протокол: `GET /api/ping`, `GET /api/state`, `POST /api/state`, заголовок `X-Gauto-Pin`.
Один JSON с полями `schema, app, device, updatedAt, rules, habits, sessions, settings, events`.
Побеждает более свежее состояние (`updatedAt`).

## Запуск

```bat
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

Сборка exe (Windows): `python build_exe.py` (нужен `pyinstaller`), результат в `dist/Gauto/`.
Готовая сборка приложена к prerelease `gauto-desktop-v3.0.0` как `Gauto-Desktop-win64.zip`.
Иконка — `resources/gauto.png`; для exe используется `resources/gauto.ico`.

## Тесты

```bat
python tests\test_sync.py
```

Тесты не требуют Qt и сети: проверяют протокол, совместимость полей с Android,
UDP-ответ, URL клиента и наличие острова/трея/автозапуска в исходниках.
