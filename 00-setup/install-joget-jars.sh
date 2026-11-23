#!/usr/bin/env bash
# Installs the Joget platform jars that the samples compile against into your local Maven repository.
# The jars are taken from your own Joget DX installation.
#
# The samples compile against five Joget platform artifacts (group org.joget): wflow-core, wflow-commons,
# wflow-plugin-base, wflow-wfengine and wflow-directory. The Joget Maven repository needs a login, so a machine
# without that login cannot download them. Every Joget installation already contains the jars in
# <JOGET_HOME>/apache-tomcat-*/webapps/jw/WEB-INF/lib. This script installs them from there with
# "mvn install:install-file" and a generated pom without dependencies.
#
# You do NOT need this script when your local Maven repository already contains these artifacts, for example
# because you built Joget from source or your Maven settings hold a login for the Joget repository.
#
#   JOGET_HOME=/opt/joget ./install-joget-jars.sh
#   ./install-joget-jars.sh /opt/joget
#   LIB_DIR=/path/with/wflow-jars ./install-joget-jars.sh        name the jar folder directly
#
# Optional environment variables:
#   JOGET_VERSION     install under this version instead of the version in the file names
#   MAVEN_REPO_LOCAL  a local Maven repository folder other than the default (~/.m2/repository)
#
# The version is read from the file name (wflow-core-9.1.2.jar gives 9.1.2). Build the samples with the same
# version:  mvn clean package "-Djoget.version=9.1.2"
set -euo pipefail
ARTIFACTS="wflow-core wflow-commons wflow-plugin-base wflow-wfengine wflow-directory"
JOGET_HOME="${1:-${JOGET_HOME:-}}"
LIB_DIR="${LIB_DIR:-}"

command -v mvn >/dev/null 2>&1 || { echo "mvn is not on the PATH. Install Apache Maven 3.8 or later." >&2; exit 1; }
if [ -z "$LIB_DIR" ]; then
  [ -n "$JOGET_HOME" ] || { echo "Give the Joget folder as the first argument (or set JOGET_HOME), or set LIB_DIR." >&2; exit 1; }
  for d in "$JOGET_HOME"/apache-tomcat*/webapps/jw/WEB-INF/lib; do
    if [ -d "$d" ]; then LIB_DIR="$d"; break; fi
  done
  [ -n "$LIB_DIR" ] || { echo "No apache-tomcat*/webapps/jw/WEB-INF/lib under $JOGET_HOME. Start Joget once so the web application is unpacked, or set LIB_DIR." >&2; exit 1; }
fi
[ -d "$LIB_DIR" ] || { echo "Folder not found: $LIB_DIR" >&2; exit 1; }
echo "Reading Joget jars from $LIB_DIR"

INSTALLED=""
for a in $ARTIFACTS; do
  FILE=""
  # wflow-core-9.1.2.jar matches; wflow-commons-jakarta-9.1.2-jakarta.jar (another artifact) does not.
  for f in "$LIB_DIR/$a"-[0-9]*.jar; do
    if [ -f "$f" ]; then FILE="$f"; break; fi
  done
  [ -n "$FILE" ] || { echo "No $a-<version>.jar in $LIB_DIR" >&2; exit 1; }
  NAME="$(basename "$FILE")"
  V="${NAME#"$a"-}"
  V="${V%.jar}"
  if [ -n "${JOGET_VERSION:-}" ]; then V="$JOGET_VERSION"; fi
  # Git Bash on Windows: Maven needs Windows-style paths.
  if command -v cygpath >/dev/null 2>&1; then FILE="$(cygpath -m "$FILE")"; fi
  ARGS=(-q -B org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file "-Dfile=$FILE"
        -DgroupId=org.joget "-DartifactId=$a" "-Dversion=$V" -Dpackaging=jar -DgeneratePom=true)
  if [ -n "${MAVEN_REPO_LOCAL:-}" ]; then ARGS+=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL"); fi
  mvn "${ARGS[@]}"
  echo "Installed org.joget:$a:$V  (from $NAME)"
  INSTALLED="$V"
done
echo
echo "Done. Build the samples with:  mvn clean package \"-Djoget.version=$INSTALLED\""
