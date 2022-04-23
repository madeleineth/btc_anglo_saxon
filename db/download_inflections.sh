#!/bin/bash

set -eu -o pipefail

artifact_url="https://output.circle-artifacts.com/output/job/f67d0e46-0d10-4baa-8d95-05f9fd39e55a/artifacts/0/output.txt.bz2"
curl -o db/generator-output.txt.bz2 --fail --location "${artifact_url}"
bzip2 -d db/generator-output.txt.bz2
