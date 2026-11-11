"""Живой «островок» Gauto 3.0 для ПК.

Максимум движения: stagger-появление лепестков, волна раскрытия, дыхание,
анимированный логотип с лепестками и живая карусель статусов в капсуле.
"""
from __future__ import annotations

import math

from PySide6.QtCore import QPointF, QRectF, Qt, QTimer, Signal
from PySide6.QtGui import (
    QBrush, QColor, QFont, QLinearGradient, QPainter, QPen, QRadialGradient,
)
from PySide6.QtWidgets import QWidget

from .theme import system_accents

TABS = ["Главная", "Правила", "Привычки", "Фокус", "Настройки"]


class IslandWidget(QWidget):
    tabSelected = Signal(int)
    toggled = Signal(bool)

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setMinimumWidth(420)
        self.setFixedHeight(258)
        self.accents = system_accents()
        self.progress = 0.0
        self.target = 0.0
        self.expanded = False
        self.active = 0
        self.statuses = ["правил пока нет"]
        self._status_index = 0
        self._status_elapsed = 0.0
        self._petal = [0.0] * len(TABS)
        self._t = 0.0
        self._phase = 0.0
        self._timer = QTimer(self)
        self._timer.timeout.connect(self._tick)
        self._timer.start(16)

    @property
    def status(self) -> str:
        return self.statuses[self._status_index % max(1, len(self.statuses))]

    def set_statuses(self, items):
        clean = [str(x)[:52] for x in items if str(x).strip()]
        self.statuses = clean or ["Gauto"]
        self._status_index = 0
        self._status_elapsed = 0.0
        self.update()

    def set_status(self, text: str):
        self.set_statuses([text])

    def _tick(self):
        self._phase += 0.016
        self._t += 0.016
        self._status_elapsed += 0.016
        if len(self.statuses) > 1 and self._status_elapsed >= 3.0:
            self._status_elapsed = 0.0
            self._status_index = (self._status_index + 1) % len(self.statuses)
        self.progress += (self.target - self.progress) * 0.17
        if abs(self.target - self.progress) < 0.002:
            self.progress = self.target
        for i in range(len(TABS)):
            delay = (i if self.expanded else (len(TABS) - 1 - i)) * 0.055
            wanted = (1.0 if self._t >= delay else 0.0) if self.expanded else (0.0 if self._t >= delay else 1.0)
            self._petal[i] += (wanted - self._petal[i]) * 0.18
        self.update()

    def set_expanded(self, value: bool):
        self.expanded = bool(value)
        self.target = 1.0 if self.expanded else 0.0
        self._t = 0.0
        self.toggled.emit(self.expanded)

    def toggle(self):
        self.set_expanded(not self.expanded)

    def set_active(self, index: int):
        self.active = max(0, min(len(TABS) - 1, index))
        self.update()

    def _geometry(self, index: int):
        w, h = self.width(), self.height()
        cx, cy = w / 2.0, h - 62.0
        angle = math.radians(-64 + index * 32)
        pp = self._petal[index]
        radius = 150.0 * pp
        x = cx + math.sin(angle) * radius
        y = cy - math.cos(angle) * radius - 8.0
        size = 66.0 * (0.55 + 0.45 * pp)
        return x, y, size, angle, pp

    def mousePressEvent(self, event):
        pos: QPointF = event.position()
        if self.progress > 0.5:
            for index in range(len(TABS)):
                x, y, size, _, pp = self._geometry(index)
                if pp > 0.5 and abs(pos.x() - x) <= size / 2 and abs(pos.y() - y) <= size / 2:
                    self.tabSelected.emit(index)
                    self.set_expanded(False)
                    return
        cx, cy = self.width() / 2.0, self.height() - 62.0
        if abs(pos.x() - cx) <= 130 and abs(pos.y() - cy) <= 42:
            self.toggle()
            return
        if self.expanded:
            self.set_expanded(False)
        super().mousePressEvent(event)

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.Antialiasing)
        w, h = self.width(), self.height()
        cx, cy = w / 2.0, h - 62.0
        accent = QColor(self.accents[self.active])

        background = QLinearGradient(0, 0, w, h)
        background.setColorAt(0, QColor("#070B18"))
        background.setColorAt(0.55, QColor("#0B1020"))
        background.setColorAt(1, QColor("#121B36"))
        painter.fillRect(self.rect(), QBrush(background))

        glow = QRadialGradient(cx, cy, w * 0.62)
        glow.setColorAt(0, QColor(accent.red(), accent.green(), accent.blue(), int(58 + 40 * self.progress)))
        glow.setColorAt(1, QColor(0, 0, 0, 0))
        painter.fillRect(self.rect(), QBrush(glow))

        amplify = 1.0 + 0.9 * self.progress
        for i in range(42):
            fx = ((i * 73 + 11) % 100) / 100.0
            fy = ((i * 47 + 29) % 100) / 100.0
            drift = math.sin(self._phase * 1.7 + i * 0.71)
            rise = math.cos(self._phase * 1.4 + i * 0.43)
            x = w * fx + drift * 22 * amplify
            y = h * fy + rise * 13 * amplify
            radius = 1.3 + 2.6 * ((i % 5) / 5.0)
            alpha = 18 + 38 * (0.5 + 0.5 * drift)
            painter.setBrush(QBrush(QColor(accent.red(), accent.green(), accent.blue(), int(alpha))))
            painter.setPen(Qt.NoPen)
            painter.drawEllipse(QPointF(x, y), radius, radius)
            if self.expanded and i % 6 == 0:
                painter.setPen(QPen(QColor(accent.red(), accent.green(), accent.blue(), 22), 1))
                painter.drawLine(QPointF(x, y), QPointF(x - drift * 26, y + rise * 16))

        # Волна раскрытия.
        if self.progress > 0.01:
            wave = self._phase % 1.0
            painter.setBrush(Qt.NoBrush)
            painter.setPen(QPen(QColor(accent.red(), accent.green(), accent.blue(), int(90 * (1 - wave) * self.progress)), 2))
            painter.drawEllipse(QPointF(cx, cy), 40 + 120 * wave, 40 + 120 * wave)

        for index, title in enumerate(TABS):
            x, y, size, _, pp = self._geometry(index)
            if pp < 0.01:
                continue
            color = QColor(self.accents[index])
            painter.setOpacity(pp)
            petal = QLinearGradient(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
            petal.setColorAt(0, color)
            petal.setColorAt(1, QColor(color.red(), color.green(), color.blue(), 150))
            painter.setBrush(QBrush(petal))
            painter.setPen(Qt.NoPen)
            painter.drawRoundedRect(QRectF(x - size / 2, y - size / 2, size, size), 22, 22)
            self._draw_glyph(painter, index, x, y, size)
            painter.setPen(QPen(QColor("#FFFFFF"), 1))
            font = QFont()
            font.setPointSize(7)
            painter.setFont(font)
            painter.drawText(QRectF(x - size / 2, y + size / 2 - 18, size, 16), Qt.AlignCenter, title)
            painter.setOpacity(1.0)

        breath = 1.0 + 0.016 * math.sin(self._phase * 2.1)
        capsule_w = (210.0 + 40.0 * self.progress) * breath
        capsule_h = 68.0 * breath
        rect = QRectF(cx - capsule_w / 2, cy - capsule_h / 2, capsule_w, capsule_h)
        capsule = QLinearGradient(rect.left(), rect.top(), rect.right(), rect.bottom())
        capsule.setColorAt(0, QColor("#1C2748"))
        capsule.setColorAt(0.55, QColor("#342063"))
        capsule.setColorAt(1, QColor("#0F4652"))
        painter.setBrush(QBrush(capsule))
        painter.setPen(QPen(QColor(255, 255, 255, 38), 1))
        painter.drawRoundedRect(rect, capsule_h / 2, capsule_h / 2)

        self._draw_logo(painter, rect.left() + 32, cy)
        painter.setPen(QPen(QColor("#FFFFFF"), 1))
        title_font = QFont()
        title_font.setPointSize(11)
        title_font.setBold(True)
        painter.setFont(title_font)
        painter.drawText(
            QRectF(rect.left() + 58, cy - 20, rect.width() - 78, 20),
            Qt.AlignLeft | Qt.AlignVCenter, f"Gauto · {TABS[self.active]}",
        )
        status_font = QFont()
        status_font.setPointSize(8)
        painter.setFont(status_font)
        painter.setPen(QPen(QColor(255, 255, 255, 165), 1))
        painter.drawText(
            QRectF(rect.left() + 58, cy + 2, rect.width() - 78, 16),
            Qt.AlignLeft | Qt.AlignVCenter, self.status,
        )
        painter.setBrush(QBrush(QColor(accent.red(), accent.green(), accent.blue(), 220)))
        painter.setPen(Qt.NoPen)
        painter.drawEllipse(QPointF(rect.right() - 16, cy), 5, 5)
        painter.end()

    def _draw_logo(self, painter: QPainter, x: float, y: float):
        """Анимированный логотип: капсула + дышащие лепестки."""
        first = QColor(self.accents[0])
        second = QColor(self.accents[1])
        for i in range(5):
            angle = math.radians(-64 + i * 32)
            wave = (math.sin(self._phase * 2.4 + i * 0.8) + 1) / 2
            reach = 13 + 6 * wave
            px = x + math.sin(angle) * reach * 0.7
            py = y - math.cos(angle) * reach * 0.7
            color = QColor(self.accents[i])
            painter.setBrush(QBrush(QColor(color.red(), color.green(), color.blue(), int(140 + 100 * wave))))
            painter.setPen(Qt.NoPen)
            painter.drawEllipse(QPointF(px, py), 3 + 1.6 * wave, 3 + 1.6 * wave)
        grad = QLinearGradient(x - 16, y, x + 16, y)
        grad.setColorAt(0, first)
        grad.setColorAt(1, second)
        painter.setBrush(QBrush(grad))
        painter.setPen(Qt.NoPen)
        painter.drawRoundedRect(QRectF(x - 16, y - 8, 32, 16), 8, 8)
        pulse = (math.sin(self._phase * 4.2) + 1) / 2
        painter.setBrush(QBrush(QColor(255, 255, 255, int(190 + 60 * pulse))))
        painter.drawEllipse(QPointF(x, y), 3 + pulse, 3 + pulse)

    @staticmethod
    def _draw_glyph(painter: QPainter, index: int, x: float, y: float, size: float):
        s = size
        painter.setBrush(Qt.NoBrush)
        painter.setPen(QPen(QColor("#FFFFFF"), max(1.6, s * 0.055)))
        if index == 0:
            painter.drawRoundedRect(QRectF(x - s * 0.18, y - s * 0.15, s * 0.36, s * 0.30), 5, 5)
            painter.drawEllipse(QPointF(x, y + s * 0.04), s * 0.045, s * 0.045)
        elif index == 1:
            for k in range(3):
                yy = y - s * 0.12 + k * s * 0.11
                painter.drawLine(QPointF(x - s * 0.17, yy), QPointF(x + s * (0.17 - k * 0.04), yy))
        elif index == 2:
            painter.drawLine(QPointF(x - s * 0.17, y), QPointF(x - s * 0.04, y + s * 0.13))
            painter.drawLine(QPointF(x - s * 0.04, y + s * 0.13), QPointF(x + s * 0.18, y - s * 0.14))
        elif index == 3:
            painter.drawEllipse(QPointF(x, y), s * 0.17, s * 0.17)
            painter.drawLine(QPointF(x, y), QPointF(x, y - s * 0.11))
            painter.drawLine(QPointF(x, y), QPointF(x + s * 0.09, y + s * 0.04))
        else:
            painter.drawEllipse(QPointF(x, y), s * 0.16, s * 0.16)
            painter.drawEllipse(QPointF(x, y), s * 0.05, s * 0.05)
