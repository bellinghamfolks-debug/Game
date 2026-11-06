"""Reproduce the 23 clips from the retained HeyGen original and word timings."""
import json, subprocess, hashlib
from pathlib import Path
root=Path(__file__).resolve().parents[2]
source=Path(__file__).resolve().parent
meta=json.loads((source/'generation.json').read_text())
lines=json.loads((source/'lines.json').read_text())
groups=[[]]
for word in meta['word_timestamps']:
    if word['word'].startswith('<break'):
        groups.append([])
    elif not word['word'].startswith('<'):
        groups[-1].append(word)
assert len(groups)==len(lines)==23
dest=root/'Millionaire/Resources/Audio/Presenter'
dest.mkdir(parents=True,exist_ok=True)
records=[]
for (name,text), words in zip(lines,groups):
    assert ''.join(text.split())==''.join(w['word'] for w in words), name
    start=max(0,words[0]['start']-.18)
    end=min(meta['duration'],words[-1]['end']+.24)
    out=dest/(name+'.m4a')
    subprocess.run(['ffmpeg','-v','error','-y','-ss',str(start),'-i',str(source/'original.mp3'),'-t',str(end-start),'-c:a','aac','-b:a','128k','-ac','2',str(out)],check=True)
    subprocess.run(['ffmpeg','-v','error','-i',str(out),'-f','null','-'],check=True)
    records.append(dict(id=name,text=text,start=start,end=end,sha256=hashlib.sha256(out.read_bytes()).hexdigest()))
(source/'clips.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
print('Validated 23 clips with exact transcript/timestamp alignment; listening review still requires playback.')
