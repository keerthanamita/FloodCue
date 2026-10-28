from pathlib import Path
src = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = src.read_text(encoding="utf-8")
lines = text.splitlines()
print("exists:", "guide_situations_remaining" in text)
print("count:", text.count("guide_situations_remaining"))
print("last line:", lines[-1])
print("ends with </resources>:", lines[-1].rstrip().endswith("</resources>"))
Path("logs/quick_split9.txt").write_text(
    "exists: " + str("guide_situations_remaining" in text) + "\n"
    "count: " + str(text.count("guide_situations_remaining")) + "\n"
    "last line: " + lines[-1] + "\n",
    encoding="utf-8",
)
print("written", Path("logs/quick_split9.txt"))
