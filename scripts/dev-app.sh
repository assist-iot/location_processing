#!/bin/sh
docker-compose -f docker/docker-compose.dev.yml up app --build --no-log-prefix
