#!/bin/bash -ex

flake8 --ignore E203,E501,W503 db inflect
isort --profile black --line-length 100 --check db inflect
mypy --python-version 3.10 --check-untyped-defs --strict --exclude venv .
black --check --line-length 100 --exclude venv .
pytest --tb native db inflect
