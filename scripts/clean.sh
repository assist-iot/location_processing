#!/bin/sh
docker rm --force --volumes lp-app-dev lp-postgres-dev lp-mqtt-explorer-dev
sbt clean
find src -name "*.semanticdb" -type f -delete
