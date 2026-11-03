"""Original procedural stereo score for Millionaire. Requires numpy and ffmpeg."""
from pathlib import Path
import numpy as np
import subprocess
import tempfile
import os
SR = 32000
ROOT = Path(__file__).resolve().parents[1] / 'Millionaire/Resources/Audio'
ROOT.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(72509)

def layer(bus, start, length, key, volume, color, pan=0.0):
    i = round(start * SR)
    n = min(round(length * SR), len(bus) - i)
    if n <= 0: return
    t = np.arange(n) / SR
    freq = 440 * 2 ** ((key - 69) / 12)
    phase = 2 * np.pi * freq * t
    release = min(length * 0.25, 0.42)
    end = np.clip((length - t) / release, 0, 1) ** 1.5
    if color == 'pad':
        tone = .45*np.sin(phase)+.25*np.sin(phase*1.003)+.18*np.sin(phase*2+.2*np.sin(t*3))+.12*np.sin(phase*3)
        env = (1-np.exp(-t*5))*end
    elif color == 'bell':
        tone = .62*np.sin(phase)+.23*np.sin(phase*2.01)*np.exp(-t*3)+.15*np.sin(phase*3.95)*np.exp(-t*4)
        env = np.minimum(t*110,1)*np.exp(-t*2.2)*end
    elif color == 'brass':
        tone = np.tanh(1.4*(np.sin(phase)+.4*np.sin(phase*2)+.18*np.sin(phase*3)))
        env = np.minimum(t*14,1)*np.exp(-t*.7)*end
    elif color == 'drum':
        tone = np.sin(2*np.pi*(46*t+2.6*(1-np.exp(-t*30))))+.15*rng.normal(size=n)*np.exp(-t*30)
        env = np.minimum(t*280,1)*np.exp(-t*8)*end
    else:
        tone = np.sin(phase)+.10*np.sin(phase*3)
        env = np.minimum(t*30,1)*np.exp(-t*1.2)*end
    amp = tone*env*volume
    bus[i:i+n,0] += amp*np.sqrt((1-pan)/2)
    bus[i:i+n,1] += amp*np.sqrt((1+pan)/2)

def write(name, bus, loop=False):
    # Cross-channel early reflections, followed by a quieter late return.
    dry = bus.copy()
    for delay, gain in [(.073,.14),(.129,.09),(.229,.055)]:
        d = int(SR * delay)
        if d >= len(bus): continue
        bus[d:] += dry[:-d,::-1]*gain
        if loop: bus[:d] += dry[-d:,::-1]*gain
    fade = min(len(bus)//8, int(SR*(.018 if loop else .10)))
    if fade:
        bus[:fade] *= np.linspace(0,1,fade)[:,None]
        bus[-fade:] *= np.linspace(1,0,fade)[:,None]
    peak = np.max(np.abs(bus))
    bus *= min(1.6, .80/max(peak,.01))
    with tempfile.TemporaryDirectory() as scratch:
        encoded = Path(scratch) / (name+'.m4a')
        subprocess.run(['ffmpeg','-loglevel','error','-y','-f','s16le','-ar',str(SR),'-ac','2','-i','pipe:0','-c:a','aac','-b:a','112k',str(encoded)],input=(np.clip(bus,-1,1)*32767).astype('<i2').tobytes(),check=True)
        import shutil
        pending = ROOT / ('.' + name + '.part')
        shutil.copyfile(encoded, pending)
        os.replace(pending, ROOT/(name+'.m4a'))

def score(name,bpm,progression,kind):
    beat = 60/bpm
    bus = np.zeros((round(len(progression)*4*beat*SR),2))
    major = kind in ('victory','intro')
    for bar,root in enumerate(progression):
        start = bar*4*beat
        intervals = (0,4 if major else 3,7,14 if major else 10)
        for k,offset in enumerate(intervals):
            layer(bus,start+.012*k,beat*3.96,root+12+offset,.17,'pad',(k-1.5)*.38)
        for k in range(2 if kind in ('menu','calm') else 4):
            step = 2 if kind in ('menu','calm') else 1
            layer(bus,start+k*step*beat,beat*.9,root-12,.36,'bass')
        motif = [0,7,10,7,3,7,14,10] if not major else [0,4,7,12,16,12,7,4]
        for j,note in enumerate(motif):
            if kind=='calm' and j%2: continue
            if kind=='suspense' and j%3: note = 0
            layer(bus,start+j*.5*beat,beat*(1.5 if j%2==0 else .7),root+24+note,.16 if j%2==0 else .1,'bell',(.45 if j%2 else -.45))
        if kind in ('intro','victory','tense','suspense'):
            for j in ([0,2,3.5] if kind!='suspense' else [0,.75,2,2.75]):
                layer(bus,start+j*beat,.55,35,.40,'drum')
        if major:
            for j,n in enumerate(([7,12,16],[4,7,12],[7,9,12],[16,14,12])[bar%4]):
                layer(bus,start+[0,1.5,2.5][j]*beat,beat*1.25,root+24+n,.23,'brass',-.12)
    write(name,bus,True)
score('intro',108,[38,34,41,36],'intro')
score('menu',82,[50,46,53,48,50,43,46,48],'menu')
score('calm',72,[45,41,48,43,45,38,41,43],'calm')
score('tense',106,[42,38,41,37,42,38,35,37],'tense')
score('suspense',116,[40,40,41,39],'suspense')
score('victory',126,[50,45,47,43,50,45,43,50],'victory')
for name,notes,step,color in [
  ('question',[66,73,78],.11,'bell'),('select',[78,85],.055,'bell'),
  ('lock',[45,52,57],.13,'brass'),('correct',[62,66,69,74],.12,'bell'),
  ('wrong',[55,51,46],.22,'brass'),('help',[74,81,78],.12,'bell'),
  ('milestone',[57,64,69,73,76,81],.16,'brass'),
  ('win',[62,66,69,74,78,81,86],.15,'brass')]:
    bus = np.zeros((int(SR*(len(notes)*step+.8)),2))
    for j,note in enumerate(notes):
        layer(bus,j*step,.68,note,.52,color,(j/len(notes)-.5)*.5)
    write(name,bus)
# Hundreds of independently timed band-shaped palm transients in a stereo room.
duration = 3.2
bus = np.zeros((int(SR*duration),2))
for i in range(350):
    s = rng.uniform(0,duration-.12)
    t = np.arange(int(.09*SR))/SR
    noise = rng.normal(size=len(t))
    shaped = np.convolve(noise,[.18,.52,.0,-.48,-.22],mode='same')
    burst = shaped*np.exp(-t*rng.uniform(43,89))*np.minimum(t*1600,1)
    envelope = min(s/.25,1,(duration-s)/.9)
    p = rng.uniform(.04,.96)
    bus[int(s*SR):int(s*SR)+len(t)] += burst[:,None]*np.array([p,1-p])*rng.uniform(.07,.27)*envelope
write('applause',bus)
