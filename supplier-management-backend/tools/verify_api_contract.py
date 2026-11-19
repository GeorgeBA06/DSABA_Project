"""Deterministic checks for HTTP surface and public DTOs; does not replace integration tests."""

from pathlib import Path
import json, re, sys

root = Path(__file__).resolve().parents[1]
java = root / "src/main/java/by/bsuir/supplier"
controllers = list((java / "controller").glob("*Controller.java"))
spec = json.loads((root / "docs/openapi.json").read_text())
ops = [
    (path, method, op)
    for path, methods in spec["paths"].items()
    for method, op in methods.items()
]
assert len(ops) == len({(path, method) for path, method, _ in ops})
for path, method, op in ops:
    assert path.startswith("/api/v1/"), (path, method)
    if path in ("/api/v1/auth/login", "/api/v1/auth/refresh"):
        assert op["security"] == []
    else:
        assert op["security"] == [{"bearerAuth": []}], (path, method)
for op in ops:
    text = json.dumps(op[2], ensure_ascii=False)
    for ref in re.findall(r"#/components/schemas/(\w+)", text):
        assert ref in spec["components"]["schemas"], ref
view = "\n".join(p.read_text() for p in (java / "dto/response").glob("*.java"))
requests = "\n".join(p.read_text() for p in (java / "dto/request").glob("*.java"))
for secret in (
    "passwordHash",
    "refreshTokenHash",
    "tokenGeneration",
    "failedLoginCount",
    "lockedUntil",
):
    assert secret not in view, secret
assert not re.search(r"\brecord\s+\w+\([^)]*\bactorId\b", requests)
for p in controllers:
    data = p.read_text()
    assert not re.search(
        r"public\s+(?:by\.bsuir\.supplier\.entity\.[\w.]+|User|Supplier|Contract|PurchaseOrder|SupplierRating)\s+\w+\s*\(",
        data,
    ), p
assert len(ops) >= 75
print(
    "PASS: %d HTTP operations, %d paths, all security requirements and schema refs consistent"
    % (len(ops), len(spec["paths"]))
)
print(
    "PASS: public response models exclude password/token hashes and user-supplied actor IDs"
)
