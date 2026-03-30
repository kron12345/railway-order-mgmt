#!/usr/bin/env bash
# Security Audit via Codex CLI
# Verwendung: ./scripts/security-audit.sh [--full | --quick]
#
# --quick  Nur die zuletzt geaenderten Dateien pruefen (default)
# --full   Komplettes Projekt pruefen

set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

MODE="${1:---quick}"

if [ "$MODE" = "--full" ]; then
    PROMPT="Fuehre einen vollstaendigen Security-Audit des gesamten Projekts durch. Pruefe auf:
1. OWASP Top 10 Schwachstellen
2. SQL Injection und JPA/HQL Injection
3. XSS (Cross-Site Scripting) in Vaadin Views
4. CSRF Konfiguration
5. Unsichere OAuth2/OIDC Konfiguration
6. Hartcodierte Secrets oder Credentials
7. Fehlende Input-Validierung an Systemgrenzen
8. Unsichere Docker-Konfiguration
9. Fehlende Security-Header
10. Unsichere Deserialisierung

Gib einen strukturierten Bericht mit Schweregrad (CRITICAL/HIGH/MEDIUM/LOW/INFO) pro Finding."
else
    CHANGED_FILES=$(git diff --name-only HEAD~1 HEAD 2>/dev/null || git diff --name-only --cached || echo "")
    if [ -z "$CHANGED_FILES" ]; then
        echo "Keine geaenderten Dateien gefunden. Verwende --full fuer kompletten Scan."
        exit 0
    fi
    PROMPT="Fuehre einen Security-Audit der folgenden zuletzt geaenderten Dateien durch:

${CHANGED_FILES}

Pruefe auf: OWASP Top 10, SQL/HQL Injection, XSS, CSRF, unsichere Konfiguration, hartcodierte Secrets, fehlende Input-Validierung.
Gib einen strukturierten Bericht mit Schweregrad (CRITICAL/HIGH/MEDIUM/LOW/INFO) pro Finding."
fi

echo "=== Railway Order Management - Security Audit ==="
echo "Modus: $MODE"
echo "Datum: $(date)"
echo "================================================="
echo ""

codex exec "$PROMPT" --full-auto 2>&1 | tee "security-audit-$(date +%Y%m%d-%H%M%S).log"
