#!/bin/sh
docker-compose -f docker/docker-compose.dev.yml up postgres pgadmin mosquitto mqtt-explorer --build
