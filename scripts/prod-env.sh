#!/bin/sh
docker-compose -f docker/docker-compose.prod.yml up postgres --build
