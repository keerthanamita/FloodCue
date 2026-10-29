from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
tail = text[-300:]
last = text.splitlines()[-1]

log_path = Path("logs/verify_hi_tail.txt")
log_path.write_text(
    "TAIL: " + repr(tail) + "\n"
    "LASTLINE: " + repr(last) + "\n"
    "ENDS_CLOSED: " + str(text.rstrip().endswith("</string>"))
    + "\n",
    encoding="utf-8",
)
print("written", log_path)
