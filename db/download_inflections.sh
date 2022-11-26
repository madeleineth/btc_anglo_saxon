#!/bin/bash

set -eu -o pipefail

artifact_url="https://output.circle-artifacts.com/output/job/f9e941f4-21ff-4875-a09b-5cb211bf7d33/artifacts/0/output.txt.bz2"
curl -o db/generator-output.txt.bz2 --fail --location "${artifact_url}"
bzip2 -d db/generator-output.txt.bz2
