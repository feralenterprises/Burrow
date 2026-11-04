#!/usr/bin/env python3
"""Reassemble exact input ZIPs from Git-tracked binary parts. Does not modify source."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parent
manifest = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
dest = ROOT / "recovered"
dest.mkdir(exist_ok=True)
for key, spec in manifest["archives"].items():
    digest = hashlib.sha256()
    output = dest / spec["filename"]
    if output.exists():
        existing = output.read_bytes()
        if len(existing) == spec["bytes"] and hashlib.sha256(existing).hexdigest() == spec["sha256"]:
            print(f"Already verified: {output}")
            continue
        raise SystemExit(f"Refusing to overwrite nonmatching file: {output}")
    data = bytearray()
    for part in spec["parts"]:
        path = ROOT / part["file"]
        raw = path.read_bytes()
        git_sha = hashlib.sha1(f"blob {len(raw)}".encode() + b"\x00" + raw).hexdigest()
        if git_sha != part["sha1_git_blob"]:
            raise SystemExit(f"Part integrity mismatch: {path}, observed {git_sha}")
        data.extend(raw)
    digest.update(data)
    observed = digest.hexdigest()
    if len(data) != spec["bytes"] or observed != spec["sha256"]:
        raise SystemExit(f"ZIP integrity mismatch {key}: {len(data)} bytes SHA-256 {observed}")
    output.write_bytes(data)
    with zipfile.ZipFile(output) as zf:
        if zf.testzip():
            output.unlink(missing_ok=True)
            raise SystemExit(f"Invalid ZIP entry inside {output}")
    print(f"Verified {key}: {output} (bytes {len(data)}, sha256 {observed})")
print("All three input archives recovered and verified. NO SOURCE FILES MODIFIED.")
