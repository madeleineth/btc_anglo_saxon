import re
import unicodedata

# Match bolded roman numerals that look like sense starts, not references to a
# sense (which are preceded by "cf.").
SENSE_START_REGEX = re.compile(r"(?<!cf\. )(<B> *[IVX]{1,4}\. *</B>)")


def fix_entities(e: str) -> str:
    """Change XML entities used in raw BT data to regular unicode."""
    e = e.replace("&alpha-tonos;", "ά")
    e = e.replace("&alpha;", "α")
    e = e.replace("&delta;", "δ")
    e = e.replace("&Delta;", "Δ")
    e = e.replace("&epsilon-tonos;", "έ")
    e = e.replace("&epsilon;", "ε")
    e = e.replace("&eta-tonos;", "ή")
    e = e.replace("&gamma;", "γ")
    e = e.replace("&Gamma;", "Γ")
    e = e.replace("&iota-tonos;", "ί")
    e = e.replace("&iota;", "ι")
    e = e.replace("&kappa;", "κ")
    e = e.replace("&lambda;", "λ")
    e = e.replace("&mu;", "μ")
    e = e.replace("&nu;", "ν")
    e = e.replace("&omega-tonos;", "ώ")
    e = e.replace("&omega;", "ω")
    e = e.replace("&omicron-tonos;", "ό")
    e = e.replace("&omicron;", "ο")
    e = e.replace("&pi;", "π")
    e = e.replace("&rho;", "ρ")
    e = e.replace("&Rho;", "Ρ")
    e = e.replace("&sigma;", "σ")
    e = e.replace("&tau;", "τ")
    e = e.replace("&Tau;", "Τ")
    e = e.replace("&upsilon;", "υ")
    e = e.replace("&upsilon-", "υ")
    e = e.replace("&alpha-", "α")
    e = e.replace("&upsilon-dasia-oxia;", "ὕ")
    e = e.replace("&iota-diar;", "ϊ")
    e = e.replace("&zeta;", "ζ")
    e = e.replace("&beta;", "β")
    e = e.replace("&Beta;", "Β")
    e = e.replace("&xi;", "ξ")
    e = e.replace("&eta;", "η")
    e = e.replace("&psi;", "ψ")
    e = e.replace("&phi;", "φ")
    e = e.replace("&upsilon-tonos;", "ύ")
    e = e.replace("&chi;", "χ")
    e = e.replace("&Chi;", "Χ")
    e = e.replace("&theta;", "θ")
    e = e.replace("&sigmaf;", "ς")
    e = e.replace("&iota-oxia;", "ί")

    e = e.replace("&aelig;", "æ")
    e = e.replace("&ealig;", "æ")  # typo
    e = e.replace("&alig;", "æ")  # typo
    e = e.replace("&aelig-circ;", "æ")
    e = e.replace("&e-sub;", "e")
    e = e.replace("&quot;", '"')
    e = e.replace("&highquote;", '"')
    e = e.replace("&lowquote;", '"')
    e = e.replace("&AElig;", "Æ")
    e = e.replace("&aelig-long;", "ǣ")
    e = e.replace("&AElig-long;", "Ǣ")
    e = e.replace("&aelig-acute;", "ǽ")
    e = e.replace("&aleig-acute;", "ǽ")  # typo
    e = e.replace("&AElig-acute;", "Ǽ")
    e = e.replace("&oelig;", "œ")
    e = e.replace("&oelig-acute;", "œ")  # oe-acute does not exist in Unicode?

    e = e.replace("&A-long;", "Ā")
    e = e.replace("&a-long;", "ā")
    e = e.replace("&a-short;", "ă")
    e = e.replace("&A-short;", "Ă")
    e = e.replace("&e-long;", "ē")
    e = e.replace("&E-long;", "Ē")
    e = e.replace("&e-short;", "ĕ")
    e = e.replace("&i-long;", "ī")
    e = e.replace("&I-long;", "Ī")
    e = e.replace("&i-longg", "īg")
    e = e.replace("&i-short;", "ĭ")
    e = e.replace("&o-long;", "ō")
    e = e.replace("&O-long;", "Ō")
    e = e.replace("&o-short;", "ŏ")
    e = e.replace("&O-short;", "Ŏ")
    e = e.replace("&u-long;", "ū")
    e = e.replace("&U-long;", "Ū")
    e = e.replace("&u-short;", "ŭ")
    e = e.replace("&y-long;", "ȳ")
    e = e.replace("&Y-long;", "Ȳ")
    e = e.replace("&y-short;", "y")

    e = e.replace("&aring;", "å")
    e = e.replace("&acirc;", "å")
    e = e.replace("&icirc;", "i")  # not in unicode
    e = e.replace("&ecirc;", "e")
    e = e.replace("&ocirc;", "o")
    e = e.replace("&ucirc;", "u")
    e = e.replace("&Ucirc;", "U")
    e = e.replace("&w-circ;", "w")
    e = e.replace("&ntilde;", "ñ")
    e = e.replace("&tilde;", "")
    e = e.replace("&c-tilde;", "c")
    e = e.replace("&n-long;", "n")
    e = e.replace("&l-bar;", "l")  # ???
    e = e.replace("&aacute;", "á")
    e = e.replace("&Aacute;", "Á")
    e = e.replace("&eacute;", "é")
    e = e.replace("&Eacute;", "É")
    e = e.replace("&iacute;", "í")
    e = e.replace("&Iacute;", "Í")
    e = e.replace("&oacute;", "ó")
    e = e.replace("&Oacute;", "Ó")
    e = e.replace("&uacute;", "ú")
    e = e.replace("&Uacute;", "Ú")
    e = e.replace("&yacute;", "ý")
    e = e.replace("&Yacute;", "Ý")

    e = e.replace("&thorn;", "þ")
    e = e.replace("&thron;", "þ")
    e = e.replace("&thorn-bar;", "ꝥ")
    e = e.replace("&THORN-bar;", "Ꝥ")
    e = e.replace("&THORN;", "Þ")
    e = e.replace("&eth;", "ð")
    e = e.replace("&ETH;", "Ð")

    e = e.replace("&Igrave;", "Ì")
    e = e.replace("&Ouml;", "Ö")
    e = e.replace("&Uuml;", "Ü")
    e = e.replace("&agrave;", "à")
    e = e.replace("&auml;", "ä")
    e = e.replace("&Auml;", "Ä")
    e = e.replace("&egrave;", "è")
    e = e.replace("&euml;", "ë")
    e = e.replace("&igrave;", "ì")
    e = e.replace("&iuml;", "ï")
    e = e.replace("&ograve;", "ò")
    e = e.replace("&ouml;", "ö")
    e = e.replace("&ugrave;", "ù")
    e = e.replace("&uuml;", "ü")

    e = e.replace("&sect;", "§")
    e = e.replace("&e-super;", "ᵉ")
    e = e.replace("&t-super;", "t")
    e = e.replace("&szlig;", "sz")
    e = e.replace("&yogh;", "ȝ")
    e = e.replace("&YOGH;", "Ȝ")
    e = e.replace("&c-hachek;", "ƈ")
    e = e.replace("&pound;", "£")
    e = e.replace("&bull;", "")
    e = e.replace("&e-odot;", "")
    e = e.replace("&b-bar;", "b")  # ???
    e = e.replace("&d-bar;", "d")  # ???
    e = e.replace("&t-udot;", "t")  # ???
    e = e.replace("&s-acute;", "s")
    e = e.replace("&dash-uncertain;", "-")
    e = e.replace("&amp;y-short;", "y")
    e = e.replace("&ethe ", "")
    e = e.replace("&para;", "¶")
    e = e.replace("&hand;", "")  # ???
    e = e.replace("&r-udot;", "r")  # ???
    e = e.replace("&dash-acute;", "-")
    e = e.replace("&c-acute;", "c")
    e = e.replace("&oslash;", "ø")
    e = e.replace("&oacite;", "ó")
    e = e.replace("&oacute ", "ó")
    e = e.replace("&e-hook;", "e")
    e = e.replace("&o-hook;", "o")
    e = e.replace("&E-hook;", "E")
    e = e.replace("&g-tilde;", "g")
    e = e.replace("&ethæs", "ðæs")
    e = e.replace("&oactute;", "ó")
    e = e.replace("&iactute;", "í")
    e = e.replace("&times;", "×")
    e = e.replace("&divide;", "÷")
    e = e.replace("&p-tilde;", "p")
    e = e.replace("&b-tilde;", "b")
    e = e.replace("&h-tilde;", "h")
    e = e.replace("&ethæt", "ðæt")
    e = e.replace("&ccedil;", "ç")
    e = e.replace("κepsilon;ρ", "κερ")
    e = e.replace("&r-long;", "r")

    e = e.replace("&b-rune;", "ᛒ")
    e = e.replace("&m-rune;", "ᛗ")
    e = e.replace("&ng-rune;", "ᛝ")
    e = e.replace("&c-rune;", "ᚳ")
    e = e.replace("&d-rune;", "ᛞ")
    e = e.replace("&e-rune;", "ᛖ")
    e = e.replace("&f-rune;", "ᚠ")
    e = e.replace("&i-rune;", "ᛁ")
    e = e.replace("&l-rune;", "ᛚ")
    e = e.replace("&n-rune;", "ᚾ")
    e = e.replace("&u-rune;", "ᚢ")
    e = e.replace("&w-rune;", "ᚹ")
    e = e.replace("&y-rune;", "ᚣ")
    e = e.replace("&p-rune;", "ᛈ")
    e = e.replace("&o-rune;", "ᚩ")
    e = e.replace("&th-rune;", "ᚦ")
    e = e.replace("&r-rune;", "ᚱ")

    return e


