import io
from pathlib import Path

src = Path("app/src/main/res/values-hi/strings_guide_content.xml")
raw = src.read_bytes()

needle = "guide_situations_remaining".encode("utf-8")
hits = []
start = 0
while True:
    i = raw.find(needle, start)
    if i < 0:
        break
    hits.append(i)
    start = i + 1

text = raw.decode("utf-8", "replace")
count_text = text.count("guide_situations_remaining")

last_line = text.splitlines()[-1]

import re
for pat in [b"guide_situations_remaining", b"guide_situation_14", b"guide_situation_16"]:
    cnt = raw.count(pat)
    buf = io.StringIO()
    buf.write("raw bytes count for " + pat.decode("utf-8") + ": " + str(cnt) + "\n")
    if cnt:
        i = raw.find(pat)
        frag = raw[max(0, i-60): i+160].decode("utf-8", "replace")
        buf.write("ctx for " + pat.decode("utf-8") + ": " + repr(frag) + "\n")
import re as _re

print("---- searching raw bytes ----")
for frag in [b"guide_situations_remaining", b"guide_situation_14", b"guide_situation_16"]:
    if frag in raw:
        print("found raw bytes:", frag.decode("utf-8"))
    else:
        print("not found raw bytes:", frag.decode("utf-8"))

print("---- first 'guide' occurrence ----")
m = _re.search(_re.escape(b"guide"), raw)
if m:
    print("'guide' found at byte:", m.start())
    print("ctx repr:", repr(raw[max(0, m.start()-40): m.start()+120].decode("utf-8", "replace")))
    report = Path("logs/double_check_utf8_report.txt")
    report.write_text(buf.getvalue(), encoding="utf-8")
    print("wrote", report)
else:
    print("no 'guide' bytes found")

print("---- regex matches ----")
for keyword in ["guide_situations_remaining", "guide_situation_14", "guide_situation_16"]:
    hits2 = list(_re.finditer(_re.escape(keyword), text, _re.IGNORECASE))
    print(keyword + ": " + str(len(hits2)) + " regex matches")
    if hits2:
        rng = text.find(keyword)
        print("first ctx repr:", repr(text[max(0, rng-40): rng+120]))


report = Path("logs/double_check_utf8_report.txt")
buf = io.StringIO()
buf.write("byte hits: " + str(hits) + "\n")
buf.write("text hits (utf8-decode count): " + str(count_text) + "\n")
buf.write("file size bytes: " + str(len(raw)) + "\n")
buf.write("last line repr: " + repr(last_line) + "\n")
buf.write("ends with </resources>\\n: " + str(raw.endswith(b"</resources>\n")) + "\n")
buf.write("ends with </resources>: " + str(raw.rstrip(b"\r\n").endswith(b"</resources>")) + "\n")
report.write_text(buf.getvalue(), encoding="utf-8")
print(buf.getvalue())
