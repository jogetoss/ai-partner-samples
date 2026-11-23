// Optional convenience: opens Settings > Form Naming Auditor in a headless browser, runs the audit for one app and
// saves the report and two screenshots. The same result comes from clicking "Run audit" on the page.
//
//   npm install                                   (once, in the repository root)
//   node trigger-audit.js <appId> [out-dir] [en|id]
//   node trigger-audit.js crm out
//
// The run calls your AI provider, so it uses tokens.
// Environment variables (all optional): JW_BASE, JW_USER, JW_PASS, CHROME_PATH. See scripts/joget-browser.js.
const fs = require('fs');
const path = require('path');
const { BASE, launch, login } = require('../scripts/joget-browser');

const [app, out = 'out', lang = 'en'] = process.argv.slice(2);
if (!app) {
  console.error('usage: node trigger-audit.js <appId> [out-dir] [en|id]');
  process.exit(2);
}

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const browser = await launch();
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1400, height: 900 });
    await login(page);
    await page.goto(`${BASE}/web/console/plugin/FormNamingAuditor`, { waitUntil: 'networkidle2', timeout: 120000 });
    if (!await page.$('#fna-app')) throw new Error('The Form Naming Auditor page was not found. Is the plugin installed, and is the user an administrator?');
    await page.type('#fna-app', app);
    await page.select('#fna-lang', lang === 'id' ? 'id' : 'en');
    await page.screenshot({ path: path.join(out, 'page-1-before.png') });
    await page.click('#fna-run');
    await page.waitForFunction(
      () => /Done|Error|did not return|failed/.test(document.getElementById('fna-status').textContent),
      { timeout: 300000 });
    console.log(await page.$eval('#fna-status', e => e.textContent));
    const report = await page.$eval('#fna-out', e => e.textContent).catch(() => '');
    fs.writeFileSync(path.join(out, 'report.md'), report);
    console.log(report.slice(0, 1500));
    await page.screenshot({ path: path.join(out, 'page-2-report.png'), fullPage: true });
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e.message); process.exit(1); });
