#!/bin/bash
cd /home/sebastian/Projects/order-mgmt
set -a
source .env
set +a
echo "Starte Railway Order Management auf Port ${SERVER_PORT:-8080}..."
echo "Keycloak: ${KEYCLOAK_ISSUER_URI}"
mvn spring-boot:run
