#!/bin/bash
echo "This pipeline has been initiated by $DRONE_SOURCE_BRANCH"
if ! [[ "${DRONE_SOURCE_BRANCH}" == "uat/hotfix"* ]]; then
  echo 'This Pull Request includes the term hotfix, skipping this Pipeline' >&2
  exit 78
fi
