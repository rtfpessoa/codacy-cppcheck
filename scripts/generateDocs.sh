#!/usr/bin/env bash
version=$1

if [ -z "$version" ] ; then
    echo 'Missing version'
    exit 1
fi

docker build . -t cppcheck-doc --build-arg toolVersion=$version
sbt "run-main codacy.cppcheck.DocGenerator $version docker run -i cppcheck-doc"
