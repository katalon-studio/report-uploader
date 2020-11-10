#!/bin/sh

set -e

if [ -z "$@"]; then
  uploader.sh
else
  exec "$@"
fi