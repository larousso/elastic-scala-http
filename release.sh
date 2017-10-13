#!/usr/bin/env bash

sbt '+ publish'

git add repository

git commit -am 'next release'

git tag -a v$1 -m "Version $1"

git push --tags

git push origin master

