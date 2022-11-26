#!/usr/bin/env python3

import io
import unittest

from abbrevs import Abbrev, read_abbrevs

TEST_ABBREV_XML = """
<!DOCTYPE document [
<!ELEMENT document ((source | PAGEBREAK | section_header)+)>
<!ELEMENT source (spellout+, heading, body)>
<!ELEMENT spellout (#PCDATA)>
<!ELEMENT heading (#PCDATA)>
<!ELEMENT body (#PCDATA)>
<!ENTITY AElig "&amp;AElig;">
]>
<document>
    <source>
        <spellout>A</spellout>
        <spellout>B</spellout>
        <heading>C</heading>
        <body>&AElig;lfric</body>
    </source>
</document>"""


class TestAbbrevs(unittest.TestCase):
    def test_read_abbrevs(self) -> None:
        with io.StringIO(TEST_ABBREV_XML) as ax:
            ab = read_abbrevs(ax)
        expected = Abbrev(spellouts=["A", "B"], heading="C", body="Ælfric")
        self.assertEqual([expected], ab)


if __name__ == "__main__":
    unittest.main()
