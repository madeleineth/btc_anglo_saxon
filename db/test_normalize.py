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


if __name__ == '__main__':
    unittest.main()
