#!/bin/bash -e

grep ^version= gradle.properties | sed s/version=//g
