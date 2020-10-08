#!/bin/bash

set -xe

repository=$1
tag=$2

function docker_tag_exists() {
    EXISTS=$(curl -s https://hub.docker.com/v2/repositories/${repository}/tags/?page_size=10000 | jq -r "[.results | .[] | .name == \"${tag}\"] | any")
    test $EXISTS = true
}

if docker_tag_exists; then
    exit 1
else
    exit 0
fi