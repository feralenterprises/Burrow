#!/usr/bin/env python3
"""Reconstitute exact R02 source ZIP without touching older repo trees."""
from pathlib import Path
from io import BytesIO
import hashlib
import zipfile

HERE = Path(__file__).resolve().parent
parts = sorted((HERE / "parts").glob("part-*.bin"))
if len(parts) != 20:
    raise SystemExit(f"Expected 20 source pieces; found {len(parts)}")
payload = b"".join(p.read_bytes() for p in parts)
expected_size = 3560073
expected_sha = "adf1d64fdb3d39b003deadff4e6ae312c27667a3b12f3a251037f14018437653"
actual_sha = hashlib.sha256(payload).hexdigest()
if len(payload) != expected_size or actual_sha != expected_sha:
    raise SystemExit(f"R02 source integrity failure: bytes={len(payload)}, sha256={actual_sha}")
output = HERE / "R02-source-extracted"
if output.exists():
    raise SystemExit(f"Refusing to overwrite existing directory: {output}")
with zipfile.ZipFile(BytesIO(payload)) as archive:
    bad = archive.testzip()
    if bad:
        raise SystemExit(f"Corrupt ZIP entry: {bad}")
    for item in archive.infolist():
        path = Path(item.filename)
        if path.is_absolute() or ".." in path.parts:
            raise SystemExit(f"Unsafe ZIP path: {item.filename}")
    output.mkdir(parents=True)
    archive.extractall(output)
print(f"Verified exact R02 ZIP: {len(payload)} bytes, SHA-256 {actual_sha}")
print(f"Extracted to: {output}")
print(f"Gradle project: {output / 'Project-Burrow'}")
