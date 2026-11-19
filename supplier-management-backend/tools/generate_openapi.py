from pathlib import Path
import re, json

p = Path(__file__).resolve().parents[1]
root = p / "src/main/java/by/bsuir/supplier"
controllers = root / "controller"
enums = {
    f.stem: re.findall(
        r"\b([A-Z][A-Z0-9_]+)\b", f.read_text().split("{", 1)[-1].split("}", 1)[0]
    )
    for f in (root / "entity/enums").glob("*.java")
}
schemas = {name: {"type": "string", "enum": values} for name, values in enums.items()}


def sep_fields(s):
    out = []
    current = ""
    level = 0
    for c in s:
        if c in "(<":
            level += 1
        if c in ")>":
            level -= 1
        if c == "," and level == 0:
            out.append(current.strip())
            current = ""
            continue
        current += c
    if current.strip():
        out.append(current.strip())
    return out


def type_schema(t):
    if t in ("String", "UUID", "Instant", "LocalDate", "LocalDateTime"):
        x = {"type": "string"}
        if t == "UUID":
            x["format"] = "uuid"
        if t == "Instant":
            x["format"] = "date-time"
        if t == "LocalDate":
            x["format"] = "date"
        return x
    if t in ("Integer", "Long", "int", "long"):
        return {
            "type": "integer",
            "format": "int64" if t in ("Long", "long") else "int32",
        }
    if t in ("boolean", "Boolean"):
        return {"type": "boolean"}
    if t == "BigDecimal":
        return {"type": "number", "format": "decimal"}
    if t.startswith("List<") or t.startswith("Set<"):
        return {"type": "array", "items": type_schema(t[t.find("<") + 1 : -1].strip())}
    if t in enums:
        return {"type": "string", "enum": enums[t]}
    if t.endswith("Request") and t in request_names:
        return {"$ref": "#/components/schemas/" + t[:-7]}
    if t in schemas or t in view_names:
        return {"$ref": "#/components/schemas/" + t}
    return {"type": "string"}


# DTOs are independent Java records. Preserve the public OpenAPI schema names
# from the previous version (e.g. `Supplier`, not `SupplierRequest`).
reqs = []
views = []
for file in sorted((root / "dto/request").glob("*Request.java")):
    reqs.extend(
        re.finditer(r"public record (\w+)\(([\s\S]*?)\)\s*\{\}", file.read_text())
    )
for file in sorted((root / "dto/response").glob("*.java")):
    views.extend(
        re.finditer(r"public record (\w+)\(([\s\S]*?)\)\s*\{\}", file.read_text())
    )
request_names = {m.group(1) for m in reqs}
view_names = {m.group(1) for m in views}
for m in reqs + views:
    name = m.group(1)
    schema_name = name[:-7] if name in request_names else name
    fields = sep_fields(m.group(2))
    props = {}
    required = []
    for field in fields:
        fld = field.replace("\n", " ").strip()
        must = any(a in fld for a in ("@NotBlank", "@NotNull", "@NotEmpty"))
        fld = re.sub(r'@\w+(?:\("[^"]*"\)|\(\d+\))?\s*', "", fld)
        match = re.match(r"(.+?)\s+(\w+)$", fld)
        if not match:
            continue
        typ, key = match.groups()
        typ = typ.strip()
        props[key] = type_schema(typ)
        if must:
            required.append(key)
    schemas[schema_name] = {"type": "object", "properties": props}
    if required:
        schemas[schema_name]["required"] = required
