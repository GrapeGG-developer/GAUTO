"""Главное окно Gauto Desktop: тот же «островок», трей и LAN-синхронизация."""
from __future__ import annotations

import os
import sys
from typing import Optional

from PySide6.QtCore import Qt, QSettings, QThread, QTimer, Signal
from PySide6.QtGui import QAction, QColor, QFont, QIcon, QPalette
from PySide6.QtWidgets import (
    QApplication, QCheckBox, QComboBox, QHBoxLayout, QHeaderView, QLabel,
    QLineEdit, QMainWindow, QMenu, QPushButton, QStackedWidget, QSystemTrayIcon,
    QTableWidget, QTableWidgetItem, QVBoxLayout, QWidget,
)

from gautosync import Client, PORT, State, discover
from .island import TABS, IslandWidget
from .theme import system_accents

DARK_BG = "#070B18"
CARD_BG = "#121B36"
TEXT = "#F4F4FB"
MUTED = "#A9B4D0"


class SyncWorker(QThread):
    finishedState = Signal(object)

    def __init__(self, host: str, port: int, pin: str, parent=None):
        super().__init__(parent)
        self.host, self.port, self.pin = host, port, pin

    def run(self):
        state = Client(self.host, self.port, self.pin).get_state()
        self.finishedState.emit(state)


