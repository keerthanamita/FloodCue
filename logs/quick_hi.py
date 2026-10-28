from pathlib import Path
src = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = src.read_text(encoding="utf-8", errors="replace")
print("exists:", "guide_situations_remaining" in text)
print("count:", text.count("guide_situations_remaining"))
print("last line:", text.splitlines()[-1])
print("ends with </resources>:", text.rstrip().endswith("</resources>"))
Path("logs/quick_hi.txt").write_text(
    "exists: " + str("guide_situations_remaining" in text) + "\n"
    "count: " + str(text.count("guide_situations_remaining")) + "\n"
    "last line: " + text.splitlines()[-1] + "\n",
    encoding="utf-8",
)
