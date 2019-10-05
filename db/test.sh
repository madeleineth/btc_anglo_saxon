#!/bin/bash -ex

flake8 db/*.py
mypy --check-untyped-defs db/*.py
yapf -d db/*.py
python -m unittest discover db
