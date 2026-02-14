#!/bin/bash
set -e

# ============================================================
# Tribal Trouble - Dev Entrypoint
# ============================================================
# Compiles from the mounted source code and runs the server.
# Recompiles every time the container starts so you always
# get your latest code changes.
#
# Environment variables:
#   SERVER_ROLE  - "matchmaker" or "router" (default: matchmaker)
#   SQL_HOST     - MySQL host (default: localhost)
#   SQL_PASS     - MySQL password for the matchmaker user
#   Plus optional: DISCORD_BOT_TOKEN, DISCORD_SERVER_ID, etc.
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

echo "Compiling server from mounted source..."
cd /app/server && ant compile
echo "Compilation complete."

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
