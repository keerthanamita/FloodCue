from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
lines = text.splitlines()

out = []
for i, ln in enumerate(lines, 1):
    if "guide_situation_16" in ln:
        out.append(f"line {i}: {ln}")

out.append("---")
out.append(f"TOTAL LINES: {len(lines)}")
out.append(f"LAST LINE: {repr(lines[-1])}")
out.append(f"ENDS CLOSED: {lines[-1].rstrip().endswith('</string>')}")

Path("logs/guide16_full.txt").write_text("\n".join(out), encoding="utf-8")
Path("logs/guide16_full.txt").write_text("\n".join(out), encoding="utf-8")
print("written")
