#!/usr/bin/env python3
"""Verifies Java API contracts and layout after the DTO/View extraction.

This is a *static* structural/contract regression check. It is NOT a replacement
for the Maven build, Jackson serialization tests, or HTTP/PostgreSQL integration.
"""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
base = root / "src/main/java/by/bsuir/supplier"
requests = sorted((base / "dto/request").glob("*.java"))
responses = sorted((base / "dto/response").glob("*.java"))
mappers = sorted((base / "mapper").glob("*ViewMapper.java"))

assert len(requests) == 40, ("Request record count", len(requests))
assert len(responses) == 34, ("Response record count", len(responses))
assert len(mappers) == 9, ("Mapper count", len(mappers))
assert not (base / "dto/Requests.java").exists()
assert not (base / "dto/Views.java").exists()

for path in requests + responses:
    source = path.read_text(encoding="utf-8")
    declarations = re.findall(r"\bpublic record\s+(\w+)\s*\(", source)
    assert declarations == [path.stem], ("Record must match filename", path, declarations)
    assert source.startswith("package by.bsuir.supplier.dto."), path
    assert "@Entity" not in source, path

for path in mappers:
    source = path.read_text(encoding="utf-8")
    assert "public final class " + path.stem in source, path
    assert "static " in source and " toView(" in source, path

for path in base.rglob("*.java"):
    source = path.read_text(encoding="utf-8")
    assert not re.search(r"\b(?:Views|Requests)\.", source), path

baseline = json.loads((root / "docs/OPENAPI_CONTRACT_BASELINE_STAGE4.json").read_text())
current = json.loads((root / "docs/openapi.json").read_text())
assert baseline == current, "OpenAPI changed; inspect potential frontend compatibility break"
operations = sum(len(verbs) for verbs in current["paths"].values())
assert operations == 97, operations

print("PASS: 40 request DTOs + 34 response DTOs, one record per Java file")
print("PASS: 9 module mappers, no monolithic Requests/Views container")
print("PASS: exactly unchanged OpenAPI contract for all 97 operations")
