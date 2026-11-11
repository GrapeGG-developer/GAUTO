"""Gauto Desktop — точка входа.

Запуск:
    pip install -r requirements.txt
    python app.py

Приложение само найдёт телефон Gauto в локальной сети, если в настройках
телефона включён «доступ с ПК», и начнёт синхронизировать правила, привычки,
фокус-сессии и статистику. Облако, аккаунт и токен не используются.
"""
from __future__ import annotations

import os
import sys

from PySide6.QtGui import QIcon
from PySide6.QtWidgets import QApplication

from ui.main_window import MainWindow


def main() -> int:
    app = QApplication(sys.argv)
    app.setApplicationName("Gauto")
    app.setOrganizationName("GrapeGG")
    app.setApplicationVersion("3.0.0")
    icon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "resources", "gauto.png")
    app.setWindowIcon(QIcon(icon_path))
    window = MainWindow(icon_path)
    window.show()
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
