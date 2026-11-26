// MCM staging test harness: boots the STAGING Paper server, connects mineflayer bots,
// runs every game, and writes results.md / results.json + raw logs.
//
// Usage (from this folder):
//   node harness.js                  -> pass1 (all games, 90s + skip), pass2 (MCC games to natural end), championship
//   node harness.js --only=skybattle,meltdown   -> restrict game ids
//   node harness.js --no-pass2 --no-champ       -> skip phases
//   node harness.js --pass1-secs=90 --pass2-mins=12 --bots=6 --out=results   (output file basename)
'use strict';
const fs = require('fs');
const path = require('path');
const { spawn, execFileSync } = require('child_process');
const mineflayer = require('mineflayer');

// ---------- config ----------
const HERE = __dirname;
const SCRATCH = path.resolve(HERE, '..');
const STAGING = path.join(SCRATCH, 'staging');
const REAL = 'C:\\Users\\LeonS\\Desktop\\Mcc';
const BUILT = path.join(REAL, 'game-plugins', '_built');
const CORE_JAR = path.join(REAL, 'plugin-build', 'target', 'MinecraftMondayCore.jar');
const JAVA = 'C:\\Users\\LeonS\\Desktop\\java\\bin\\java.exe';
const HOST = '127.0.0.1', PORT = 25570, MC_VERSION = '1.21.1';
const BUILTIN = ['sg', 'parkour', 'dropper', 'tntrun', 'spleef', 'kitpvp', 'survival', 'oneshot'];

const args = Object.fromEntries(process.argv.slice(2).map(a => {
  const m = a.match(/^--([^=]+)(?:=(.*))?$/); return m ? [m[1], m[2] ?? true] : [a, true];
}));
const NBOTS = +(args.bots || 6);
const PASS1_SECS = +(args['pass1-secs'] || 90);
const PASS2_MINS = +(args['pass2-mins'] || 12);
const CHAMP_SECS = +(args['champ-secs'] || 180);
const OUT = String(args.out || 'results');
const ONLY = args.only ? String(args.only).split(',').map(s => s.trim().toLowerCase()) : null;

const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
const LOGDIR = path.join(HERE, 'logs', RUN_ID);
fs.mkdirSync(LOGDIR, { recursive: true });
const serverLog = fs.createWriteStream(path.join(LOGDIR, 'server-stdout.log'));
const botLog = fs.createWriteStream(path.join(LOGDIR, 'bots.log'));
const harnessLog = fs.createWriteStream(path.join(LOGDIR, 'harness.log'));

const sleep = ms => new Promise(r => setTimeout(r, ms));
const ts = () => new Date().toISOString().slice(11, 23);
function hlog(...a) { const s = `[${ts()}] ${a.join(' ')}`; console.log(s); harnessLog.write(s + '\n'); }
function blog(...a) { botLog.write(`[${ts()}] ${a.join(' ')}\n`); }

process.on('uncaughtException', e => { hlog('UNCAUGHT', e && e.stack || e); });
process.on('unhandledRejection', e => { hlog('UNHANDLED', e && e.stack || e); });

// ---------- state ----------
const results = { runId: RUN_ID, startedAt: new Date().toISOString(), games: {}, pass2: {}, champ: null,
  lag: [], botEvents: [], globalIssues: [], registeredIds: [], notes: [] };
let currentLabel = 'boot';
const lines = []; // {t, raw, lvl, msg, label}
const lineWaiters = new Set();
let java = null, stdin = null, shuttingDown = false;

// ---------- jar copy ----------
function copyJars() {
  const dst = path.join(STAGING, 'plugins');
  const copied = [];
  for (const f of fs.readdirSync(BUILT).filter(f => f.toLowerCase().endsWith('.jar'))) {
    fs.copyFileSync(path.join(BUILT, f), path.join(dst, f)); copied.push(f);
  }
  if (fs.existsSync(CORE_JAR)) { fs.copyFileSync(CORE_JAR, path.join(dst, 'MinecraftMondayCore.jar')); copied.push('MinecraftMondayCore.jar (core)'); }
  hlog('copied jars:', copied.join(', '));
  results.notes.push('Jars copied: ' + copied.join(', '));
}

