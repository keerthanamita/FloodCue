#!/usr/bin/env python3
"""FloodCue audit helpers — first pass: can each activity resolve its own strings?"""

import re
import subprocess
from datetime import datetime
from pathlib import Path

PROJECT = Path("app/src/main")
OUT = Path("logs/feature_audit_pass1.txt")

JAVA_FILES = sorted((PROJECT / "java/com/floodcue/app").glob("*.java"))
TEST_FILES = sorted((PROJECT / "test/java/com/floodcue/app").glob("*.java"))
MANIFEST = PROJECT / "AndroidManifest.xml"
BUILD_GRADLE = PROJECT / "build.gradle"

# Strings files explicitly built into the native debug variant.
STRINGS_FILES = []
for p in sorted((PROJECT / "res").glob("values*/strings_*.xml")):
    if not p.is_file():
        continue
    text = p.read_text(encoding="utf-8", errors="replace")
    # quick well-formedness: count open/close string tags
    open_count = len(re.findall(r'<string\b', text))
    close_count = text.count("</string>")
    STRINGS_FILES.append((p.relative_to(PROJECT), open_count, close_count))

REGEX_FIND = re.compile(r'\{?[Rr]\.(string|Plurals|stringArray|bool|color|dimen|style|drawable|mipmap|raw)\s*\.([A-Za-z0-9_]+)\}?')

def java_uses(path):
    text = path.read_text(encoding="utf-8", errors="replace")
    return re.findall(REGEX_FIND, text)


def collect():
    rows = []
    for p in JAVA_FILES:
        uses = java_uses(p)
        if not uses:
            continue
        for kind, name in uses:
            rows.append((p.relative_to(PROJECT), kind, name))
    return rows


def report():
    uses = collect()
    by_activity = {}
    for src, kind, name in uses:
        by_activity.setdefault(src, set()).add(name)

    lines = []
    ts = datetime.now().isoformat()
    lines.append("FloodCue — feature audit, pass 1")
    lines.append(f"Generated: {ts}")
    lines.append("")
    lines.append("What this pass checks")
    lines.append(
        "  • Every Activity/Fragment/java source file under main/java/"
        "that references Android resources with the R.<type>.<name> pattern "
        "(including getString(R.string.x), setText(R.string.x) and bare R.string.x)."
    )
    lines.append(
        "  • It does not run a full build or install, so it cannot prove the app "
        "launches. It is a static cross-reference of string usage vs. string files."
    )
    lines.append("")
    lines.append("Source files scanned")
    lines.append(f"  Java sources with R. references: {len(by_activity)}")
    lines.append(f"  String resource files found: {len(STRINGS_FILES)}")
    lines.append("")
    lines.append("Strings resource files (quick sanity heads)")
    by_locale = {}
    for rel, open_c, close_c in STRINGS_FILES:
        loc = rel.parent.name
        by_locale.setdefault(loc, []).append(rel)
    lines.append('Localized files per locale (strings_*.xml under values-*)')
    for loc in sorted(by_locale):
        paths = sorted(p.as_posix() for p in by_locale[loc])
        lines.append(f'  {loc}: {len(paths)} files')
        for p in paths:
            lines.append(f'    - {p}')
    lines.append('')
    lines.append('Locales present in res/values-* but with no strings_*.xml entries')
    all_locales = {p.name for p in (PROJECT / 'res').glob('values-*') if p.is_dir()}
    covered = set(by_locale)
    missing_locales = sorted(all_locales - covered)
    if missing_locales:
        for m in missing_locales:
            lines.append(f'  - {m}')
    else:
        lines.append('  (all locales have at least one strings file)')
    lines.append('')
    lines.append('Localized files per locale (strings_*.xml under values-*)')
    for loc in sorted(by_locale):
        paths = sorted(p.as_posix() for p in by_locale[loc])
        lines.append(f'  {loc}: {len(paths)} files')
        for p in paths:
            lines.append(f'    - {p}')
    lines.append('')
    lines.append('Locales present in res/values-* but with no strings_*.xml entries')
    all_locales = {p.name for p in (PROJECT / 'res').glob('values-*') if p.is_dir()}
    covered = set(by_locale)
    missing_locales = sorted(all_locales - covered)
    if missing_locales:
        for m in missing_locales:
            lines.append(f'  - {m}')
    else:
        lines.append('  (all locales have at least one strings file)')
    lines.append('')
    for rel, open_c, close_c in STRINGS_FILES:
        ok = "ok" if open_c == close_c else f"MISMATCH open={open_c} close={close_c}"
        lines.append(f"  {rel.as_posix()}  {ok}")
    lines.append("")
    lines.append("Localized files per locale (strings_*.xml under values-*)")
    for loc in sorted(by_locale):
        paths = sorted(p.as_posix() for p in by_locale[loc])
        lines.append(f"  {loc}: {len(paths)} files")
        for p in paths:
            lines.append(f"    - {p}")
    lines.append("")
    lines.append("Locales present in res/values-* but with no strings_*.xml entries")
    all_locales = {p.name for p in (PROJECT / "res").glob("values-*") if p.is_dir()}
    covered = set(by_locale)
    missing_locales = sorted(all_locales - covered)
    if missing_locales:
        for m in missing_locales:
            lines.append(f"  - {m}")
    else:
        lines.append("  (all locales have at least one strings file)")
    lines.append("")

    if not by_activity:
        lines.append("No R.<type>.<name> references detected in the main sources scanned.")
        lines.append(
            "If the app really uses getString/setText with resources, re-run from a fuller "
            "source set or with a broader regex."
        )
        lines.append("")
        OUT.write_text("\n".join(lines), encoding="utf-8")
        print("Wrote", OUT)
        return

    order = sorted(by_activity)
    for src in order:
        names = sorted(by_activity[src])
        lines.append(f"### {src}")
        lines.append(f"# used string/us-related names ({len(names)}):")
        for n in names:
            lines.append(f"  - {n}")
        lines.append("")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("Wrote", OUT)

    # quick cross-check: names referenced but with no obvious strings file match
    all_names = set()
    for src, kind, name in uses:
        if kind in ("string", "stringArray", "Plurals"):
            all_names.add(name)

    file_names = set()
    for rel, open_c, close_c in STRINGS_FILES:
        p = (PROJECT / rel)
        if not p.is_file():
            continue
        text = p.read_text(encoding="utf-8", errors="replace")
        file_names.update(re.findall(r'<string\s+name="([A-Za-z0-9_]+)"', text))
    for p in sorted((PROJECT / "res").glob("values*/strings.xml")):
        if not p.is_file():
            continue
        text = p.read_text(encoding="utf-8", errors="replace")
        file_names.update(re.findall(r'<string\s+name="([A-Za-z0-9_]+)"', text))

    unknown = sorted(all_names - file_names)
    lines.append("Cross-check: referenced names not found in any scanned strings_*.xml")
    if unknown:
        for n in unknown:
            lines.append(f"  - {n}")
        lines.append("")
        lines.append(
            "Note: some of these may still exist under non-standard resource types, "
            "generated XML, or the base values/strings.xml that this scanner skipped."
        )
    else:
        lines.append("  (none found in this pass)")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("Updated", OUT)


if __name__ == "__main__":
    report()
