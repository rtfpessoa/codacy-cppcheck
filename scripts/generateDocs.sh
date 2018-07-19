#!/usr/bin/env bash

set -e

current_directory="$( cd "$( dirname "$0" )" && pwd )"

OUTPUT_FILE="$(mktemp)"

${current_directory}/publish-base.sh
docker run -i codacy/codacy-cppcheck-base:latest
cppcheck --errorlist > $OUTPUT_FILE
sbt "run-main codacy.cppcheck.DocGenerator $OUTPUT_FILE"