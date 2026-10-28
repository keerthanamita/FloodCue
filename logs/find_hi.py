from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
raw = path.read_bytes()
needle = "guide_situations_remaining".encode("utf-8")
n = len(needle)
idx = 0
hits = []
while True:
    i = raw.find(needle, idx)
    if i < 0:
        break
    hits.append(i)
    idx = i + 1

print("hits:", hits)
if not hits:
    print("NOT FOUND")
    raise SystemExit(1)

for h in hits[:1]:
    start = max(0, h - 40)
    end = min(len(raw), h + 220)
    seg = raw[start:end]
    Path("logs/hi_segment.bin").write_bytes(seg)
    print("segment written length:", len(seg))
    print("repr:", repr(bytes(seg).decode("utf-8", "replace"))[:300])
