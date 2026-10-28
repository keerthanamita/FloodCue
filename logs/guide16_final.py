from pathlib import Path
import io
import sys

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
lines = text.splitlines()
buf = io.StringIO()
buf.write(f"LAST LINE: {repr(lines[-1])}\n")
buf.write(f"TOTAL LINES: {len(lines)}\n")
buf.write(f"ENDS CLOSED: {lines[-1].rstrip().endswith('</string>')}\n")

Path("logs/guide16_final.txt").write_text(buf.getvalue(), encoding="utf-8")
print(buf.getvalue())
sys.stdout.flush()
