#!/bin/bash

set -ex

mkdir -p logs
ant compile matchmaker
java -Djdk.crypto.KeyAgreement.legacyKDF=true -cp "build/matchmaking.jar:../common/lib/java/commons-pool-1.2.jar:../common/lib/java/commons-dbcp-1.2.1.jar:../common/lib/java/commons-collections-3.1.jar:../common/lib/java/mysql-connector-j-9.3.0.jar" com.oddlabs.matchserver.MatchmakingServer 
