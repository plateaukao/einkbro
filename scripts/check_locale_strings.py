#!/usr/bin/env python3
"""Check locale strings.xml files against the default values/strings.xml.

Fails (exit 1) when a locale defines a key that no longer exists in the
default file ("extra" keys — these are dead translations and usually mean
a key was renamed or removed without updating the locale).

Missing keys are reported as a summary only: translations are allowed to
lag behind the default locale, Android falls back to English at runtime.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

RES_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")


def keys(path):
    root = ET.parse(path).getroot()
    return {
        (el.tag, el.get("name"))
        for el in root
        if el.tag in ("string", "string-array", "plurals")
        and el.get("translatable") != "false"
    }


def main():
    default_path = os.path.join(RES_DIR, "values", "strings.xml")
    default = keys(default_path)
    print(f"default locale: {len(default)} translatable keys")

    failed = False
    for path in sorted(glob.glob(os.path.join(RES_DIR, "values-*", "strings.xml"))):
        locale = os.path.basename(os.path.dirname(path)).replace("values-", "")
        if locale.startswith("night"):
            continue
        k = keys(path)
        extra = k - default
        missing = default - k
        line = f"{locale:10s} missing={len(missing):4d} extra={len(extra)}"
        if extra:
            failed = True
            names = ", ".join(sorted(n for _, n in extra))
            line += f"  FAIL — keys not in default: {names}"
        print(line)

    if failed:
        print("\nERROR: locale files define keys that do not exist in "
              "values/strings.xml. Remove the stale entries.", file=sys.stderr)
        return 1
    print("\nOK: no stale keys in any locale.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
