"""Сборка Gauto.exe через PyInstaller (Windows)."""
from __future__ import annotations

import subprocess
import sys

COMMAND = [
    sys.executable, "-m", "PyInstaller",
    "--noconfirm", "--clean", "--onedir", "--windowed",
    "--name", "Gauto",
    "--icon", "resources/gauto.ico",
    "--add-data", "resources/gauto.png;resources",
    "--hidden-import", "PySide6.QtCore",
    "--hidden-import", "PySide6.QtGui",
    "--hidden-import", "PySide6.QtWidgets",
    "app.py",
]

if __name__ == "__main__":
    raise SystemExit(subprocess.call(COMMAND))
