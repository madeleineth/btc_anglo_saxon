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


if __name__ == '__main__':
    unittest.main()
