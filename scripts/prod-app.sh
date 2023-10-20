#!/bin/sh
docker-compose -f docker/docker-compose.prod.yml up app --build --no-log-prefix
