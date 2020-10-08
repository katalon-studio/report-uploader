#!/bin/bash

set -ex

git status
git add -A
git commit -m update
git push origin build-image