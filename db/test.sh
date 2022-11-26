#!/bin/bash -ex

flake8 --ignore E203,E501,W503 db/*.py
isort --profile black --line-length 100 --check db/*.py
mypy --python-version 3.7 --check-untyped-defs --strict db/*.py
black --check --line-length 100 db/*.py
pytest --tb native db
