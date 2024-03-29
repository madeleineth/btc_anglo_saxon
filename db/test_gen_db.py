#!/usr/bin/env python3

import gen_db


def test_linkify() -> None:
    # Checks that text wrapped in <B> is not linked (it tends to be Mod. E.
    # or Latin), that the longest abbreviations are linked, and that
    # normalization works as expected ("Hwæt" -> "hwaet").
    term_nid = {"hwaet": 7, "a": 8, "b": 9, "a b": 10, "c": 11}
    abbrevs = ["A.", "b.", "A. b."]
    rex = gen_db.compile_linkify_regex(abbrevs, min_len=3)
    s = "<B>a</B> Hwæt! A. b. c"
    h = gen_db.linkify(s, term_nid, rex, current_nid=11, skip=0)
    eh = (
        '<B>a</B> <a href="https://btc.invalid/7">Hwæt</a>! '
        '<a href="https://btc.invalid/10">A. b.</a> c'
    )
    assert eh == h


def test_linkify_skip() -> None:
    term_nid = {"aaa": 8}
    rex = gen_db.compile_linkify_regex([], min_len=3)
    h = gen_db.linkify("aaa aaa", term_nid, rex, current_nid=11, skip=1)
    eh = 'aaa <a href="https://btc.invalid/8">aaa</a>'
    assert eh == h


def test_linkify_variant() -> None:
    term_nid = {"aaa": 8}
    rex = gen_db.compile_linkify_regex([], min_len=3)
    h = gen_db.linkify("aaa v. aaa", term_nid, rex, current_nid=11, skip=8)
    eh = 'aaa v. <a href="https://btc.invalid/8">aaa</a>'
    assert eh == h


def test_linkify_with_dashes() -> None:
    term_nid = {"a": 8, "aa": 9}
    rex = gen_db.compile_linkify_regex([], min_len=1)
    h = gen_db.linkify("a a-a a- -a", term_nid, rex, current_nid=0, skip=0)
    eh = '<a href="https://btc.invalid/8">a</a> ' '<a href="https://btc.invalid/9">a-a</a> a- -a'
    assert eh == h
