#!/usr/bin/env bash
# Installs the Agent Builder API jar from 00-setup/lib into your local Maven repository, so the samples can compile.
#
# The samples declare the dependency org.joget.ai.agent:agent-builder-api:8.2.5-ALPHA (scope "provided").
# That artifact is not on any public Maven repository, so it is shipped in this folder and installed with
# "mvn install:install-file". The pom next to the jar declares no dependencies, so Maven resolves nothing else.
# Run this once per machine (and again when you receive a newer API jar).
#
#   ./install-api-jar.sh                 install into the default local repository (~/.m2/repository)
#   ./install-api-jar.sh /path/to/repo   install into another local repository folder
set -euo pipefail
VERSION="8.2.5-ALPHA"
HERE="$(cd "$(dirname "$0")" && pwd)"
JAR="$HERE/lib/agent-builder-api-$VERSION.jar"
POM="$HERE/lib/agent-builder-api-$VERSION.pom"
[ -f "$JAR" ] || { echo "Not found: $JAR" >&2; exit 1; }
[ -f "$POM" ] || { echo "Not found: $POM" >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "mvn is not on the PATH. Install Apache Maven 3.8 or later." >&2; exit 1; }

# Git Bash on Windows: Maven needs Windows-style paths.
if command -v cygpath >/dev/null 2>&1; then JAR="$(cygpath -m "$JAR")"; POM="$(cygpath -m "$POM")"; fi

ARGS=(-q -B org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file "-Dfile=$JAR" "-DpomFile=$POM")
if [ "${1:-}" != "" ]; then ARGS+=("-Dmaven.repo.local=$1"); fi

mvn "${ARGS[@]}"
echo "Installed org.joget.ai.agent:agent-builder-api:$VERSION"
