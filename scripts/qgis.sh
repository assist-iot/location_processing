#!/bin/sh
xhost +
docker-compose -f docker/docker-compose.dev.yml up qgis --build
xhost -
