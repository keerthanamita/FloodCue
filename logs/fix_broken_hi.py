from pathlib import Path

src = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = src.read_text(encoding="utf-8", errors="replace")

TARGET_LINE = "guide_situations_remaining"
idx = text.find(TARGET_LINE)
if idx < 0:
    raise SystemExit("guide_situations_remaining not found in current file")

# Capture the current line that contains the target, for diagnostic reporting.
target_line_end = text.find("\n", idx)
if target_line_end < 0:
    target_line_end = len(text)
current_line_fragment = text[idx - 40 : target_line_end + 1]

# Find the closing </string> that should follow; we only keep up to and including
# the </string> that belongs to guide_situations_remaining, then close with
# </resources> so the file is valid XML.
close = text.find("</string>", idx)
if close < 0:
    raise SystemExit("no </string> after guide_situations_remaining")
close_end = close + len("</string>")

kept = text[: close_end].rstrip("\n\r") + "\n</resources>\n"

out = Path("logs/fix_broken_hi_preview.xml")
out.write_text(kept, encoding="utf-8")

# Validate the patched XML before writing it back to the real source file.
import xml.dom.minidom as minidom
parsed = minidom.parseString(kept)
assert parsed.documentElement.tagName == "resources"

src.write_text(kept, encoding="utf-8")

report = Path("logs/fix_broken_hi_report.txt")
report_lines = [
    "operated on:", str(src),
    "target line fragment before patch:",
    current_line_fragment,
    "patched file ends with </resources>:",
    str(kept.rstrip().endswith("</resources>")),
    "patched XML parses OK:",
    str(parsed.documentElement.tagName == "resources"),
    "strings in patched file:",
    str(len(parsed.getElementsByTagName("string"))),
    "last line of patched file:",
    repr(kept.splitlines()[-1]),
    "target still present as a legitimate string after patch:",
    str(TARGET_LINE in kept),
]
report.write_text("\n".join(report_lines), encoding="utf-8")
print("done; report follows:")
print("\n".join(report_lines))
