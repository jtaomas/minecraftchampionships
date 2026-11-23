"""Synthesised sound stings for Block Showdown (numpy -> Ogg Vorbis via soundfile).

Every sound is generated from scratch here: additive/FM tones, filtered noise
and simple envelopes. Melodies are original. Output is mono 44.1 kHz (mono is
required for positional playback in Minecraft), peak-normalised to -4 dBFS.
"""
import numpy as np

SR = 44100
PEAK = 10 ** (-4 / 20)


def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def note_hz(name):
    names = {'C': -9, 'C#': -8, 'D': -7, 'D#': -6, 'Eb': -6, 'E': -5, 'F': -4, 'F#': -3, 'G': -2,
             'G#': -1, 'Ab': -1, 'A': 0, 'A#': 1, 'Bb': 1, 'B': 2}
    n, o = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names[n] + 12 * (o - 4)) / 12)


def env(n, a=0.005, d=0.1, s=0.0, r=0.05, hold=None):
    """ADSR-ish envelope over n samples (exponential decay to sustain)."""
    t = np.arange(n) / SR
    e = np.zeros(n)
    A = max(1, int(a * SR))
    e[:A] = np.linspace(0, 1, A)
    rest = t[A:] - a
    e[A:] = s + (1 - s) * np.exp(-rest / max(d, 1e-4))
    R = int(r * SR)
    if R > 0 and R < n:
        e[-R:] *= np.linspace(1, 0, R) ** 2
    return e


def lowpass_fast(x, cutoff):
    from numpy.fft import rfft, irfft, rfftfreq
    X = rfft(x)
    f = rfftfreq(len(x), 1 / SR)
    X *= 1 / np.sqrt(1 + (f / cutoff) ** 4)
    return irfft(X, len(x))


def highpass_fast(x, cutoff):
    from numpy.fft import rfft, irfft, rfftfreq
    X = rfft(x)
    f = rfftfreq(len(x), 1 / SR)
    X *= 1 / np.sqrt(1 + (cutoff / np.maximum(f, 1e-3)) ** 4)
    return irfft(X, len(x))


def bell(f, dur, decay=0.25, bright=1.0):
    t = t_axis(dur)
    mod = np.sin(2 * np.pi * f * 3.5 * t) * bright * np.exp(-t / (decay * 0.5))
    x = np.sin(2 * np.pi * f * t + 2.0 * mod)
    x += 0.35 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t / (decay * 0.6))
    return x * env(len(t), 0.002, decay, 0, 0.02)


def brass(f, dur, vib=0.0, cutoff=2500, a=0.03):
    t = t_axis(dur)
    ph = 2 * np.pi * f * t + vib * np.sin(2 * np.pi * 5.5 * t) * np.clip(t * 3, 0, 1)
    x = np.zeros_like(t)
    for k in range(1, 12):  # band-limited saw
        x += np.sin(k * ph) / k * (0.92 ** k)
    x = lowpass_fast(x, cutoff)
    return x * env(len(t), a, dur * 0.9, 0.75, min(0.08, dur * 0.3))


def pluck(f, dur, decay=0.18):
    t = t_axis(dur)
    x = np.sign(np.sin(2 * np.pi * f * t)) * 0.5 + 0.5 * np.sin(2 * np.pi * f * 2 * t)
    x = lowpass_fast(x, 3500)
    return x * env(len(t), 0.002, decay, 0, 0.02)


def noise(dur, seed=1):
    return np.random.default_rng(seed).standard_normal(int(SR * dur))


def mix(total, *parts):
    """parts: (start_seconds, signal, gain)"""
    out = np.zeros(int(SR * total))
    for start, sig, gain in parts:
        i = int(start * SR)
        j = min(len(out), i + len(sig))
        out[i:j] += sig[:j - i] * gain
    return out


def finish(x, fade=0.01):
    x = x - np.mean(x)
    F = int(fade * SR)
    x[:8] *= np.linspace(0, 1, 8)
    x[-F:] *= np.linspace(1, 0, F)
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.2)  # gentle soft-clip glue
    return (x / np.max(np.abs(x)) * PEAK).astype(np.float32)


