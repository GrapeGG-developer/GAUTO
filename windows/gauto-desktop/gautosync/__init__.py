"""LAN-синхронизация Gauto Desktop ⇄ Gauto на телефоне."""
from .protocol import PORT, DISCOVERY_PORT, DISCOVER_MAGIC, HERE_MAGIC, PIN_HEADER, SCHEMA, State, merge, parse_here
from .client import Client
from .discovery import discover

__all__ = [
    "PORT", "DISCOVERY_PORT", "DISCOVER_MAGIC", "HERE_MAGIC", "PIN_HEADER", "SCHEMA",
    "State", "merge", "parse_here", "Client", "discover",
]
