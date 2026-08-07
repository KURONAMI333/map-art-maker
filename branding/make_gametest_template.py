"""Empty structure template for the GameTests.

Every GameTest needs a structure to run inside. Three things have to be right or the server reports
"Couldn't find template": the NBT must be gzipped, the folder is singular (structure/, not
structures/), and the test class needs @PrefixGameTestTemplate(false) so the template name is not
prefixed with the class name. See PLAYBOOK_GAMETEST.md Phase 2.
"""

import os

import nbtlib
from nbtlib.tag import Compound, Int, List

DATA_VERSION = 3955  # 1.21.1


def build(path, size=(3, 3, 3)):
    structure = nbtlib.File(
        Compound(
            {
                "size": List[Int]([Int(v) for v in size]),
                "entities": List[Compound]([]),
                "blocks": List[Compound]([]),
                "palette": List[Compound]([]),
                "DataVersion": Int(DATA_VERSION),
            }
        ),
        gzipped=True,
    )
    os.makedirs(os.path.dirname(path), exist_ok=True)
    structure.save(path)
    return nbtlib.load(path)


if __name__ == "__main__":
    out = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "common",
        "src",
        "main",
        "resources",
        "data",
        "map_art_maker",
        "structure",
        "empty3x3x3.nbt",
    )
    loaded = build(out)
    print("wrote", out)
    print("round trip keys:", list(loaded.keys()), "size:", loaded["size"])