def snare(dur=0.15, seed=3):
    n = highpass_fast(noise(dur, seed), 1500)
    t = t_axis(dur)
    body = np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.03)
    return (n * 0.6 + body * 0.6) * env(len(t), 0.001, 0.05, 0, 0.01)


def kick(dur=0.25):
    t = t_axis(dur)
    f = 50 + 90 * np.exp(-t / 0.04)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * env(len(t), 0.001, 0.12, 0, 0.02)


def cymbal(dur=1.2, seed=7):
    n = highpass_fast(noise(dur, seed), 5000)
    return n * env(len(n), 0.002, 0.45, 0, 0.1)


# ---------------------------------------------------------------- the stings
def countdown_tick():
    t = t_axis(0.16)
    click = highpass_fast(noise(0.16, 2), 3000) * np.exp(-t / 0.004)
    body = bell(1046.5, 0.16, 0.06, 0.6)
    wood = np.sin(2 * np.pi * 1568 * t) * np.exp(-t / 0.02)
    return finish(mix(0.16, (0, body, 1.0), (0, wood, 0.5), (0, click, 0.3)))


def countdown_go():
    chord = ['C5', 'E5', 'G5', 'C6']
    parts = [(0, brass(note_hz(n), 0.6, 0.0, 4000, 0.008), 0.5) for n in chord]
    parts += [(0, bell(note_hz('C7'), 0.5, 0.18), 0.35), (0, kick(), 0.9),
              (0, cymbal(0.7), 0.25), (0, snare(), 0.5)]
    return finish(mix(0.7, *parts))


def point_gained():
    return finish(mix(0.32, (0, bell(note_hz('E6'), 0.2, 0.08), 0.8),
                      (0.07, bell(note_hz('B6'), 0.25, 0.12), 1.0)))


def elimination():
    d = 0.42
    t = t_axis(d)
    f = 720 * np.exp(-t / 0.12) + 90
    ph = 2 * np.pi * np.cumsum(f) / SR
    zap = np.sign(np.sin(ph)) * 0.6 + np.sin(ph * 0.5) * 0.4
    zap = lowpass_fast(zap, 2600) * env(len(t), 0.002, 0.18, 0, 0.05)
    crunch = lowpass_fast(noise(d, 5), 1800) * env(len(t), 0.001, 0.05, 0, 0.02)
    return finish(mix(d, (0, zap, 1.0), (0, crunch, 0.45), (0, kick(0.3), 0.6)))


def team_eliminated():
    seq = [('G3', 0.0, 0.28), ('D#3', 0.26, 0.28), ('C3', 0.52, 0.75)]
    parts = []
    for n, s, d in seq:
        f = note_hz(n)
        parts.append((s, brass(f, d, 0.02, 1400), 0.6))
        parts.append((s, brass(f * 0.5, d, 0.0, 900), 0.4))
    parts.append((0.52, kick(0.4), 0.8))
    parts.append((0.52, lowpass_fast(cymbal(0.9, 9), 3000), 0.15))
    return finish(mix(1.35, *parts))


def round_win():
    # original 4-bar sting in C major: pickup triplet, leap, held tonic chord
    seq = [('G4', 0.00, 0.11), ('G4', 0.12, 0.11), ('G4', 0.24, 0.11), ('C5', 0.36, 0.22),
           ('D5', 0.60, 0.11), ('E5', 0.72, 0.22), ('G5', 0.96, 0.75)]
    parts = []
    for n, s, d in seq:
        f = note_hz(n)
        parts.append((s, brass(f, d, 0.03, 3200), 0.55))
        parts.append((s, pluck(f * 2, min(d, 0.2), 0.1), 0.12))
    for n in ['C4', 'E4', 'G4', 'C5']:
        parts.append((0.96, brass(note_hz(n), 0.8, 0.02, 2000, 0.02), 0.25))
    parts += [(0.36, kick(), 0.6), (0.72, kick(), 0.6), (0.96, kick(), 0.8),
              (0.96, cymbal(0.9), 0.25), (0.24, snare(), 0.25), (0.6, snare(), 0.25)]
    return finish(mix(1.85, *parts))


