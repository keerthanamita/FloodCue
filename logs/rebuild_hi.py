from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8")
lines = text.splitlines(True)  # keep line endings
keep_idx = -1
for i, ln in enumerate(lines):
    if "guide_situations_remaining" in ln:
        # keep everything up to and including the prior well-formed closing string tag
        for j in range(i, -1, -1):
            if lines[j].strip().endswith("</string>"):
                keep_idx = j
                break
        break

if keep_idx < 0:
    raise SystemExit("no prior valid closing string tag found")

keep = "".join(lines[: keep_idx + 1])
path.write_text(keep, encoding="utf-8")

log = Path("logs/rebuild_hi_done.txt")
log.write_text(
    "keep through line " + str(keep_idx + 1) + " (0-based " + str(keep_idx) + ")\n"
    "bytes: " + str(len(keep.encode("utf-8"))) + "\n"
    "ends closed: " + str(keep.rstrip().endswith("</string>")) + "\n"
    "last line: " + repr(keep.splitlines()[-1]) + "\n"
    "trailing: " + repr(keep[-120:]) + "\n",
    encoding="utf-8",
)
print("done", log)