class MainWindow(QMainWindow):
    def __init__(self, icon_path: str):
        super().__init__()
        self.icon_path = icon_path
        self.setWindowTitle("Gauto · автоматизация личной жизни")
        self.setWindowIcon(QIcon(icon_path))
        self.resize(940, 720)
        self.setMinimumSize(760, 600)
        self.state: Optional[State] = None
        self.worker: Optional[SyncWorker] = None
        self._apply_palette()

        root = QWidget()
        self.setCentralWidget(root)
        layout = QVBoxLayout(root)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        layout.addWidget(self._build_top_bar())
        self.stack = QStackedWidget()
        for index in range(5):
            self.stack.addWidget(self._build_page(index))
        layout.addWidget(self.stack, 1)

        self.island = IslandWidget()
        self.island.tabSelected.connect(self._select_tab)
        self.island.toggled.connect(lambda expanded: self.statusBar().showMessage(
            "остров раскрыт — выберите категорию" if expanded else "", 2000
        ))
        island_row = QHBoxLayout()
        island_row.addStretch(1)
        island_row.addWidget(self.island)
        island_row.addStretch(1)
        layout.addLayout(island_row)

        self._build_tray()
        self.sync_timer = QTimer(self)
        self.sync_timer.timeout.connect(self._pull)
        self.sync_timer.start(6000)
        self.statusBar().showMessage("Gauto Desktop · локальная сеть, без облака")

    # ---------- оформление ----------

    def _apply_palette(self):
        palette = QPalette()
        palette.setColor(QPalette.Window, QColor(DARK_BG))
        palette.setColor(QPalette.WindowText, QColor(TEXT))
        palette.setColor(QPalette.Base, QColor(CARD_BG))
        palette.setColor(QPalette.AlternateBase, QColor("#1B2745"))
        palette.setColor(QPalette.Text, QColor(TEXT))
        palette.setColor(QPalette.Button, QColor(CARD_BG))
        palette.setColor(QPalette.ButtonText, QColor(TEXT))
        palette.setColor(QPalette.Highlight, QColor(system_accents()[0]))
        palette.setColor(QPalette.HighlightedText, QColor(TEXT))
        QApplication.instance().setPalette(palette)

    def _build_top_bar(self) -> QWidget:
        bar = QWidget()
        bar.setFixedHeight(62)
        bar.setStyleSheet(f"background:{CARD_BG}; border-bottom:1px solid #26345A;")
        row = QHBoxLayout(bar)
        row.setContentsMargins(16, 8, 16, 8)
        logo = QLabel()
        logo.setPixmap(QIcon(self.icon_path).pixmap(34, 34))
        row.addWidget(logo)
        title = QLabel("Gauto")
        font = QFont()
        font.setPointSize(15)
        font.setBold(True)
        title.setFont(font)
        row.addWidget(title)
        self.top_subtitle = QLabel("· Главная")
        self.top_subtitle.setStyleSheet(f"color:{MUTED};")
        row.addWidget(self.top_subtitle)
        row.addStretch(1)
        search = QPushButton("поиск")
        search.setFlat(True)
        search.clicked.connect(self._focus_search)
        row.addWidget(search)
        self.bell = QPushButton("уведомления · 0")
        self.bell.setFlat(True)
        self.bell.clicked.connect(self._show_events)
        row.addWidget(self.bell)
        return bar

    def _card(self, title: str, subtitle: str = "") -> QWidget:
        card = QWidget()
        card.setStyleSheet(
            f"background:{CARD_BG}; border:1px solid #26345A; border-radius:18px;"
        )
        box = QVBoxLayout(card)
        box.setContentsMargins(18, 14, 18, 14)
        head = QLabel(title)
        font = QFont()
        font.setPointSize(12)
        font.setBold(True)
        head.setFont(font)
        box.addWidget(head)
        if subtitle:
            sub = QLabel(subtitle)
            sub.setStyleSheet(f"color:{MUTED};")
            sub.setWordWrap(True)
            box.addWidget(sub)
        return card

    def _build_page(self, index: int) -> QWidget:
        page = QWidget()
        box = QVBoxLayout(page)
        box.setContentsMargins(24, 18, 24, 8)
        box.setSpacing(12)

        if index == 0:
            box.addWidget(self._card("Gauto · главная",
                "Правила, привычки и фокус-сессии синхронизируются с телефоном по локальной сети."))
            row = QHBoxLayout()
            self.level_label = QLabel("уровень 1 · 0 xp")
            self.rule_label = QLabel("активное правило: —")
            self.streak_label = QLabel("стрик 0 дн.")
            for label in (self.level_label, self.rule_label, self.streak_label):
                label.setStyleSheet(f"background:{CARD_BG}; border-radius:14px; padding:12px; color:{TEXT};")
                row.addWidget(label)
            box.addLayout(row)
            hint = self._card("не стопайся",
                "Домашние дела закрывают YouTube, сон делает экран монохромным, "
                "фокус-сессия держит блокировки. Всё это настраивается на телефоне "
                "и приезжает сюда само.")
            box.addWidget(hint)
        elif index == 1:
            box.addWidget(self._card("правила", "Дни, время, яркость, громкость, блокировки и ч/б слой."))
            self.rules_table = QTableWidget(0, 5)
            self.rules_table.setHorizontalHeaderLabels(["время", "название", "яркость", "громкость", "вкл"])
            self.rules_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
            box.addWidget(self.rules_table, 1)
        elif index == 2:
            box.addWidget(self._card("привычки", "Стрики, недельная цель и очки к уровню разработчика."))
            self.habits_table = QTableWidget(0, 4)
            self.habits_table.setHorizontalHeaderLabels(["привычка", "стрик", "неделя", "xp"])
            self.habits_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
            box.addWidget(self.habits_table, 1)
        elif index == 3:
            box.addWidget(self._card("фокус", "Сессии Pomodoro с блокировкой отвлекающих приложений."))
            self.focus_label = QLabel("завершённых сессий: 0")
            box.addWidget(self.focus_label)
            self.focus_table = QTableWidget(0, 3)
            self.focus_table.setHorizontalHeaderLabels(["сессия", "минут", "статус"])
            self.focus_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
            box.addWidget(self.focus_table, 1)
        else:
            box.addWidget(self._card("синхронизация с ПК",
                "Телефон поднимает локальный сервер:8765 и отвечает на UDP-поиск:8766. "
                "Облако, аккаунт и токен не нужны."))
            form = QWidget()
            form.setStyleSheet(f"background:{CARD_BG}; border-radius:18px;")
            fbox = QVBoxLayout(form)
            fbox.setContentsMargins(18, 14, 18, 14)
            self.host_edit = QLineEdit()
            self.host_edit.setPlaceholderText("IP телефона, например 192.168.1.42")
            fbox.addWidget(QLabel("адрес телефона"))
            fbox.addWidget(self.host_edit)
            self.pin_edit = QLineEdit()
            self.pin_edit.setPlaceholderText("PIN из настроек Gauto на телефоне")
            fbox.addWidget(QLabel("PIN"))
            fbox.addWidget(self.pin_edit)
            buttons = QHBoxLayout()
            find_btn = QPushButton("найти телефон")
            find_btn.clicked.connect(self._discover)
            buttons.addWidget(find_btn)
            connect_btn = QPushButton("подключиться")
            connect_btn.clicked.connect(self._pull)
            buttons.addWidget(connect_btn)
            push_btn = QPushButton("отправить на телефон")
            push_btn.clicked.connect(self._push)
            buttons.addWidget(push_btn)
            fbox.addLayout(buttons)
            self.sync_status = QLabel("не подключено")
            self.sync_status.setStyleSheet(f"color:{MUTED};")
            fbox.addWidget(self.sync_status)
            self.autostart = QCheckBox("запускать вместе с Windows")
            self.autostart.stateChanged.connect(self._set_autostart)
            fbox.addWidget(self.autostart)
            self.autostart.setChecked(self._autostart_enabled())
            box.addWidget(form)
            box.addWidget(self._card("обновления",
                "Android-приложение Gauto обновляется само из репозитория "
                "GrapeGG-developer/GAUTO. ПК-клиент обновляется из исходников."))
        box.addStretch(1)
        return page

    # ---------- остров ----------

    def _select_tab(self, index: int):
        self.stack.setCurrentIndex(index)
        self.island.set_active(index)
        self.top_subtitle.setText("· " + TABS[index][0])

    def _focus_search(self):
        self._select_tab(1)

    def _show_events(self):
        events = self.state.events[-5:] if self.state else []
        self.statusBar().showMessage(" · ".join(events) if events else "событий пока нет", 6000)

    # ---------- синхронизация ----------

    def _client(self) -> Optional[Client]:
        host = self.host_edit.text().strip()
        if not host:
            self.sync_status.setText("сначала найдите телефон или введите IP")
            return None
        return Client(host, PORT, self.pin_edit.text().strip())

    def _discover(self):
        found = discover()
        if not found:
            self.sync_status.setText("телефон не найден — проверьте, что включён доступ с ПК и общий Wi-Fi")
            return
        item = found[0]
        self.host_edit.setText(item["host"])
        self.sync_status.setText(f"найден {item['device']} · {item['host']}:{item['port']}")
        self._pull()

    def _pull(self):
        client = self._client()
        if client is None or (self.worker is not None and self.worker.isRunning()):
            return
        self.worker = SyncWorker(client.host, client.port, client.pin, self)
        self.worker.finishedState.connect(self._on_state)
        self.worker.start()

    def _on_state(self, state):
        if state is None:
            self.sync_status.setText("нет связи — проверьте IP, PIN и доступ с ПК")
            return
        self.state = state
        self._render(state)
        self.sync_status.setText(f"синхронизировано · {state.device} · правил {len(state.rules)}")
        settings = state.settings or {}
        xp = int(settings.get("xp", 0) or 0)
        level = 1 + int((max(xp, 0) / 50.0) ** 0.5)
        self.island.set_statuses([
            f"{len(state.rules)} правил · {len(state.habits)} привычек",
            f"уровень {level} · {xp} xp",
            f"стрик {int(settings.get('streak', 0) or 0)} дн.",
            f"фокус-сессий: {sum(1 for s in state.sessions if s.get('completed'))}",
            f"синхронизация: {state.device}",
        ])

    def _render(self, state: State):
        settings = state.settings or {}
        xp = int(settings.get("xp", 0) or 0)
        level = 1 + int((max(xp, 0) / 50.0) ** 0.5)
        self.level_label.setText(f"уровень {level} · {xp} xp")
        self.streak_label.setText(f"стрик {int(settings.get('streak', 0) or 0)} дн.")
        active = next((r for r in state.rules if r.get("enabled", True)), None)
        self.rule_label.setText(
            f"активное правило: {active.get('name', '—')}" if active else "активное правило: —"
        )
        self.bell.setText(f"уведомления · {len(state.events) % 10}")

        self.rules_table.setRowCount(len(state.rules))
        for row, rule in enumerate(state.rules):
            values = [
                f"{int(rule.get('hour', 0)):02d}:{int(rule.get('minute', 0)):02d}",
                str(rule.get("name", "")),
                f"{int(rule.get('brightness', 0))}%",
                f"{int(rule.get('volume', 0))}%",
                "да" if rule.get("enabled", True) else "нет",
            ]
            for col, value in enumerate(values):
                self.rules_table.setItem(row, col, QTableWidgetItem(value))

        self.habits_table.setRowCount(len(state.habits))
        for row, habit in enumerate(state.habits):
            done = habit.get("doneDates", []) or []
            values = [
                str(habit.get("title", "")), str(len(done)),
                f"{min(len(done), int(habit.get('targetPerWeek', 7) or 7))}/{habit.get('targetPerWeek', 7)}",
                str(habit.get("xp", 0)),
            ]
            for col, value in enumerate(values):
                self.habits_table.setItem(row, col, QTableWidgetItem(value))

        done_sessions = [s for s in state.sessions if s.get("completed")]
        self.focus_label.setText(f"завершённых сессий: {len(done_sessions)}")
        self.focus_table.setRowCount(min(len(state.sessions), 20))
        for row, session in enumerate(state.sessions[-20:]):
            values = [
                str(session.get("label", "")), str(session.get("minutes", 0)),
                "готово" if session.get("completed") else "в процессе",
            ]
            for col, value in enumerate(values):
                self.focus_table.setItem(row, col, QTableWidgetItem(value))

    def _push(self):
        client = self._client()
        if client is None or self.state is None:
            self.sync_status.setText("сначала подключитесь и получите состояние")
            return
        accepted = client.push_state(self.state)
        if accepted is None:
            self.sync_status.setText("не удалось отправить — проверьте PIN и доступ с ПК")
        else:
            self.state = accepted
            self.sync_status.setText("изменения отправлены на телефон")

    # ---------- трей и автозапуск ----------

    def _build_tray(self):
        self.tray = QSystemTrayIcon(QIcon(self.icon_path), self)
        self.tray.setToolTip("Gauto Desktop")
        menu = QMenu()
        open_action = QAction("открыть", self)
        open_action.triggered.connect(self.showNormal)
        menu.addAction(open_action)
        sync_action = QAction("синхронизировать", self)
        sync_action.triggered.connect(self._pull)
        menu.addAction(sync_action)
        quit_action = QAction("выход", self)
        quit_action.triggered.connect(QApplication.instance().quit)
        menu.addAction(quit_action)
        self.tray.setContextMenu(menu)
        self.tray.activated.connect(
            lambda reason: self.showNormal() if reason == QSystemTrayIcon.ActivationReason.Trigger else None
        )
        self.tray.show()

    def _autostart_key(self) -> QSettings:
        return QSettings(
            "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
            QSettings.Format.NativeFormat,
        )

    def _autostart_enabled(self) -> bool:
        return bool(self._autostart_key().value("Gauto", ""))

    def _set_autostart(self, state):
        settings = self._autostart_key()
        checked = int(state) != 0
        if checked:
            target = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app.py")
            settings.setValue("Gauto", f'"{sys.executable}" "{target}"')
        else:
            settings.remove("Gauto")

    def closeEvent(self, event):
        if QSystemTrayIcon.isSystemTrayAvailable():
            self.hide()
            self.tray.showMessage(
                "Gauto", "приложение работает в трее и продолжает синхронизацию",
                QSystemTrayIcon.MessageIcon.Information, 2500,
            )
            event.ignore()
        else:
            event.accept()
