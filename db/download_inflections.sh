#!/bin/bash

# Don't pass -x. A token is on the command line.
set -eu -o pipefail

artifact_url="https://43-210972025-gh.circle-artifacts.com/0/output.txt.bz2"
curl -o db/generator-output.txt.bz2 --fail --location "${artifact_url}?circle-token=${CIRCLECI_ARTIFACT_TOKEN}"
bzip2 -d db/generator-output.txt.bz2
