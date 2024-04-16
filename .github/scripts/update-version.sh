#!/bin/bash -e

version=$1
alpha_version=${version}-alpha

sed -Ei "s/version=.*/version=$version/" gradle.properties

