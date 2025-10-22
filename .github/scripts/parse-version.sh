#!/bin/bash -e

version=$1

if [[ -z "$version" ]]; then
 echo "Unexpected version argument: $version"
 exit 1
fi

if [[ $version =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(-(rc\.([0-9]+)))?$ ]]; then
  version_major="${BASH_REMATCH[1]}"
  version_minor="${BASH_REMATCH[2]}"
  version_patch="${BASH_REMATCH[3]}"
  version_rc="${BASH_REMATCH[6]}"
else
  echo "unexpected version: '$version'"
  exit 1
fi

cat <<EOF
{
  "version": "$version",
  "major": "$version_major",
  "minor": "$version_minor",
  "patch": "$version_patch",
  "rc": "$version_rc"
}
EOF
