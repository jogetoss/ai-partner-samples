// Uploads a plugin jar through the Joget console, exactly like Settings > Manage Plugins > Upload.
// Optional convenience: the manual upload in the console gives the same result.
//
//   npm install                              (once, in the repository root)
//   node 00-setup/deploy-upload.js <jar>
//
// Environment variables (all optional): JW_BASE, JW_USER, JW_PASS, CHROME_PATH. See scripts/joget-browser.js.
const path = require('path');
const fs = require('fs');
const { BASE, launch, login } = require('../scripts/joget-browser');

const jar = process.argv[2] ? path.resolve(process.argv[2]) : null;
if (!jar || !fs.existsSync(jar)) {
  console.error('usage: node deploy-upload.js <jar>   (the jar file was not given or does not exist)');
  process.exit(2);
}

(async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage();
    await login(page);
    await page.goto(`${BASE}/web/console/setting/plugin/upload`, { waitUntil: 'networkidle2', timeout: 120000 });
    const input = await page.$('input[type=file]');
    if (!input) throw new Error('The upload form was not found. Is the user an administrator? Page: ' + page.url());
    await input.uploadFile(jar);
    const submit = await page.$('input[type=submit], button[type=submit], .form-button');
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle2', timeout: 300000 }).catch(() => {}),
      submit.click()
    ]);
    await new Promise(r => setTimeout(r, 5000));
    console.log('Uploaded', path.basename(jar), 'to', BASE);
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e.message); process.exit(1); });
