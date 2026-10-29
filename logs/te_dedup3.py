from pathlib import Path
import re

te_dir = Path("app/src/main/res/values-te")
master_path = te_dir / "strings.xml"

# Reload master each run so this stays correct if strings.xml already includes
# the previously-removed keys.
master_text = master_path.read_text(encoding="utf-8")
master_keys = set(re.findall(r'<string name="([^"]+)"', master_text))

affected = []
for path in te_dir.glob("strings_*.xml"):
    if path.name == "strings.xml":
        continue
    text = path.read_text(encoding="utf-8")
    keys = set(re.findall(r'<string name="([^"]+)"', text))
    dups = keys & master_keys
    if not dups:
        continue
    lines = text.splitlines(True)
    out = []
    for ln in lines:
        m = re.match(r'\s*<string name="([^"]+)"', ln)
        if m and m.group(1) in dups:
            continue
        out.append(ln)
    path.write_text("".join(out), encoding="utf-8")
    affected.append((path.name, len(dups)))

for name, n in affected:
    print("emptied", name, "->", n)
print("done affected:", [a[0] for a in affected])
