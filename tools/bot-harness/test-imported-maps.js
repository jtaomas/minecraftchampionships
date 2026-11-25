'use strict';

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const mineflayer = require('mineflayer');

const ROOT_DIR = path.resolve(__dirname, '../..');
const SERVER_DIR = path.join(ROOT_DIR, '_candidates', 'staging-test');
const JAVA_BIN = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.13.10002-hotspot\\bin\\java.exe';
const RESULTS_MD = path.join(__dirname, 'results-imported-maps.md');
const PORT = 25570;

const SG_MAPS = ['breeze_island', 'holiday_resort', 'turbulence', 'teweran_sg1', 'sg4', 'kalas'];
const BB_MAPS = ['trains', 'steampunk', 'bowser', 'beach', 'jaws', 'waterfalls'];
const RSR_MAPS = ['acryme_2', 'acryme_3', 'acryme_4', 'acryme_5', 'acryme_8', 'acryme_9'];

const NOISE_PATTERNS = [
  /ViaVersion\]/,
  /OldCombatMechanics\]/,
  /PaperVersionFetcher/,
  /Error obtaining version information/,
  /OFFLINE\/INSECURE MODE/,
  /make no attempt to authenticate/,
  /opens up the ability for hackers/,
  /set "online-mode" to "true"/,
  /You have .* plugin\(s\) that might not be compatible/,
  /SPARK/,
  /spark\]/i,
  /Multiverse-Core\]/,
  /TAB\]/,
  /Loaded .* mappings/i
];

const BENIGN_WARN_PATTERNS = [
  /moved too quickly/i,
  /moved wrongly/i,
  /lost connection: Disconnected/i,
  /Ignoring unknown attribute/i,
  /Can't keep up!/i,
  /Fetching packet/i,
  /commands\.place\.structure\.failed/i
];

const sleep = ms => new Promise(r => setTimeout(r, ms));
const rnd = (a, b) => a + Math.random() * (b - a);

function log(msg) {
  const ts = new Date().toISOString().slice(11, 19);
  console.log(`[harness ${ts}] ${msg}`);
}

