#!/usr/bin/env python3
"""Atlas of every text string each screen loads through Android string resources."""

import re
from datetime import datetime
from pathlib import Path

PROJECT = Path("app/src/main")
OUT = Path("logs/feature_string_mapping.txt")

XML1 = re.compile(r'<string\s+name="([A-Za-z0-9_]+)"\s*>(?P<val>(?:(?!</string>).)*)</string>', re.S)

# Sub-list screens: the file only carries the root label; the real rows are authored in code or the companion items file.
child_only = {
    "values-kn/strings_tips_items.xml",
    "values-kn/strings_checklist_items.xml",
    "values-kn/strings_guide_content.xml",
    "values-te/strings_tips_items.xml",
    "values-te/strings_checklist_items.xml",
    "values-te/strings_guide_content.xml",
    "values-hi/strings_tips_items.xml",
    "values-hi/strings_checklist_items.xml",
    "values-hi/strings_guide_content.xml",
}

child_only_names = {
    "tips_variable",
    "tips_content_list",
    "checklist_content_list",
    "guide_content_list",
}

# Statement of intent: what each string name is for. Add notes as screens are reviewed.
INTENT = {
    "about_bridgefy_heading": "About Bridgefy: screen title.",
    "about_bridgefy_intro": "About Bridgefy: company intro body.",
    "about_bridgefy_company_body": "About Bridgefy: company facts.",
    "about_bridgefy_app_body": "About Bridgefy: consumer app description.",
    "about_bridgefy_applications_intro": "About Bridgefy: when-it-helps intro.",
    "about_bridgefy_applications_body": "About Bridgefy: example use-cases.",
    "about_bridgefy_ai_body": "About Bridgefy: AI + mesh networking note.",
    "about_bridgefy_bluetooth_body": "About Bridgefy: BLE note.",
    "about_bridgefy_floodcue_body": "About Bridgefy: how FloodCue relates.",
    "about_bridgefy_get_app": "About Bridgefy: Play Store CTA.",
    "about_bridgefy_back_title": "About Bridgefy: back button label.",
    "about_bridgefy_legal_title": "About Bridgefy: legal/body heading.",
    "about_bridgefy_licenses_title": "About Bridgefy: third-party licenses heading.",
    "about_bridgefy_licenses_body": "About Bridgefy: license list.",
    "about_bridgefy_privacy_title": "About Bridgefy: privacy note heading.",
    "about_bridgefy_privacy_body": "About Bridgefy: privacy note body.",
    "about_bridgefy_safety_title": "About Bridgefy: safety disclaimer heading.",
    "about_bridgefy_safety_body": "About Bridgefy: safety disclaimer body.",
    "about_bridgefy_title": "About Bridgefy: top title (toolbar).",
    "about_floodcue_body": "About FloodCue: app description.",
    "about_floodcue_version": "About FloodCue: build/version line.",
    "guide_header_what_to_do": "Survival guide: 'what to do' header.",
    "guide_header_never": "Survival guide: 'never' header.",
    "guide_header_important_warnings": "Survival guide: warnings header.",
    "guide_situation_1_title": "Guide: rising flood water.",
    "guide_situation_1_subtitle": "Guide: rising flood water subtitle.",
    "guide_situation_2_title": "Guide: trapped in house.",
    "guide_situation_2_subtitle": "Guide: trapped in house subtitle.",
    "guide_situation_3_title": "Guide: trapped in vehicle.",
    "guide_situation_3_subtitle": "Guide: trapped in vehicle subtitle.",
    "guide_situation_4_title": "Guide: near power lines.",
    "guide_situation_4_subtitle": "Guide: near power lines subtitle.",
    "guide_situation_5_title": "Guide: someone injured.",
    "guide_situation_5_subtitle": "Guide: someone injured subtitle.",
    "guide_situation_6_title": "Guide: family member missing.",
    "guide_situation_6_subtitle": "Guide: family member missing subtitle.",
    "guide_situation_7_title": "Guide: child/elder/disabled needs help.",
    "guide_situation_7_subtitle": "Guide: child/elder/disabled subtitle.",
    "guide_situation_8_title": "Guide: no phone network.",
    "guide_situation_8_subtitle": "Guide: no phone network subtitle.",
    "guide_situation_9_title": "Guide: battery nearly dead.",
    "guide_situation_9_subtitle": "Guide: battery nearly dead subtitle.",
    "guide_situation_10_title": "Guide: attracting rescuers.",
    "guide_situation_10_subtitle": "Guide: attracting rescuers subtitle.",
    "guide_situation_11_title": "Guide: snakes/animals in flood water.",
    "guide_situation_11_subtitle": "Guide: snakes/animals subtitle.",
    "guide_situation_12_title": "Guide: warning issued, no water yet.",
    "guide_situation_12_subtitle": "Guide: warning issued subtitle.",
    "guide_situation_13_title": "Guide: no safe drinking water.",
    "guide_situation_13_subtitle": "Guide: no safe drinking water subtitle.",
    "guide_situation_14_title": "Guide: food may be contaminated.",
    "guide_situation_14_subtitle": "Guide: food contaminated subtitle.",
    "guide_situation_15_title": "Guide: returned home after flood.",
    "guide_situation_15_subtitle": "Guide: returned home subtitle.",
    "guide_situation_16_title": "Guide: emergency contacts / SOS checklist.",
    "guide_situation_16_subtitle": "Guide: SOS checklist subtitle.",
    "guide_situation_16_block_1_header": "Guide: SOS checklist 'include' header.",
    "guide_situation_16_block_2_header": "Guide: SOS checklist 'example' header.",
    "guide_situation_16_block_3_header": "Guide: SOS checklist 'note' header.",
    "guide_content_list": "Survival guide index (child list content).",
    "tips_content_list": "Tips index (child list content).",
    "tips_variable": "Tips screen: single-item placeholder (real content elsewhere).",
    "checklist_content_list": "Checklist index (child list content).",
}

