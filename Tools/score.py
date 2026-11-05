"""Original procedural stereo score for Millionaire. Requires numpy and ffmpeg."""
from pathlib import Path
import numpy as np
import subprocess
import shutil
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
    elif color == 'keys':
        # Tines lose upper partials before the rounded fundamental.
        tone = .72*np.sin(phase+.9*np.exp(-t*5)*np.sin(phase*2))+.23*np.sin(phase*2.002)+.10*np.sin(phase*3.98)*np.exp(-t*3)
        env = np.minimum(t*190,1)*np.exp(-t*1.65)*end
    elif color == 'flute':
        wave = phase + .08*np.sin(t*2*np.pi*4.7)
        tone = .82*np.sin(wave)+.13*np.sin(2*wave)+.05*np.sin(3*wave)
        env = np.minimum(t*9,1)*(1-.18*np.exp(-t*8))*end
    elif color == 'lute':
        # Double-course plucked string, treble damped progressively after attack.
        tone = np.zeros(n)
        for harmonic in range(1,9):
            strength = np.sin(harmonic*1.07)/harmonic
            tone += strength*(.54*np.sin(phase*harmonic)+.46*np.sin(phase*harmonic*1.0019))*np.exp(-t*(1+.58*harmonic))
        env = np.minimum(t*540,1)*end
    elif color == 'synth':
        tone = np.sin(phase)+.34*np.sin(phase*2)*np.exp(-t*7)+.22*np.sin(phase*3)*np.exp(-t*9)+.15*np.sin(phase*5)*np.exp(-t*13)
        env = np.minimum(t*160,1)*np.exp(-t*2.1)*end
    elif color == 'strings':
        wave = phase+.045*np.sin(t*2*np.pi*5.3)
        tone = np.zeros(n)
        for h in range(1,8):
            tone += (.55*np.sin(wave*h)+.28*np.sin(wave*h*1.0025)+.17*np.sin(wave*h*.9982)) / h**1.25
        env = np.minimum(t*17,1)*(.72+.28*np.sin(t*5.1)**2)*end
    elif color == 'rim':
        tone = .47*np.sin(phase)+.32*np.sin(phase*2.37)+.21*np.sin(phase*3.87)
        env = np.minimum(t*1000,1)*np.exp(-t*27)*end
    elif color in ('hat','snare','cymbal'):
        # Colour noise with a differentiator and add a low membrane to snares.
        noise = rng.normal(size=n)
        high = np.diff(noise,prepend=0)*.32
        tone = high if color != 'snare' else .56*high+.47*np.sin(phase*(1+.024*np.exp(-t*30)))
        rate = {'hat':42, 'snare':19, 'cymbal':3.1}[color]
        env = np.minimum(t*600,1)*np.exp(-t*rate)*end
    elif color == 'timpani':
        sweep = phase+3*(1-np.exp(-t*18))
        tone = .67*np.sin(sweep)+.22*np.sin(sweep*1.52)+.11*np.sin(sweep*1.99)
        env = np.minimum(t*170,1)*np.exp(-t*3.3)*end
    elif color == 'choir':
        # Cluster harmonics near vowel formants, with a slowly widening ensemble.
        tone = np.zeros(n)
        for h in range(1,12):
            hz = h*freq
            weight = (.8/h + .40*np.exp(-((hz-750)/220)**2) + .27*np.exp(-((hz-1160)/220)**2))
            tone += weight*(.55*np.sin(phase*h)+.45*np.sin(phase*h*1.0024))
        env = np.minimum(t*2.5,1)*end*.55
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
        pending = ROOT / ('.' + name + '.part')
        shutil.copyfile(encoded, pending)
        os.replace(pending, ROOT/(name+'.m4a'))

