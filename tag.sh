#!/usr/bin/env bash

sbt '+ publish'

git commit -am 'next release'

git tag -a v$1 -m "Version $1"

git push --tags

git push origin master

