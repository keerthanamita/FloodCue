from pathlib import Path
import tempfile
import xml.dom.minidom as minidom

src = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = src.read_text(encoding="utf-8", errors="replace")

# sentinel line to append
marker_line = "</resources>\n"

# ensure file ends with marker_line
if not text.endswith(marker_line):
    # strip the trailing partial /resources or anything after last real content
    # then append cleanly
    last_valid_close = text.rfind("</string>")
    if last_valid_close < 0:
        raise SystemExit("no </string> found")
    cut = text[: last_valid_close + len("</string>")]
    cleaned = cut.rstrip("\n\r") + "\n" + marker_line
    src.write_text(cleaned, encoding="utf-8")
    print("rewrote; new length:", len(cleaned))
else:
    print("already ends with </resources>\\n")

# verify parse + tail
fixed = src.read_text(encoding="utf-8", errors="replace")
parsed = minidom.parseString(fixed)
print("parsed OK; strings count:", len(parsed.getElementsByTagName("string")))
print("ends with </resources>:", fixed.rstrip().endswith("</resources>"))
print("last line:", repr(fixed.splitlines()[-1]))

# write out a report file
report = Path("logs/force_clean_hi_report.txt")
report.write_text(
    "fixed file written to: " + str(src) + "\n"
    "new length bytes: " + str(len(fixed.encode("utf-8"))) + "\n"
    "parsed OK: True\n"
    "strings count: " + str(len(parsed.getElementsByTagName("string"))) + "\n"
    "ends with </resources>: " + str(fixed.rstrip().endswith("</resources>")) + "\n"
    "last line: " + repr(fixed.splitlines()[-1]) + "\n",
    encoding="utf-8",
)
print("report written", report)
