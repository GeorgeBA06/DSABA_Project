#!/usr/bin/env bash
set -euo pipefail
umask 077
mkdir -p secrets
if [[ -e secrets/jwt-private.pem || -e secrets/jwt-public.pem ]]; then
    echo 'Keys exist; refusing to overwrite. Back them up and remove explicitly to rotate.' >&2
    exit 1
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/jwt-private.pem
openssl pkey -in secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem
chmod 600 secrets/jwt-private.pem
printf 'RSA JWT keys created under secrets/ (private key NOT tracked).\n'
