import xml.etree.ElementTree as ET
from typing import IO, List, NamedTuple

import normalize


class Abbrev(NamedTuple):
    """Python representation of an entry in oebt_abbreviations.xml."""
    spellouts: List[str]
    heading: str
    body: str


def read_abbrevs(abbrevs: IO[str]) -> List[Abbrev]:
    """Parse the XML from `abbrevs` into a list of `Abbrev` objects."""
    root = ET.parse(abbrevs).getroot()
    r = []  # type: List[Abbrev]
    for node in root.findall('source'):
        spellouts = [
            normalize.fix_entities(x.text or '')
            for x in node.findall('spellout')
        ]
        heading = normalize.fix_entities(node.findtext('heading', ''))
        body = normalize.fix_entities(node.findtext('body', ''))
        r.append(Abbrev(spellouts=spellouts, heading=heading, body=body))
    return r
