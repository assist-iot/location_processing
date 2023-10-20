#!/bin/sh
docker run \
  --rm \
  --interactive \
  --tty \
  --volume "$(pwd):/work" \
  "backplane/sql-formatter" \
  "$@"
