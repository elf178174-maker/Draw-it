import numpy as np, wave, os

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'res', 'raw')

def note(freq, dur, amp=1.0, partials=((1.0, 1.0, 1.0), (4.0, 0.30, 1.9), (9.2, 0.10, 2.8))):
    """Warm mallet tone: inharmonic partials, fast attack, exponential decay."""
    n = int(SR * dur)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for mult, level, decay_scale in partials:
        env = np.exp(-t * (3.2 * decay_scale))
        out += level * env * np.sin(2 * np.pi * freq * mult * t)
    # 4 ms raised-cosine attack so nothing clicks
    a = int(SR * 0.004)
    out[:a] *= (1 - np.cos(np.linspace(0, np.pi, a))) / 2
    return out * amp

def glass(freq, dur, amp=1.0):
    """Softer, purer bell for the freeze cue."""
    return note(freq, dur, amp, partials=((1.0, 1.0, 0.75), (2.0, 0.34, 1.1), (5.4, 0.08, 2.0)))

def place(canvas, sig, at):
    i = int(SR * at)
    end = min(len(canvas), i + len(sig))
    canvas[i:end] += sig[:end - i]

def finish(canvas, name, peak=0.86):
    # gentle 40 ms fade-out, then normalise
    f = min(int(SR * 0.04), len(canvas))
    canvas[-f:] *= np.linspace(1, 0, f)
    m = np.max(np.abs(canvas))
    if m > 0:
        canvas = canvas / m * peak
    data = (canvas * 32767).astype('<i2')
    path = os.path.join(OUT, name)
    with wave.open(path, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(data.tobytes())
    print(f"{name}: {len(canvas)/SR:.2f}s  {os.path.getsize(path)//1024} KB")

# Pentatonic on C: C5 D5 E5 G5 A5 C6 D6 E6
C5, D5, E5, G5, A5 = 523.25, 587.33, 659.25, 783.99, 880.00
C6, D6, E6, G6 = 1046.50, 1174.66, 1318.51, 1567.98
C4, G4 = 261.63, 392.00

# --- streak extended: three rising notes over a soft low body ---
dur = 1.15
c = np.zeros(int(SR * dur))
place(c, note(C4, 1.1, 0.22), 0.0)          # warm body underneath
place(c, note(G5, 0.9, 0.62), 0.000)
place(c, note(C6, 0.9, 0.66), 0.085)
place(c, note(E6, 0.9, 0.58), 0.170)
finish(c, 'streak_up.wav')

# --- milestone: a longer flourish that keeps climbing, with a shimmer tail ---
dur = 2.0
c = np.zeros(int(SR * dur))
place(c, note(C4, 1.9, 0.26), 0.0)
place(c, note(G4, 1.7, 0.18), 0.02)
for i, f in enumerate((C5, E5, G5, C6, E6, G6)):
    place(c, note(f, 1.5, 0.52 - i * 0.03), 0.07 * i)
place(c, glass(C6 * 2, 1.0, 0.14), 0.46)     # shimmer
place(c, glass(G6 * 2, 0.9, 0.10), 0.54)
finish(c, 'streak_milestone.wav')

# --- freeze spent: two soft glassy notes falling, plus a breath of air ---
dur = 1.05
c = np.zeros(int(SR * dur))
place(c, glass(A5, 0.85, 0.50), 0.0)
place(c, glass(E5, 0.95, 0.44), 0.130)
n = int(SR * 0.5)
air = np.random.default_rng(7).normal(0, 1, n)
k = 48
air = np.convolve(air, np.ones(k) / k, mode='same')       # dull the noise
air *= np.concatenate([np.linspace(0, 1, n // 3), np.linspace(1, 0, n - n // 3)]) ** 2
place(c, air * 0.05, 0.02)
finish(c, 'streak_freeze.wav')