class Passage:
    """Place notes on a beat grid; overlapping durations provide phrased tails."""
    def __init__(self, bpm, beats_per_bar, bars):
        self.beat = 60/bpm
        self.meter = beats_per_bar
        self.samples = np.zeros((round(bars*beats_per_bar*self.beat*SR),2))

    def note(self, bar, beat, duration, note, velocity, voice, pan=0.0):
        layer(self.samples,(bar*self.meter+beat)*self.beat,duration*self.beat,note,velocity,voice,pan)

    def chord(self, bar, beat, duration, notes, velocity, voice):
        for note,pan in zip(notes,np.linspace(-.50,.50,len(notes))):
            self.note(bar,beat,duration,note,velocity,voice,pan)

    def export(self, name):
        write(name,self.samples,True)


def glass_waltz():
    # Stage I: a spacious three-beat waltz for electric piano and breathy flute.
    p = Passage(78,3,12)
    harmony = [(38,54,57,64),(43,55,59,66),(40,55,59,62),(45,55,61,64),
               (38,54,57,62),(35,54,57,62),(40,55,59,66),(45,55,61,69),
               (43,55,59,66),(42,54,57,64),(40,52,59,62),(45,55,61,64)]
    melody = [(0,.0,1.55,69),(0,2,.65,66),(1,0,2.3,67),(2,.5,1.7,71),
              (3,0,1.25,69),(3,2,.7,64),(4,.5,1.8,66),(5,0,1.5,62),
              (6,1,1.5,64),(7,0,2.35,69),(8,.5,1.7,71),(9,0,1.3,69),
              (10,0,1.65,67),(10,2,.7,64),(11,.5,1.8,61)]
    for bar,(bass,*voicing) in enumerate(harmony):
        p.note(bar,0,2,bass,.38,'keys',-.16)
        p.chord(bar,1,1.45,voicing,.17,'keys')
        p.note(bar,2.12,.72,voicing[-1]+12,.105,'bell',.48)
    for bar,beat,duration,key in melody:
        p.note(bar,beat,duration,key,.23,'flute',.18)
    p.export('stage1')


def lantern_dance():
    # Stage II: six-eighth Arabic plucked melody, answering itself across the stereo field.
    p = Passage(174,6,12)
    roots = [50,50,48,50,55,53,51,50,55,51,45,50]
    phrases = [[0,1,4,7,5],[7,8,7,5,4],[5,7,5,4,1],[0,4,1,0,-2],
               [5,7,8,12,10],[8,7,5,4,1],[5,4,1,0,1],[0,4,7,5,4],
               [7,8,12,13,12],[8,7,5,4,1],[4,5,4,1,-2],[0,1,4,1,0]]
    for i,(root,phrase) in enumerate(zip(roots,phrases)):
        for j,(beat,n) in enumerate(zip([0,1,1.75,3,4.5],phrase)):
            p.note(i,beat,1.62 if j==4 else 1.36,62+n,.41 if j in (0,3) else .28,'lute',-.22)
            if i%3==2 and j>2: p.note(i,beat+.17,.85,74+n,.15,'lute',.49)
        for beat,vel in [(0,.48),(3,.33),(5.4,.18)]:
            p.note(i,beat,1.25,root-8,vel,'timpani',-.08)
        for beat in [1.5,2,4,5]:
            p.note(i,beat,.48,68+(i%3),.19,'rim',.36)
        p.note(i,0,4.8,root-12,.19,'strings',-.30)
        p.note(i,3,1.7,root+7,.22,'lute',.24)
    p.export('stage2')


def neon_current():
    # Stage III: syncopated electronic four-on-the-floor with an offbeat chord hook.
    p = Passage(110,4,12)
    roots = [41,41,37,39,41,44,37,39,34,37,39,39]
    for i,r in enumerate(roots):
        for beat in [0,1,2,3]: p.note(i,beat,.7,35,.50,'drum')
        for beat in [1,3]: p.note(i,beat,.42,51,.28,'snare',.16)
        for j in range(8):
            p.note(i,j*.5,.27,84,.16 if j%2 else .08,'hat',.5 if j%2 else -.46)
        for j,(beat,d) in enumerate([(0,.61),(.75,.23),(1.5,.40),(2.25,.43),(3,.38),(3.75,.2)]):
            p.note(i,beat,d,r+([0,12,7,0,10,7][j]),.41,'synth',-.09)
        upper = [r+19,r+22,r+26]
        for beat in [.5,2.75]:
            p.chord(i,beat,.43,upper,.19,'keys')
        if i in [1,3,5,7,9,11]:
            hook = [12,15,19] if i%4==1 else [22,19,15]
            for j,interval in enumerate(hook):
                p.note(i,1.5+.5*j,.64,r+12+interval,.24,'synth',.30)
    p.export('stage3')