// ---------- server ----------
const LINE_RE = /^\[(\d\d:\d\d:\d\d)(?:\] \[[^\]]*\/| )(INFO|WARN|ERROR|FATAL)\]:? ?(.*)$/;
function onServerLine(raw) {
  raw = raw.replace(/\x1b\[[0-9;]*[A-Za-z]/g, '').replace(/\r$/, '');
  const m = raw.match(LINE_RE);
  const L = { t: Date.now(), raw, lvl: m ? m[2] : null, msg: m ? m[3] : raw, label: currentLabel, idx: lines.length };
  lines.push(L);
  serverLog.write(raw + '\n');
  const lag = raw.match(/Can't keep up! .*?Running (\d+)ms or (\d+) ticks behind/);
  if (lag) results.lag.push({ label: currentLabel, ms: +lag[1], ticks: +lag[2], time: m ? m[1] : '' });
  for (const w of [...lineWaiters]) { if (w.re.test(L.msg) || w.re.test(raw)) { lineWaiters.delete(w); clearTimeout(w.timer); w.resolve(L); } }
}
function waitLine(re, timeoutMs, sinceIdx = null) {
  if (sinceIdx !== null) { for (let i = sinceIdx; i < lines.length; i++) if (re.test(lines[i].msg) || re.test(lines[i].raw)) return Promise.resolve(lines[i]); }
  return new Promise(resolve => {
    const w = { re, resolve, timer: setTimeout(() => { lineWaiters.delete(w); resolve(null); }, timeoutMs) };
    lineWaiters.add(w);
  });
}
function send(cmd) { if (!stdin || java.exitCode !== null) return; hlog('> ' + cmd); stdin.write(cmd + '\n'); }

async function startServer() {
  const logf = path.join(STAGING, 'logs', 'latest.log');
  java = spawn(JAVA, ['-Xms1G', '-Xmx4G', '-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-jar', 'paper.jar', '--nogui'],
    { cwd: STAGING, stdio: ['pipe', 'pipe', 'pipe'], windowsHide: true });
  stdin = java.stdin;
  hlog('java pid', java.pid);
  let buf = '';
  const feed = d => { buf += d.toString('utf8'); let i; while ((i = buf.indexOf('\n')) >= 0) { onServerLine(buf.slice(0, i)); buf = buf.slice(i + 1); } };
  java.stdout.on('data', feed); java.stderr.on('data', feed);
  java.on('exit', c => hlog('java exited code', c));
  const done = await waitLine(/Done \(\d/, 15 * 60 * 1000);
  if (!done) throw new Error('server did not finish booting');
  await sleep(3000);
  send('list'); // throwaway (first stdin line may get a stray BOM)
  await sleep(2000);
}

async function stopServer() {
  if (!java || java.exitCode !== null) return;
  send('stop');
  const exited = await Promise.race([new Promise(r => java.once('exit', () => r(true))), sleep(60000).then(() => false)]);
  if (!exited) { hlog('stop hung >60s, killing java'); results.notes.push('Server stop hung >60s; killed.'); try { execFileSync('taskkill', ['/PID', String(java.pid), '/T', '/F']); } catch {} }
}

// ---------- bots ----------
const bots = {}; // name -> {bot, name, connected, kicks:[], ...}
function rnd(a, b) { return a + Math.random() * (b - a); }
function botEvent(name, type, detail) {
  const e = { t: new Date().toISOString(), label: currentLabel, bot: name, type, detail: String(detail).slice(0, 500) };
  results.botEvents.push(e); blog(name, type, e.detail, '@', currentLabel);
}
function reasonText(r) {
  try { if (typeof r === 'string') { try { r = JSON.parse(r); } catch { return r; } }
    const out = []; (function walk(x) { if (!x) return; if (typeof x === 'string') return out.push(x); if (x.text) out.push(x.text); if (x.value && typeof x.value === 'object') walk(x.value.text?.value ?? x.value); if (Array.isArray(x.extra)) x.extra.forEach(walk); if (x.extra?.value?.value) x.extra.value.value.forEach(walk); if (x.translate) out.push('[' + x.translate + ']'); })(r);
    return out.join('') || JSON.stringify(r);
  } catch { return String(r); }
}
function makeBot(name) {
  const st = bots[name] || (bots[name] = { name, connected: false, deaths: 0, reconnects: 0 });
  let bot;
  try { bot = mineflayer.createBot({ host: HOST, port: PORT, username: name, version: MC_VERSION, auth: 'offline', hideErrors: true, checkTimeoutInterval: 60000 }); }
  catch (e) { botEvent(name, 'create-error', e.stack || e); return scheduleReconnect(name); }
  st.bot = bot;
  let activity = null;
  bot.once('spawn', () => {
    st.connected = true; botEvent(name, 'spawn', `${fmtPos(bot)} dim=${bot.game.dimension}`);
    activity = setInterval(() => { try { act(bot); } catch (e) { blog(name, 'act-error', e.message); } }, 800);
  });
  bot.on('physicsTick', () => { try { guard(bot); } catch {} });
  bot.on('respawn', () => blog(name, 'respawn/world-change', `dim=${bot.game.dimension} mode=${bot.game.gameMode}`));
  bot.on('death', () => { st.deaths++; blog(name, 'death'); });
  bot.on('kicked', (reason, loggedIn) => botEvent(name, 'kicked', `${reasonText(reason)} (loggedIn=${loggedIn})`));
  bot.on('error', e => botEvent(name, 'error', e && (e.stack || e.message) || e));
  bot.on('windowOpen', w => { blog(name, 'windowOpen', w.title ? reasonText(w.title) : w.type); setTimeout(() => { try { bot.closeWindow(w); } catch {} }, 1500); });
  bot.on('messagestr', (m, pos) => { if (pos !== 'game_info') blog(name, 'chat', m); });
  bot.on('end', reason => {
    st.connected = false; if (activity) clearInterval(activity);
    blog(name, 'end', reason);
    if (!shuttingDown) { botEvent(name, 'disconnected', reason); scheduleReconnect(name); }
  });
}
function scheduleReconnect(name) { if (shuttingDown) return; bots[name].reconnects++; setTimeout(() => !shuttingDown && makeBot(name), 6000); }
// Edge guard: only ever walk forward, and stop + turn around if there is no floor within 4 blocks ahead.
function edgeAhead(bot) {
  const p = bot.entity.position, yaw = bot.entity.yaw;
  const dx = -Math.sin(yaw), dz = -Math.cos(yaw);
  for (const d of [0.8, 1.6]) {
    const x = p.x + dx * d, z = p.z + dz * d;
    let floor = false;
    for (let dy = 1; dy <= 4; dy++) { const b = bot.blockAt(p.offset(x - p.x, -dy, z - p.z)); if (b && b.boundingBox === 'block') { floor = true; break; } }
    if (!floor) return true;
  }
  return false;
}
function guard(bot) {
  if (!bot.entity || !bot.entity.onGround || !bot.getControlState('forward')) return;
  if (edgeAhead(bot)) { bot.setControlState('forward', false); bot.setControlState('sprint', false); bot.look(bot.entity.yaw + Math.PI + rnd(-0.5, 0.5), 0, true).catch(() => {}); }
}
function act(bot) {
  if (!bot.entity) return;
  const r = Math.random();
  if (r < 0.25) { // change movement
    for (const c of ['forward', 'back', 'left', 'right', 'sprint']) bot.setControlState(c, false);
    if (Math.random() < 0.7) bot.setControlState('forward', true);
    if (Math.random() < 0.3) bot.setControlState('sprint', true);
  }
  bot.setControlState('jump', Math.random() < 0.15);
  if (Math.random() < 0.4) bot.look(bot.entity.yaw + rnd(-1.2, 1.2), rnd(-0.6, 0.6), false).catch(() => {});
  if (Math.random() < 0.12) bot.swingArm();
  if (Math.random() < 0.05) { bot.activateItem(); setTimeout(() => { try { bot.deactivateItem(); } catch {} }, 600); }
  if (Math.random() < 0.04) bot.setQuickBarSlot(Math.floor(Math.random() * 9));
}
const fmtPos = b => b.entity ? `${b.entity.position.x.toFixed(1)},${b.entity.position.y.toFixed(1)},${b.entity.position.z.toFixed(1)}` : '?';
async function connectBots() {
  for (let i = 1; i <= NBOTS; i++) { makeBot('Bot' + i); await sleep(1500); }
  await sleep(6000);
  hlog('bots connected:', Object.values(bots).filter(b => b.connected).map(b => b.name).join(','));
}

// server-side + client-side snapshot of every bot
async function snapshot(tag) {
  const since = lines.length;
  for (const name of Object.keys(bots)) send(`data get entity ${name} Dimension`);
  await sleep(2500);
  const out = {};
  for (const [name, st] of Object.entries(bots)) {
    const b = st.bot;
    let serverDim = null;
    for (let i = since; i < lines.length; i++) {
      const m = lines[i].msg.match(new RegExp(`^${name} has the following entity data: "?([^"]+)"?`));
      if (m) serverDim = m[1];
      if (lines[i].msg.includes('No entity was found') && serverDim === null) serverDim = serverDim; // handled below
    }
    out[name] = { connected: st.connected, pos: b ? fmtPos(b) : '?', clientDim: b?.game?.dimension ?? null,
      serverDim: serverDim ?? (st.connected ? 'n/a' : 'offline'), mode: b?.game?.gameMode ?? null, hp: b?.health != null ? Math.round(b.health) : null };
  }
  hlog(`snapshot ${tag}:`, Object.entries(out).map(([n, s]) => `${n}@${s.serverDim} ${s.pos} ${s.mode}`).join(' | '));
  return out;
}

// ---------- issue extraction ----------
const NOISE = [/ViaVersion\] The Vanilla protocol/, /OldCombatMechanics\] Attack range/, /PaperVersionFetcher/, /Error obtaining version information/,
  /Unknown or incomplete command/, /<--\[HERE\]/, /OFFLINE\/INSECURE MODE/, /make no attempt to authenticate/, /opens up the ability for hackers/, /set "online-mode" to "true"/];
const BENIGN = /moved too quickly|moved wrongly|lost connection: Disconnected|Ignoring unknown attribute/;
function pluginOf(text) {
  const m1 = text.match(/\[(Mcm\w+|MinecraftMondayCore)\]/); if (m1) return m1[1];
  const m2 = text.match(/(?:to|for|plugin) (Mcm\w+|MinecraftMondayCore) v/); if (m2) return m2[1];
  const m3 = text.match(/com\.mcm\.games\.(\w+)/); if (m3) return 'Mcm:' + m3[1];
  if (/com\.mcm\./.test(text)) return 'MinecraftMondayCore';
  const m4 = text.match(/^\[([\w-]+)\] /); if (m4) return m4[1];
  return 'server';
}
function extractIssues(fromIdx, toIdx) {
  const issues = []; let cur = null;
  for (let i = fromIdx; i < Math.min(toIdx, lines.length); i++) {
    const L = lines[i];
    if (L.lvl) {
      if (cur) { issues.push(cur); cur = null; }
      if (L.lvl === 'WARN' || L.lvl === 'ERROR' || L.lvl === 'FATAL') cur = { lvl: L.lvl, head: L.msg, body: [], label: L.label };
    } else if (cur && L.raw.trim()) cur.body.push(L.raw);
  }
  if (cur) issues.push(cur);
  return issues.filter(x => !NOISE.some(re => re.test(x.head))).map(x => {
    const text = x.head + '\n' + x.body.join('\n');
    const frames = x.body.filter(l => /^\s+at /.test(l)).map(l => l.trim());
    const causes = x.body.filter(l => /^(Caused by:|[\w.$]+(Exception|Error)\b)/.test(l.trim())).map(l => l.trim());
    const ownFrames = frames.filter(f => /com\.mcm\./.test(f));
    const sig = (x.head.replace(/Bot\d+/g, 'BotN').replace(/-?\d+(\.\d+)?/g, '#') + '|' + causes.join('|') + '|' + (ownFrames[0] || frames[0] || '')).slice(0, 400);
    return { lvl: x.lvl, plugin: BENIGN.test(x.head) ? 'bot-movement(benign)' : pluginOf(text), head: x.head, causes, topFrames: (ownFrames.length ? ownFrames : frames).slice(0, 4), full: text, sig, label: x.label };
  });
}
function dedupe(issues) {
  const map = new Map();
  for (const i of issues) { const e = map.get(i.sig); if (e) e.count++; else map.set(i.sig, { ...i, count: 1 }); }
  return [...map.values()];
}

// ---------- game runs ----------
// A bot counts as "in the arena" if its server-side world differs from the lobby world, or (for games
// that use the lobby world) it is >120 blocks from the lobby centre.
let LOBBY_DIM = 'minecraft:overworld';
function inArena(lobby, snap) {
  const moved = [];
  for (const [n, s] of Object.entries(snap)) {
    if (!s.connected || !s.serverDim || s.serverDim === 'n/a') continue;
    let far = false;
    if (s.pos !== '?') { const b = s.pos.split(',').map(Number); far = Math.hypot(b[0], b[2]) > 120; }
    const isLobby = s.serverDim === LOBBY_DIM || s.serverDim === 'minecraft:mcc_lobby'; // core's MCC hub island counts as lobby
    if (!isLobby || (far && s.serverDim === LOBBY_DIM)) moved.push(n);
  }
  return moved;
}
const botsSince = idx => results.botEvents.slice(idx).filter(e => e.type === 'kicked' || e.type === 'error' || e.type === 'disconnected');

async function runGame(id, mode) {
  // mode: 'pass1' (watch PASS1_SECS then skip) | 'pass2' (natural end, hard timeout)
  const key = id; const label = `${mode}:${id}`; currentLabel = label;
  hlog(`===== ${label} =====`);
  const r = { id, mode, started: false, name: null, startDelayMs: null, endReason: null, endedBy: null, durationSec: null,
    returnedToLobby: null, movedAt10: [], movedAt60: [], snaps: {}, issues: [], botProblems: [], notes: [] };
  (mode === 'pass1' ? results.games : results.pass2)[key] = r;
  const lobby = await snapshot('lobby'); r.snaps.lobby = lobby;
  const fromIdx = lines.length, botIdx = results.botEvents.length;
  const tCmd = Date.now();
  send(`mcm play ${id}`);
  const start = await waitLine(/\[MCM\] >>> (.+?) <<<|>>> .+ <<</, 30000, fromIdx);
  const quickReply = lines.slice(fromIdx, fromIdx + 15).filter(l => l.lvl === 'INFO').map(l => l.msg).slice(0, 4);
  if (!start) {
    r.notes.push('No ">>> name <<<" within 30s. Console replies: ' + quickReply.join(' / '));
    send('mcm stop'); await sleep(8000);
  } else {
    r.started = true; r.startDelayMs = start.t - tCmd;
    r.name = (start.msg.match(/>>> (.+?) <<</) || [])[1];
    const t0 = start.t;
    const overRe = /=== (.+) OVER! \((.*)\) ===/;
    const limitMs = mode === 'pass1' ? PASS1_SECS * 1000 : PASS2_MINS * 60000;
    let over = null;
    const waitOver = async untilT => { while (!over && Date.now() < untilT) { over = await waitLine(overRe, Math.max(1, Math.min(5000, untilT - Date.now())), start.idx + 1); } };
    await waitOver(t0 + 10000);
    if (!over) { r.snaps.t10 = await snapshot('t+10s'); r.movedAt10 = inArena(lobby, r.snaps.t10); }
    await waitOver(t0 + 60000);
    if (!over) { r.snaps.t60 = await snapshot('t+60s'); r.movedAt60 = inArena(lobby, r.snaps.t60); }
    await waitOver(t0 + limitMs);
    if (!over) {
      r.endedBy = mode === 'pass1' ? 'skip' : 'timeout-skip';
      send('mcm next');
      over = await waitLine(overRe, 20000, lines.length - 1);
      if (!over) { r.notes.push('No OVER line within 20s of /mcm next; sending /mcm stop'); send('mcm stop'); over = await waitLine(overRe, 15000); }
    } else r.endedBy = 'natural';
    if (over) { const m = over.msg.match(overRe) || over.raw.match(overRe); r.endReason = m ? m[2] : over.msg; r.durationSec = Math.round((over.t - t0) / 1000); }
    await sleep(10000);
    r.snaps.after = await snapshot('after');
    const stillAway = inArena(lobby, r.snaps.after);
    r.returnedToLobby = stillAway.length === 0;
    if (stillAway.length) r.notes.push('Not back in lobby 10s after end: ' + stillAway.map(n => `${n}@${r.snaps.after[n].serverDim} ${r.snaps.after[n].pos}`).join(', '));
  }
  r.issues = dedupe(extractIssues(fromIdx, lines.length));
  r.botProblems = botsSince(botIdx);
  r.lag = results.lag.filter(l => l.label === label);
  // FAIL on any ERROR, or any WARN carrying an exception, that isn't benign bot-movement noise
  const pluginErr = r.issues.filter(i => !/benign/.test(i.plugin) && (i.lvl !== 'WARN' || /Exception|\tat |^\s+at /m.test(i.full)));
  r.pass = r.started && !!r.endReason && r.returnedToLobby !== false && pluginErr.length === 0
    && r.botProblems.filter(b => b.type === 'kicked').length === 0
    && (mode === 'pass2' || r.movedAt10.length + r.movedAt60.length > 0 || r.endedBy === 'natural');
  hlog(`result ${label}: started=${r.started} end=${r.endReason} by=${r.endedBy} dur=${r.durationSec}s moved10=${r.movedAt10.length} moved60=${r.movedAt60.length} lobby=${r.returnedToLobby} issues=${r.issues.length} PASS=${r.pass}`);
  writeResults();
  return r;
}

async function runChampionship() {
  currentLabel = 'championship'; hlog('===== championship =====');
  const c = { started: false, firstGame: null, domeLines: [], domeTriggered: false, chosen: null, nextGame: null, issues: [], notes: [] };
  results.champ = c;
  const fromIdx = lines.length;
  send('mcm start');
  const s = await waitLine(/>>> (.+?) <<</, 30000, fromIdx);
  if (!s) { c.notes.push('no >>> after /mcm start'); }
  else {
    c.started = true; c.firstGame = (s.msg.match(/>>> (.+?) <<</) || [])[1];
    const over = await waitLine(/OVER! \(/, 45000, s.idx + 1);
    if (!over) { send('mcm next'); c.notes.push('skipped first game after 45s to reach the between-games vote'); }
    const until = Date.now() + Math.max(60000, CHAMP_SECS * 1000 - 45000);
    const dome = await waitLine(/Decision Dome|\[Dome\]/, until - Date.now(), fromIdx);
    c.domeTriggered = !!dome;
    if (dome) {
      const chosen = await waitLine(/\[Decision Dome\] .* was chosen|Vote did not finish within 90s/, Math.max(1000, until - Date.now() + 60000), dome.idx);
      if (chosen) c.chosen = chosen.msg;
      const next = await waitLine(/>>> (.+?) <<</, 30000, chosen ? chosen.idx : dome.idx);
      if (next) c.nextGame = (next.msg.match(/>>> (.+?) <<</) || [])[1];
      await sleep(15000);
    }
    c.domeLines = lines.slice(fromIdx).filter(l => /Dome|Decision|Next event|vote/i.test(l.msg)).map(l => l.msg).slice(0, 40);
  }
  send('mcm stop'); await sleep(10000);
  c.issues = dedupe(extractIssues(fromIdx, lines.length));
  c.pass = c.started && c.domeTriggered && !!c.nextGame && !c.issues.some(i => i.lvl === 'ERROR');
  hlog(`championship: dome=${c.domeTriggered} chosen=${c.chosen} next=${c.nextGame}`);
  writeResults();
}

// ---------- reporting ----------
function esc(s) { return String(s ?? '').replace(/\|/g, '\\|').replace(/\n/g, ' '); }
function writeResults() {
  fs.writeFileSync(path.join(HERE, OUT + '.json'), JSON.stringify(results, null, 1));
  const o = [];
  o.push(`# MCM staging bot test - run ${RUN_ID}`, '', `Raw logs: \`${LOGDIR}\``, `Bots: ${NBOTS}, pass1 watch ${PASS1_SECS}s then /mcm next; pass2 natural end (timeout ${PASS2_MINS} min).`, '');
  for (const n of results.notes) o.push('- ' + n);
  o.push('', '## Pass 1 (all games: play, watch, skip)', '', '| id | name | result | started | ended | duration | bots in arena @10s / @60s | back in lobby | issues | bot kicks/errors |', '|---|---|---|---|---|---|---|---|---|---|');
  for (const r of Object.values(results.games)) o.push(`| ${r.id} | ${esc(r.name)} | **${r.pass ? 'PASS' : 'FAIL'}** | ${r.started ? 'yes' : 'NO'} | ${esc(r.endReason ?? '-')} (${r.endedBy ?? '-'}) | ${r.durationSec ?? '-'}s | ${r.movedAt10.length}/${r.movedAt60.length} | ${r.returnedToLobby ?? '-'} | ${r.issues.map(i => `${i.lvl} ${i.plugin} x${i.count}`).join('; ') || '-'} | ${r.botProblems.length} |`);
  if (Object.keys(results.pass2).length) {
    o.push('', '## Pass 2 (MCC games to natural end)', '', '| id | name | result | end reason | ended by | duration | bots in arena @10s/@60s | back in lobby | issues |', '|---|---|---|---|---|---|---|---|---|');
    for (const r of Object.values(results.pass2)) o.push(`| ${r.id} | ${esc(r.name)} | **${r.pass ? 'PASS' : 'FAIL'}** | ${esc(r.endReason ?? '-')} | ${r.endedBy ?? '-'} | ${r.durationSec ?? '-'}s | ${r.movedAt10.length}/${r.movedAt60.length} | ${r.returnedToLobby ?? '-'} | ${r.issues.map(i => `${i.lvl} ${i.plugin} x${i.count}`).join('; ') || '-'} |`);
  }
  if (results.champ) {
    const c = results.champ;
    o.push('', '## Championship / Decision Dome', '', `- Result: **${c.pass ? 'PASS' : 'FAIL'}**; first game: ${c.firstGame}; dome triggered: ${c.domeTriggered}; chosen: ${esc(c.chosen)}; next game started: ${c.nextGame}`);
    for (const n of c.notes) o.push('- ' + n);
    o.push('', '```', ...c.domeLines, '```');
  }
  o.push('', '## Server lag ("Can\'t keep up")', '');
  if (!results.lag.length) o.push('None.'); else for (const l of results.lag) o.push(`- ${l.time} ${l.label}: ${l.ms} ms (${l.ticks} ticks)${l.ms > 2000 ? ' **>2s**' : ''}`);
  o.push('', '## Per-game details', '');
  for (const r of [...Object.values(results.games), ...Object.values(results.pass2)]) {
    o.push(`### ${r.mode}: ${r.id} (${r.name ?? '?'}) - ${r.pass ? 'PASS' : 'FAIL'}`, '');
    for (const n of r.notes) o.push('- ' + n);
    for (const [k, s] of Object.entries(r.snaps)) o.push(`- positions ${k}: ` + Object.entries(s).map(([n, v]) => `${n} ${v.serverDim} (${v.pos}) ${v.mode ?? ''}`).join('; '));
    for (const b of r.botProblems) o.push(`- bot ${b.bot} ${b.type}: ${esc(b.detail)}`);
    for (const i of r.issues) o.push('', `**${i.lvl}** [${i.plugin}] x${i.count}: ${esc(i.head)}`, '', '```', i.full.split('\n').slice(0, 40).join('\n'), '```');
    o.push('');
  }
  if (results.champ?.issues?.length) { o.push('### championship issues'); for (const i of results.champ.issues) o.push('', `**${i.lvl}** [${i.plugin}] x${i.count}: ${esc(i.head)}`, '', '```', i.full.split('\n').slice(0, 40).join('\n'), '```'); }
  if (results.globalIssues.length) { o.push('', '## Boot / other issues', ''); for (const i of results.globalIssues) o.push('', `**${i.lvl}** [${i.plugin}] x${i.count}: ${esc(i.head)}`, '', '```', i.full.split('\n').slice(0, 30).join('\n'), '```'); }
  const kicks = results.botEvents.filter(e => e.type === 'kicked' || e.type === 'error');
  o.push('', '## Bot kicks / errors', '');
  if (!kicks.length) o.push('None.'); else for (const e of kicks) o.push(`- ${e.t} [${e.label}] ${e.bot} ${e.type}: ${esc(e.detail)}`);
  fs.writeFileSync(path.join(HERE, OUT + '.md'), o.join('\n') + '\n');
}

// ---------- main ----------
async function main() {
  copyJars();
  await startServer();
  results.globalIssues = dedupe(extractIssues(0, lines.length));
  // registered ids
  let idx = lines.length; send('mcm games');
  await waitLine(/Championship order/, 10000, idx); await sleep(500);
  const reg = [];
  for (const L of lines.slice(idx)) { const m = L.msg.match(/^([a-z0-9_]+) - (.+?) \(/); if (m) reg.push(m[1]); }
  results.registeredIds = reg;
  hlog('registered MCC games:', reg.join(', '));
  let ids = [...BUILTIN, ...reg.filter(x => !BUILTIN.includes(x))];
  if (ONLY) ids = ids.filter(x => ONLY.includes(x));
  await connectBots();
  currentLabel = 'idle';
  const init = await snapshot('initial');
  const dims = Object.values(init).map(s => s.serverDim).filter(d => d && d.startsWith('minecraft:'));
  if (dims.length) LOBBY_DIM = dims.sort((a, b) => dims.filter(x => x === b).length - dims.filter(x => x === a).length)[0];
  hlog('lobby dimension:', LOBBY_DIM);
  let pass1ok = true;
  if (!args['no-pass1']) {
    for (const id of ids) {
      if (java.exitCode !== null) { pass1ok = false; hlog('server died'); break; }
      try { await runGame(id, 'pass1'); } catch (e) { hlog('runGame error', e.stack); pass1ok = false; }
    }
  }
  if (pass1ok && !args['no-pass2']) {
    for (const id of ids.filter(x => !BUILTIN.includes(x))) {
      if (java.exitCode !== null) break;
      try { await runGame(id, 'pass2'); } catch (e) { hlog('runGame error', e.stack); }
    }
  }
  if (!args['no-champ'] && java.exitCode === null) { try { await runChampionship(); } catch (e) { hlog('champ error', e.stack); } }
}

async function shutdown(code) {
  shuttingDown = true; currentLabel = 'shutdown';
  for (const st of Object.values(bots)) { try { st.bot?.quit(); } catch {} }
  await sleep(2000);
  await stopServer();
  results.finishedAt = new Date().toISOString();
  writeResults();
  hlog('harness finished; results.md written');
  serverLog.end(); botLog.end(); harnessLog.end();
  setTimeout(() => process.exit(code), 1000);
}
process.on('SIGINT', () => { hlog('SIGINT'); shutdown(130); });
process.on('exit', () => { try { if (java && java.exitCode === null) execFileSync('taskkill', ['/PID', String(java.pid), '/T', '/F']); } catch {} });

main().then(() => shutdown(0), e => { hlog('FATAL', e.stack || e); shutdown(1); });
