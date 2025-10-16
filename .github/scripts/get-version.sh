#!/bin/bash -e

otel_android_version=$(grep ^version= gradle.properties | sed s/version=// | tr -d '\r')

if [[ $otel_android_version =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(-(rc\.([0-9]+)))?$ ]]; then
  otel_android_version_major="${BASH_REMATCH[1]}"
  otel_android_version_minor="${BASH_REMATCH[2]}"
  otel_android_version_patch="${BASH_REMATCH[3]}"
  otel_android_version_rc="${BASH_REMATCH[6]}"
else
  echo "unexpected version: '$otel_android_version'"
  exit 1
fi

cat <<EOF
{
  "version": "$otel_android_version",
  "major": "$otel_android_version_major",
  "minor": "$otel_android_version_minor",
  "patch": "$otel_android_version_patch",
  "rc": "$otel_android_version_rc"
}
EOF