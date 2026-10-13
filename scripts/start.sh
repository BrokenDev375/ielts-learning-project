#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ ! -f .env ]; then cp .env.example .env; fi
docker compose up -d --build --wait
printf 'Open http://localhost:5173 (or APP_PORT configured in .env)\n'
