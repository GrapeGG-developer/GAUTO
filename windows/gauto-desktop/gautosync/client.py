"""HTTP-клиент к телефону Gauto (только локальная сеть)."""
from __future__ import annotations

import json
import urllib.error
import urllib.request
from typing import Optional

from .protocol import PIN_HEADER, PORT, State


class Client:
    def __init__(self, host: str, port: int = PORT, pin: str = "", timeout: float = 3.0):
        self.host = host.strip()
        self.port = int(port)
        self.pin = pin.strip()
        self.timeout = timeout

    def url(self, path: str) -> str:
        return f"http://{self.host}:{self.port}{path}"

    def _request(self, path: str, method: str = "GET", body: str = "") -> str:
        data = body.encode("utf-8") if body else None
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            PIN_HEADER: self.pin,
        }
        req = urllib.request.Request(self.url(path), data=data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=self.timeout) as response:
            return response.read().decode("utf-8")

    def ping(self) -> bool:
        try:
            return json.loads(self._request("/api/ping")).get("ok") is True
        except (urllib.error.URLError, ValueError, OSError):
            return False

    def get_state(self) -> Optional[State]:
        try:
            return State.from_json(self._request("/api/state"))
        except (urllib.error.URLError, ValueError, OSError):
            return None

    def push_state(self, state: State) -> Optional[State]:
        try:
            return State.from_json(self._request("/api/state", "POST", state.fresh().to_json()))
        except (urllib.error.URLError, ValueError, OSError):
            return None
