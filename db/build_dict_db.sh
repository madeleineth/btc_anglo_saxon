#!/bin/bash

# Use `oe_bosworthtoller.txt.bz2`, `oebt_abbreviations.xml`, and
# `generator-output.txt` to generate the sqlite3 databases
# `app/src/main/res/raw/dictdb` and a random revision identifier,
# `app/src/main/res/raw/dictdb_rev`.
#
# If `DICT_LIMIT_LINES` is set, limit the number of lines used in the input
# files each to that many lines. This can be used to smoke-test the process;
# otherwise it takes around ten minutes to run.

set -eux -o pipefail

bzip2 -ckd db/oe_bosworthtoller.txt.bz2 | db/parse_scanned_bt.py > db/oe_bt.json

head "-${DICT_LIMIT_LINES:-1000000000}" < db/generator-output.txt > db/generator-output-trimmed.txt

db/gen_db.py ${DICT_LIMIT_LINES:+--limit $DICT_LIMIT_LINES} --bt-dict db/oe_bt.json \
    --inflections db/generator-output-trimmed.txt --abbrevs db/oebt_abbreviations.xml --extra-forms db/extra-forms.txt \
    --mod-eng-dictionary /usr/share/dict/words --output app/src/main/res/raw/dictdb
