from pathlib import Path

saved = Path("logs/raw_hi_check_print.txt")
out = Path("logs/recall_hi_todo.txt")
out.write_text("current tail report:\n" + saved.read_text(encoding="utf-8"), encoding="utf-8")
print("wrote", out)
