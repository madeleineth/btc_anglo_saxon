#!/bin/bash -e
# DO NOT ADD THE -x FLAG, A KEY IS USED IN THIS SCRIPT.

# Rebuild the word database without a size limit.
# shellcheck source=/dev/null
source venv/bin/activate
./db/build_dict_db.sh

./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest

keyfile="$(mktemp)"
trap 'rm -f "$keyfile"' EXIT
echo "$GCP_KEY" | base64 -d > "$keyfile"
gcloud auth activate-service-account "--key-file=$keyfile"

gcloud firebase test android run --project anglo-saxon-dict \
    --app ./app/build/outputs/apk/debug/app-debug.apk \
    --test ./app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
