from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
raw = path.read_bytes()
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

print("hits:", hits)
if not hits:
    print("NOT FOUND")
    raise SystemExit(1)

h = hits[0]
s = max(0, h - 40)
e = min(len(raw), h + 220)
seg = raw[s:e]
Path("logs/hi_segment.bin").write_bytes(seg)
print("segment written length:", len(seg))
print("repr:", repr(seg.decode("utf-8", "replace"))[:300])
