#!/bin/bash

set -xe

repository=$1
name=$2
tag=$3

docker tag ${name} ${repository}:${tag}