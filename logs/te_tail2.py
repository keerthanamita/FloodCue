from pathlib import Path
p = Path("app/src/main/res/values-te/strings_guide_content.xml")
text = p.read_text(encoding="utf-8")
lines = text.splitlines()
report = Path("logs/te_tail2.txt")
buf = []
buf.append("total lines: " + str(len(lines)))
buf.append("last 4 lines:")
for i, ln in enumerate(lines[-4:], len(lines) - 3):
    buf.append(str(i) + " " + repr(ln))
buf.append("ends with </resources>: " + str(text.rstrip().endswith("</resources>")))
report.write_text("\n".join(buf), encoding="utf-8")
Path("logs/te_tail2_done.txt").write_text("\n".join(buf), encoding="utf-8")
print("wrote", Path("logs/te_tail2_done.txt"))
