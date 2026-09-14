#!/bin/bash
#
# Regenerates the FinP2P API models in skeleton/ from the OpenAPI spec.
#
# Source of truth for the spec is owneraio/finp2p-core:api/. To pick up a newer
# router API, refresh both files first (they are $ref-linked):
#   skeleton/src/main/resources/dlt-adapter-api.yaml
#   skeleton/src/main/resources/common-external-components.yaml
#
# Generator version is PINNED. Do not bump it casually: 7.8.0 renames public enum
# constants (WIRETRANSFER -> WIRE_TRANSFER), which is a source-breaking change for
# downstream adapters that consume this library.
#
# Only the model package is regenerated. The client scaffolding (ApiClient, JSON, ...)
# is checked in and does not depend on the model set.
set -euo pipefail

GENERATOR_VERSION="7.6.0"
JAVA_PACKAGE="io.ownera.ledger.adapter.api"

ROOT="$(cd "$(dirname "$0")" && pwd)"
SPEC_FILE="$ROOT/skeleton/src/main/resources/dlt-adapter-api.yaml"
MODEL_DIR="$ROOT/skeleton/src/main/java/io/ownera/ledger/adapter/api/model"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

JAR="${OPENAPI_GENERATOR_JAR:-$WORK/openapi-generator-cli.jar}"
if [ ! -f "$JAR" ]; then
  echo "==> fetching openapi-generator-cli $GENERATOR_VERSION"
  curl -sSLf -o "$JAR" \
    "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${GENERATOR_VERSION}/openapi-generator-cli-${GENERATOR_VERSION}.jar"
fi

echo "==> generating"
java -jar "$JAR" generate \
  -i "$SPEC_FILE" \
  -g java \
  -o "$WORK/out" \
  --library native \
  --package-name "$JAVA_PACKAGE" \
  --api-package "$JAVA_PACKAGE.api" \
  --model-package "$JAVA_PACKAGE.model" \
  --model-name-prefix "API" \
  --additional-properties=skipFormModel=false,additionalProperties=false \
  >/dev/null

echo "==> applying post-generation patches"
"$ROOT/scripts/postgen-patches.sh" "$WORK/out/src/main/java/io/ownera/ledger/adapter/api/model"

echo "==> installing models"
rm -rf "$MODEL_DIR"
cp -r "$WORK/out/src/main/java/io/ownera/ledger/adapter/api/model" "$MODEL_DIR"

echo "==> done. Review 'git status' — new endpoints in the spec still need controllers."
