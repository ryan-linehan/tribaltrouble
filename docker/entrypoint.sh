#!/bin/bash
set -e

# ============================================================
# Tribal Trouble Server Entrypoint
# ============================================================
# Environment variables:
#   SERVER_ROLE      - "matchmaker" or "router" (required)
#   SQL_HOST         - MySQL host (default: localhost)
#   SQL_PASS         - MySQL password for the matchmaker user
#   DISCORD_BOT_TOKEN  - Discord bot token (optional)
#   DISCORD_SERVER_ID  - Discord server/guild ID (optional)
#   WEBSITE_DOMAIN     - Website domain (optional)
# ============================================================

SERVER_ROLE="${SERVER_ROLE:-matchmaker}"

# Generate server.properties from environment variables
cat > /app/server/server.properties <<EOF
SQL_HOST=${SQL_HOST:-localhost}
SQL_PASS=${SQL_PASS:-}
DISCORD_BOT_TOKEN=${DISCORD_BOT_TOKEN:-}
DISCORD_SERVER_ID=${DISCORD_SERVER_ID:-}
WEBSITE_DOMAIN=${WEBSITE_DOMAIN:-}
NATIVE_CHIEF_EMOJI=${NATIVE_CHIEF_EMOJI:-}
VIKING_CHIEF_EMOJI=${VIKING_CHIEF_EMOJI:-}
EOF

# Build the classpath
CLASSPATH="server/build/classes"
CLASSPATH="${CLASSPATH}:common/build/classes"
CLASSPATH="${CLASSPATH}:common/static"
CLASSPATH="${CLASSPATH}:common/lib/java/*"
CLASSPATH="${CLASSPATH}:ivy_lib/*"

cd /app/server

case "${SERVER_ROLE}" in
  matchmaker)
    echo "Starting Tribal Trouble Matchmaker Server..."
    exec java \
      -Djdk.crypto.KeyAgreement.legacyKDF=true \
      -cp "../${CLASSPATH}" \
      com.oddlabs.matchserver.MatchmakingServer
    ;;
  router)
    echo "Starting Tribal Trouble Router Server..."
    exec java \
      -cp "../${CLASSPATH}" \
      com.oddlabs.routerserver.RouterServer
    ;;
  *)
    echo "Error: SERVER_ROLE must be 'matchmaker' or 'router', got '${SERVER_ROLE}'"
    exit 1
    ;;
esac
