#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

ACTION="${1:-build}"

case "$ACTION" in
  build)
    ./gradlew buildPlugin
    ZIP=$(ls plugin/build/distributions/*.zip 2>/dev/null | head -1)
    echo ""
    echo "==> Plugin: $ZIP"
    ;;
  run)
    ./gradlew runIde
    ;;
  clean)
    ./gradlew clean
    ;;
  *)
    echo "Usage: $0 {build|run|clean}"
    exit 1
    ;;
esac
