#!/bin/bash

set -xe

repository=$1
tag=$2

docker rmi ${repository}:${tag} || echo 'No image to delete.'