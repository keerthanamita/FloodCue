from pathlib import Path

src = Path("app/src/main/res/values-hi/strings_guide_content.xml")
raw = src.read_bytes()

needle = "guide_situations_remaining".encode("utf-8")
n = len(needle)

hits = []
start = 0
while True:
    i = raw.find(needle, start)
    if i < 0:
        break
    hits.append(i)
    start = i + 1

print("byte hits:", hits)
print("text hits:", raw.decode("utf-8", "replace").count("guide_situations_remaining"))

if not hits:
    raise SystemExit("NOT FOUND - cannot patch")

h = hits[0]
start_slice = max(0, h - 80)
end_slice = min(len(raw), h + 240)
seg = raw[start_slice:end_slice]
print("segment repr:", repr(seg.decode("utf-8", "replace"))[:300])

close = raw.find(b"</string>", h)
if close < 0:
    raise SystemExit("no </string> after target")
close_end = close + len(b"</string>")

kept = raw[:close_end].rstrip(b"\n\r\b ") + b"\n</resources>\n"

import xml.dom.minidom as minidom
minidom.parseString(kept)  # will raise on invalid XML

src.write_bytes(kept)

report = Path("logs/brute_force_hi_report.txt")
report_lines = [
    "target byte hits:", str(hits),
    "patched -> ends with </resources>:",
    str(kept.rstrip().endswith(b"</resources>")),
    "XML parse OK: True",
    "strings after patch:",
    str(len(minidom.parseString(kept).getElementsByTagName("string"))),
    "last line of patched file:",
    repr(kept.decode("utf-8", "replace").splitlines()[-1]),
    "target still present after patch:",
    str(b"guide_situations_remaining" in kept),
]
report.write_text("\n".join(report_lines), encoding="utf-8")
print("report:")
print("\n".join(report_lines))
