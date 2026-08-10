#!/bin/bash -e

version=$1

sed -Ei "s/version=.*/version=$version/" gradle.properties
sed -Ei "s/(\"io.opentelemetry.android:opentelemetry-android-bom:).*SNAPSHOT\"/\1${version}-alpha-SNAPSHOT\"/" README.md
