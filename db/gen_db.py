#!/usr/bin/env python3

from html import escape
from sqlite3 import Connection
from typing import Any, Dict, Iterable, List, Optional
import argparse
import base64
import csv
import json
import logging
import os
import re
import sqlite3

from abbrevs import Abbrev, read_abbrevs
from normalize import (acute_to_macron_in_nonitalic, ascify,
                       split_senses_into_paragraphs)

# This can be made more rigorous, but these are words appearing at least 50
# times in Beowulf.
VERY_COMMON_WORDS = set([
    'ac', 'aefter', 'aer', 'aet', 'he', 'hie', 'him', 'his', 'ic', 'in', 'mid',
    'ne', 'ofer', 'on', 'ond', 'se', 'swa', 'tha', 'thaer', 'thaes', 'thaet',
    'tham', 'the', 'thone', 'thonne', 'thu', 'to', 'under', 'waes', 'with'
])

# Don't link these, since they overlap with Mod. E. words (and also aren't in
# "web2").
EXTRA_MOD_ENG_WORDS = ['grimm', 'things', 'thomas', 'words']


def add_inflected_terms_to_db(inflections_path: str,
                              db: Connection,
                              limit: Optional[int] = None) -> None:
    """Add output from the morphological generator to `idx`."""
    fieldnames = [
        'rowid', 'formi', 'BT', 'title', 'stem', 'form', 'form_parts', 'var',
        'probability', 'function', 'wright', 'paradigm', 'para_id',
        'wordclass', 'class1', 'class2', 'class3', 'comment'
    ]
    formi_idx = fieldnames.index('formi')
    title_idx = fieldnames.index('title')
    with open(inflections_path, 'rt', encoding='UTF-8') as f:
        rd = csv.reader(f, dialect=csv.excel_tab)
        c = db.cursor()
        try:
            # Put inflected terms, including duplicates, in `idx_tmp`. `term`
            # is the inflected form; `join_term` is the canonical form.
            c.execute('CREATE TEMP TABLE idx_tmp (term TEXT, join_term TEXT)')
            n = 0
            for row in rd:
                n += 1
                term = ascify(row[formi_idx].strip())
                join_term = ascify(row[title_idx].strip())
                if len(term) > 0 and len(join_term) > 0:
                    c.execute('INSERT INTO idx_tmp VALUES (?, ?)',
                              (term, join_term))
                if n % 100000 == 0:
                    logging.info('Loaded {:,} inflected terms...'.format(n))
                if limit is not None and n >= limit:
                    break
            logging.info('Deduplicating {:,} terms into "idx"...'.format(n))
            c.execute('CREATE TEMP TABLE idx_new AS SELECT DISTINCT '
                      'idx_tmp.term, idx.nid FROM idx_tmp LEFT JOIN idx ON '
                      'idx_tmp.join_term = idx.term')
            c.execute('INSERT INTO idx SELECT term, nid FROM idx_new '
                      'WHERE nid IS NOT NULL')
            # Log statistics.
            c.execute('SELECT COUNT(*) FROM idx_new WHERE nid IS NOT NULL')
            n_new = c.fetchone()[0]
            c.execute('SELECT COUNT(*) FROM idx_new WHERE nid IS NULL')
            n_unmatched = c.fetchone()[0]
            c.execute('SELECT term FROM idx_new WHERE nid IS NULL LIMIT 10')
            examples = ', '.join([x[0] for x in c if x[0] is not None])
            logging.info('{:,} inflected terms added, {:,} unmatched.'.format(
                n_new, n_unmatched))
            logging.info('Unmatched examples: %s', examples)
            c.execute('DROP TABLE idx_new')
            c.execute('DROP TABLE idx_tmp')
        finally:
            c.close()


