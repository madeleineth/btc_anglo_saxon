from generation import Generation


def test_simple_generation() -> None:
    rules = {
        "categ": {
            "rules": {
                "a": "<{b}>",
                "b": "[{c}]",
            }
        }
    }
    gen = Generation("categ", rules, {"c": "x"})
    assert gen.generate("a") == ["<[x]>"]


def test_lists_and_substitutions() -> None:
    rules = {
        "parent": {
            "rules": {
                "swapped": {
                    "from": "arg",
                    "match": "(.)(.)",
                    "value": "\\2\\1",
                }
            }
        },
        "categ": {
            "inherit": "parent",
            "rules": {
                "x": ["<{arg}>", ">{swapped}<"],
            },
        },
    }
    gen = Generation("categ", rules, {"arg": "xy"})
    assert gen.generate("x") == ["<xy>", ">yx<"]