function stripAnsi(s) {
  return s.replace(/\x1B\[[0-9;]*[A-Za-z]/g, '');
}

const serverLines = [];
const botChatLines = [];
let srv = null;
let stdoutBuf = '';
let shuttingDown = false;
const bots = {}; // name -> { name, bot, connected, kicks, errors, logs }

function parseLogLine(rawLine) {
  const clean = stripAnsi(rawLine).replace(/\r$/, '');
  if (!clean.trim()) return;
  const m = clean.match(/^\[(\d{2}:\d{2}:\d{2})(?:\]\s+\[[^/\]]+\/|\s+)(INFO|WARN|ERROR|FATAL)\]:\s*(.*)$/);
  const lvl = m ? m[2] : null;
  const msg = m ? m[3] : clean;
  const entry = m
    ? { t: Date.now(), time: m[1], lvl, msg, raw: clean }
    : { t: Date.now(), time: null, lvl: null, msg: clean, raw: clean };
  serverLines.push(entry);
  if (/Done \(|Loaded .* imported|Prepared imported SG map|>>> .* <<<|OVER!|ERROR|FATAL|Exception/.test(clean)) {
    log(`[srv] ${clean.slice(0, 180)}`);
  }
}

function sendCmd(cmd) {
  if (!srv || !srv.stdin.writable) return;
  log(`> ${cmd}`);
  srv.stdin.write(cmd + '\n');
}

async function waitLine(re, timeoutMs, fromIdx = 0) {
  const deadline = Date.now() + timeoutMs;
  let i = fromIdx;
  while (Date.now() < deadline) {
    while (i < serverLines.length) {
      const L = serverLines[i];
      const match = L.msg.match(re) || L.raw.match(re);
      if (match) return { idx: i, ...L, match };
      i++;
    }
    await sleep(100);
  }
  return null;
}

function extractIssues(fromIdx, toIdx = serverLines.length) {
  const issues = [];
  let cur = null;
  for (let i = fromIdx; i < Math.min(toIdx, serverLines.length); i++) {
    const L = serverLines[i];
    if (L.lvl) {
      if (cur) { issues.push(cur); cur = null; }
      if (L.lvl === 'WARN' || L.lvl === 'ERROR' || L.lvl === 'FATAL') {
        cur = { lvl: L.lvl, head: L.msg, body: [] };
      }
    } else if (cur && L.raw.trim()) {
      cur.body.push(L.raw.trim());
    }
  }
  if (cur) issues.push(cur);

  return issues
    .filter(it => !NOISE_PATTERNS.some(re => re.test(it.head)))
    .filter(it => !(it.lvl === 'WARN' && BENIGN_WARN_PATTERNS.some(re => re.test(it.head)) && it.body.length === 0))
    .filter(it => it.lvl === 'ERROR' || it.lvl === 'FATAL' || it.body.some(l => /Exception|Error|\tat /.test(l)) || /Exception|Could not|Failed/i.test(it.head));
}

async function waitChat(re, timeoutMs, fromIdx = 0) {
  const deadline = Date.now() + timeoutMs;
  let i = fromIdx;
  while (Date.now() < deadline) {
    while (i < botChatLines.length) {
      const L = botChatLines[i];
      const match = L.msg.match(re);
      if (match) return { idx: i, ...L, match };
      i++;
    }
    await sleep(100);
  }
  return null;
}

function makeBot(name) {
  return new Promise((resolve, reject) => {
    const state = { name, bot: null, connected: false, kicks: [], errors: [] };
    bots[name] = state;
    let settled = false;
    const timer = setTimeout(() => {
      if (!settled) { settled = true; reject(new Error(`${name} spawn timeout`)); }
    }, 30000);

    const bot = mineflayer.createBot({
      host: '127.0.0.1',
      port: PORT,
      username: name,
      version: '1.21.1',
      auth: 'offline',
      hideErrors: false
    });
    state.bot = bot;

    bot.once('spawn', () => {
      state.connected = true;
      if (!settled) { settled = true; clearTimeout(timer); resolve(state); }
    });
    bot.on('messagestr', msg => {
      if (msg && msg.trim()) botChatLines.push({ t: Date.now(), bot: name, msg: msg.trim() });
    });
    bot.on('kicked', reason => {
      const txt = typeof reason === 'string' ? reason : JSON.stringify(reason);
      log(`[bot:${name}] KICKED: ${txt}`);
      state.kicks.push(txt);
    });
    bot.on('error', err => {
      log(`[bot:${name}] ERROR: ${err.message}`);
      state.errors.push(err.message);
      if (!settled) { settled = true; clearTimeout(timer); reject(err); }
    });
    bot.on('end', reason => {
      state.connected = false;
      if (!shuttingDown) log(`[bot:${name}] ended: ${reason}`);
    });
  });
}

async function assignTwoTeams() {
  // Put Bot1 & Bot2 on team slot 0, Bot3 & Bot4 on team slot 1 via /team GUI
  const names = ['Bot1', 'Bot2', 'Bot3', 'Bot4'];
  for (let idx = 0; idx < names.length; idx++) {
    const st = bots[names[idx]];
    if (!st?.connected || !st.bot) continue;
    const teamTargetIdx = idx < 2 ? 0 : 1;
    await new Promise(res => {
      const to = setTimeout(res, 2500);
      st.bot.once('windowOpen', async win => {
        clearTimeout(to);
        try {
          const woolSlots = [];
          for (let s = 0; s < Math.min(27, win.slots.length); s++) {
            const it = win.slots[s];
            if (it && it.name && it.name.includes('wool')) woolSlots.push(s);
          }
          if (woolSlots[teamTargetIdx] != null) {
            await st.bot.clickWindow(woolSlots[teamTargetIdx], 0, 0);
          }
        } catch (e) {
          log(`team GUI click failed for ${st.name}: ${e.message}`);
        }
        await sleep(250);
        res();
      });
      st.bot.chat('/team');
    });
  }
}

async function queryServerDims() {
  const since = serverLines.length;
  for (const name of Object.keys(bots)) {
    sendCmd(`data get entity ${name} Dimension`);
  }
  await sleep(1200);
  const dims = {};
  for (const name of Object.keys(bots)) {
    dims[name] = null;
    for (let i = since; i < serverLines.length; i++) {
      const m = serverLines[i].msg.match(new RegExp(`^${name} has the following entity data: "?([^"]+)"?$`));
      if (m) dims[name] = m[1];
    }
  }
  return dims;
}

function edgeAhead(bot) {
  if (!bot.entity) return false;
  const p = bot.entity.position, yaw = bot.entity.yaw;
  const dx = -Math.sin(yaw), dz = -Math.cos(yaw);
  for (const d of [0.8, 1.6]) {
    let floor = false;
    for (let dy = 1; dy <= 3; dy++) {
      const b = bot.blockAt(p.offset(dx * d, -dy, dz * d));
      if (b && b.boundingBox === 'block') { floor = true; break; }
    }
    if (!floor) return true;
  }
  return false;
}

async function roamBots(durationMs, opts = {}) {
  const endAt = Date.now() + durationMs;
  while (Date.now() < endAt) {
    for (const st of Object.values(bots)) {
      const bot = st.bot;
      if (!st.connected || !bot?.entity) continue;
      if (opts.rsrAction) {
        const rod = bot.inventory.items().find(i => i.name === 'blaze_rod');
        if (rod) {
          const hotbarIdx = rod.slot - 36;
          if (hotbarIdx >= 0 && hotbarIdx <= 8) bot.setQuickBarSlot(hotbarIdx);
          if (Math.random() < 0.45) {
            try { bot.activateItem(); } catch {}
          }
        }
      }
      if (edgeAhead(bot)) {
        bot.setControlState('forward', false);
        bot.look(bot.entity.yaw + Math.PI + rnd(-0.4, 0.4), rnd(-0.2, 0.2), true).catch(() => {});
      } else {
        bot.setControlState('forward', true);
        if (Math.random() < 0.25) bot.setControlState('jump', true);
        else bot.setControlState('jump', false);
        if (Math.random() < 0.35) bot.look(bot.entity.yaw + rnd(-0.8, 0.8), rnd(-0.3, 0.3), false).catch(() => {});
      }
      if (Math.random() < 0.3) bot.swingArm();
    }
    await sleep(350);
  }
  for (const st of Object.values(bots)) {
    const bot = st.bot;
    if (!bot) continue;
    for (const c of ['forward', 'back', 'left', 'right', 'sprint', 'jump']) {
      try { bot.setControlState(c, false); } catch {}
    }
  }
}

async function inspectBots(requireSolidUnder = true) {
  // Wait briefly for chunks around newly teleported bots
  await sleep(1200);
  const report = {};
  for (const [name, st] of Object.entries(bots)) {
    const bot = st.bot;
    if (!st.connected || !bot?.entity) {
      report[name] = { ok: false, reason: 'disconnected/no entity' };
      continue;
    }
    const p = bot.entity.position;
    let below = null, feet = null, head = null;
    for (let tries = 0; tries < 6; tries++) {
      below = bot.blockAt(p.offset(0, -0.3, 0));
      if (!below || below.name === 'air') below = bot.blockAt(p.offset(0, -1.0, 0));
      // Sample waist height (0.55) so half-slabs/stairs/cake at feet.y (.5) are not falsely flagged as suffocation
      feet = bot.blockAt(p.offset(0, 0.55, 0));
      head = bot.blockAt(p.offset(0, 1.5, 0));
      if (below && feet && head) break;
      await sleep(300);
    }
    const feetSolid = feet && feet.boundingBox === 'block';
    const headSolid = head && head.boundingBox === 'block';
    const belowHazard = below && (below.name === 'lava' || below.name === 'flowing_lava');
    const belowSolid = below && below.boundingBox === 'block';
    const items = bot.inventory.items().map(i => i.name);
    const chestSlot = bot.inventory.slots[6]?.name || null;
    const alive = bot.health == null || bot.health > 0;
    const yOk = p.y > -50 && p.y < 320;
    const ok = st.connected && alive && yOk && !feetSolid && !headSolid && !belowHazard && (!requireSolidUnder || belowSolid);
    report[name] = {
      ok,
      pos: `${p.x.toFixed(1)},${p.y.toFixed(1)},${p.z.toFixed(1)}`,
      hp: bot.health,
      below: below?.name ?? 'null',
      feet: feet?.name ?? 'null',
      head: head?.name ?? 'null',
      chest: chestSlot,
      items: [...new Set(items)]
    };
  }
  return report;
}

async function waitBotsInDim(expectedDim, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const dims = await queryServerDims();
    if (Object.values(dims).every(d => d === expectedDim)) return dims;
    await sleep(600);
  }
  return await queryServerDims();
}

