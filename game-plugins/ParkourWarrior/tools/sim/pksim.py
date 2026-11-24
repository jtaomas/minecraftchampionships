"""Minimal Java 1.21 player-movement simulator (side view x/y) for jump-limit tables.
Constants (vanilla LivingEntity/Player, 1.21.1): jump 0.42, gravity 0.08, vertical drag 0.98,
horizontal drag 0.91 (x slip 0.6 on ground -> 0.546), sprint movement speed 0.13,
ground accel = speed*0.21600002/slip^3, air accel 0.026 sprinting (0.02 walking),
input magnitude 0.98 (W only) or 1.0 (W+A/D 45-degree strafe), sprint-jump boost 0.2,
box 0.6 x 1.8, momentum threshold 0.003. Y is moved before X.
Free-flight trick: landing at height dy happens on the tick whose start y >= dy and end y < dy;
on that tick Y is resolved at the start x, so max landing near-edge g = x_start + 0.3."""

HW = 0.3; H = 1.8

def simulate(L, inp=0.98, ceiling=None, ceil_end=0.0, policy="single", jt=None, start_v=0.0,
             ticks=70, x_start=None):
    """Platform x in [-L, 0] (top y=0), back wall at x=-L. Ceiling (solid, bottom at `ceiling`)
    over x in [-L, ceil_end]. policy 'single': jump only at tick jt. 'spam': jump whenever on ground.
    Returns list of (x_start, y_start, y_end) per airborne tick after the final takeoff, and takeoff info."""
    x = (-L + HW) if x_start is None else x_start; y = 0.0; vx = start_v; vy = 0.0; og = True
    hist = []
    last_jump_t = None
    for t in range(ticks):
        jump = og and ((policy == "single" and t == jt) or (policy == "spam"))
        if jump:
            vy = 0.42; vx += 0.2; last_jump_t = t
        vx += (0.13 * inp) if og else (0.026 * inp)
        fr = 0.546 if og else 0.91
        # --- Y move
        dy = vy; cy = False
        on_plat = (x - HW) < 0.0 - 1e-9 and (x + HW) > -L
        if on_plat and dy < 0 and y >= -1e-9 and y + dy < 0:
            dy = -y; cy = True
        if ceiling is not None and dy > 0:
            under = (x - HW) < ceil_end - 1e-9 and (x + HW) > -L
            if under and y + H <= ceiling + 1e-9 and y + H + dy > ceiling:
                dy = ceiling - (y + H); cy = True
        y0 = y; x0 = x
        y = y + dy
        # --- X move (ceiling side face: only if head overlaps ceiling height range)
        dx = vx
        if ceiling is not None and y + H > ceiling + 1e-9 and x + HW <= -L + 1e-9:
            pass
        x = x + dx
        og = cy and vy < 0
        if cy: vy = 0.0
        hist.append((t, x0, y0, y, og, jump))
        vx *= fr
        vy = (vy - 0.08) * 0.98
        if abs(vx) < 0.003: vx = 0.0
        if abs(vy) < 0.003: vy = 0.0
        if y < -8: break
    return hist, last_jump_t

def gap_for(hist, last_jump_t, dy):
    if last_jump_t is None: return None
    for (t, x0, y0, y1, og, jump) in hist:
        if t < last_jump_t: continue
        if t > last_jump_t and y0 >= dy - 1e-9 and y1 < dy and y1 < y0:
            return x0 + HW
    return None

def best_gap(L, dy, **kw):
    best = None
    for jt in range(0, 40):
        hist, lj = simulate(L, policy="single", jt=jt, **kw)
        # must take off from the platform: jump tick start had og True (guaranteed) -> fine
        g = gap_for(hist, lj, dy)
        if g is not None and (best is None or g > best): best = g
    return best

def chain_speed(inp):
    """Steady-state horizontal speed at the start of a jump tick when chain sprint-jumping on flat ground."""
    v = 0.0
    for _ in range(200):
        # one full jump cycle of 12 ticks starting from ground with stored v
        vj = v + 0.13 * inp + 0.2
        vv = vj * 0.546
        for k in range(11):
            vv = vv + 0.026 * inp
            vv *= 0.91
        v = vv
    return v

if __name__ == "__main__":
    dys = [1.0, 0.5, 0.0, -0.5, -1.0, -2.0, -3.0, -4.0]
    print("dy:", dys)
    for inp, lab in [(0.98, "W only"), (1.0, "45 strafe")]:
        for L in [1, 2, 3, 4, 5, 8, 30]:
            row = [best_gap(L, d, inp=inp) for d in dys]
            print(lab, "platform L=%2d" % L, " ".join("%6.3f" % r if r else "   -  " for r in row))
        cv = chain_speed(inp)
        row = []
        for d in dys:
            hist, lj = simulate(1, inp=inp, policy="single", jt=0, start_v=cv, x_start=0.3-1e-6)
            row.append(gap_for(hist, lj, d))
        print(lab, "chain-jump mm      ", " ".join("%6.3f" % r if r else "   -  " for r in row), " v=%.4f" % cv)
    # ceilings directly over the takeoff (jump under a ceiling of height c, open after x=ceil_end=0)
    print("head-hitter over takeoff block (ceiling covers platform only), L=30")
    for c in [2.0, 2.5, 3.0, 1.8125 + 0.0]:
        row = [best_gap(30, d, inp=1.0, ceiling=c, ceil_end=0.0) for d in dys]
        print(" ceiling %.4f" % c, " ".join("%6.3f" % r if r else "   -  " for r in row))
    # hh momentum: spam-jump under 2bc ceiling that ends at ceil_end, final jump in the open
    print("hh momentum (2bc ceiling over run-up ending at e, spam jump), best over e")
    for L in [3, 4, 6, 10, 20]:
        row = []
        for d in dys:
            b = None
            for k in range(0, 100):
                e = -L + k * (L / 100.0)
                hist, lj = simulate(L, inp=1.0, ceiling=2.0, ceil_end=e, policy="spam", ticks=200)
                # find last jump that is a full jump: use the first takeoff after x-0.3 > e
                g = None
                # recompute from last jump: emulate by stopping spam after full jump: take the jump whose airtime starts beyond e
                for (t, x0, y0, y1, og, jump) in hist:
                    if jump and x0 - HW >= e:
                        g = gap_for(hist, t, d); break
                if g is not None and (b is None or g > b): b = g
            row.append(b)
        print(" L=%2d" % L, " ".join("%6.3f" % r if r else "   -  " for r in row))