def seven_steps():
    # Stage IV: orchestral 7/8 grouped 2+2+3, with bowed clusters and tuned drum answers.
    p = Passage(180,7,12)
    bass = [38,38,34,36,38,41,33,35,38,39,34,33]
    for i,r in enumerate(bass):
        for j,off in enumerate([0,2,4]):
            p.note(i,off,1.50 if j<2 else 2.1,r+12,.35,'strings',-.46)
            p.note(i,off+.09,1.5,r+19,.19,'strings',.31)
            p.note(i,off,1.35,r,.48 if j==0 else .28,'timpani',-.06)
        for off in [1,3,5,6]:
            p.note(i,off,.55,r+31,.20,'rim',.40)
        top = ([0,1,0,7], [3,5,3,1], [7,8,7,5])[i//4]
        p.note(i,.18,4.55,62+top[i%4],.20,'strings',.37)
        p.note(i,4.8,1.94,62+top[(i+1)%4],.19,'strings',.32)
        if i in [3,7,11]: p.note(i,5.2,1.6,46,.22,'cymbal',-.40)
    p.export('stage4')


def final_orbit():
    # Stage V: half-time brass/choir processional. Silence between the massive downbeats
    # leaves space for the question, then timpani rolls and a rising horn answer the choir.
    p = Passage(76,4,12)
    bases = [38,38,35,34,31,33,38,41,34,35,33,33]
    leads = [(0,57,3),(1,60,2.5),(2,62,2),(3,65,2.7),(4,64,3),(5,61,3.4),
             (6,62,2),(7,65,2.4),(8,69,2),(9,67,2.5),(10,64,2.2),(11,61,2.6)]
    for i,r in enumerate(bases):
        p.chord(i,0,3.86,[r+12,r+19,r+24],.19,'choir')
        p.note(i,0,3.1,r-5,.25,'bass',-.04)
        for off,amp in [(0,.68),(2.5,.30),(3,.42)]:
            p.note(i,off,.83,r,amp,'timpani',-.16)
        p.note(i,1.5,.55,r+19,.25,'brass',.45)
        if i%3==2:
            for j in range(6): p.note(i,3+j/6,.22,r+12,.12+j*.037,'timpani',.35)
        if i in [0,4,8]: p.note(i,0,2,50,.30,'cymbal',-.48)
    for bar,key,length in leads:
        p.note(bar,.24,length,key,.34,'brass',-.29)
        if bar%2==1: p.note(bar,2.8,1,key+7,.22,'brass',.45)
    p.export('stage5')


def lock_bed():
    # Locked answer: two irregular heartbeat thumps beneath a narrow rising glass interval.
    p = Passage(94,4,4)
    for i in range(4):
        for off,vol in [(0,.50),(.46,.32),(2.25,.43),(2.70,.26)]:
            p.note(i,off,.65,32+i%2,vol,'timpani')
        p.note(i,0,3.9,47+i%2,.24,'strings',-.30)
        p.note(i,.5,3.4,65+i,.12,'pad',.37)
        for off in [1.25,3.3]: p.note(i,off,.65,83+i,.12,'bell',.20)
    p.export('suspense')


def entrance():
    p = Passage(102,4,4)
    roots = [38,34,43,45]
    for i,r in enumerate(roots):
        p.note(i,0,1.0,r,.67,'timpani')
        p.chord(i,0,3.8,[r+19,r+24,r+28],.24,'strings')
        if i == 0: p.note(i,0,2.2,48,.33,'cymbal',.33)
        for j,(off,k) in enumerate([(0,62),(1.5,69),(2.25,73),(3,74)]):
            p.note(i,off,1.12 if j==0 else .72,k+[0,-4,2,0][i],.41,'brass',-.16)
    p.export('intro')


def lobby():
    p = Passage(88,4,8)
    chords = [(42,57,61,64,68),(38,57,61,64,66),(45,55,59,64,67),(40,56,59,62,66)]*2
    for i,(r,*notes) in enumerate(chords):
        p.note(i,0,3.8,r,.28,'bass',-.09)
        p.chord(i,0,3.9,notes[:3],.20,'pad')
        for j,off in enumerate([.36,1.12,2.63]):
            p.note(i,off,1.0,notes[(j+i)%4]+12,.21,'keys',[-.36,.20,.45][j])
        if i%2: p.note(i,3.05,.80,notes[-1],.15,'flute',.12)
    p.export('menu')


def coronation():
    p = Passage(121,4,8)
    roots = [38,45,47,43,38,43,45,38]
    phrase = [[62,66,69,74],[73,69,64,69],[71,74,78,76],[74,71,67,69],
              [74,78,81,78],[79,74,71,74],[76,73,69,73],[74,78,81,86]]
    for i,r in enumerate(roots):
        third = 3 if i==2 else 4
        p.chord(i,0,3.92,[r+12,r+12+third,r+19,r+24],.22,'choir')
        for j,off in enumerate([0,.78,1.55,2.5]):
            p.note(i,off,1.23 if j==3 else .65,phrase[i][j],.42,'brass',-.15)
            p.note(i,off,1.2,phrase[i][j]-12,.20,'strings',.34)
        for j in range(8):
            p.note(i,j*.5,.61,r+12+[0,third,7,12,7,third,7,12][j],.18,'strings',-.47)
        for off in [0,2,3.5]: p.note(i,off,.7,r,.47,'timpani')
        if i in [0,4,7]: p.note(i,0,1.8,50,.35,'cymbal',.5)
        if i%2: p.note(i,3,.45,52,.20,'snare',.2)
    p.export('victory')


def checkpoint():
    # Brief two-bar cadence with a bell accent as the guaranteed sum is revealed.
    p = Passage(125,4,2)
    for i,root in enumerate([43,50]):
        p.chord(i,0,3.5,[root+12,root+16,root+19],.24,'strings')
        p.note(i,0,1.1,root,.56,'timpani')
    for beat,note in [(0,62),(.75,67),(1.5,71),(2.5,74),(3.3,78)]:
        p.note(0,beat,.77,note,.43,'brass',-.12)
    p.note(1,0,2.7,78,.39,'brass')
    p.note(1,0,2.6,86,.36,'bell',.32)
    write('milestone',p.samples)


glass_waltz()
lantern_dance()
neon_current()
seven_steps()
final_orbit()
lock_bed()
entrance()
lobby()
coronation()
checkpoint()
for name,notes,step,color in [
  ('question',[66,73,78],.11,'bell'),('select',[78,85],.055,'bell'),
  ('lock',[45,52,57],.13,'brass'),('correct',[62,66,69,74],.12,'bell'),
  ('wrong',[55,51,46],.22,'brass'),('help',[74,81,78],.12,'bell'),
  ('win',[62,66,69,74,78,81,86],.15,'brass')]:
    bus = np.zeros((int(SR*(len(notes)*step+.8)),2))
    for j,note in enumerate(notes):
        layer(bus,j*step,.68,note,.52,color,(j/len(notes)-.5)*.5)
    write(name,bus)
tick = np.zeros((round(.20*SR),2))
layer(tick,0,.12,79,.46,'rim',-.10)
layer(tick,.052,.13,86,.27,'bell',.16)
write('countdown',tick)

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
