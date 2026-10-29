#!/usr/bin/env python3
"""FloodCue audit pass 2 — layout coverage: does each Activity have a matching layout + manifest entry?"""

from datetime import datetime
from pathlib import Path

PROJECT = Path("app/src/main")
OUT = Path("logs/feature_audit_pass2.txt")

JAVA_DIR = PROJECT / "java/com/floodcue/app"
LAYOUT_DIR = PROJECT / "res/layout"
MANIFEST = PROJECT / "AndroidManifest.xml"

CASE_LAYOUTS = {
    "FloodCueActivity": "activity_flood_cue",
    "OfflineMapViewActivity": "activity_offline_map_view",
    "OfflineMapsActivity": "activity_offline_maps",
    "OfflineCommunicationActivity": "activity_offline_communication",
    "SosActivity": "activity_sos",
    "SosEmergencyActivity": "activity_sos_emergency",
    "EmergencyGuideActivity": "activity_emergency_guide",
    "EmergencyContactsActivity": "activity_emergency_contacts",
    "ChecklistActivity": "activity_checklist",
    "TipsActivity": "activity_tips",
    "WeatherActivity": "activity_weather",
    "FloodAlertActivity": "activity_flood_alert",
    "LoginActivity": "activity_login",
    "CreateAccountActivity": "activity_create_account",
    "SetupActivity": "activity_setup",
    "LanguageActivity": "activity_language",
    "HomeActivity": "activity_home",
    "ProfileDetailsActivity": "activity_profile_details",
    "SettingsActivity": "activity_settings",
    "AboutBridgefyActivity": "activity_about_bridgefy",
    "AboutFloodCueActivity": "activity_about_floodcue",
}


def layout_name_from_activity(java_path):
    base = java_path.stem
    if base in CASE_LAYOUTS:
        return CASE_LAYOUTS[base] + ".xml"
    if base.endswith("Activity"):
        core = base[:-8]
    else:
        core = base
    return f"activity_{core.lower()}.xml"


def manifest_activities():
    if not MANIFEST.is_file():
        return set()
    text = MANIFEST.read_text(encoding="utf-8", errors="replace")
    import re
    return set(re.findall(r'<activity[^>]*\bname="([^"]+)"', text))


def java_activities_dir(directory):
    out = []
    if not directory.is_dir():
        return out
    for p in sorted(directory.glob("*.java")):
        if not p.is_file():
            continue
        text = p.read_text(encoding="utf-8", errors="replace")
        if "extends Activity" not in text and "extends AppCompatActivity" not in text:
            continue
        out.append(p.relative_to(PROJECT))
    return out


def java_activities():
    return java_activities_dir(JAVA_DIR)


def _cls_for_rel(rel):
    return "com.floodcue.app." + rel.with_suffix("").as_posix().replace("/", ".")


def rel_base(rel):
    return rel.with_suffix("").as_posix().replace("/", ".")


def _expected_layouts_for_dir(directory):
    out = {}
    if not directory.is_dir():
        return out
    for p in sorted(directory.glob("*.java")):
        if not p.is_file():
            continue
        text = p.read_text(encoding="utf-8", errors="replace")
        if "extends Activity" not in text and "extends AppCompatActivity" not in text:
            continue
        rel = p.relative_to(PROJECT)
        stem = p.stem
        out[stem] = {
            "rel": rel,
            "cls": _cls_for_rel(rel),
            "layout": layout_name_from_activity(rel),
        }
    return out


def run():
    lines = []
    lines.append("FloodCue — feature audit, pass 2")
    lines.append(f"Generated: {datetime.now().isoformat()}")
    lines.append("")
    lines.append(
        "What this pass checks\n"
        "  • Every Activity class under main/java/com/floodcue/app that extends "
        "Activity / AppCompatActivity.\n"
        "  • Whether res/layout contains a conventionally named layout for it.\n"
        "  • Whether AndroidManifest.xml declares an <activity> with that class name.\n"
        "  • These are the structural prerequisites for the feature screens to be "
        "reachable from the app; a missing layout or manifest entry usually means the "
        "screen will crash on launch or is unreachable."
    )
    lines.append("")

    manifest_set = manifest_activities()
    java_index = _expected_layouts_for_dir(JAVA_DIR)
    java_index = dict(java_index)
    java_list = list(java_index.values())
    layout_names = {p.stem for p in LAYOUT_DIR.glob("activity_*.xml") if p.is_file()}

    lines.append(f"Activity classes found in source (java/): {len(java_list)}")
    lines.append(f"Declared activities in manifest: {len(manifest_set)}")
    lines.append(f"Layout files matching activity_*.xml: {len(layout_names)}")
    lines.append("")

    if not java_list:
        lines.append("No Activity subclasses detected in the source set scanned.")
        lines.append(
            "Re-run from a fuller source set if the app really defines activities there."
        )
        lines.append("")
        OUT.write_text("\n".join(lines), encoding="utf-8")
        print("Wrote", OUT)
        return

    for entry in java_list:
        rel = entry["rel"]
        java_full = rel.as_posix()
        cls = entry["cls"]
        layout_name = entry["layout"]
        layout_present = layout_name in layout_names
        layout_path = f"res/layout/{layout_name}" if layout_present else None
        declared = cls in manifest_set

        lines.append(f"### {java_full}")
        lines.append(f"# class: {cls}")
        lines.append(f"# expected layout: {layout_name}")
        lines.append(f"# layout present: {layout_present}" + (f"  ({layout_path})" if layout_present else ""))
        lines.append(f"# declared in manifest: {declared}")
        lines.append("")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("Wrote", OUT)

    problems = []
    for entry in java_list:
        rel = entry["rel"]
        cls = entry["cls"]
        layout_name = entry["layout"]
        layout_present = layout_name in layout_names
        declared = cls in manifest_set
        if not layout_present and not declared:
            note = "missing layout and not declared"
        elif not layout_present:
            note = "missing layout"
        elif not declared:
            note = "not declared in manifest"
        else:
            note = "ok"
        problems.append((rel.as_posix(), cls, layout_name, note, layout_present, declared))

    lines.append("Problem summary")
    if problems:
        for java_full, cls, layout_name, note, layout_present, declared in sorted(problems):
            if note == "ok":
                lines.append(f"  ok: {java_full} -> {cls}, layout={layout_name}, present={layout_present}, declared={declared}")
            else:
                lines.append(f"  - {java_full}: {note} (expected layout: {layout_name})")
    else:
        lines.append("  (no missing-layout / undeclared-activity problems found)")
    lines.append("")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("Updated", OUT)


if __name__ == "__main__":
    run()
