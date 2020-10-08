#!/bin/bash

set -xe

name=$1

docker build -t ${name} -f Dockerfile .