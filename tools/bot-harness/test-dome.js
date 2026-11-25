'use strict';
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { execFileSync } = require('child_process');
const path = require('path');

const RCON = path.resolve(__dirname, '../../tools/rcon.py');
const HOST = '127.0.0.1';
const PORT = 25580;
const VERSION = '1.21.1';
const NAMES = ['DomeBot1', 'DomeBot2', 'DomeBot3', 'DomeBot4'];
const DIAL = new Vec3(0.5, 65.0, -61.5);

const sleep = ms => new Promise(r => setTimeout(r, ms));
function rcon(cmd) {
  try {
    const out = execFileSync('python', [RCON, cmd], { encoding: 'utf8' });
    console.log('RCON>', cmd, '=>', out.trim().split('\n').slice(-2).join(' | '));
    return out;
  } catch (e) {
    console.log('RCON FAILED>', cmd, e.message);
    return '';
  }
}

async function connect(name) {
  return new Promise((resolve, reject) => {
    const bot = mineflayer.createBot({ host: HOST, port: PORT, username: name, auth: 'offline', version: VERSION });
    const timer = setTimeout(() => reject(new Error(name + ' join timeout')), 30000);
    bot.once('spawn', () => {
      clearTimeout(timer);
      console.log(name, 'spawned at', bot.entity.position);
      bot.on('message', msg => console.log(name, 'CHAT', msg.toString()));
      resolve(bot);
    });
    bot.on('kicked', reason => console.log(name, 'KICKED', reason));
    bot.on('error', err => console.log(name, 'ERROR', err.message));
  });
}

async function throwEgg(bot) {
  const egg = bot.inventory.items().find(i => i.name === 'egg' || i.name === 'minecraft:egg');
  if (!egg) {
    console.log(bot.username, 'has no egg; inventory=', bot.inventory.items().map(i => i.name + 'x' + i.count).join(','));
    return false;
  }
  const quick = (egg.slot >= 36 && egg.slot <= 44) ? egg.slot - 36 : (egg.slot <= 8 ? egg.slot : 0);
  bot.setQuickBarSlot(quick);
  await sleep(300);
  bot.lookAt(DIAL, true);
  await sleep(250);
  console.log(bot.username, 'mode=' + bot.game.gameMode, 'quick=' + bot.quickBarSlot, 'holding', bot.heldItem && bot.heldItem.name, 'at', bot.entity.position);
  bot.activateItem();
  console.log(bot.username, 'sent activateItem');
  return true;
}

async function main() {
  const bots = [];
  try {
    for (const name of NAMES) bots.push(await connect(name));
    await sleep(2500);
    for (const name of NAMES) rcon('op ' + name);
    rcon('mcm fast off');
    rcon('mcm play decisiondome');
    await sleep(6500);
    for (const bot of bots) {
      await throwEgg(bot);
      await sleep(350);
    }
    await sleep(2500);
    console.log('STATUS AFTER THROW:\n' + rcon('dome status'));
    rcon('dome end');
    console.log('Ended voting; waiting for settle/tally/reveal...');
    await sleep(23000);
    console.log('STATUS AFTER REVEAL:\n' + rcon('dome status'));
    rcon('dome cancel');
    rcon('mcm stop');
    await sleep(2000);
  } finally {
    for (const bot of bots) {
      try { bot.quit(); } catch (_) {}
    }
    await sleep(1000);
    process.exit(0);
  }
}

main().catch(e => { console.error('TEST FAILED', e); process.exit(1); });
