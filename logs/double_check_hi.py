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

print("byte hits:", hits)
print("text hits:", raw.decode("utf-8", "replace").count("guide_situations_remaining"))
print("file size bytes:", len(raw))
print("last 60 bytes repr:", repr(raw[-60:]))
print("last line repr:", repr(raw.decode("utf-8", "replace").splitlines()[-1]))

import io
buf = io.StringIO()
buf.write("byte hits: " + str(hits) + "\n")
buf.write("text hits: " + str(raw.decode("utf-8", "replace").count("guide_situations_remaining")) + "\n")
buf.write("file size bytes: " + str(len(raw)) + "\n")
buf.write("last line repr: " + repr(raw.decode("utf-8", "replace").splitlines()[-1]) + "\n")
Path("logs/double_check_hi_report.txt").write_text(buf.getvalue(), encoding="utf-8")
print(buf.getvalue())
print("report written")