def ascify(s: str) -> str:
    """Normalize `s` to an ASCII form.

    The normalized form has only lowercase letters and spaces, suitable for use
    as search terms.
    """
    s = s.lower()
    s = unicodedata.normalize("NFKD", s)
    s = s.replace("æ", "ae")
    s = s.replace("þ", "th")
    s = s.replace("ð", "th")
    s = re.sub("[^a-z ]", "", s)
    s = re.sub("[^a-z]+", " ", s).strip()
    return s


def acute_to_macron(s: str) -> str:
    s = unicodedata.normalize("NFKD", s)
    s = s.replace("\u0301", "\u0304")
    s = unicodedata.normalize("NFKC", s)
    return s


def acute_to_macron_in_nonitalic(text: str) -> str:
    # Map text like 'foo<I>bar</I>baz' to ['foo', '<I>bar</I>', 'baz']
    groups = re.split(r"(<I>.*?</I>)", text, flags=re.DOTALL)
    # Remove macrons only in the non-italic elements.
    converted_groups = [x if x.startswith("<I>") else acute_to_macron(x) for x in groups]
    return "".join(converted_groups)


def split_senses_into_paragraphs(text: str) -> str:
    """Insert paragraph tags (which auto-close) at the beginning of senses."""
    return SENSE_START_REGEX.sub(r"<p>\1", text)
