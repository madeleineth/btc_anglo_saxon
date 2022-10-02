#!/bin/bash

set -eu -o pipefail

artifact_url="https://output.circle-artifacts.com/output/job/556983ac-9970-40e4-8535-2dd8bba73865/artifacts/0/output.txt.bz2"
curl -o db/generator-output.txt.bz2 --fail --location "${artifact_url}"
bzip2 -d db/generator-output.txt.bz2
