// Optional convenience: checks which plugins of this repository are installed on a Joget server.
// The console shows the installed jars in Settings > Manage Plugins; this script lists the plugin classes in them.
//
//   npm install                         (once, in the repository root)
//   node 00-setup/check-install.js
//
// It logs in as an administrator, reads the plugin list of the server, and prints one line per
// expected plugin. Exit code 0 = Agent Builder and every sample plugin are installed; 1 = something is missing.
//
// Environment variables (all optional): JW_BASE, JW_USER, JW_PASS, CHROME_PATH. See scripts/joget-browser.js.
const { BASE, launch, login } = require('../scripts/joget-browser');

const EXPECTED = [
  ['Agent Builder', 'org.joget.ai.agent.AgentBuilder'],
  ['Agent Builder', 'org.joget.ai.config.CentralConfig'],
  ['Composer MCP (optional, Joget DX 9.1 or later)', 'org.joget.ai.mcp.ComposerMcpFilter'],
  ['01-agent-builder-tools', 'org.joget.samples.agenttools.IndonesianIdValidatorTool'],
  ['01-agent-builder-tools', 'org.joget.samples.agenttools.NikPiiGuard'],
  ['01-agent-builder-tools', 'org.joget.samples.agenttools.NpwpFormatEnhancer'],
  ['02-system-agent', 'org.joget.samples.systemagent.NamingAuditorPage'],
  ['02-system-agent', 'org.joget.samples.systemagent.NamingCheckTool'],
  ['03-composer-custom-tool', 'org.joget.samples.composertool.AcmeStandardsTool'],
  ['06-custom-llm-provider', 'org.joget.samples.llm.GatewayProvider'],
  ['06-custom-llm-provider', 'org.joget.samples.llm.GatewayAgentLLM'],
  ['07-custom-composer', 'org.joget.samples.checklist.ChecklistComposerChat'],
  ['07-custom-composer', 'org.joget.samples.checklist.ChecklistComposerAgentTool']
];

(async () => {
  const browser = await launch();
  let missing = 0;
  try {
    const page = await browser.newPage();
    await login(page);
    await page.goto(`${BASE}/web/console/setting/plugin`, { waitUntil: 'networkidle2', timeout: 180000 });
    const result = await page.evaluate(async (url) => {
      const r = await fetch(url, { credentials: 'include' });
      if (r.status !== 200) return { status: r.status };
      const j = await r.json();
      return { status: 200, rows: (j.data || []).map(x => ({ id: x.id, name: x.name, type: x.plugintype, version: x.version })) };
    }, `${BASE}/web/json/plugin/list?start=0&rows=10000`);
    if (result.status !== 200) throw new Error('The plugin list answered HTTP ' + result.status + '. Is the user an administrator?');

    console.log(`Joget at ${BASE}: ${result.rows.length} plugins`);
    for (const [group, className] of EXPECTED) {
      const row = result.rows.find(r => r.id === className);
      const optional = group.indexOf('optional') >= 0;
      if (row) {
        console.log(`FOUND    ${group}: ${row.name} [${row.type}] version ${row.version}`);
      } else {
        console.log(`${optional ? 'ABSENT ' : 'MISSING'}  ${group}: ${className}`);
        if (!optional) missing++;
      }
    }
  } finally {
    await browser.close();
  }
  process.exit(missing ? 1 : 0);
})().catch(e => { console.error(e.message); process.exit(1); });