def checkpoint():
    parts = [(0, bell(note_hz('A5'), 0.3, 0.12), 0.7), (0.06, bell(note_hz('E6'), 0.35, 0.15), 0.8),
             (0.12, bell(note_hz('A6'), 0.45, 0.2), 0.55)]
    sh = highpass_fast(noise(0.45, 11), 7000) * env(int(SR * 0.45), 0.05, 0.12, 0, 0.1)
    parts.append((0.05, sh, 0.12))
    return finish(mix(0.6, *parts))


def vote_cast():
    t = t_axis(0.14)
    f = 500 + 900 * (1 - np.exp(-t / 0.03))
    ph = 2 * np.pi * np.cumsum(f) / SR
    blip = np.sin(ph) * env(len(t), 0.002, 0.05, 0, 0.02)
    pop = lowpass_fast(noise(0.14, 4), 1200) * np.exp(-t / 0.006)
    return finish(mix(0.16, (0, blip, 1.0), (0, pop, 0.5)))


def vote_locked():
    t = t_axis(0.12)
    thunk = lowpass_fast(noise(0.12, 6), 600) * np.exp(-t / 0.015)
    parts = [(0, thunk, 0.9), (0, kick(0.2), 0.7)]
    for i, n in enumerate(['D5', 'F#5', 'A5', 'D6']):
        parts.append((0.06 + i * 0.045, bell(note_hz(n), 0.45, 0.18, 0.5), 0.45))
    return finish(mix(0.75, *parts))


def podium_fanfare():
    # original ~3.4 s fanfare in D major with snare roll lead-in
    parts = []
    roll_t = 0.0
    while roll_t < 0.6:
        parts.append((roll_t, snare(0.1, int(roll_t * 1000)), 0.18 + roll_t * 0.5))
        roll_t += 0.05
    seq = [('A4', 0.65, 0.14), ('D5', 0.80, 0.14), ('F#5', 0.95, 0.14), ('A5', 1.10, 0.45),
           ('F#5', 1.58, 0.14), ('A5', 1.73, 0.6), ('B5', 2.35, 0.14), ('C#6', 2.5, 0.14),
           ('D6', 2.65, 0.85)]
    for n, s, d in seq:
        f = note_hz(n)
        parts.append((s, brass(f, d, 0.035, 3400), 0.5))
        parts.append((s, brass(f / 2, d, 0.0, 1500), 0.22))
    for chord_t, chord, dur in [(1.10, ['D4', 'F#4', 'A4'], 0.45), (1.73, ['B3', 'D4', 'G4'], 0.6),
                                (2.65, ['D4', 'F#4', 'A4', 'D5'], 0.85)]:
        for n in chord:
            parts.append((chord_t, brass(note_hz(n), dur, 0.02, 1800, 0.02), 0.2))
        parts.append((chord_t, kick(), 0.8))
        parts.append((chord_t, cymbal(1.0, int(chord_t * 10)), 0.22))
    parts.append((2.65, bell(note_hz('D7'), 0.8, 0.3), 0.2))
    return finish(mix(3.6, *parts), fade=0.15)


SOUNDS = {
    'countdown_tick': (countdown_tick, 'Countdown ticks'),
    'countdown_go': (countdown_go, 'Round starts'),
    'point_gained': (point_gained, 'Points gained'),
    'elimination': (elimination, 'Player eliminated'),
    'team_eliminated': (team_eliminated, 'Team eliminated'),
    'round_win': (round_win, 'Round won'),
    'checkpoint': (checkpoint, 'Checkpoint reached'),
    'vote_cast': (vote_cast, 'Vote cast'),
    'vote_locked': (vote_locked, 'Vote locked in'),
    'podium_fanfare': (podium_fanfare, 'Podium fanfare'),
}


def write_all(out_dir):
    import soundfile as sf
    import os
    os.makedirs(out_dir, exist_ok=True)
    info = {}
    for name, (fn, _) in SOUNDS.items():
        x = fn()
        path = os.path.join(out_dir, name + '.ogg')
        sf.write(path, x, SR, format='OGG', subtype='VORBIS')
        info[name] = round(len(x) / SR, 2)
    return info
