#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"
cd ${ROOT_DIR}

print_usage() {
  cat <<EOF
Usage: $(basename $0) release_version

All versions MUST NOT begin with 'v'. Example: 1.2.3.
EOF
}

if [[ $# < 1 ]]
then
  print_usage
  exit 1
fi

release_version=$1
release_tag="v$1"

if [[ ! $release_version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-rc\.[0-9]+)?$ ]]
then
  echo "Invalid release version: $release_version"
  echo "Release version must follow the pattern major.minor.patch, e.g. 1.2.3"
  exit 1
fi

git tag -m "Release $release_version" -s "$release_tag"
git push origin "$release_tag"
