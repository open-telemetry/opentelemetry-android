#!/bin/bash -e

version=$1

sed -Ei "s/version=.*/version=$version/" gradle.properties

