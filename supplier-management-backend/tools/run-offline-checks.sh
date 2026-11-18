#!/usr/bin/env bash
set -euo pipefail
python3 tools/verify_model.py
python3 tools/verify_api_offline.py
python3 tools/verify_xlsx_offline.py
python3 tools/verify_api_contract.py
python3 tools/verify_dto_refactor.py
