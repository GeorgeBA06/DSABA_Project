#!/usr/bin/env python3
"""Populate a development database through the SupplierHub API (Python 3.9+)."""

import argparse
import getpass
import json
import os
import re
import sys
import uuid
from datetime import datetime, timedelta
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, ProxyHandler
from zoneinfo import ZoneInfo


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api", default="http://127.0.0.1:8081/api/v1")
    parser.add_argument("--admin-login", default="admin")
    parser.add_argument(
        "--batch", default=uuid.uuid4().hex[:8], help="Unique dataset label"
    )
    args = parser.parse_args()
    if not re.fullmatch(r"[a-zA-Z0-9_-]{1,16}", args.batch):
        parser.error(
            "--batch must contain 1–16 letters, digits, underscores or hyphens"
        )
    base = args.api.rstrip("/")
    if urlsplit(base).scheme not in ("http", "https"):
        parser.error("--api must be an HTTP(S) URL")
    today = datetime.now(ZoneInfo("Europe/Minsk")).date()
    admin_password = os.environ.get("DEMO_ADMIN_PASSWORD") or getpass.getpass(
        "Пароль администратора: "
    )
    demo_password = os.environ.get("DEMO_USER_PASSWORD") or getpass.getpass(
        "Пароль демонстрационных сотрудников: "
    )
    if not 12 <= len(demo_password) <= 128 or len(demo_password.encode()) > 72:
        parser.error(
            "Demo password must have 12–128 characters and at most 72 UTF-8 bytes"
        )
    # Local API requests should reach the application directly.
    http = (
        build_opener(ProxyHandler({}))
        if urlsplit(base).hostname in ("localhost", "127.0.0.1", "::1")
        else build_opener()
    )
    tokens = {}

    def call(who, method, path, body=None):
        headers = {"Accept": "application/json"}
        if who:
            headers["Authorization"] = "Bearer " + tokens[who]
        data = None
        if body is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(body).encode()
        try:
            with http.open(
                Request(base + path, data=data, headers=headers, method=method),
                timeout=30,
            ) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except HTTPError as error:
            # Report server messages only, never request bodies or tokens.
            raise RuntimeError(
                f"{method} {path}: HTTP {error.code}: {error.read().decode()[:500]}"
            ) from None

    tokens["admin"] = call(
        None,
        "POST",
        "/auth/login",
        {"login": args.admin_login, "password": admin_password},
    )["accessToken"]
    logins = {role: f"demo.{role}.{args.batch}" for role in ("specialist", "manager")}
    page = 0
    while True:
        users = call("admin", "GET", f"/admin/users?page={page}&size=100")
        if any(user["login"] in logins.values() for user in users["items"]):
            raise RuntimeError(
                "Этот набор уже существует или был создан частично. Выберите новый --batch."
            )
        page += 1
        if page >= users["totalPages"]:
            break
    print(f"Создаётся демонстрационный набор {args.batch}", flush=True)
    for role, code, name in (
        ("specialist", "PROCUREMENT_SPECIALIST", "Анна Демонстрационная"),
        ("manager", "PROCUREMENT_MANAGER", "Иван Демонстрационный"),
    ):
        call(
            "admin",
            "POST",
            "/admin/users",
            {
                "login": logins[role],
                "password": demo_password,
                "displayName": name,
                "roles": [code],
            },
        )
        tokens[role] = call(
            None,
            "POST",
            "/auth/login",
            {"login": logins[role], "password": demo_password},
        )["accessToken"]

    def day(offset=0):
        return (today + timedelta(days=offset)).isoformat()

    def create(path, body, who="specialist"):
        return call(who, "POST", path, body)

    def approve(path):
        return create(
            path + "/decision",
            {"approve": True, "comment": "Согласовано для демонстрации"},
            "manager",
        )

    category = create(
        "/catalog/categories",
        {"code": f"DEMO-{args.batch}", "name": "Офисное оборудование"},
        "manager",
    )
    unit = create(
        "/catalog/units", {"code": f"D-{args.batch}", "name": "Штука"}, "manager"
    )
    products = [
        create(
            "/catalog/products",
            {
                "sku": f"D-{args.batch}-{i}",
                "name": name,
                "categoryId": category["id"],
                "unitId": unit["id"],
            },
        )
        for i, name in enumerate(
            ("Монитор 27 дюймов", "Клавиатура", "Лазерный принтер")
        )
    ]

    def supplier(name, active=True):
        result = create(
            "/suppliers",
            {
                "name": f"{name} [DEMO {args.batch}]",
                "contactName": "Демонстрационный контакт",
                "email": f"demo-{args.batch}@example.org",
                "address": "Минск",
                "phone": "",
                "registrationNumber": "",
            },
        )
        sid = result["id"]
        for product in products:
            create(
                f"/suppliers/{sid}/products",
                {"productId": product["id"], "supplierItemCode": product["sku"]},
            )
        if active:
            request = create(
                f"/suppliers/{sid}/status-requests",
                {"status": "ACTIVE", "reason": "Проверены условия сотрудничества"},
            )
            approve(f"/suppliers/status-requests/{request['id']}")
        return result

    reliable = supplier("ОфисТехника")
    problem = supplier("ТехноПоставка")
    candidate = supplier("Новый партнёр", False)
    request = create(
        f"/suppliers/{candidate['id']}/status-requests",
        {"status": "ACTIVE", "reason": "Заявка ожидает решения руководителя"},
    )

    def contract(sup):
        result = create(
            "/contracts",
            {
                "supplierId": sup["id"],
                "terms": {
                    "number": f"Д-{args.batch}-{sup['id'][:4]}",
                    "signedOn": day(),
                    "validFrom": day(-60),
                    "validTo": day(365),
                    "terms": "Оплата в BYN, поставка офисного оборудования, гарантия 12 месяцев.",
                },
            },
        )
        version = call("specialist", "GET", f"/contracts/{result['id']}/versions")[0]
        path = f"/contracts/versions/{version['id']}"
        create(path + "/submit", None)
        approve(path)
        return result

    contracts = {sup["id"]: contract(sup) for sup in (reliable, problem)}

    def order(sup, number, state="APPROVED"):
        result = create(
            "/orders",
            {
                "number": f"ДЕМО-{args.batch}-{number}",
                "supplierId": sup["id"],
                "contractId": contracts[sup["id"]]["id"],
                "items": [
                    {
                        "productId": products[0]["id"],
                        "quantity": 10,
                        "unitPrice": 650,
                        "dueOn": day(),
                    }
                ],
            },
        )
        version = call("specialist", "GET", f"/orders/{result['id']}/versions")[0]
        path = f"/orders/versions/{version['id']}"
        if state != "DRAFT":
            create(path + "/submit", None)
        if state in ("APPROVED", "REJECTED"):
            create(
                path + "/decision",
                {
                    "approve": state == "APPROVED",
                    "comment": "Проверены условия"
                    if state == "APPROVED"
                    else "Необходимо уточнить количество",
                },
                "manager",
            )
        return result, version

    def deliver(result, version, quantity, accepted):
        segment = call("specialist", "GET", f"/orders/versions/{version['id']}/lines")[
            0
        ]
        delivery = create(
            f"/fulfillment/orders/{result['id']}/deliveries",
            {
                "arrivedOn": day(),
                "externalNumber": result["number"],
                "items": [{"segmentId": segment["id"], "quantity": quantity}],
            },
        )
        path = f"/fulfillment/deliveries/{delivery['id']}"
        create(path + "/confirm", None)
        received = call("specialist", "GET", path + "/lines")[0]
        inspection = create(
            path + "/acceptances",
            {
                "inspectedOn": day(),
                "items": [
                    {
                        "deliveryLineId": received["id"],
                        "accepted": accepted,
                        "qualityRejected": quantity - accepted,
                        "otherRejected": 0,
                    }
                ],
            },
        )
        path = f"/fulfillment/acceptances/{inspection['id']}"
        create(path + "/confirm", None)
        return segment, received, call("specialist", "GET", path + "/lines")[0]

    for index in range(3):
        result, version = order(reliable, f"ИСПОЛНЕН-{index + 1}")
        deliver(result, version, 10, 10)
        create(
            f"/orders/{result['id']}/close",
            {"reason": "Поставка выполнена полностью"},
            "manager",
        )
    result, version = order(problem, "ЧАСТИЧНО")
    segment, received, accepted = deliver(result, version, 6, 4)
    claim = create(
        "/relationships/claims",
        {
            "number": f"ПР-{args.batch}",
            "supplierId": problem["id"],
            "orderLineId": segment["orderLineId"],
            "deliveryLineId": received["id"],
            "type": "QUALITY",
            "description": "Повреждение корпуса двух мониторов",
            "requirements": "Заменить повреждённые изделия",
            "responseDueOn": day(10),
        },
    )
    create(f"/relationships/claims/{claim['id']}/send", {"channel": "EMAIL"})
    create(f"/relationships/claims/{claim['id']}/begin-resolution", None)
    ret = create(
        f"/fulfillment/orders/{result['id']}/returns",
        {
            "returnedOn": day(),
            "reason": "Скрытый дефект после приёмки",
            "items": [
                {
                    "acceptanceLineId": accepted["id"],
                    "quantity": 1,
                    "defectDescription": "Нестабильная работа подсветки",
                }
            ],
        },
    )
    create(f"/fulfillment/returns/{ret['id']}/submit", None)
    approve(f"/fulfillment/returns/{ret['id']}")
    for state in ("DRAFT", "PENDING", "REJECTED"):
        order(reliable, state, state)
    for sup in (reliable, problem):
        create(
            "/relationships/interactions",
            {
                "supplierId": sup["id"],
                "channel": "PHONE",
                "subject": "Обсуждение поставки",
                "content": "Уточнили график и условия гарантии",
                "outcome": "Договорённости зафиксированы",
                "occurredAt": datetime.now(ZoneInfo("Europe/Minsk")).isoformat(),
            },
        )
    assessment = create(
        "/ratings/expert-assessments",
        {
            "supplierId": reliable["id"],
            "periodStart": day(-29),
            "periodEnd": day(),
            "applicableFrom": day(),
            "applicableTo": day(29),
            "score": 100,
            "justification": "Документы предоставляются вовремя, поставки выполнены полностью",
        },
    )
    path = f"/ratings/expert-assessments/{assessment['id']}"
    create(path + "/submit", None)
    approve(path)
    for sup, expected in (
        (reliable, "PUBLISHED"),
        (problem, "PRELIMINARY"),
        (candidate, "INSUFFICIENT_DATA"),
    ):
        rating = create(
            "/ratings/calculate",
            {"supplierId": sup["id"], "scope": "OVERALL"},
            "manager",
        )
        if rating["state"] != expected:
            raise RuntimeError(
                f"Ожидался рейтинг {expected}, получен {rating['state']}"
            )
    print(
        "Готово: 3 поставщика, 3 товара, 2 договора, 7 заказов, 4 поступления и приёмки, возврат, претензия, взаимодействия и 3 рейтинга."
    )
    for role, login in logins.items():
        print(f"{role}: {login}")
    print("Используйте пароль демонстрационных сотрудников, заданный при запуске.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, URLError, TimeoutError, KeyboardInterrupt) as error:
        print(f"Заполнение остановлено: {error}", file=sys.stderr)
        sys.exit(1)
