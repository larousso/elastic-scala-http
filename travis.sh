#!/usr/bin/env bash

if test "$TRAVIS_PULL_REQUEST" = "false"
then
    if [ -z "$TRAVIS_TAG" ];
    then
      sbt ++$TRAVIS_SCALA_VERSION ';test'
    else
      sbt ++$TRAVIS_SCALA_VERSION ";test;publish"
    fi
fi

