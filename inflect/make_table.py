#!/usr/bin/env python3
# Generates a YAML table of verb inflections.

import os
import re
import sys

import yaml

from generation import Generation

PARTS = [
    "plain-inf",
    "to-inf",
    "pres-part",
    "pres-ind-1sg",
    "pres-ind-2sg",
    "pres-ind-3sg",
    "pres-ind-pl",
    "imp-sg",
    "imp-pl",
    "pres-sub-sg",
    "pres-sub-pl",
    "past-ind-1sg",
    "past-ind-2sg",
    "past-ind-3sg",
    "past-ind-pl",
    "past-sub-sg",
    "past-sub-pl",
    "past-part",
]


def merge_forms(words: dict[str, dict[str, str]], new_words: dict[str, dict[str, str]]) -> None:
    for w in new_words:
        if w in words:
            for k in set(words[w].keys()).union(set(new_words[w].keys())) - {"mod-e"}:
                assert k in words[w], (k, w)
                assert k in new_words[w], (k, w)
                assert words[w][k] == new_words[w][k], (w, k, words[w][k], new_words[w][k])
        else:
            words[w] = new_words[w]


def read_rule_based(data_dir: str) -> dict[str, dict[str, str]]:
    with open(os.path.join(data_dir, "rules.yaml"), "rt") as y:
        rules = yaml.safe_load(y)
    with open(os.path.join(data_dir, "words.yaml"), "rt") as y:
        word_categories = yaml.safe_load(y)
    with open(os.path.join(data_dir, "explicit.yaml"), "rt") as y:
        explicit = yaml.safe_load(y)
    for word, rule in explicit.items():
        pseudo_category = f"explicit-{word}"
        rules[pseudo_category] = rule
        word_categories[pseudo_category] = [word]  # fake category with a single word in it
    words: dict[str, dict[str, str]] = {}
    for categ, word_list in word_categories.items():
        for word in word_list:
            m = re.match(r"([^,]+), (.+)$", word)
            if m:
                spellings = m.group(1).split("/")
                mod_e = m.group(2)
            else:
                spellings = word.split("/")
                mod_e = None
            for spelling in spellings:
                init = {"plain-inf": spelling}
                if mod_e:
                    init["mod-e"] = mod_e
                gen = Generation(categ, rules, init)
                words[spelling] = {}
                for part in PARTS + ["mod-e"]:
                    if part in ("plain-inf", "past-ind-1sg"):
                        continue
                    w = "/".join(gen.generate(part))
                    if part == "past-part" and w and not w.startswith("(ge)"):
                        w = "(ge)" + w
                    if part == "mod-e" and w and not w.startswith("to "):
                        w = "to " + w
                    if w:
                        words[spelling][part] = w
    return words


def read_wikidata(data_dir: str) -> dict[str, dict[str, str]]:
    with open(os.path.join(data_dir, "wikidata.yaml"), "rt") as inp:
        words: dict[str, dict[str, str]] = yaml.safe_load(inp)
    for w in words:
        if "mod-e" in words[w] and not words[w]["mod-e"].startswith("to "):
            words[w]["mod-e"] = "to " + words[w]["mod-e"]
    return words


def read_all_forms() -> dict[str, dict[str, str]]:
    """Returns a mapping from infinitive to part-to-form."""
    data_dir = os.path.join(os.path.dirname(__file__), "data")
    words = read_wikidata(data_dir)
    rule_based = read_rule_based(data_dir)
    merge_forms(words, rule_based)
    return words


def main() -> None:
    words = read_all_forms()
    yaml.dump(words, stream=sys.stdout, allow_unicode=True)


if __name__ == "__main__":
    main()
