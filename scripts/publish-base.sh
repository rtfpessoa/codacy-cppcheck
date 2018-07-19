#!/usr/bin/env bash

set -e

TOOL_VERSION="$(cat .cppcheckVersion)"
docker build --no-cache -t "codacy/codacy-cppcheck-base:latest" -f Dockerfile.base . --build-arg toolVersion=$TOOL_VERSION