#!/bin/bash

HERE=$(realpath $(dirname $0))

echo "These are the files I'm going to change"
echo $(find $HERE/.. | grep "\.java$")

$HERE/google-java-format_linux-x86-64 --aosp  -r $(find $HERE/.. | grep "\.java$")

git diff --name-only
