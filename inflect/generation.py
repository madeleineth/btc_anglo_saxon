import re
from typing import Any


class Generation:
    """Generator of inflected forms for a single word.

    `rules` is a mapping from category to "inherit" and "rules" objects.
    "inherit" specifies a (possibly-recursive) parent category to use if a rule
    does not specify how to generate a form. "rules" is a mapping from
    form-name to rule. A rule is either:
        - a format string (which may refer to other forms named in braced
          substrings, e.g. "{root}aþ" to say that this form is the "root" form
          with the "aþ" suffix added. A list of format strings may be used if a
          form has multiple variations.
        - a dict with keys "from", "match", and "value". "from" names another
          form. "match" is a regular expression. "value" is a replacement
          string (as in `re.sub`) which may contain numeric backreferences.
    The firt type of rule is used mainly to add endings; the second is used
    mainly to remove them. It's probably easier to just look at
    `inflect/data/rules.yaml` to figure out how this is used.

    `categ` specifies the category for the work being generated (which must be
    a key into `rules`).

    `initial-parts` specifies the parts of speech to bootstrap from, typically
    {'plain-inf': [...]}.
    """

    def __init__(self, categ: str, rules: dict[str, Any], initial_parts: dict[str, str]) -> None:
        self.categ = categ
        self.rules = self.flatten_rules(categ, rules)
        self.parts: dict[str, list[str]] = {k: [v] for k, v in initial_parts.items()}

    @staticmethod
    def flatten_rules(categ: str, rules: dict[str, dict[str, Any]]) -> dict[str, list[Any]]:
        """Generate rules for `categ` only, flattening all "inherit" references."""
        depth = 0
        all_rules: dict[str, list[Any]] = {}
        while True:
            if depth >= 20:
                raise ValueError(f"Probable infinite loop expanding rules for {categ}.")
            current_rules = rules[categ]
            for r, v in current_rules.get("rules", {}).items():
                if r not in all_rules:
                    if isinstance(v, str) or isinstance(v, dict):
                        v = [v]
                    all_rules[r] = v
            parent = current_rules.get("inherit")
            if parent is None:
                break
            if parent not in rules:
                raise ValueError(f"Category '{categ}' specifies a nonexistent parent, '{parent}'.")
            categ = parent
            depth += 1
        return all_rules

    def generate(self, part: str) -> list[str]:
        if part in self.parts:
            return self.parts[part]
        if part not in self.rules:
            raise ValueError(f"No such part '{part}' in rules {self.rules}.")
        r = self.rules[part]
        assert isinstance(r, list)
        self.parts[part] = []
        for rr in r:
            self.parts[part].extend(self._generate_from(rr))
        self.parts[part] = sorted(set(self.parts[part]))
        return self.parts[part]

    def _generate_from(self, r: Any) -> list[str]:
        if isinstance(r, str):
            return self._generate_from_pattern(r)
        elif isinstance(r, dict):
            return self._generate_from_substitution(r)
        else:
            raise ValueError("Unsupported rule '{r}'.")

    def _generate_from_pattern(self, r: str) -> list[str]:
        m = re.search(r"{([\w-]*)}", r)
        if m is None:  # No {...} expressions left.
            return [r]
        else:
            sub_part = m.group(1)
            values = []
            for v in self.generate(sub_part):
                r2 = re.sub("{" + sub_part + "}", v, r)
                values.extend(self._generate_from_pattern(r2))
            return values

    def _generate_from_substitution(self, r: dict[str, str]) -> list[str]:
        values: list[str] = []
        for v in self.generate(r["from"]):
            m = re.match("^" + r["match"] + "$", v)
            if m is None:
                raise ValueError(
                    f"Value of {r['from']}, '{v}', does not match pattern '{r['match']}'."
                )
            try:
                values.append(m.expand(r["value"]))
            except re.error as e:
                raise ValueError(f"Error expanding '{r['match']}' with '{r['value']}'") from e
        return values
