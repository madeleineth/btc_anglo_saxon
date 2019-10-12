#!/usr/bin/env python3

from html import escape
from typing import Dict, List, Optional
import argparse
import base64
import csv
import json
import logging
import os
import re
import sqlite3

from abbrevs import Abbrev, read_abbrevs
from normalize import ascify


def add_inflected_terms_to_db(inflections_path: str,
                              db: sqlite3.Connection,
                              limit: Optional[int] = None) -> None:
    """Add output from the morphological generator to `idx`."""
    fieldnames = [
        'rowid', 'formi', 'BT', 'title', 'stem', 'form', 'form_parts', 'var',
        'probability', 'function', 'wright', 'paradigm', 'para_id',
        'wordclass', 'class1', 'class2', 'class3', 'comment'
    ]
    with open(inflections_path, 'rt', encoding='UTF-8') as f:
        rd = csv.DictReader(f, fieldnames=fieldnames, dialect=csv.excel_tab)
        c = db.cursor()
        try:
            # Put inflected terms, including duplicates, in `idx_tmp`. `term`
            # is the inflected form; `join_term` is the canonical form.
            c.execute('CREATE TEMP TABLE idx_tmp (term TEXT, join_term TEXT)')
            n = 0
            for row in rd:
                n += 1
                term = ascify(row['formi'].strip())
                join_term = ascify(row['title'].strip())
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


def add_extra_forms_to_db(extra_forms_path: str,
                          db: sqlite3.Connection) -> None:
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


def max_nid_in_use(db: sqlite3.Connection) -> int:
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
    return parser.parse_args()


def linkify_abbrevs(html: str, abbrev_nid: Dict[str, int]) -> str:
    # This is ugly. The extra complexity is to handle the case where we have
    # abbreviations where one is a substring of another, e.g. "A B" and "B". We
    # don't want a result like <a href="...">A <a href="...">B</a></a>.
    for ab, nid in sorted(abbrev_nid.items(), key=lambda x: -len(x[0])):
        replacement = '<a href="btc://%d">XXNID%dXX</a>' % (nid, nid)
        html = html.replace(ab, replacement)
    for ab, nid in abbrev_nid.items():
        html = html.replace('XXNID%dXX' % (nid, ), ab)
    return html


def add_bt_terms_to_db(oe_bt_path: str, db: sqlite3.Connection,
                       abbrev_nid: Dict[str, int]) -> None:
    """Add terms from `oe_bt_path` to `idx` and `defns` in `db`."""
    c = db.cursor()
    with open(oe_bt_path, 'rt', encoding='UTF-8') as oe_bt_file:
        oe_bt = json.load(oe_bt_file)
        n = max_nid_in_use(db)
        for term, entry in oe_bt.items():
            n += 1
            html = ''.join('<div>' + h + '</div>' for h in entry['defns'])
            html = linkify_abbrevs(html, abbrev_nid)
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


def build_defn_idx(db: sqlite3.Connection) -> None:
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


def add_abbrevs_to_db(abbrevs: List[Abbrev],
                      db: sqlite3.Connection) -> Dict[str, int]:
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
                               db: sqlite3.Connection) -> Dict[str, int]:
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


def main() -> None:
    args = parse_args()
    logging.basicConfig(format='%(asctime)s: %(message)s',
                        level=logging.INFO,
                        datefmt='%Y-%m-%dT%H:%M:%S')

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
    add_bt_terms_to_db(args.bt_dict, db, abbrev_nid)
    add_extra_forms_to_db(args.extra_forms, db)
    add_inflected_terms_to_db(args.inflections, db, limit=args.limit)

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
