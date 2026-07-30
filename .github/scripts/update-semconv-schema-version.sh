#!/bin/bash -e

# Updates the upstream semantic convention version in both the registry path and schema URL.
version=$1
manifest=semconv/model/manifest.yaml

sed -Ei \
  -e "s|(semantic-conventions@v)[0-9]+\\.[0-9]+\\.[0-9]+|\\1$version|" \
  -e "s|(schema_url: https://opentelemetry.io/schemas/)[0-9]+\\.[0-9]+\\.[0-9]+$|\\1$version|" \
  "$manifest"
