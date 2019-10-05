#!/bin/bash -e
# Don't pass -x. A token is on the command line.

artifact_url="https://20-210972025-gh.circle-artifacts.com/0/root/project/output.txt.bz2"
curl --fail --location "${artifact_url}?circle-token=${CIRCLECI_ARTIFACT_TOKEN}" | bzip2 -dc > db/generator-output.txt
