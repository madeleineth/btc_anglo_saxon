#!/usr/bin/env python3

import io

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


def test_read_abbrevs() -> None:
    with io.StringIO(TEST_ABBREV_XML) as ax:
        ab = read_abbrevs(ax)
    expected = Abbrev(spellouts=["A", "B"], heading="C", body="Ælfric")
    assert [expected] == ab
