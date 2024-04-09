#!/bin/bash -e

version=$1
alpha_version=${version}-alpha

sed -Ei "s/(\"io.opentelemetry.android:android-agent:).*\"/\1$alpha_version\"/" README.md

