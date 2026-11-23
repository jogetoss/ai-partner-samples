#!/usr/bin/env bash
# Optional convenience: installs a plugin jar on a Joget DX server that runs on this machine.
# The manual route gives the same result: Settings > Manage Plugins > Upload.
#
#   ./deploy-plugin.sh <jar>              upload through the console form (needs Node, "npm install" in the
#                                         repository root, and Chrome)
#   ./deploy-plugin.sh <jar> --via-copy   copy the jar into $JOGET_HOME/wflow/app_plugins instead (no browser)
#
# Environment variables:
#   JOGET_HOME   the Joget installation folder. When set, an installed jar with the same file name is first copied
#                to $JOGET_HOME/wflow/app_plugins_backup. Required for --via-copy.
#   JW_BASE      Joget address with context path   default http://localhost:8080/jw
#   JW_USER      a Joget administrator             default admin
#   JW_PASS      the password of that user         default admin
#   CHROME_PATH  full path of Chrome, Chromium or Edge (found automatically in the usual places)
set -euo pipefail
JAR="${1:-}"
MODE="${2:-}"
[ -n "$JAR" ] && [ -f "$JAR" ] || { echo "usage: deploy-plugin.sh <jar> [--via-copy]" >&2; exit 2; }
HERE="$(cd "$(dirname "$0")" && pwd)"
NAME="$(basename "$JAR")"
LIVE=""

if [ -n "${JOGET_HOME:-}" ]; then
  [ -d "$JOGET_HOME/wflow" ] || { echo "No wflow folder under $JOGET_HOME. Check JOGET_HOME." >&2; exit 1; }
  LIVE="$JOGET_HOME/wflow/app_plugins/$NAME"
  if [ -f "$LIVE" ]; then
    mkdir -p "$JOGET_HOME/wflow/app_plugins_backup"
    BAK="$JOGET_HOME/wflow/app_plugins_backup/${NAME%.jar}-$(date +%Y%m%d-%H%M%S).jar"
    cp "$LIVE" "$BAK"
    echo "Backed up the installed jar to $BAK"
  fi
elif [ "$MODE" = "--via-copy" ]; then
  echo "--via-copy needs the JOGET_HOME environment variable." >&2; exit 1
else
  echo "JOGET_HOME is not set: no backup of an installed jar is made."
fi

if [ "$MODE" = "--via-copy" ]; then
  cp "$JAR" "$LIVE"
  echo "Copied to $LIVE. Joget loads it within a few seconds."
else
  command -v node >/dev/null 2>&1 || { echo "node is not on the PATH. Install Node.js 18 or later, or use --via-copy, or upload the jar in the console." >&2; exit 1; }
  node "$HERE/deploy-upload.js" "$JAR"
fi

if [ -n "$LIVE" ]; then
  sleep 8
  [ -f "$LIVE" ] && ls -l "$LIVE"
fi
echo "Check: Settings > Manage Plugins lists the plugin, and the Joget log shows no error for the bundle."
