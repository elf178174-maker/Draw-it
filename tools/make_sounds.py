"""
Synthesises every sound in the app from scratch: no samples, no licences.

    python3 tools/make_sounds.py

Writes Ogg Vorbis files into app/src/main/res/raw (needs numpy and ffmpeg).

The palette is bright and punchy, in the spirit of game UI sound: crisp
attacks, rising intervals for good news, short tails, and everything in
C major pentatonic so the beats of a celebration sound like one piece.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'res', 'raw')
rng = np.random.default_rng(2024)

# -- notes -----------------------------------------------------------------------


def hz(name):
    """'C6' -> frequency. Sharps as 'C#6'."""
    names = {'C': 0, 'C#': 1, 'D': 2, 'D#': 3, 'E': 4, 'F': 5, 'F#': 6, 'G': 7, 'G#': 8, 'A': 9, 'A#': 10, 'B': 11}
    pitch, octave = name[:-1], int(name[-1])
    midi = 12 * (octave + 1) + names[pitch]
    return 440.0 * 2 ** ((midi - 69) / 12)


def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def env_ad(n, attack=0.002, decay=0.3, curve=1.0):
    """Fast attack, exponential decay."""
    t = np.arange(n) / SR
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    d = np.exp(-t / max(decay, 1e-4) * curve)
    return a * d


def fade(sig, ms_in=1.5, ms_out=25):
    i = int(SR * ms_in / 1000)
    o = int(SR * ms_out / 1000)
    if i:
        sig[:i] *= np.linspace(0, 1, i)
    if o:
        sig[-o:] *= np.linspace(1, 0, o)
    return sig


# -- voices -----------------------------------------------------------------------


def fm_bell(f, dur, ratio=3.5, index=2.4, idx_decay=0.09, decay=0.35, amp=1.0):
    """Bright struck bell: the modulation index dies fast, so the attack is
    metallic and the tail turns pure."""
    t = t_axis(dur)
    idx = index * np.exp(-t / idx_decay)
    mod = np.sin(2 * np.pi * f * ratio * t) * idx
    car = np.sin(2 * np.pi * f * t + mod)
    return car * env_ad(len(t), 0.0015, decay) * amp


def marimba(f, dur=0.5, amp=1.0, decay=0.22):
    t = t_axis(dur)
    body = np.sin(2 * np.pi * f * t) * env_ad(len(t), 0.001, decay)
    over = 0.35 * np.sin(2 * np.pi * f * 3.93 * t) * env_ad(len(t), 0.001, decay * 0.18)
    click = 0.25 * rng.normal(0, 1, len(t)) * env_ad(len(t), 0.0003, 0.004)
    return (body + over + click) * amp


def glock(f, dur=0.6, amp=1.0, decay=0.4):
    t = t_axis(dur)
    s = np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t / (decay * 0.3)) \
        + 0.18 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t / (decay * 0.12))
    return s * env_ad(len(t), 0.001, decay) * amp


def glass(f, dur=1.0, amp=1.0, decay=0.6):
    t = t_axis(dur)
    s = np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.32 * t + 0.4) * np.exp(-t / (decay * 0.5)) \
        + 0.25 * np.sin(2 * np.pi * f * 4.25 * t + 1.1) * np.exp(-t / (decay * 0.25))
    trem = 1 + 0.08 * np.sin(2 * np.pi * 6.5 * t)
    return s * trem * env_ad(len(t), 0.003, decay) * amp


def saw_lead(f, dur, amp=1.0, cutoff=(800, 5200, 900), attack=0.012, release=0.12, vibrato=0.0):
    """A filtered sawtooth with a filter envelope: reads as a cheerful brass."""
    t = t_axis(dur)
    ph = 2 * np.pi * f * t
    if vibrato:
        ph += vibrato * np.sin(2 * np.pi * 5.5 * t) * np.clip(t / 0.15, 0, 1)
    nharm = int(min(40, (SR / 2) / f))
    s = sum(((-1) ** (k + 1)) * np.sin(k * ph) / k for k in range(1, nharm + 1))
    lo, hi, end = cutoff
    fc = np.interp(t, [0, attack * 2, dur], [lo, hi, end])
    s = lowpass_tv(s, fc)
    env = np.clip(t / attack, 0, 1) * np.clip((dur - t) / release, 0, 1)
    return s * env * amp


def sine_drop(f0, f1, dur, amp=1.0, decay=0.2):
    """Pitch-dropping sine: thumps and booms."""
    t = t_axis(dur)
    f = f1 + (f0 - f1) * np.exp(-t / (dur * 0.25))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * env_ad(len(t), 0.001, decay) * amp


def blip(f0, f1, dur, amp=1.0, decay=0.06):
    """A bubbly pitch blip, the core of pops and drops."""
    t = t_axis(dur)
    f = f1 + (f0 - f1) * np.exp(-t / 0.018)
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph) + 0.25 * np.sin(2 * ph)
    return s * env_ad(len(t), 0.0008, decay) * amp


def noise(dur, amp=1.0):
    return rng.normal(0, 1, int(SR * dur)) * amp


# -- filters ------------------------------------------------------------------------


def biquad_bp_tv(x, fc, q=2.0):
    """Band-pass with a centre frequency that can change every sample."""
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    fc = np.broadcast_to(fc, x.shape)
    for i in range(len(x)):
        w0 = 2 * np.pi * min(fc[i], SR * 0.45) / SR
        alpha = np.sin(w0) / (2 * q)
        b0, b2 = alpha, -alpha
        a0, a1, a2 = 1 + alpha, -2 * np.cos(w0), 1 - alpha
        yi = (b0 * x[i] + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1 = x1, x[i]
        y2, y1 = y1, yi
        y[i] = yi
    return y


def lowpass_tv(x, fc):
    """One-pole low-pass with a moving cutoff; cheap and smooth."""
    fc = np.broadcast_to(fc, x.shape)
    a = 1 - np.exp(-2 * np.pi * np.clip(fc, 20, SR * 0.45) / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += a[i] * (x[i] - acc)
        y[i] = acc
    return y


def highpass(x, fc):
    return x - lowpass_tv(x, np.full(x.shape, fc))


def reverb(x, size=0.6, mix=0.18, tone=6000):
    """Convolution with decaying, darkened noise: a soft room."""
    n = int(SR * size)
    ir = rng.normal(0, 1, n) * np.exp(-np.arange(n) / SR / (size / 5))
    ir = lowpass_tv(ir, np.full(n, tone))
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    size_out = len(x) + n
    wet = np.fft.irfft(np.fft.rfft(x, size_out) * np.fft.rfft(ir, size_out), size_out)
    out = np.concatenate([x, np.zeros(n)])
    return out * (1 - mix) + wet * mix * 1.4


# -- assembly ---------------------------------------------------------------------------


def soft_end(sig, ms=45):
    """Every voice fades out over its last few ms, so cut-offs never click."""
    sig = np.array(sig, dtype=float)
    k = min(len(sig), int(SR * ms / 1000))
    if k > 1:
        sig[-k:] *= np.cos(np.linspace(0, np.pi / 2, k)) ** 2
    return sig


def layer(*sigs):
    """Sums signals of different lengths, padding the short ones."""
    n = max(len(x) for x in sigs)
    out = np.zeros(n)
    for x in sigs:
        out[:len(x)] += soft_end(x)
    return out



class Mix:
    def __init__(self, dur):
        self.buf = np.zeros(int(SR * dur))

    def add(self, sig, at=0.0, gain=1.0):
        sig = soft_end(sig)
        i = int(SR * at)
        end = i + len(sig)
        if end > len(self.buf):
            # Grow rather than chop: a cut-off voice is a click.
            self.buf = np.concatenate([self.buf, np.zeros(end - len(self.buf))])
        self.buf[i:end] += sig * gain
        return self


def master(sig, peak_db=-1.0, drive=1.4, tail_ms=30):
    sig = np.tanh(sig / (np.max(np.abs(sig)) + 1e-9) * drive)
    sig -= np.mean(sig)
    sig = fade(sig, 0.8, tail_ms)
    sig = sig / (np.max(np.abs(sig)) + 1e-9) * (10 ** (peak_db / 20))
    # Trim trailing silence.
    above = np.where(np.abs(sig) > 10 ** (-60 / 20))[0]
    if len(above):
        sig = sig[:min(len(sig), above[-1] + int(SR * 0.02))]
        sig = fade(sig, 0, 8)
    return sig


def write(name, sig, peak_db=-1.0, drive=1.4, verb=None, tail_ms=30):
    if verb:
        sig = reverb(sig, *verb)
    sig = master(sig, peak_db, drive, tail_ms)
    data = (np.clip(sig, -1, 1) * 32767).astype('<i2')
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + '.wav')
        with wave.open(wav, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(data.tobytes())
        ogg = os.path.join(OUT, name + '.ogg')
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', ogg], check=True)
        if os.environ.get('KEEP_WAV'):
            os.makedirs(os.environ['KEEP_WAV'], exist_ok=True)
            with wave.open(os.path.join(os.environ['KEEP_WAV'], name + '.wav'), 'wb') as w:
                w.setnchannels(1)
                w.setsampwidth(2)
                w.setframerate(SR)
                w.writeframes(data.tobytes())
    print(f"{name:14s} {len(sig) / SR:5.2f}s  {os.path.getsize(ogg) // 1024:3d} KB")


# -- the sounds --------------------------------------------------------------------------


def make_all():
    # A bubbly select pop for chips, tabs and shop items.
    write('pop', layer(blip(1250, 560, 0.12, 1.0, 0.045), 0.15 * marimba(hz('C6'), 0.1, 1.0, 0.03)), peak_db=-4)

    # One tick of a rolling counter: a tiny woodblock.
    tk = layer(marimba(hz('G6'), 0.05, 1.0, 0.012), biquad_bp_tv(noise(0.05, 0.6), 2600, 4) * env_ad(int(SR * 0.05), 0.0005, 0.008))
    write('tick', tk, peak_db=-6, drive=1.1)

    # Drawing saved: the bright two-note "you did it".
    m = Mix(0.9)
    m.add(layer(fm_bell(hz('C6'), 0.5, 3.5, 2.0, 0.07, 0.18), 0.5 * marimba(hz('C6'), 0.4, 1.0, 0.16)), 0.0)
    m.add(layer(fm_bell(hz('G6'), 0.8, 3.5, 2.2, 0.08, 0.32), 0.5 * marimba(hz('G6'), 0.6, 1.0, 0.2)), 0.085, 1.05)
    m.add(glock(hz('G7'), 0.5, 0.18, 0.2), 0.09)
    write('ding', m.buf, verb=(0.5, 0.14))

    # The flame catching: air rushing in, then a soft "fwoomp" and crackle.
    m = Mix(1.1)
    sweep = np.interp(t_axis(0.42), [0, 0.42], [350, 3800])
    m.add(biquad_bp_tv(noise(0.42), sweep, 1.4) * np.clip(t_axis(0.42) / 0.36, 0, 1) ** 2, 0.0, 0.5)
    m.add(sine_drop(170, 48, 0.5, 1.0, 0.18), 0.34)
    m.add(lowpass_tv(noise(0.4), np.full(int(SR * 0.4), 900)) * env_ad(int(SR * 0.4), 0.003, 0.08), 0.33, 1.6)
    crack = np.zeros(int(SR * 0.7))
    for _ in range(26):
        p = rng.integers(0, len(crack) - 400)
        crack[p:p + 300] += rng.normal(0, 1, 300) * np.exp(-np.arange(300) / 40) * rng.uniform(0.2, 1)
    crack = highpass(crack, 1800) * np.linspace(1, 0, len(crack)) ** 1.5
    m.add(crack, 0.38, 0.35)
    write('ignite', m.buf, drive=1.6, verb=(0.4, 0.1))

    # Streak extended: a quick climb and a ringing major chord.
    m = Mix(1.6)
    m.add(sine_drop(110, 55, 0.4, 0.7, 0.15), 0.0)
    for i, n in enumerate(['C6', 'E6', 'G6', 'C7']):
        m.add(fm_bell(hz(n), 0.9, 3.5, 2.0, 0.06, 0.28), i * 0.055, 0.9 - i * 0.05)
        m.add(marimba(hz(n) / 2, 0.4, 0.35, 0.12), i * 0.055)
    for n in ['C6', 'E6', 'G6']:
        m.add(glass(hz(n), 1.3, 0.22, 0.55), 0.22)
    sh = sum(np.sin(2 * np.pi * hz(n) * 2 * t_axis(1.1) + rng.uniform(0, 6)) for n in ['C7', 'E7', 'G7'])
    m.add(sh * env_ad(int(SR * 1.1), 0.08, 0.35) * (1 + 0.5 * np.sin(2 * np.pi * 9 * t_axis(1.1))), 0.25, 0.05)
    write('streak', m.buf, verb=(0.8, 0.2))

    # A day of the week filling in: bubbly and tuned (the app plays it at
    # rising pitches across the week).
    write('dayfill', layer(blip(1500, 1046.5, 0.26, 0.9, 0.09), 0.6 * marimba(hz('C6'), 0.26, 1.0, 0.1)), peak_db=-3)

    # An ink drop landing in the counter: "bling".
    m = Mix(0.5)
    m.add(blip(1700, hz('B6'), 0.2, 0.6, 0.05), 0.0)
    m.add(fm_bell(hz('B6'), 0.3, 2.0, 1.4, 0.03, 0.08), 0.0, 0.7)
    m.add(fm_bell(hz('E7'), 0.8, 2.0, 1.4, 0.04, 0.16), 0.055)
    write('coin', m.buf, peak_db=-3, verb=(0.3, 0.1))

    # Goal reached / chest opened: a little brass fanfare with a big finish.
    m = Mix(2.6)
    for i, n in enumerate(['G5', 'C6', 'E6']):
        m.add(saw_lead(hz(n), 0.13, 0.5, (700, 4800, 1200), 0.008, 0.04), i * 0.1)
    for n, g in [('C5', 0.45), ('E5', 0.4), ('G5', 0.4), ('C6', 0.5)]:
        m.add(saw_lead(hz(n), 1.25, g, (600, 3600, 900), 0.03, 0.5, vibrato=0.25), 0.3)
    m.add(sine_drop(90, 45, 0.9, 1.2, 0.3), 0.3)
    m.add(highpass(noise(1.6), 5000) * env_ad(int(SR * 1.6), 0.01, 0.45), 0.3, 0.22)
    for i, n in enumerate(['C7', 'E7', 'G7', 'C8']):
        m.add(glock(hz(n), 0.6, 0.25, 0.25), 0.36 + i * 0.07)
    write('fanfare', m.buf, drive=1.5, verb=(1.0, 0.22))

    # The chest rattling before it opens.
    m = Mix(0.55)
    for i in range(6):
        knock = layer(marimba(hz('E3') * rng.uniform(0.9, 1.15), 0.08, 0.8, 0.025),
                      biquad_bp_tv(noise(0.06), 1100, 1.5) * env_ad(int(SR * 0.06), 0.0005, 0.012) * 0.8)
        m.add(knock, i * 0.075 + rng.uniform(-0.008, 0.008), rng.uniform(0.7, 1.0))
    write('rattle', m.buf, peak_db=-2, drive=1.3)

    # Evolution, part one: the build-up while the flame shakes.
    d = 1.7
    t = t_axis(d)
    m = Mix(d)
    m.add(biquad_bp_tv(noise(d), np.interp(t, [0, d], [300, 7000]), 1.2) * (t / d) ** 2, 0, 0.6)
    gl = 2 ** (t / d)  # one octave up
    for n in ['C4', 'G4', 'C5']:
        ph = 2 * np.pi * np.cumsum(hz(n) * gl) / SR
        s = sum(np.sin(k * ph) / k for k in range(1, 12))
        m.add(lowpass_tv(s, np.interp(t, [0, d], [500, 4000])) * (t / d) ** 1.6 * (0.7 + 0.3 * np.sin(2 * np.pi * (4 + 14 * t / d) * t)), 0, 0.25)
    write('evolve_rise', m.buf, drive=1.3, tail_ms=4)

    # Evolution, part two: the reveal.
    m = Mix(2.8)
    m.add(sine_drop(95, 34, 1.2, 1.4, 0.45), 0.0)
    m.add(lowpass_tv(noise(0.6), np.full(int(SR * 0.6), 1600)) * env_ad(int(SR * 0.6), 0.002, 0.1), 0.0, 1.2)
    for n in ['C6', 'E6', 'G6', 'B6', 'D7']:
        m.add(fm_bell(hz(n), 2.0, 3.5, 2.2, 0.08, 0.7), 0.04, 0.45)
    for i, n in enumerate(['C7', 'D7', 'E7', 'G7', 'A7', 'C8']):
        m.add(glock(hz(n), 0.7, 0.3, 0.3), 0.2 + i * 0.05)
    m.add(highpass(noise(1.8), 6000) * env_ad(int(SR * 1.8), 0.005, 0.55), 0.0, 0.25)
    write('evolve_boom', m.buf, drive=1.6, verb=(1.3, 0.28))

    # Buying something: "ka-ching".
    m = Mix(1.1)
    m.add(biquad_bp_tv(noise(0.06), 6000, 1.2) * env_ad(int(SR * 0.06), 0.0005, 0.015), 0.0, 1.2)
    m.add(fm_bell(hz('E7'), 0.8, 1.41, 3.0, 0.05, 0.3), 0.04, 0.8)
    m.add(fm_bell(hz('B6'), 0.9, 1.41, 3.0, 0.05, 0.35), 0.11, 0.9)
    for _ in range(9):
        f = hz('C8') * rng.uniform(0.85, 1.3)
        m.add(glock(f, 0.25, 0.25, 0.06), rng.uniform(0.12, 0.5))
    write('purchase', m.buf, verb=(0.6, 0.18))

    # Equipping an item: a swish into a pop.
    m = Mix(0.5)
    m.add(biquad_bp_tv(noise(0.18), np.interp(t_axis(0.18), [0, 0.18], [900, 5000]), 1.5) * np.sin(np.pi * t_axis(0.18) / 0.18), 0, 0.5)
    m.add(blip(1400, 700, 0.15, 1.0, 0.05), 0.15)
    m.add(glock(hz('E7'), 0.3, 0.3, 0.12), 0.16)
    write('equip', m.buf, peak_db=-2)

    # A freeze saving the streak: glassy, descending, icy air.
    m = Mix(1.6)
    for i, n in enumerate(['A6', 'E6', 'C6']):
        m.add(glass(hz(n), 1.1, 0.5, 0.45), i * 0.12)
    m.add(biquad_bp_tv(noise(1.0), np.interp(t_axis(1.0), [0, 1.0], [7000, 3000]), 2.5) * env_ad(int(SR * 1.0), 0.05, 0.35), 0.0, 0.25)
    ice = np.zeros(int(SR * 0.6))
    for _ in range(14):
        p = rng.integers(0, len(ice) - 200)
        ice[p:p + 150] += rng.normal(0, 1, 150) * np.exp(-np.arange(150) / 20)
    m.add(highpass(ice, 4000), 0.05, 0.3)
    write('freeze', m.buf, verb=(0.9, 0.25))

    # The streak ending: a gentle, sympathetic "aww".
    m = Mix(1.2)
    t = t_axis(0.45)
    for i, (n, d) in enumerate([('E5', 0.36), ('C5', 0.7)]):
        tt = t_axis(d)
        bend = 1 - 0.03 * np.clip((tt - d * 0.5) / (d * 0.5), 0, 1) * (i == 1)
        ph = 2 * np.pi * np.cumsum(hz(n) * bend * (1 + 0.006 * np.sin(2 * np.pi * 5 * tt))) / SR
        s = np.sin(ph) + 0.3 * np.sin(3 * ph) / 3 + 0.12 * np.sin(5 * ph) / 5
        m.add(s * np.clip(tt / 0.02, 0, 1) * np.exp(-tt / (d * 0.6)), i * 0.3, 0.8)
    write('lost', m.buf, drive=1.1, verb=(0.6, 0.15))

    # Not enough ink: a soft double "bonk".
    m = Mix(0.4)
    for i, n in enumerate(['D4', 'A3']):
        tt = t_axis(0.16)
        ph = 2 * np.pi * hz(n) * tt
        s = np.sin(ph) + 0.33 * np.sin(3 * ph) + 0.2 * np.sin(5 * ph)
        m.add(lowpass_tv(s, np.full(len(tt), 1400)) * env_ad(len(tt), 0.003, 0.07), i * 0.12)
    write('error', m.buf, peak_db=-3, drive=1.2)

    # A celebration sliding in.
    d = 0.45
    tt = t_axis(d)
    write('whoosh', biquad_bp_tv(noise(d), np.interp(tt, [0, d * 0.6, d], [500, 3200, 1400]), 1.3) * np.sin(np.pi * tt / d) ** 2, peak_db=-5, drive=1.2)

    # Something unlocked: a quick sparkle up the pentatonic scale.
    m = Mix(1.3)
    for i, n in enumerate(['C6', 'D6', 'E6', 'G6', 'A6', 'C7', 'D7', 'E7']):
        m.add(glock(hz(n), 0.6, 0.6 - i * 0.03, 0.25), i * 0.042)
    m.add(glass(hz('C7'), 1.8, 0.2, 0.5), 0.32)
    write('unlock', m.buf, verb=(0.8, 0.22))


if __name__ == '__main__':
    make_all()
