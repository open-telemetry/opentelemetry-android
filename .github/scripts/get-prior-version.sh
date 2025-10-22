#!/bin/bash -e

from_version_json=$("$(dirname "$0")/parse-version.sh" "$1")
major=$(echo "$from_version_json" | jq -r '.major')
minor=$(echo "$from_version_json" | jq -r '.minor')
patch=$(echo "$from_version_json" | jq -r '.patch')
rc=$(echo "$from_version_json" | jq -r '.rc')

if [[ $rc -gt 1 ]]; then
  prior_version="$major.$minor.$patch-rc.$((rc - 1))"
else
  if [[ $patch == 0 ]]; then
    if [[ $minor == 0 ]]; then
      prior_major=$((major - 1))
      prior_minor=$(grep -Po "^## Version $prior_major.\K[0-9]+" CHANGELOG.md | head -1)
      prior_version="$prior_major.$prior_minor"
    else
      prior_version="$major.$((minor - 1)).0"
    fi
  else
    prior_version="$major.$minor.$((patch - 1))"
  fi
fi

echo "$prior_version"
