from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8")
lines = text.splitlines(True)  # keep line endings

# find the incomplete line
text = path.read_text(encoding="utf-8")
again = text.find("guide_situations_remaining")
print("re-search idx:", again)
print("re-search idx inner:", repr(text[again-20:again+120]))

lines2 = text.splitlines(True)
for i, ln in enumerate(lines2):
    if "guide_situations_remaining" in ln:
        print("line", i, repr(ln[:160]))
        break

last = lines2[-1]
print("LAST LINE:", repr(last))
print("ENDS CLOSED:", last.rstrip().endswith("</string>"))

