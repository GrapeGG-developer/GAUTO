"""UDP-поиск телефона Gauto в локальной сети."""
from __future__ import annotations

import socket
import time
from typing import Dict, List

from .protocol import DISCOVERY_PORT, DISCOVER_MAGIC, parse_here


def discover(timeout: float = 2.2, port: int = DISCOVERY_PORT) -> List[Dict[str, str]]:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    sock.settimeout(0.45)
    payload = DISCOVER_MAGIC.encode("utf-8")
    for target in ("255.255.255.255", "<broadcast>"):
        try:
            sock.sendto(payload, (target, port))
        except OSError:
            continue

    found: Dict[str, Dict[str, str]] = {}
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            data, addr = sock.recvfrom(256)
        except socket.timeout:
            continue
        except OSError:
            break
        info = parse_here(data.decode("utf-8", "replace"))
        if info:
            found[addr[0]] = {
                "host": addr[0],
                "port": str(info["port"]),
                "device": info["device"],
            }
    sock.close()
    return list(found.values())
