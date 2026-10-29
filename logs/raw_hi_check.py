from pathlib import Path

text = Path("app/src/main/res/values-hi/strings_guide_content.xml").read_text(encoding="utf-8", errors="replace")
lines = text.splitlines()

report = Path("logs/raw_hi_check.txt")
line_fragments = []
line_fragments.append("RAW ENDS </resources>: " + str(text.rstrip().endswith("</resources>")))
line_fragments.append("RAW last line: " + repr(lines[-1]))
line_fragments.append("RAW last-6 lines:")
for ln in lines[-6:]:
    line_fragments.append(repr(ln))
line_fragments.append("RAW line count: " + str(len(lines)))
report.write_text("\n".join(line_fragments), encoding="utf-8")
Path("logs/raw_hi_check_print.txt").write_text(report.read_text(encoding="utf-8"), encoding="utf-8")
print("wrote", Path("logs/raw_hi_check_print.txt"))
