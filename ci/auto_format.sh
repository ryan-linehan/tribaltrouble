#!/bin/bash

HERE=$(realpath $(dirname $0))

$HERE/google-java-format_linux-x86-64 --aosp  -r $(find $HERE/.. | grep "\.java$")
