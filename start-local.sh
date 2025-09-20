#!/bin/bash
# 로컬 개발용 시작 스크립트

echo "Starting local development environment..."
echo "API: http://localhost"
echo "Traefik Dashboard: http://localhost:8088"
echo "Spring Boot Direct: http://localhost:8080"

docker compose up --build
