#!/bin/bash -e

from_version_json=$("$(dirname "$0")/parse-version.sh" "$1")
major=$(echo "$from_version_json" | jq -r '.major')
minor=$(echo "$from_version_json" | jq -r '.minor')
patch=$(echo "$from_version_json" | jq -r '.patch')
rc=$(echo "$from_version_json" | jq -r '.rc')

if [ "$rc" ] && [ "$rc" -gt 1 ]; then
  prior_version="$major.$minor.$patch-rc.$((rc - 1))"
else
  if [ "$patch" -eq 0 ]; then
    if [ "$minor" -eq 0 ]; then
      prior_major=$((major - 1))
      prior_version=$(grep -m 1 -Eo "^## Version $prior_major\.[0-9]+\.[0-9]+ " CHANGELOG.md | grep -Eo "[0-9]+\.[0-9]+\.[0-9]+" )
    else
      prior_version="$major.$((minor - 1)).0"
    fi
  else
    prior_version="$major.$minor.$((patch - 1))"
  fi
fi

echo "$prior_version"
