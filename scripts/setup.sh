#!/usr/bin/env bash

set -euo pipefail

echo "Checking local prerequisites..."

has_java=false
if command -v java >/dev/null 2>&1; then
  java_major_version="$(java -version 2>&1 | sed -n '1s/.*"\([0-9][0-9]*\).*/\1/p')"
  if [ -n "${java_major_version}" ] && [ "${java_major_version}" -ge 17 ]; then
    has_java=true
  fi
fi

has_curl=true
if ! command -v curl >/dev/null 2>&1; then
  has_curl=false
fi

if [ "${has_java}" = true ] && [ "${has_curl}" = true ]; then
  chmod +x ./mvnw ./scripts/*.sh
  echo "Java and curl are already available."
  echo "Next steps:"
  echo "  ./scripts/start-service.sh"
  echo "  ./scripts/run-tests.sh"
  echo "  ./scripts/smoke-test.sh"
  exit 0
fi

echo "Some prerequisites are missing."

if [ "$(uname -s)" = "Darwin" ] && command -v brew >/dev/null 2>&1; then
  if [ "${has_java}" = false ]; then
    brew install openjdk@17
  fi
  if [ "${has_curl}" = false ]; then
    brew install curl
  fi
elif command -v apt-get >/dev/null 2>&1; then
  if [ "${has_java}" = false ] || [ "${has_curl}" = false ]; then
    sudo apt-get update
  fi
  if [ "${has_java}" = false ]; then
    sudo apt-get install -y openjdk-17-jdk
  fi
  if [ "${has_curl}" = false ]; then
    sudo apt-get install -y curl
  fi
else
  echo "Please install Java 17+ and curl, then rerun this script."
  exit 1
fi

chmod +x ./mvnw ./scripts/*.sh

echo "Setup complete."
echo "Next steps:"
echo "  ./scripts/start-service.sh"
echo "  ./scripts/run-tests.sh"
echo "  ./scripts/smoke-test.sh"
