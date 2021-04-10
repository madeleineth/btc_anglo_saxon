#!/usr/bin/env python3

import unittest

import normalize


class TestNormalize(unittest.TestCase):

    def test_fix_entities(self) -> None:
        fixed = normalize.fix_entities(
            '&f-rune;&u-rune;&th-rune;&o-rune;&r-rune;&c-rune;')
        self.assertEqual('ᚠᚢᚦᚩᚱᚳ', fixed)

    def test_ascify(self) -> None:
        ascified = normalize.ascify('Gúð cyning? ')
        self.assertEqual('guth cyning', ascified)

    def test_acute_to_macron(self) -> None:
        self.assertEqual('foō BǢR', normalize.acute_to_macron('foó BǼR'))

    def test_acute_to_macron_in_nonitalic(self) -> None:
        s = normalize.acute_to_macron_in_nonitalic('foó<I>bǽr\n</I>báz')
        self.assertEqual('foō<I>bǽr\n</I>bāz', s)

    def test_split_senses_into_paragraphs(self) -> None:
        s1 = 'foo <B>II.</B> bar cf. <B>I.</B><B> III. </B>baz'
        s2 = 'foo <p><B>II.</B> bar cf. <B>I.</B><p><B> III. </B>baz'
        self.assertEqual(s2, normalize.split_senses_into_paragraphs(s1))


if __name__ == '__main__':
    unittest.main()
