from pathlib import Path
p = Path("app/src/main/res/values-te/strings_guide_content.xml")
text = p.read_text(encoding="utf-8")
lines = text.splitlines()
report = Path("logs/te_tail.txt")
buf = []
buf.append("total lines: " + str(len(lines)))
buf.append("last 4 lines:")
for i, ln in enumerate(lines[-4:], len(lines) - 3):
    buf.append(str(i) + " " + repr(ln))
p.write_text("\n".join(lines[:-2]) + "\n</resources>\n", encoding="utf-8")
buf.append("rewrote file with final </resources> close")
report.write_text("\n".join(buf), encoding="utf-8")
Path("logs/te_tail_done.txt").write_text("\n".join(buf), encoding="utf-8")
print("wrote", Path("logs/te_tail_done.txt"))