function kickBaseline() {
  const b = {};
  for (const [n, s] of Object.entries(bots)) b[n] = s.kicks.length;
  return b;
}

function newKicksSince(base) {
  const out = [];
  for (const [n, s] of Object.entries(bots)) {
    if (s.kicks.length > (base[n] || 0)) out.push(`${n}:${s.kicks.slice(base[n] || 0).join(';')}`);
  }
  return out;
}

async function stopGameAndWaitLobby(fromIdx) {
  sendCmd('mcm stop');
  await waitLine(/=== .+ OVER!|Returned to lobby/i, 12000, fromIdx);
  await sleep(1800);
}

async function testSgMap(mapId) {
  const fromIdx = serverLines.length;
  const kicks0 = kickBaseline();
  sendCmd(`mcm map hungergames ${mapId}`);
  const mapAck = await waitLine(new RegExp(`Next map of Hunger Games.*${mapId}`, 'i'), 5000, fromIdx);
  sendCmd('mcm play hungergames');
  const started = await waitLine(/\[MCM\] >>> Hunger Games/, 10000, fromIdx);
  const prep = await waitLine(new RegExp(`Prepared imported SG map '${mapId}': (\\d+) chests, (\\d+) pedestals`), 45000, fromIdx);
  const expectedDim = `minecraft:mcc_hg_${mapId}`;
  const dims = await waitBotsInDim(expectedDim, 15000);
  await sleep(1500);
  sendCmd('mcm skipwait');
  const env = await inspectBots(true);
  await roamBots(2500);
  const envAfter = await inspectBots(false);
  await stopGameAndWaitLobby(fromIdx);
  // Give Windows world unload + async delete a brief settle window before next SG map
  await sleep(1500);

  const issues = extractIssues(fromIdx);
  const kicks = newKicksSince(kicks0);
  const chests = prep ? Number(prep.match[1]) : 0;
  const pedestals = prep ? Number(prep.match[2]) : 0;
  const allInDim = Object.values(dims).every(d => d === expectedDim);
  const uniqueSpawns = new Set(Object.values(env).map(e => e.pos)).size;
  const allBotsOk = Object.values(env).every(e => e.ok) && Object.values(envAfter).every(e => e.ok) && uniqueSpawns === 4;
  const pass = !!mapAck && !!started && !!prep && chests > 0 && pedestals >= 4 && allInDim && allBotsOk && issues.length === 0 && kicks.length === 0;
  return {
    game: 'hungergames', map: mapId, pass, chests, pedestals, uniqueSpawns, allInDim, allBotsOk,
    dims, env, envAfter, issues, kicks,
    note: !prep ? 'missing Prepared log' : !allInDim ? `dims=${JSON.stringify(dims)}` : ''
  };
}

