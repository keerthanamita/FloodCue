from pathlib import Path
import re

saved = Path("logs/raw_hi_check_print.txt")
text = saved.read_text(encoding="utf-8")
out = Path("logs/recall_hi_todo2.txt")
out.write_text("current tail report:\n" + text + "\n---- diagnostic ----\n", encoding="utf-8")
print("wrote", out)
