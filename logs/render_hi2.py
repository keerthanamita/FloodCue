from pathlib import Path
import tempfile

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
fixed = text  # already clean after git restore

with tempfile.NamedTemporaryFile("wb", suffix=".xml", delete=False) as f:
    f.write(fixed.encode("utf-8"))
tmp = Path(f.name)

tail = tmp.read_text(encoding="utf-8", errors="replace")[-160:]
log = Path("logs/render_hi_tail.txt")
log.write_text("tmp: " + str(tmp) + "\n" + "tail: " + repr(tail) + "\n", encoding="utf-8")
print("written", log)
