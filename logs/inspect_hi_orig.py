from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
lines = text.splitlines(True)
print("total lines:", len(lines))
print("last line repr:", repr(lines[-1]))

for i, ln in enumerate(lines):
    if "guide_situations_remaining" in ln:
        print("found at line", i + 1, repr(ln[:300]))
        break
else:
    print("NOT FOUND guide_situations_remaining")
