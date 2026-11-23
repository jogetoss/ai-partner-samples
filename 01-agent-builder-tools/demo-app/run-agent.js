// Optional convenience: runs one demo agent without opening the builder, through the same address that the
// Preview tab of the agent builder uses, and prints the events of the run.
//
//   npm install                                   (once, in the repository root)
//   node run-agent.js <agent> "<message>" [out-dir]
//   node run-agent.js acme_id_check "Tolong cek NIK 3174011501900001 dan NPWP 092542944407000"
//
// <agent> is a file name in the agents folder without .json: acme_id_check, acme_id_check_plain or
// acme_pii_input_block. With [out-dir] the raw event stream is also saved as <out-dir>/<agent>.sse.txt.
//
// Before you run it: import acme_tools.jwa, and make sure the agent files name an AI credential that exists
// (see the README). The run calls your AI provider, so it uses tokens.
//
// Environment variables (all optional): JW_BASE, JW_USER, JW_PASS, CHROME_PATH. See scripts/joget-browser.js.
//   AI_SERVICE   use this AI credential name instead of the one in the agent file (for this run only)
//   AI_MODEL     use this model id instead of the one in the agent file (for this run only)
const fs = require('fs');
const path = require('path');
const { BASE, launch, login } = require('../../scripts/joget-browser');

const [agentFile, message, outDir] = process.argv.slice(2);
if (!agentFile || !message) {
  console.error('usage: node run-agent.js <agent> "<message>" [out-dir]');
  process.exit(2);
}
let definition = fs.readFileSync(path.join(__dirname, 'agents', agentFile + '.json'), 'utf8');
// AI_SERVICE / AI_MODEL replace the credential name and the model of the agent file for this run only.
if (process.env.AI_SERVICE || process.env.AI_MODEL) {
  const d = JSON.parse(definition);
  d.agent.llm.forEach(l => {
    if (process.env.AI_SERVICE) l.properties.aiConfigName = process.env.AI_SERVICE;
    if (process.env.AI_MODEL) l.properties.model = process.env.AI_MODEL;
  });
  definition = JSON.stringify(d);
}

(async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage();
    await login(page);
    // Open the app first so the request has an app context, like the builder.
    await page.goto(`${BASE}/web/console/app/acme_tools/1/agents`, { waitUntil: 'networkidle2', timeout: 120000 });
    const sse = await page.evaluate(async (base, json, msg) => {
      const res = await fetch(`${base}/web/json/plugin/org.joget.ai.agent.AgentBuilder/service?action=runAgent`, {
        method: 'POST', credentials: 'include',
        headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
        body: JSON.stringify({ json, runId: 'RUN-' + crypto.randomUUID(), inputs: JSON.stringify({ query: msg }) })
      });
      return res.status + '\n' + await res.text();
    }, BASE, definition, message);
    const events = sse.split('\n').filter(l => l.startsWith('data:'))
      .map(l => { try { return JSON.parse(l.slice(5)); } catch (e) { return { raw: l }; } });

    console.log('HTTP', sse.split('\n')[0], 'events:', events.length);
    events.forEach(e => console.log(' -', e.type, e.status || '', (e.label || e.raw || '').toString().replace(/\s+/g, ' ').slice(0, 160)));
    if (outDir) {
      fs.mkdirSync(outDir, { recursive: true });
      fs.writeFileSync(path.join(outDir, `${agentFile}.sse.txt`), sse);
    }
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e.message); process.exit(1); });
