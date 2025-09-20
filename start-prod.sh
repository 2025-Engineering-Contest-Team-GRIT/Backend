#!/bin/bash
# 배포용 시작 스크립트

echo "Starting production environment..."
echo "API: https://api.guidance.cloud"
echo "Traefik Dashboard: https://traefik.guidance.cloud"
echo "Make sure DNS is configured for guidance.cloud domain!"

docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build
