from pathlib import Path

te_dir = Path("app/src/main/res/values-te")

for name, keys in [
    ("strings_about_floodcue.xml", {"about_floodcue_version_label"}),
    ("strings_about_bridgefy.xml", {"settings_about_bridgefy", "about_bridgefy_back_title", "about_bridgefy_heading"}),
]:
    path = te_dir / name
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(True)
    kept = []
    removed = []
    for ln in lines:
        stripped = ln.strip()
        if stripped.startswith("<string name=\"") and stripped.endswith("/>") is False and "/>" not in stripped:
            pass
        if "<string name=\"" in stripped and "/>" not in stripped:
            start = stripped.find("name=\"") + 6
            end = stripped.find("\"", start)
            key = stripped[start:end]
            if key in keys:
                removed.append(key)
                continue
        kept.append(ln)
    if removed:
        path.write_text("".join(kept), encoding="utf-8")
        print("removed", name, removed)
    else:
        print("no change", name)
