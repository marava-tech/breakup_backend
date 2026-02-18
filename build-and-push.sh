#!/bin/bash
set -e

IMAGE="maravatechnologies/breakup-stories-api:${1:-latest}"

mvn package -DskipTests -q
docker build -t "$IMAGE" .
docker push "$IMAGE"

echo "Done: $IMAGE"
