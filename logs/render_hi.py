from pathlib import Path
import tempfile

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")

bad = "\ufeff\ufeff\ufeff 💰 कैसे सहायता आप कर सकते हैं".encode("utf-8")
print("bad count:", text.count("guide_situations_remaining"))
print("blue feff count:", text.count("\ufeff\ufeff\ufeff 💰"))

fixed = text.replace("guide_situations_remaining", "guide_situations_remaining")
print("after first replace, count:", fixed.count("guide_situations_remaining"))

with tempfile.NamedTemporaryFile("wb", suffix=".xml", delete=False) as f:
    f.write(fixed.encode("utf-8"))
tmp = Path(f.name)
print("written tmp:", tmp)
print("tmp tail repr:", repr(tmp.read_text(encoding="utf-8", errors="replace")[-160:]))
