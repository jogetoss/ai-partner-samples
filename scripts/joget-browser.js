// Shared helper for the optional Node scripts in this repository: starts a headless Chrome and logs in to Joget.
//
// Why a browser: the Joget console protects its forms with a CSRF token, so a plain HTTP POST to the login
// address is rejected. The scripts therefore log in through the real login form, like a person does.
//
// Setup (once, in the repository root):   npm install
//
// Environment variables (all optional):
//   JW_BASE      Joget address with context path     default http://localhost:8080/jw
//   JW_USER      a Joget administrator               default admin
//   JW_PASS      the password of that user           default admin
//   CHROME_PATH  full path of Chrome, Chromium or Edge; found automatically in the usual places when not set
const fs = require('fs');

let puppeteer;
try {
  puppeteer = require('puppeteer-core');
} catch (e) {
  console.error('puppeteer-core is not installed. Run "npm install" in the repository root first.');
  process.exit(2);
}

const BASE = (process.env.JW_BASE || 'http://localhost:8080/jw').replace(/\/+$/, '');
const USER = process.env.JW_USER || 'admin';
const PASS = process.env.JW_PASS || 'admin';

function findChrome() {
  if (process.env.CHROME_PATH) return process.env.CHROME_PATH;
  const candidates = [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/usr/bin/google-chrome',
    '/usr/bin/google-chrome-stable',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser'
  ];
  const found = candidates.find(p => fs.existsSync(p));
  if (!found) {
    throw new Error('Chrome was not found. Set CHROME_PATH to the full path of Chrome, Chromium or Edge.');
  }
  return found;
}

/** Starts a headless browser. */
async function launch() {
  return puppeteer.launch({
    headless: 'new',
    executablePath: findChrome(),
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
    defaultViewport: { width: 1500, height: 950 }
  });
}

/** Logs in through the Joget login form. Throws when the login is refused. */
async function login(page) {
  await page.goto(`${BASE}/web/login`, { waitUntil: 'networkidle2', timeout: 120000 });
  if (await page.$('#loginForm input[name="j_username"]')) {
    await page.evaluate((u, p) => {
      const f = document.getElementById('loginForm');
      f.querySelector('input[name="j_username"]').value = u;
      f.querySelector('input[name="j_password"]').value = p;
    }, USER, PASS);
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle2', timeout: 120000 }),
      page.evaluate(() => HTMLFormElement.prototype.submit.call(document.getElementById('loginForm')))
    ]);
  }
  if (/login_error|\/web\/login/.test(page.url()) && await page.$('#loginForm input[name="j_username"]')) {
    throw new Error(`Login to ${BASE} as ${USER} failed. Check JW_BASE, JW_USER and JW_PASS.`);
  }
  // The landing page redirects once more with JavaScript. Wait until that has finished, otherwise it can
  // interrupt the first page the calling script opens.
  await new Promise(r => setTimeout(r, 3000));
  await page.waitForNetworkIdle({ idleTime: 1000, timeout: 60000 }).catch(() => {});
  return page.url();
}

module.exports = { puppeteer, BASE, USER, PASS, findChrome, launch, login };
