from pathlib import Path

path = Path("app/src/main/res/values-hi/strings_guide_content.xml")
text = path.read_text(encoding="utf-8")

bad = "\ufeff\ufeff\ufeff 💰"
if bad in text:
    fixed = text.replace(bad, "💰")
    path.write_text(fixed, encoding="utf-8")
    print("fixed blue-angle sequence count:", text.count(bad))
    log = Path("logs/fix_blue_angles.txt")
    log_lines = [
        f"fixed: {text.count(bad)}",
        f"title line after fix: {next(l for l in fixed.splitlines() if 'guide_situation_14_title' in l)}",
    ]
    log.write_text("\n".join(log_lines), encoding="utf-8")
    print("\n".join(log_lines))
else:
    cnt = text.count("\ufeff\ufeff\ufeff 💰")
    log = Path("logs/fix_blue_angles.txt")
    log_lines = [f"skip: blue angle count = {cnt}"]
    log.write_text("\n".join(log_lines), encoding="utf-8")
    print("skip; count", cnt)
