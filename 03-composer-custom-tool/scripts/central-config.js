// Optional convenience: creates a tool template or a skill in AI Central Config from a file, or lists them.
// The same result comes from clicking through Settings > AI Central Config (tabs "Tool Templates" and "Skills").
//
//   npm install                                           (once, in the repository root)
//   node central-config.js tool-template <template.json>
//   node central-config.js skill <SKILL.md>               (alternative to copying the folder into app_ai_skills)
//   node central-config.js list-tool-templates
//   node central-config.js list-skills
//
// Environment variables (all optional): JW_BASE, JW_USER, JW_PASS, CHROME_PATH. See scripts/joget-browser.js.
//
// The script calls the web service behind the AI Central Config page. That service is not a public API and can
// change between Agent Builder versions. It accepts administrators only and needs the console CSRF token, which
// is why the script works through a logged-in browser session.
const fs = require('fs');
const { BASE, launch, login } = require('../../scripts/joget-browser');

/** Reads a SKILL.md into the JSON that the Skills tab posts. Understands the simple frontmatter of a skill file. */
function parseSkill(text) {
  const m = text.replace(/^﻿/, '').match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n([\s\S]*)$/);
  if (!m) throw new Error('SKILL.md must start with a --- frontmatter block');
  const meta = { composers: [], tools: [] };
  let list = null;
  for (const line of m[1].split(/\r?\n/)) {
    const t = line.trim();
    if (!t) continue;
    if (list && t.startsWith('- ')) { list.push(t.slice(2).trim()); continue; }
    const i = t.indexOf(':');
    if (i <= 0) continue;
    const key = t.slice(0, i).trim();
    let rest = t.slice(i + 1).trim();
    if (key === 'composers' || key === 'tools') { list = meta[key]; if (rest && rest !== '[]') list.push(rest); continue; }
    list = null;
    if (rest.startsWith('"')) rest = JSON.parse(rest);
    meta[key] = rest;
  }
  return { name: meta.name, description: meta.description, status: meta.status || 'active',
    composers: meta.composers, tools: meta.tools, instructions: m[2].trim() };
}

async function call(page, action, data) {
  return page.evaluate(async (base, action, data) => {
    const body = new URLSearchParams({ _a: action, ...data });
    const tn = (window.ConnectionManager && ConnectionManager.tokenName) || '_csrf';
    const tv = (window.ConnectionManager && ConnectionManager.tokenValue) || '';
    body.append(tn, tv);
    const r = await fetch(base + '/web/json/plugin/org.joget.ai.config.CentralConfig/service', {
      method: 'POST', body, credentials: 'include',
      headers: { 'X-Requested-With': 'XMLHttpRequest', 'Content-Type': 'application/x-www-form-urlencoded' } });
    return { status: r.status, text: await r.text() };
  }, BASE, action, data);
}

(async () => {
  const [cmd, file] = process.argv.slice(2);
  const needsFile = cmd === 'tool-template' || cmd === 'skill';
  if (!cmd || (needsFile && !file)) {
    console.error('usage: node central-config.js tool-template <template.json> | skill <SKILL.md> | list-tool-templates | list-skills');
    process.exit(2);
  }
  const browser = await launch();
  try {
    const page = await browser.newPage();
    await login(page);
    // Any console page provides the CSRF token.
    await page.goto(`${BASE}/web/console/home`, { waitUntil: 'networkidle2', timeout: 120000 });

    let r;
    if (cmd === 'tool-template') {
      r = await call(page, 'createToolTemplate', { _toolTemplateData: fs.readFileSync(file, 'utf8') });
    } else if (cmd === 'skill') {
      r = await call(page, 'createSkill', { _skillData: JSON.stringify(parseSkill(fs.readFileSync(file, 'utf8'))) });
    } else if (cmd === 'list-tool-templates') {
      r = await call(page, 'loadToolTemplates', {});
    } else if (cmd === 'list-skills') {
      r = await call(page, 'loadSkills', {});
    } else { throw new Error('unknown command ' + cmd); }

    let out = r.text;
    try {
      const j = JSON.parse(r.text);
      if (cmd === 'list-tool-templates') out = JSON.stringify(j.aiToolTemplates.properties.aiToolTemplates.map(t => ({ name: t.name, toolClass: t.toolClass, status: t.status, composers: t.composers })), null, 1);
      else if (cmd === 'list-skills') out = JSON.stringify((j.aiSkills.properties ? j.aiSkills.properties.aiSkills : j.aiSkills).map(s => ({ name: s.name, status: s.status, composers: s.composers, tools: s.tools })), null, 1);
      else out = JSON.stringify({ success: j.success, error: j.error, errorCode: j.errorCode });
    } catch (e) { /* print the raw answer */ }
    console.log('HTTP ' + r.status + ' ' + out);
    process.exitCode = r.status === 200 ? 0 : 1;
  } finally {
    await browser.close();
  }
})().catch(e => { console.error('ERR ' + e.message); process.exit(1); });
