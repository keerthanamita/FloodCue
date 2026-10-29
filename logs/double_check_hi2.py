from pathlib import Path
import io

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

last_line = raw.decode("utf-8", "replace").splitlines()[-1]
report = Path("logs/double_check_hi2_report.txt")
buf = io.StringIO()
buf.write("byte hits: " + str(hits) + "\n")
buf.write("text hits (utf8-decode count): " + str(raw.decode("utf-8", "replace").count(needle.decode("utf-8"))) + "\n")
buf.write("file size bytes: " + str(len(raw)) + "\n")
buf.write("last line repr: " + repr(last_line) + "\n")
buf.write("ends with </resources>\\n: " + str(raw.endswith(b"</resources>\n")) + "\n")
buf.write("ends with </resources>: " + str(raw.rstrip(b"\r\n").endswith(b"</resources>")) + "\n")
report.write_text(buf.getvalue(), encoding="utf-8")
print(buf.getvalue())
