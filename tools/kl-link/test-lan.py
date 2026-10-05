#!/usr/bin/env python3
"""Exercise actual emulation and TCP with two isolated libretro processes."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile

core, fixture, runner = map(lambda p: str(Path(p).resolve()), sys.argv[1:])
with tempfile.TemporaryDirectory(prefix="kl-link-test-") as scratch:
    root = Path(scratch)
    jobs = []
    logs = []
    try:
        for role in (0, 1):
            folder = root / str(role)
            folder.mkdir()
            (folder / "kl-link-session.cfg").write_text(f"{role} 127.0.0.1 {'0123456789abcdef'*2}\n")
            log = root / f"{role}.log"
            stream = log.open("w")
            jobs.append(subprocess.Popen([runner, core, str(folder), fixture], stderr=stream))
            stream.close()
            logs.append(log)
        for process in jobs:
            if process.wait(timeout=50):
                raise RuntimeError("Frontend exited unsuccessfully")
        for role, log in enumerate(logs):
            text = log.read_text()
            if "GBA Wi-Fi Link ready:" not in text:
                raise RuntimeError(f"P{role}: no ready confirmation\n{text}")
            rows = re.findall(r"periodic fixture P\d status=[0-9a-f]+/[0-9a-f]+ transfers=(\d+)/(\d+) errors=(\d+)/(\d+) timeouts=(\d+)/(\d+)", text)
            if not rows or not any(int(a) > 200 and int(b) > 200 and c == d == e == f == "0" for a,b,c,d,e,f in rows):
                raise RuntimeError(f"P{role}: cable transfer verification failed\n{text}")
            reasons = re.findall(r"failure schema=\d .*?reason=(\d+)", text)
            # User stop, unload and the opposite frontend stopping are expected at teardown.
            if any(int(reason) not in (10, 13, 14) for reason in reasons):
                raise RuntimeError(f"P{role}: unexpected session failure\n{text}")
            print(f"P{role}: real TCP cable, >200 transfers, zero data errors/timeouts")
    finally:
        for process in jobs:
            if process.poll() is None:
                process.kill()
                process.wait()