def add_extra_forms_to_db(extra_forms_path: str, db: Connection) -> None:
    """Add manually-collected forms to `idx`."""
    n = 0
    c = db.cursor()
    try:
        c.execute('CREATE TEMP TABLE idx_tmp (term TEXT, headword TEXT)')
        with open(extra_forms_path, 'rt', encoding='UTF-8') as f:
            for line in f:
                headword, forms = line.split()
                for form in forms.split(','):
                    c.execute('INSERT INTO idx_tmp VALUES (?, ?)',
                              (ascify(form), ascify(headword)))
                    n += 1
        c.execute('INSERT INTO idx SELECT DISTINCT idx_tmp.term, idx.nid '
                  'FROM idx_tmp JOIN idx ON idx_tmp.headword = idx.term')
        c.execute('DROP TABLE idx_tmp')
    finally:
        c.close()
    logging.info('Added %d extra forms.', n)


def max_nid_in_use(db: Connection) -> int:
    c = db.cursor()
    try:
        c.execute('SELECT MAX(nid) FROM defns')
        max_str = c.fetchone()[0]
        return 0 if max_str is None else int(max_str)
    finally:
        c.close()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument('--bt-dict', required=True)
    parser.add_argument('--inflections', required=True)
    parser.add_argument('--abbrevs', required=True)
    parser.add_argument('--extra-forms', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--limit', type=int, default=None)
    parser.add_argument('--mod-eng-dictionary', required=True)
    return parser.parse_args()


def add_bt_terms_to_db(oe_bt_path: str, db: Connection) -> None:
    """Add terms from `oe_bt_path` to `idx` and `defns` in `db`."""
    c = db.cursor()
    with open(oe_bt_path, 'rt', encoding='UTF-8') as oe_bt_file:
        oe_bt = json.load(oe_bt_file)
        n = max_nid_in_use(db)
        for term, entry in oe_bt.items():
            n += 1
            html = ''.join('<div>' + h + '</div>' for h in entry['defns'])
            title = ' / '.join(sorted(entry['headwords']))
            c.execute('INSERT INTO idx VALUES (?, ?)', (term, n))
            c.execute("INSERT INTO defns VALUES (?, ?, ?, 'e')",
                      (n, title, html))
    c.close()
    logging.info('Wrote {:,} terms from {}.'.format(n, oe_bt_path))


def tokenize_html(h: str) -> str:
    """Return non-HTML tokens from a defnition, `h`.

    The return value is suitable for indexing. This way, you can search the
    indexed text for things like "thaet" and get the entry for "þæt".
    """
    h = re.sub('<[^>]+>', '', h)
    h = ascify(h)
    return h


def build_defn_idx(db: Connection) -> None:
    """Generate a full-text index in `defn_idx` and `defn_content`.

    We use an external-content table so that we can index on definitions with
    normalized non-ASCII characters and stripped HTML, but still have the full
    HTML for rendering.

    The "terms" column is of the form "/term1/term2/.../".
    """
    c = db.cursor()
    try:
        c.execute('CREATE TEMP TABLE idx_uniq AS SELECT DISTINCT * FROM idx')
        c.execute('CREATE TABLE defn_content (id INTEGER PRIMARY KEY, '
                  'title TEXT, html TEXT, terms TEXT, entry_type TEXT)')
        c.execute('CREATE VIRTUAL TABLE defn_idx '
                  'USING fts4(title, html, terms, entry_type, '
                  'content="defn_content", notindexed="title",'
                  'notindexed="entry_type")')
        c.execute("INSERT INTO defn_content SELECT defns.nid, title, html, "
                  "'/' || GROUP_CONCAT(term, '/') || '/', entry_type "
                  "FROM defns LEFT JOIN idx_uniq ON defns.nid = idx_uniq.nid "
                  "GROUP BY defns.nid")
        c.execute('DROP TABLE idx_uniq')
        for row in c.execute('SELECT id, title, html, terms, entry_type '
                             'FROM defn_content').fetchall():
            html_tokens = tokenize_html(row[2])
            c.execute(('INSERT INTO defn_idx (docid, title, html, terms, '
                       'entry_type) VALUES (?, ?, ?, ?, ?)'),
                      (row[0], row[1], html_tokens, row[3], row[4]))
    finally:
        c.close()


def add_abbrevs_to_db(abbrevs: List[Abbrev], db: Connection) -> Dict[str, int]:
    """Add entries to `defns` and `idx` for abbreviations in `abbrevs`."""
    nid = 1 + max_nid_in_use(db)
    abbrev_nid = {}  # type: Dict[str, int]
    c = db.cursor()
    for a in abbrevs:
        for sp in a.spellouts:
            c.execute('INSERT INTO idx VALUES (?, ?)', (ascify(sp), nid))
        html = '<B>%s</B> (abbrev.) %s' % (escape(a.heading), escape(a.body))
        c.execute("INSERT INTO defns VALUES (?, ?, ?, 'a')",
                  (nid, a.heading, html))
        for s in a.spellouts:
            abbrev_nid[s] = nid
        nid += 1
    c.close()
    return abbrev_nid


def read_abbrevs_and_add_to_db(abbrevs_path: str,
                               db: Connection) -> Dict[str, int]:
    """Add abbreviations from `abbrevs_path` to `idx` and `defns`."""
    with open(abbrevs_path, 'rt') as ab:
        abbrevs = read_abbrevs(ab)
    abbrev_nid = add_abbrevs_to_db(abbrevs, db)
    logging.info('Added %d abbreviations.', len(abbrevs))
    return abbrev_nid


def write_rev_file(output_base_path: str, nbytes=16) -> None:
    """Write a random number to a resource with `output_base_path` as a prefix.

    This way, the app can tell when the dictionary is updated without hashing
    the entire file.
    """
    rev_path = output_base_path + '_rev'
    with open(rev_path, 'wb') as rev:
        with open('/dev/urandom', 'rb') as rand:
            rr = base64.b16encode(rand.read(nbytes))
        rev.write(rr)
    logging.info('Wrote rev %s to %s.', rr, rev_path)


def read_term_nid_mapping(db: Connection) -> Dict[str, int]:
    logging.info('Reading term-nid mapping...')
    c = db.cursor()
    # When deciding what a term links to, give abbreviations precedence, then
    # long entries.
    c.execute(
        'SELECT term, idx.nid FROM idx LEFT JOIN defns ON idx.nid = defns.nid '
        'ORDER BY entry_type = \'a\' DESC, LENGTH(html) DESC')
    term_nid = {}  # type: Dict[str, int]
    for row in c:
        term, nid = row
        if term not in term_nid:
            term_nid[term] = nid
    return term_nid


def linkify(defn: str, term_nid: Dict[str, int], rex: Any, current_nid: int,
            skip: int) -> str:
    """Return `defn` with HTML links added for terms in `term_nid`.

    `rex` is a regex, generated by `compile_linkify_regex`, for matching
    linkable strings. `current_nid` is the nid of `defn`; it makes no sense to
    link to yourself. `skip` is how many characters from the beginning of the
    entry not to link. This is a hack to avoid noisy links to variant
    spellings.
    """

    def preceded_by_v(m):
        return (m.start(0) >= 3
                and m.string[(m.start(0) - 3):m.start(0)] == 'v. ')

    def replace(m):
        assert len(m[0]) > 0, 'm = %r, should have nonzero length' % (m, )
        if (m[0].startswith('<') or
            (m.start(0) < skip and not preceded_by_v(m) and ' ' not in m[0])
                or re.search('[0-9]', m[0])):
            return m[0]
        # Don't linkify words beginning or ending with dashes, since they tend
        # to refer to variant spellings of compounds, not the affix itself.
        # e.g. the "here-" in "here- or hare-toga" does not refer to "here-",
        # it refers to "heretoga". Parsing that out is beyond the scope of this
        # code now.
        if m[0][0] == '-' or m[0][-1] == '-':
            return m[0]
        nid = term_nid.get(ascify(m[0]))
        if nid is not None and nid != current_nid:
            return '<a href="https://btc.invalid/%d">%s</a>' % (nid, m[0])
        else:
            return m[0]

    return rex.sub(replace, defn)


def compile_linkify_regex(abbrevs: Iterable[str], min_len: int) -> Any:
    # There are three parts of the regex:
    #
    # (1) An explicit match of <B> and <I> HTML blocks, so they can be ignored.
    #     Otherwise we'd match individual tagged words.
    # (2) An explicit OR of the abbreviation literals. This way we can match
    #     multi-word terms with periods. There are only a few hundred, so we
    #     can put them all in a regex. It is preceded by \b to keep things like
    #     "DER." from matching "R.", but since abbreviations tend to have "."
    #     at the end, we omit the \b at the end, since it wouldn't match and is
    #     not necessary.
    # (3) A generic "word" term, used for regular O.E. words. Dashes can only
    #     show up in the middle, but that is enforced by `linkify`.
    #
    # Matches to this regex are considered for linkification in `linkify`
    # though (1) is always dropped.
    ab = '|'.join(re.escape(a) for a in sorted(abbrevs, key=lambda x: -len(x)))
    if ab == '':
        ab = 'PLACEHOLDER ABBREV FOR TESTS WITH NO ABBREVS'
    return re.compile(r'(<[BI]>.*?</[BI]>|\b(%s)|[-\w]{%d,})' % (ab, min_len))


def linkify_and_normalize_defns(db: Connection, term_nid: Dict[str, int],
                                abbrevs: Iterable[str]) -> None:
    """Replace the `html` column of `defns` in `db` with linkified entries.

    `term_nid` has the link information. `abbrevs` is a list of abbreviations;
    it is used when generating a regex so that it can match multi-word terms.

    Also convert acute accents in nonitalic text to macrons to match current
    usage.
    """
    logging.info('Linkifying definitions...')
    new_defn: Dict[int, str] = {}
    c = db.cursor()
    c.execute('SELECT nid, html FROM defns')
    rex = compile_linkify_regex(abbrevs, min_len=4)
    for nid_str, term_html in c:
        nid = int(nid_str)
        term_html = linkify(term_html, term_nid, rex, nid, skip=40)
        term_html = split_senses_into_paragraphs(term_html)
        term_html = acute_to_macron_in_nonitalic(term_html)
        new_defn[nid] = term_html
    logging.info('Writing linkified definitions...')
    for n, d in new_defn.items():
        c.execute('UPDATE defns SET html = ? WHERE nid = ?', (d, n))


def main() -> None:
    args = parse_args()
    logging.basicConfig(format='%(asctime)s: %(message)s',
                        level=logging.INFO,
                        datefmt='%Y-%m-%dT%H:%M:%S')

    with open(args.mod_eng_dictionary, 'rt') as mod_eng_dict:
        mod_e_words = mod_eng_dict.read().strip().split()
        exclude_words = set(mod_e_words).union(
            set(EXTRA_MOD_ENG_WORDS)).union(VERY_COMMON_WORDS)

    # Create a new dictionary file with temporary tables for the terms.
    # `entry_type` is 'a' for abbreviations and 'e' for dictionary entries.
    try:
        os.remove(args.output)
    except OSError:
        pass
    db = sqlite3.connect(args.output, timeout=3600)
    c = db.cursor()
    c.execute('CREATE TEMP TABLE idx (term TEXT NOT NULL, nid INT NOT NULL)')
    c.execute('CREATE TEMP TABLE defns (nid INT PRIMARY KEY, '
              'title TEXT NOT NULL, html TEXT NOT NULL, '
              'entry_type CHAR(1) NOT NULL)')
    c.close()

    # Add the definitions, inflected forms, and abbreviations to `idx` and
    # `defns`.
    abbrev_nid = read_abbrevs_and_add_to_db(args.abbrevs, db)
    add_bt_terms_to_db(args.bt_dict, db)
    add_extra_forms_to_db(args.extra_forms, db)
    add_inflected_terms_to_db(args.inflections, db, limit=args.limit)

    term_nid = read_term_nid_mapping(db)
    for w in exclude_words:
        if w in term_nid:
            del term_nid[w]
    linkify_and_normalize_defns(db, term_nid, abbrev_nid.keys())

    # Create `defn_content` and `defn_idx`, full-text-indexed versions of the
    # dictionary.
    build_defn_idx(db)

    # Clear tables other than `defn_content` and `defn_idx`, and clean up the
    # database so we can ship it.
    c = db.cursor()
    c.execute('DROP TABLE idx')
    c.execute('DROP TABLE defns')
    c.close()
    db.commit()
    c = db.cursor()
    c.execute('VACUUM')
    c.close()
    db.commit()
    db.close()

    write_rev_file(args.output)


if __name__ == '__main__':
    main()
