from pathlib import Path
import xml.dom.minidom as minidom

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
try:
    parsed = minidom.parseString(text)
    root = parsed.documentElement
    log = Path("logs/verify_broken_xml.txt")
    last_byte_newline = repr(text[-1])
    last_line = repr(text.splitlines()[-1])
    ends_resources = text.rstrip().endswith("</resources>")
    closure_after = text.rstrip().endswith("</resources>\n")
    log_lines = [
        "parse: OK",
        "root tag:", root.tagName,
        "strings count:", str(len(parsed.getElementsByTagName("string"))),
        "last byte is newline:", last_byte_newline,
        "last line:", last_line,
        "ends with </resources>:", str(ends_resources),
        "closure after last string:", str(closure_after),
    ]
    log.write_text("\n".join(log_lines), encoding="utf-8")
    print("parsed OK")
    print("\n".join(log_lines))
except Exception as e:
    log = Path("logs/verify_broken_xml.txt")
    log.write_text(f"parse failed: {e}\n", encoding="utf-8")
    print("parse failed", e)
