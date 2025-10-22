#!/bin/bash -e

grep ^version= gradle.properties | sed s/version=// | tr -d '\r'