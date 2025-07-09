#!/bin/bash

set -ex

export HERE=$(realpath $(dirname $0))

cd $HERE/..
ant dist

cp -r $HERE/../build/dist/common $HERE/TribalTrouble.AppDir/
cp -r $HERE/../build/dist/linux-x86 $HERE/TribalTrouble.AppDir/

rm -rf $HERE/TribalTrouble.AppDir/usr

mkdir -p $HERE/TribalTrouble.AppDir/usr/lib

cd $HERE

jlink --output TribalTrouble.AppDir/usr/lib/jre --add-modules java.base,java.desktop,java.prefs,java.rmi,java.sql,jdk.unsupported --compress=2 --no-header-files  --no-man-pages --strip-debug

./appimagetool-x86_64.AppImage TribalTrouble.AppDir

mv $HERE/TribalTrouble-x86_64.AppImage $HERE/../../binaries/

