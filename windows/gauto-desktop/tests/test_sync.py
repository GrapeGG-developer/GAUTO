"""Тесты протокола синхронизации Gauto Desktop (без Qt и без сети)."""
from __future__ import annotations

import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from gautosync import (  # noqa: E402
    DISCOVERY_PORT, DISCOVER_MAGIC, HERE_MAGIC, PIN_HEADER, PORT, SCHEMA,
    State, merge, parse_here,
)
from gautosync.client import Client  # noqa: E402
from gautosync.protocol import here  # noqa: E402


class ProtocolTest(unittest.TestCase):
    def test_constants_match_android(self):
        self.assertEqual(1, SCHEMA)
        self.assertEqual(8765, PORT)
        self.assertEqual(8766, DISCOVERY_PORT)
        self.assertEqual("GAUTO_DISCOVER_v1", DISCOVER_MAGIC)
        self.assertEqual("GAUTO_HERE_v1", HERE_MAGIC)
        self.assertEqual("X-Gauto-Pin", PIN_HEADER)

    def test_state_round_trip_uses_same_field_names_as_kotlin(self):
        state = State(device="Gauto phone", updatedAt=1234, rules=[{"id": "a", "name": "дела"}])
        raw = state.to_json()
        for key in ("schema", "app", "device", "updatedAt", "rules", "habits", "sessions", "settings", "events"):
            self.assertIn(f'"{key}"', raw)
        back = State.from_json(raw)
        self.assertIsNotNone(back)
        self.assertEqual("Gauto phone", back.device)
        self.assertEqual(1234, back.updatedAt)
        self.assertEqual("дела", back.rules[0]["name"])

    def test_garbage_and_foreign_schema_rejected(self):
        self.assertIsNone(State.from_json("просто текст"))
        self.assertIsNone(State.from_json('{"schema":99}'))
        self.assertIsNone(State.from_json("[]"))

    def test_newer_state_wins(self):
        local = State(updatedAt=100, device="телефон")
        newer = State(updatedAt=200, device="ПК")
        older = State(updatedAt=50, device="ПК")
        self.assertEqual("ПК", merge(local, newer).device)
        self.assertEqual("телефон", merge(local, older).device)
        self.assertEqual("телефон", merge(local, State(updatedAt=100, device="ПК")).device)

    def test_discovery_parsing(self):
        msg = here(port=8765, device="Gauto|phone")
        parsed = parse_here(msg)
        self.assertIsNotNone(parsed)
        self.assertEqual(8765, parsed["port"])
        self.assertEqual("Gauto phone", parsed["device"])
        self.assertIsNone(parse_here("чужой|1|1|dev"))
        self.assertIsNone(parse_here("GAUTO_HERE_v1|abc|1|dev"))
        self.assertIsNone(parse_here("GAUTO_HERE_v1|8765|99|dev"))
        self.assertIsNone(parse_here("GAUTO_HERE_v1|70000|1|dev"))

    def test_client_urls_and_pin_header(self):
        client = Client("192.168.1.42", pin="1234")
        self.assertEqual("http://192.168.1.42:8765/api/state", client.url("/api/state"))
        self.assertEqual("1234", client.pin)
        self.assertEqual(3.0, client.timeout)


class SourceGuardTest(unittest.TestCase):
    def _read(self, path):
        with open(os.path.join(ROOT, path), encoding="utf-8") as handle:
            return handle.read()

    def test_desktop_has_island_animations_tray_and_autostart(self):
        island = self._read("ui/island.py")
        for marker in ("paintEvent", "QTimer", "tabSelected = Signal", "drawRoundedRect", "QRadialGradient"):
            self.assertIn(marker, island)
        window = self._read("ui/main_window.py")
        for marker in ("QSystemTrayIcon", "CurrentVersion\\\\Run", "IslandWidget", "SyncWorker", "_pull", "_push"):
            self.assertIn(marker, window)
        self.assertIn("Gauto", self._read("app.py"))
        self.assertIn("PySide6", self._read("requirements.txt"))

    def test_no_cloud_no_tokens(self):
        for path in ("gautosync/client.py", "gautosync/protocol.py", "ui/main_window.py", "app.py"):
            text = self._read(path)
            self.assertNotIn("ghp_", text)
            self.assertNotIn("github_pat_", text)
            self.assertNotIn("api.github.com", text)


if __name__ == "__main__":
    unittest.main(verbosity=2)
