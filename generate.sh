#!/bin/sh

# Generator version: 7.8.0 (vs 7.6.0)

SPEC_FILE="src/main/resources/dlt-adapter-api.yaml"
JAVA_PACKAGE="io.ownera.ledger.adapter.api"
OUT_FOLDER="target/generated-sources"

./openapi-generator-cli generate \
  -i "$SPEC_FILE" \
  -g java \
  -o "$OUT_FOLDER" \
  --library native \
  --package-name "$JAVA_PACKAGE" \
  --api-package "$JAVA_PACKAGE.api" \
  --model-package "$JAVA_PACKAGE.model" \
  --model-name-prefix "API"
