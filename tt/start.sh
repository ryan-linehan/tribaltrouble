#!/bin/bash

export SCRIPT_DIR=$(realpath $(dirname $0))

cd $SCRIPT_DIR/../common

java -ea -Djava.library.path=${SCRIPT_DIR} -Xmx1G -Djdk.crypto.KeyAgreement.legacyKDF=true -cp ".:*" com.oddlabs.tt.Main
