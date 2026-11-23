"""Convert the provided Diarmuid recordings to normalized mono Vorbis assets."""
import argparse
import json
import re
import subprocess
from pathlib import Path
import imageio_ffmpeg

parser = argparse.ArgumentParser()
parser.add_argument("source", type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
destination = root / "src/main/resources/assets/typemoonworld/sounds/voice/diarmuid_ua_duibhne"
destination.mkdir(parents=True, exist_ok=True)
mapping = {"攻击1.mp3": "attack1", "攻击2.mp3": "attack2", "无法行动1.mp3": "fail1",
           "无法行动2.mp3": "fail2", "胜利1.mp3": "victory1", "胜利2.mp3": "victory2", "宝具.mp3": "np"}
report = {}
ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
for name, key in mapping.items():
    source = args.source / name
    if not source.is_file():
        raise FileNotFoundError(source)
    process = subprocess.run([ffmpeg, "-hide_banner", "-y", "-i", str(source), "-vn", "-ac", "1", "-ar", "48000",
                              "-af", "loudnorm=I=-16:TP=-1.5:LRA=11", "-c:a", "libvorbis", "-q:a", "4",
                              str(destination / f"{key}.ogg")], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if process.returncode:
        raise RuntimeError(process.stderr)
    duration = re.search(r"Duration: (\d+):(\d+):([\d.]+)", process.stderr)
    seconds = int(duration[1]) * 3600 + int(duration[2]) * 60 + float(duration[3])
    report[key] = {"source": name, "seconds": seconds, "bytes": (destination / f"{key}.ogg").stat().st_size}
print(json.dumps(report, ensure_ascii=False, indent=2))
