from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8")

idx = text.find("guide_situations_remaining")
if idx < 0:
    raise SystemExit("guide_situations_remaining not found")
end = text.find("</string>", idx)
if end < 0:
    raise SystemExit("end </string> not found")
end = text.find("\n", end) + 1
keep = text[:end]
path.write_text(keep, encoding="utf-8")

log = Path("logs/fix_hi_tail_done.txt")
log.write_text(
    "Wrote: " + str(len(keep.encode("utf-8"))) + " bytes\n"
    "TAIL: " + repr(keep[-260:]) + "\n"
    "CLOSED: " + str(keep.rstrip().endswith("</string>")) + "\n",
    encoding="utf-8",
)
print("done")
