from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8", errors="replace")
lines = text.splitlines()

log = Path("logs/verify_resources_tail.txt")
with log.open("w", encoding="utf-8") as f:
    f.write(f"TOTAL LINES: {len(lines)}\n")
    f.write(f"LAST LINE: {repr(lines[-1])}\n")
    f.write(f"ENDS_CLOSED: {lines[-1].rstrip().endswith('</resources>')}\n")
    if len(lines) >= 3:
        f.write(f"LAST 3 LINES:\n")
        for i, ln in enumerate(lines[-3:], len(lines) - 2):
            f.write(f"  line {i}: {ln}\n")
print("written", log)