schemas["MeView"] = {
    "type": "object",
    "properties": {
        "user": {"$ref": "#/components/schemas/UserView"},
        "roles": {"type": "array", "items": {"$ref": "#/components/schemas/RoleCode"}},
    },
}
schemas["Tokens"] = {
    "type": "object",
    "properties": {
        "accessToken": {"type": "string"},
        "refreshToken": {"type": "string"},
        "tokenType": {"type": "string"},
        "expiresInSeconds": {"type": "integer"},
    },
    "required": ["accessToken", "refreshToken", "tokenType", "expiresInSeconds"],
}
schemas["ErrorBody"] = {
    "type": "object",
    "properties": {
        "code": {"type": "string"},
        "message": {"type": "string"},
        "timestamp": {"type": "string", "format": "date-time"},
    },
}
schemas["PageResponse"] = {
    "type": "object",
    "properties": {
        "items": {"type": "array", "items": {}},
        "totalElements": {"type": "integer"},
        "totalPages": {"type": "integer"},
        "page": {"type": "integer"},
        "size": {"type": "integer"},
    },
}
schemas["SupplierReportRow"] = {
    "type": "object",
    "properties": {
        "supplierId": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "status": {"type": "string"},
        "latestScore": {"type": "number"},
        "band": {"type": "string"},
        "ratingState": {"type": "string"},
    },
}
schemas["BusinessDashboard"] = {
    "type": "object",
    "properties": {
        key: {"type": "integer", "format": "int64"}
        for key in (
            "suppliers",
            "products",
            "contracts",
            "orders",
            "deliveries",
            "claims",
            "ratings",
        )
    },
}
schemas["AdminDashboard"] = {
    "type": "object",
    "properties": {"users": {"type": "integer", "format": "int64"}},
}
schemas["OfferView"] = {
    "type": "object",
    "properties": {
        "supplierId": {"type": "string", "format": "uuid"},
        "productId": {"type": "string", "format": "uuid"},
        "supplierItemCode": {"type": "string"},
        "active": {"type": "boolean"},
    },
}
paths = {}
for f in sorted(controllers.glob("*Controller.java")):
    src = f.read_text()
    baseMatch = re.search(r'@RequestMapping\("([^"]+)"\)', src)
    if not baseMatch:
        continue
    base = baseMatch.group(1)
    for m in re.finditer(r"@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^)]*)\))?", src):
        method = m.group(1).lower()
        ann = m.group(2) or ""
        pathmatch = re.search(r'"([^\"]+)"', ann)
        suffix = pathmatch.group(1) if pathmatch else ""
        if suffix.startswith("/") or suffix == "":
            path = base + suffix
        else:
            path = base + "/" + suffix
        following = src[m.end() :]
        sig = re.search(r"public\s+([\w.<>,?\[\] ]+?)\s+(\w+)\s*\(", following)
        if not sig:
            print("Cannot parse", f, m.group(0))
            continue
        ret = sig.group(1).strip()
        name = sig.group(2)
        end_sig = following.find("{", sig.end())
        signature = following[:end_sig] if end_sig != -1 else following[:500]
        # skip anything beyond method start: signatures are short
        request = re.search(r"@RequestBody\s+(\w+Request)", signature)
        pms = []
        for var in re.findall(r"\{(\w+)\}", path):
            pms.append(
                {
                    "name": var,
                    "in": "path",
                    "required": True,
                    "schema": {
                        "type": "string",
                        "format": "uuid" if var in ("id", "orderId") else "string",
                    },
                }
            )
        if "@RequestParam" in signature:
            for q in ("page", "size"):
                if re.search(r"\bint\s+" + q + r"\b", signature):
                    pms.append(
                        {
                            "name": q,
                            "in": "query",
                            "required": False,
                            "schema": {
                                "type": "integer",
                                "minimum": 0 if q == "page" else 1,
                                "maximum": 100 if q == "size" else 10000000,
                            },
                        }
                    )

        def response_type(t):
            if t.startswith("PageResponse<"):
                child = t[len("PageResponse<") : -1].split(".")[-1]
                return {
                    "allOf": [{"$ref": "#/components/schemas/PageResponse"}],
                    "properties": {
                        "items": {
                            "type": "array",
                            "items": {"$ref": "#/components/schemas/" + child},
                        }
                    },
                }
            if t.startswith("List<"):
                child = t[len("List<") : -1].split(".")[-1]
                return {
                    "type": "array",
                    "items": {"$ref": "#/components/schemas/" + child},
                }
            if t in view_names:
                return {"$ref": "#/components/schemas/" + t}
            if t == "Tokens":
                return {"$ref": "#/components/schemas/Tokens"}
            if t == "MeView":
                return {"$ref": "#/components/schemas/MeView"}
            if t in (
                "BusinessDashboard",
                "AdminDashboard",
                "SupplierReportRow",
                "OfferView",
            ):
                return {"$ref": "#/components/schemas/" + t}
            if t == "void":
                return None
            return {"type": "object"}

        is_xlsx = "suppliers.xlsx" in path
        res = response_type(ret)
        responses = {}
        if ret == "void":
            responses["204"] = {"description": "Успешно, тело отсутствует"}
        else:
            code = (
                "201"
                if "@ResponseStatus(HttpStatus.CREATED)" in following[: sig.start()]
                else "200"
            )
            content = (
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                if is_xlsx
                else "application/json"
            )
            if is_xlsx:
                res = {"type": "string", "format": "binary"}
            responses[code] = {
                "description": "Успешно",
                "content": {content: {"schema": res}},
            }
        for code, desc in [
            ("400", "Некорректный запрос"),
            ("401", "Требуется вход"),
            ("403", "Недостаточно прав"),
            ("409", "Конфликт данных"),
            ("422", "Нарушение бизнес-правила"),
        ]:
            responses[code] = {
                "description": desc,
                "content": {
                    "application/json": {
                        "schema": {"$ref": "#/components/schemas/ErrorBody"}
                    }
                },
            }
        op = {
            "operationId": f.stem + "_" + name,
            "tags": [f.stem.replace("Controller", "")],
            "responses": responses,
            "security": []
            if path in ("/api/v1/auth/login", "/api/v1/auth/refresh")
            else [{"bearerAuth": []}],
        }
        if pms:
            op["parameters"] = pms
        if request:
            op["requestBody"] = {
                "required": True,
                "content": {
                    "application/json": {
                        "schema": {
                            "$ref": "#/components/schemas/" + request.group(1)[:-7]
                        }
                    }
                },
            }
        paths.setdefault(path, {})[method] = op
spec = {
    "openapi": "3.0.3",
    "info": {
        "title": "Supplier Management API",
        "description": "Минимальный API управления поставщиками; роли действуют на сервере, actorId берётся из JWT; схема актуальна для Stage 4.",
        "version": "0.5.0",
    },
    "servers": [{"url": "http://localhost:8080"}],
    "paths": paths,
    "components": {
        "securitySchemes": {
            "bearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}
        },
        "schemas": schemas,
    },
}
(p / "src/main/resources/static").mkdir(exist_ok=True, parents=True)
for x in (p / "src/main/resources/static/openapi.json", p / "docs/openapi.json"):
    x.write_text(json.dumps(spec, ensure_ascii=False, indent=2) + "\n")
print(
    "Operations",
    sum(len(v) for v in paths.values()),
    "Paths",
    len(paths),
    "Schemas",
    len(schemas),
)
print(
    "Unbound referenced schemas:",
    sorted(
        {
            v
            for v in re.findall(r"#/components/schemas/(\w+)", json.dumps(spec))
            if v not in schemas
        }
    ),
)
print("Listed endpoints")
print(
    "\n".join(f"{m.upper()} {path}" for path, methods in paths.items() for m in methods)
)
