#!/bin/bash -e

version=$("$(dirname "$0")/get-version.sh")
prior_version=$("$(dirname "$0")/get-prior-version.sh" "$version")

range="v$prior_version..HEAD"

echo "## Unreleased"
echo
echo "### Migration notes"
echo
echo
echo "### 🌟 New instrumentation"
echo
echo
echo "### 📈 Enhancements"
echo
echo
echo "### 🛠️ Bug fixes"
echo
echo
echo "### 🧰 Tooling"
echo

git log --reverse \
        --perl-regexp \
        --author='^(?!renovate\[bot\] )' \
        --pretty=format:"- %s" \
        "$range" \
  | sed -E 's,\(#([0-9]+)\)$,\n  ([#\1](https://github.com/open-telemetry/opentelemetry-android/pull/\1)),'