"""Протокол Gauto: один JSON, без облака, аккаунта и токена.

Совместим с Kotlin-классом com.gauto.app.core.GautoState (те же имена полей).
"""
from __future__ import annotations

import json
import time
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List, Optional

SCHEMA = 1
PORT = 8765
DISCOVERY_PORT = 8766
DISCOVER_MAGIC = "GAUTO_DISCOVER_v1"
HERE_MAGIC = "GAUTO_HERE_v1"
PIN_HEADER = "X-Gauto-Pin"


@dataclass
class State:
    schema: int = SCHEMA
    app: str = "Gauto"
    device: str = "Gauto Desktop"
    updatedAt: int = 0
    rules: List[Dict[str, Any]] = field(default_factory=list)
    habits: List[Dict[str, Any]] = field(default_factory=list)
    sessions: List[Dict[str, Any]] = field(default_factory=list)
    settings: Dict[str, Any] = field(default_factory=dict)
    events: List[str] = field(default_factory=list)

    def to_json(self) -> str:
        return json.dumps(asdict(self), ensure_ascii=False)

    @classmethod
    def from_json(cls, text: str) -> Optional["State"]:
        try:
            raw = json.loads(text)
            if not isinstance(raw, dict):
                return None
            if int(raw.get("schema", 0)) != SCHEMA:
                return None
            return cls(
                schema=SCHEMA,
                app=str(raw.get("app", "Gauto")),
                device=str(raw.get("device", "")),
                updatedAt=int(raw.get("updatedAt", 0)),
                rules=list(raw.get("rules", []) or []),
                habits=list(raw.get("habits", []) or []),
                sessions=list(raw.get("sessions", []) or []),
                settings=dict(raw.get("settings", {}) or {}),
                events=[str(x) for x in (raw.get("events", []) or [])],
            )
        except (ValueError, TypeError):
            return None

    def fresh(self) -> "State":
        return State(
            schema=SCHEMA, app="Gauto", device=self.device,
            updatedAt=int(time.time() * 1000),
            rules=self.rules, habits=self.habits, sessions=self.sessions,
            settings=self.settings, events=self.events,
        )


def merge(local: State, remote: State) -> State:
    """Побеждает более свежее состояние; при равенстве остаётся локальное."""
    return remote if remote.updatedAt > local.updatedAt else local


def here(port: int = PORT, device: str = "Gauto phone", schema: int = SCHEMA) -> str:
    return f"{HERE_MAGIC}|{port}|{schema}|{device.replace('|', ' ')[:48]}"


def parse_here(message: str) -> Optional[Dict[str, Any]]:
    parts = (message or "").strip().split("|")
    if len(parts) < 4 or parts[0] != HERE_MAGIC:
        return None
    try:
        port = int(parts[1])
        schema = int(parts[2])
    except ValueError:
        return None
    if not (1 <= port <= 65535) or schema != SCHEMA:
        return None
    return {"port": port, "schema": schema, "device": parts[3]}
