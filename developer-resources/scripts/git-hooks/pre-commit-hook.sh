#!/usr/bin/env bash

echo "Running pre-commit hook ..."

git stash -u --keep-index -q

make clean build
RESULT=$?

git stash pop -q

if [ $RESULT -ne 0 ]; then
  echo "Pre-commit hook failed"
  exit 1
fi

echo "Pre-commit hook succeeded"
exit 0
