from pathlib import Path
import sys
import io

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
count = text.count("guide_situations_remaining")

log = Path("logs/hi_summary.txt")
lines = []
lines.append(f"count guide_situations_remaining: {count}")

tail = text[-200:]
lines.append(f"tail repr: {repr(tail)}")
lines.append(f"last line repr: {repr(text.splitlines()[-1])}")

log.write_text("\n".join(lines), encoding="utf-8")
print("written", log)
