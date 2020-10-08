#!/bin/bash

set -xe

repository=$1
tag=$2

docker push ${repository}:${tag}