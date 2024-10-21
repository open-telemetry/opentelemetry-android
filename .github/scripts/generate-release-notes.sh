#!/bin/bash -e

# Generates release notes, like what appears in GitHub release pages.

VERSION=$1
PRIOR_VERSION=$2
INST_VER=$3
SDK_VER=$4

cat > /tmp/release-notes.txt << EOF
This release is based on OpenTelemetry Java Instrumentation $INST_VER and
the OpenTelemetry Java Core (sdk/api/exporters) $SDK_VER.

Note that many artifacts have the \`-alpha\` suffix attached to their version number, reflecting
that they are still alpha quality and will continue to have breaking changes. Please see the
[VERSIONING.md](https://github.com/open-telemetry/opentelemetry-android/blob/main/VERSIONING.md#opentelemetry-android-versioning) for more details.

EOF

sed -n "0,/^## Version $VERSION /d;/^## Version /q;p" CHANGELOG.md > /tmp/CHANGELOG_SECTION.md

# the complex perl regex is needed because markdown docs render newlines as soft wraps
# while release notes render them as line breaks
perl -0pe 's/(?<!\n)\n *(?!\n)(?![-*] )(?![1-9]+\. )/ /g' /tmp/CHANGELOG_SECTION.md \
    >> /tmp/release-notes.txt

cat >> /tmp/release-notes.txt << EOF
 ### 🙇 Thank you

 This release was possible thanks to the following contributors:

EOF

.github/scripts/generate-release-contributors.sh "v${PRIOR_VERSION}" >> /tmp/release-notes.txt