def load_text(path):
    return XML1.findall(path.read_text(encoding="utf-8", errors="replace"))


def collect():
    rows = []
    for locale_dir in sorted((PROJECT / "res").glob("values-*")):
        if not locale_dir.is_dir():
            continue
        locale = locale_dir.name
        for xml in sorted(locale_dir.glob("strings_*.xml")):
            if not xml.is_file():
                continue
            names = load_text(xml)
            if not names:
                continue
            rel = xml.relative_to(PROJECT)
            only_child = rel.as_posix() in child_only
            for name in names:
                rows.append((locale, rel.as_posix(), name,
                             only_child and name in child_only_names))
    return rows


def by_name(rows):
    g = {}
    for locale, file, name, child in rows:
        g.setdefault(name, []).append((locale, file, child))
    return g


def sort_key(name):
    if not isinstance(name, str):
        return (99, repr(name))
    # group big content families first, then alphabetic
    score = {
        "guide_situation_": 1,
        "guide_header_": 2,
        "guide_content_list": 3,
        "about_bridgefy_": 4,
        "about_floodcue_": 5,
        "tips_": 6,
        "checklist_": 7,
        "sos_": 8,
        "flood_alert_": 9,
        "weather_": 10,
        "offline_comm_": 11,
        "language_": 12,
    }
    base = re.sub(r"_(title|subtitle|header|line_\d+|block_\d+_line_\d+)$", "", name)
    for k, o in score.items():
        if base.startswith(k):
            return (o, base)
    return (99, base)


def write():
    rows = collect()
    g = by_name(rows)

    lines = []
    lines.append("FloodCue — feature string mapping")
    lines.append(f"Generated: {datetime.now().isoformat()}")
    lines.append(
        f"Locales scanned: {len({r[0] for r in rows})}; "
        f"string names: {len(g)}; "
        f"files: {len({r[1] for r in rows})}"
    )
    lines.append("")
    lines.append(
        "How to read this report:\n"
        "  • Each entry = one Android string name. Under it, every locale/file that defines it.\n"
        "  • 'child-shorthand: yes' means the file is a sub-list screen whose real rows are "
        "authored in code or in the companion items file; this file only repeats the root label.\n"
        "  • Where the name is not in the intent table, the screen that loads it still needs a "
        "quick confirmation note. Start there when auditing a feature.\n"
        "  • Strings shared by all locales but missing from a specific locale = localization gap.\n"
    )
    lines.append("")

    ordered = sorted(g.items(), key=lambda kv: sort_key(kv[0]))
    for name, entries in ordered:
        desc = INTENT.get(name, "not yet mapped — confirm the screen that loads it")
        lines.append(f"### {name}")
        lines.append(f"# intent: {desc}")
        lines.append("")

        by_locale = {}
        for locale, file, child in entries:
            by_locale.setdefault(locale, []).append((file, child))
        repos = sorted(by_locale)
        missing_in = sorted(
            {r.name for r in (PROJECT / "res").glob("values-*") if r.is_dir()}
            - set(by_locale)
        )

        for locale in repos:
            items = by_locale[locale]
            shorthand = any(c for _, c in items)
            tag = "   [child-shorthand: yes]" if shorthand else ""
            lines.append(f"- {locale}:{tag}")
            for f, _ in items:
                lines.append(f"    {f}")

        if missing_in:
            lines.append(f"- missing from locales: {', '.join(missing_in)}")

        lines.append("")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {OUT} — {len(g)} string names, {len(ordered)} entries")
    unmapped = sorted(n for n in g if n not in INTENT)
    if unmapped:
        print(f"Unmapped string names ({len(unmapped)}):")
        for n in unmapped:
            print("   -", n)
    else:
        print("All scanned string names have an intent note.")


if __name__ == "__main__":
    write()
