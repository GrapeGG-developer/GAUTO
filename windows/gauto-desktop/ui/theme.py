"""Цвета Gauto Desktop: системный акцент Windows + фирменный fallback."""
from __future__ import annotations

from PySide6.QtCore import QSettings
from PySide6.QtGui import QColor

DEFAULT = ["#8B5CF6", "#22D3EE", "#34D399", "#FBBF24", "#F472B6"]


def system_accents() -> list[str]:
    """Material You-подобный акцент на ПК: берём ColorizationColor из DWM."""
    try:
        settings = QSettings(
            r"HKEY_CURRENT_USER\Software\Microsoft\Windows\DWM",
            QSettings.Format.NativeFormat,
        )
        raw = int(settings.value("ColorizationColor", 0) or 0)
        if raw == 0:
            return DEFAULT
        base = QColor((raw >> 16) & 0xFF, (raw >> 8) & 0xFF, raw & 0xFF)
        hue = base.hslHueF()
        if hue < 0:
            return DEFAULT
        return [QColor.fromHslF((hue + shift) % 1.0, 0.62, 0.62, 1.0).name() for shift in (0.0, 0.14, 0.33, 0.10, 0.87)]
    except Exception:
        return DEFAULT
