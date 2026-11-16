#!/usr/bin/env sh
set -eu
tag=0000000000000000000000000000000000000000
for environment in dev staging production; do
  helm lint deploy/helm/little-list -f "deploy/environments/$environment.yaml" --set-string image.tag="$tag"
  helm template little-list deploy/helm/little-list -f "deploy/environments/$environment.yaml" --set-string image.tag="$tag" > /dev/null
done
