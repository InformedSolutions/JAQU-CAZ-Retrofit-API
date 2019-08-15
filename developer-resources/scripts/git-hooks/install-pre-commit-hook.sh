#!/usr/bin/env bash

PROJECT_ROOT_DIR="${0%/*}/../../.."
GIT_HOOKS_SCRIPTS_DIR="${0%/*}"
GIT_HOOKS_DIR="$PROJECT_ROOT_DIR/.git/hooks"
PRE_COMMIT_FILENAME="pre-commit"

pushd $GIT_HOOKS_DIR > /dev/null

if [ -f $PRE_COMMIT_FILENAME ]; then
  echo "Aborting: a pre-commit hook already exists in '.git/hooks' directory."
else
  ln -s "../../$GIT_HOOKS_SCRIPTS_DIR/pre-commit-hook.sh" "pre-commit"
  echo "Created pre-commit hook."
fi

popd > /dev/null
