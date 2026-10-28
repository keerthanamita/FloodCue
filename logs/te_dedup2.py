from pathlib import Path

te_dir = Path("app/src/main/res/values-te")

dedicated = {}

for path in te_dir.glob("strings_*.xml"):
    if path.name == "strings.xml":
        continue
    text = path.read_text(encoding="utf-8")
    keys = set()
    import re
    for mt in re.finditer(r'<string name="([^"]+)"', text):
        keys.add(mt.group(1))
    dedicated[path.name] = keys

master = (te_dir / "strings.xml").read_text(encoding="utf-8")
master_keys = set(re.findall(r'<string name="([^"]+)"', master))

changed = {}
for name, keys in dedicated.items():
    dups = keys & master_keys
    if dups:
        path = te_dir / name
        text = path.read_text(encoding="utf-8")
        lines = text.splitlines(True)
        out = []
        dropped = set()
        for ln in lines:
            m = re.match(r'\s*<string name="([^"]+)"', ln)
            if m and m.group(1) in dups:
                dropped.add(m.group(1))
                continue
            out.append(ln)
        path.write_text("".join(out), encoding="utf-8")
        changed[name] = sorted(dropped)
    else:
        print("skip", name, "all keys kept")

for name, dups in changed.items():
    print("emptied", name, "->", len(dups))
print("done", "affected:", list(changed.keys()))