async function testBattleBoxMap(mapId) {
  const fromIdx = serverLines.length;
  const chatIdx = botChatLines.length;
  const kicks0 = kickBaseline();
  sendCmd(`mcm map battlebox ${mapId}`);
  const mapAck = await waitLine(new RegExp(`Next map of Battle Box.*${mapId}`, 'i'), 5000, fromIdx);
  sendCmd('mcm play battlebox');
  const started = await waitLine(/\[MCM\] >>> Battle Box/, 10000, fromIdx);
  const dims = await waitBotsInDim('minecraft:mcc_battlebox', 20000);
  await sleep(1500);
  // Skip INTRO wait so we transition into arena spawn (KIT) once buildDone
  sendCmd('mcm skipwait');
  const kitChat = await waitChat(/Pick a kit|Round \d+\//i, 12000, chatIdx);
  await sleep(800);
  // Pick a kit slot or close open kit menu cleanly
  for (const st of Object.values(bots)) {
    const bot = st.bot;
    if (bot?.currentWindow) {
      try {
        await bot.clickWindow(11, 0, 0);
      } catch {
        try { bot.closeWindow(bot.currentWindow); } catch {}
      }
    }
  }
  await sleep(300);
  // Skip KIT wait to open gates into FIGHT
  sendCmd('mcm skipwait');
  await sleep(800);
  const env = await inspectBots(true);
  await roamBots(2500);
  const envAfter = await inspectBots(true);
  await stopGameAndWaitLobby(fromIdx);

  const issues = extractIssues(fromIdx);
  const kicks = newKicksSince(kicks0);
  const allInDim = Object.values(dims).every(d => d === 'minecraft:mcc_battlebox');
  const allBotsOk = Object.values(env).every(e => e.ok) && Object.values(envAfter).every(e => e.ok);
  const pass = !!mapAck && !!started && !!kitChat && allInDim && allBotsOk && issues.length === 0 && kicks.length === 0;
  return { game: 'battlebox', map: mapId, pass, kitChat: !!kitChat, allInDim, allBotsOk, dims, env, envAfter, issues, kicks };
}

async function testRocketSpleefMap(mapId) {
  const fromIdx = serverLines.length;
  const kicks0 = kickBaseline();
  sendCmd(`mcm map rocketspleef ${mapId}`);
  const mapAck = await waitLine(new RegExp(`Next map of Rocket Spleef.*${mapId}`, 'i'), 5000, fromIdx);
  sendCmd('mcm play rocketspleef');
  const started = await waitLine(/\[MCM\] >>> Rocket Spleef/, 10000, fromIdx);
  const dims = await waitBotsInDim('minecraft:mcc_rocketspleef', 20000);
  const envStart = await inspectBots(true);
  await sleep(1500);
  sendCmd('mcm skipwait');
  await sleep(600);
  await roamBots(2600, { rsrAction: true });
  const envAfter = await inspectBots(false);
  await stopGameAndWaitLobby(fromIdx);

  const issues = extractIssues(fromIdx);
  const kicks = newKicksSince(kicks0);
  const allInDim = Object.values(dims).every(d => d === 'minecraft:mcc_rocketspleef');
  const gearOk = Object.values(envStart).every(e =>
    e.ok && e.chest === 'elytra' && e.items.includes('blaze_rod') && e.items.includes('feather'));
  const aliveAfter = Object.values(envAfter).every(e => e.hp == null || e.hp > 0);
  const pass = !!mapAck && !!started && allInDim && gearOk && aliveAfter && issues.length === 0 && kicks.length === 0;
  return { game: 'rocketspleef', map: mapId, pass, allInDim, gearOk, aliveAfter, dims, envStart, envAfter, issues, kicks };
}

async function testBingo() {
  const fromIdx = serverLines.length;
  const kicks0 = kickBaseline();
  sendCmd('mcm play bingo');
  const started = await waitLine(/\[MCM\] >>> Bingo/, 15000, fromIdx);
  const cardLog = await waitLine(/Bingo card env/i, 20000, fromIdx);
  const dims = await waitBotsInDim('minecraft:mcc_bingo', 25000);
  await sleep(1500);
  sendCmd('mcm skipwait');
  const env = await inspectBots(false);
  await roamBots(2500);
  const envAfter = await inspectBots(false);
  await stopGameAndWaitLobby(fromIdx);

  const issues = extractIssues(fromIdx);
  const kicks = newKicksSince(kicks0);
  const allInDim = Object.values(dims).every(d => d === 'minecraft:mcc_bingo');
  const cardsOk = Object.values(env).every(e =>
    (e.items.includes('knowledge_book') || e.items.includes('filled_map')) && (e.hp == null || e.hp > 0));
  const pass = !!started && !!cardLog && allInDim && cardsOk && issues.length === 0 && kicks.length === 0;
  return { game: 'bingo', map: 'procedural', pass, cardLog: cardLog?.msg || null, allInDim, cardsOk, dims, env, envAfter, issues, kicks };
}

async function main() {
  log(`Starting Paper 1.21.1 in ${SERVER_DIR}`);
  srv = spawn(JAVA_BIN, ['-Xms1G', '-Xmx4G', '-jar', 'paper.jar', 'nogui'], {
    cwd: SERVER_DIR,
    stdio: ['pipe', 'pipe', 'pipe']
  });
  srv.stdout.on('data', chunk => {
    stdoutBuf += chunk.toString('utf8');
    const parts = stdoutBuf.split('\n');
    stdoutBuf = parts.pop();
    for (const p of parts) parseLogLine(p);
  });
  srv.stderr.on('data', chunk => {
    for (const l of chunk.toString('utf8').split('\n')) {
      if (l.trim()) parseLogLine('[STDERR] ' + l);
    }
  });

  const done = await waitLine(/Done \([^)]+\)! For help/, 180000, 0);
  if (!done) throw new Error('Server failed to finish booting within 180s');
  log('Paper booted cleanly!');

  sendCmd('mcm fast on');
  await sleep(1000);

  // Verify loaded maps reported via /mcm map commands
  const mapQueryIdx = serverLines.length;
  sendCmd('mcm map hungergames');
  sendCmd('mcm map battlebox');
  sendCmd('mcm map rocketspleef');
  await waitLine(/Rocket Spleef Rush maps:/i, 6000, mapQueryIdx);
  await sleep(400);
  const mapListLines = serverLines.slice(mapQueryIdx).map(l => l.msg).filter(m => /maps:/i.test(m));

  // Connect 4 bots cleanly
  for (let i = 1; i <= 4; i++) {
    await makeBot('Bot' + i);
    await sleep(400);
  }
  await sleep(1500);
  await assignTwoTeams();
  log('All 4 bots connected & assigned teams!');

  const sgResults = [];
  for (const m of SG_MAPS) {
    log(`--- Testing SG: ${m} ---`);
    sgResults.push(await testSgMap(m));
  }

  const bbResults = [];
  for (const m of BB_MAPS) {
    log(`--- Testing Battle Box: ${m} ---`);
    bbResults.push(await testBattleBoxMap(m));
  }

  const rsrResults = [];
  for (const m of RSR_MAPS) {
    log(`--- Testing Rocket Spleef: ${m} ---`);
    rsrResults.push(await testRocketSpleefMap(m));
  }

  log('--- Testing Bingo ---');
  const bingoResult = await testBingo();

  shuttingDown = true;
  for (const st of Object.values(bots)) {
    try { st.bot?.quit(); } catch {}
  }
  sendCmd('stop');
  await new Promise(res => {
    const to = setTimeout(() => { try { srv.kill('SIGKILL'); } catch {} res(); }, 15000);
    srv.once('exit', () => { clearTimeout(to); res(); });
  });

  const all = [...sgResults, ...bbResults, ...rsrResults, bingoResult];
  const passed = all.filter(r => r.pass).length;
  const linesOut = [
    '# Imported Maps Live Bot Verification Results',
    '',
    `- **Date**: ${new Date().toISOString()}`,
    `- **Summary**: ${passed} / ${all.length} scenarios PASSED`,
    '',
    '## Registered Maps Check',
    '```',
    ...mapListLines.slice(0, 20),
    '```',
    '',
    '## Results Table',
    '| Game | Map | Pass | Key Checks | Issues |',
    '|---|---|---|---|---|'
  ];

  for (const r of all) {
    let summary = '';
    if (r.game === 'hungergames') summary = `dim=${r.allInDim}, chests=${r.chests}, pedestals=${r.pedestals}, spawns=${r.uniqueSpawns}/4, footing=${r.allBotsOk}`;
    else if (r.game === 'battlebox') summary = `dim=${r.allInDim}, footing=${r.allBotsOk}`;
    else if (r.game === 'rocketspleef') summary = `dim=${r.allInDim}, gearOk=${r.gearOk}, alive=${r.aliveAfter}`;
    else summary = `dim=${r.allInDim}, cardsOk=${r.cardsOk}`;
    const errTxt = [...r.kicks, ...r.issues.map(i => `${i.lvl}:${i.head}`)].join(' | ') || 'none';
    linesOut.push(`| ${r.game} | ${r.map} | ${r.pass ? 'PASS' : 'FAIL'} | ${summary} | ${errTxt.slice(0, 120)} |`);
  }

  const failures = all.filter(r => !r.pass);
  if (failures.length) {
    linesOut.push('', '## Failure Details');
    for (const f of failures) {
      linesOut.push(`### ${f.game}:${f.map}`, '```json', JSON.stringify(f, null, 2), '```');
    }
  }

  fs.writeFileSync(RESULTS_MD, linesOut.join('\n') + '\n', 'utf8');
  log(`Results written to ${RESULTS_MD} (${passed}/${all.length} passed)`);
  process.exit(failures.length ? 1 : 0);
}

main().catch(err => {
  console.error(err);
  shuttingDown = true;
  try { srv?.stdin?.write('stop\n'); } catch {}
  setTimeout(() => { try { srv?.kill('SIGKILL'); } catch {} process.exit(2); }, 5000);
});
