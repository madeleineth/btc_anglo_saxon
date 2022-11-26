#!/usr/bin/env python3

import normalize


def test_fix_entities() -> None:
    fixed = normalize.fix_entities("&f-rune;&u-rune;&th-rune;&o-rune;&r-rune;&c-rune;")
    assert "ᚠᚢᚦᚩᚱᚳ" == fixed


def test_ascify() -> None:
    ascified = normalize.ascify("Gúð cyning? ")
    assert "guth cyning" == ascified


def test_acute_to_macron() -> None:
    assert "foō BǢR" == normalize.acute_to_macron("foó BǼR")


def test_acute_to_macron_in_nonitalic() -> None:
    s = normalize.acute_to_macron_in_nonitalic("foó<I>bǽr\n</I>báz")
    assert "foō<I>bǽr\n</I>bāz" == s


def test_split_senses_into_paragraphs() -> None:
    s1 = "foo <B>II.</B> bar cf. <B>I.</B><B> III. </B>baz"
    s2 = "foo <p><B>II.</B> bar cf. <B>I.</B><p><B> III. </B>baz"
    assert s2 == normalize.split_senses_into_paragraphs(s1)
