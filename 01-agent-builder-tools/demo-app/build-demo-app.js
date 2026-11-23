// Generates acme_tools.jwa: a tiny Joget app with three Agent Builder agents that use the sample tool, guard and
// enhancer. The generated .jwa is delivered next to this file, so you only need this script to change the agents.
//
//   npm install          (once, in the repository root; this script needs adm-zip)
//   node build-demo-app.js
//
// The agents use an Anthropic model through the AI credential named below. No API key is stored in the file.
// To use another credential name or model, set the environment variables and run the script again:
//   AI_SERVICE   name of an Anthropic credential in Settings > AI Central Config > AI Credentials   default "Anthropic"
//   AI_MODEL     model id                                                                           default "claude-sonnet-4-6"
// Or import the delivered .jwa and choose your own LLM service in each agent in the builder.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
let AdmZip;
try {
  AdmZip = require('adm-zip');
} catch (e) {
  console.error('adm-zip is not installed. Run "npm install" in the repository root first.');
  process.exit(2);
}

const APP = 'acme_tools';
const AI_SERVICE = process.env.AI_SERVICE || 'Anthropic';   // name of the credential in AI Central Config
const MODEL = process.env.AI_MODEL || 'claude-sonnet-4-6';
const TOOL = 'org.joget.samples.agenttools.IndonesianIdValidatorTool';
const GUARD = 'org.joget.samples.agenttools.NikPiiGuard';
const ENHANCER = 'org.joget.samples.agenttools.NpwpFormatEnhancer';

const uid = () => crypto.randomBytes(16).toString('hex').toUpperCase();
const el = (className, props) => ({ className, properties: Object.assign({ id: uid(), isEnabled: 'true', personalizedName: '' }, props) });
const text = (prompt, role = 'USER') => el('org.joget.ai.agent.lib.TextPromptAgentPrompt', { msgRole: role, prompt });
const llm = () => [{ className: 'org.joget.ai.agent.lib.AnthropicAgentLLM', properties: { id: uid(), provider: 'Anthropic',
  aiConfigName: AI_SERVICE, apiKey: '', apiUrl: '', model: MODEL, anthropicVersion: '2023-06-01', maxTokens: '2048',
  temperature: '0.2', topK: '', topP: '', thinkingBudgetTokens: '', reasoningEffort: '' } }];

function agent(id, name, description, prompts, { tools = [], guards = [], enhancers = [] } = {}) {
  return {
    className: 'org.joget.ai.agent.AgentBuilder',
    properties: { id: 'AI-' + id, name, description, enableTokenStreaming: '', agentDebugMode: '',
      inputs: [{ name: 'query', label: 'Message', type: 'textfield', description: 'The user message' }], variables: [],
      maxIterations: '10', maxToolCallDepth: '10', parallelToolCalls: '', parallelToolCallThreads: '4',
      llmRetryMaxAttempts: '2', llmRetryInitialDelayMs: '1000', llmRetryMaxDelayMs: '10000',
      llmConnectTimeoutSeconds: '30', llmSocketTimeoutSeconds: '120', contextWindowEnabled: 'true',
      contextWindowStrategy: 'TRUNCATE', contextWindowMaxMessages: '0', contextWindowMaxChars: '400000',
      contextWindowWarnThresholdPct: '80', contextWindowPreserveSystem: 'true', contextWindowPreserveFirst: 'true', logo: '' },
    agent: { llm: llm(), prompt: [] },
    tasks: [{ properties: { id: uid().toLowerCase(), label: 'Answer' }, prompt: prompts, tool: tools, enhancer: enhancers, decision: [], guard: guards }]
  };
}

const SYSTEM = 'You are an HR and finance helper at an Indonesian company. Answer in Bahasa Indonesia, briefly. ' +
  'When the user gives a NIK or an NPWP, ALWAYS call validate_indonesian_id and base your answer only on its result. ' +
  'Quote the number exactly as the tool returned it.';
const USER = 'Pesan pengguna: ${query}';

const toolProps = { idTypes: 'BOTH', mode: 'strict', returnFullNumber: 'true',
  extraProvinces: [{ code: '99', name: 'Provinsi Contoh (demo)' }] };

const agents = [
  // 1. tool + enhancer + output guard. Returning the full number on purpose, so the guard has something to mask.
  agent('acme_id_check', 'Acme 1 ID Check (tool + guard + enhancer)',
    'Validates NIK/NPWP with the custom tool; the guard masks any full NIK in the answer; the enhancer formats NPWP and adds a Bahasa notice.',
    [text(SYSTEM, 'SYSTEM'), text(USER)],
    { tools: [el(TOOL, toolProps)],
      guards: [el(GUARD, { direction: 'output', onViolation: 'sanitize', redirectTaskId: '' })],
      enhancers: [el(ENHANCER, { notice: 'Catatan: pengecekan hanya memeriksa format, bukan data resmi DJP atau Dukcapil.' })] }),
  // 2. same tool, no guard, no enhancer: shows the difference.
  agent('acme_id_check_plain', 'Acme 2 ID Check (tool only)',
    'Same tool without guard and enhancer, to show what the answer looks like without them.',
    [text(SYSTEM, 'SYSTEM'), text(USER)], { tools: [el(TOOL, toolProps)] }),
  // 3. input guard in block mode on a plain chat agent.
  agent('acme_pii_input_block', 'Acme 3 PII Input Block (guard only)',
    'The guard blocks the task when the request contains a full NIK.',
    [text('You are a helpful assistant. Answer in Bahasa Indonesia, briefly.', 'SYSTEM'), text(USER)],
    { guards: [el(GUARD, { direction: 'input', onViolation: 'block', redirectTaskId: '' })] })
];

const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
const ts = '2026-11-05 21:00:00.0 IST';
const dates = `<dateCreated class="java.sql.Timestamp">${ts}</dateCreated>\n         <dateModified class="java.sql.Timestamp">${ts}</dateModified>`;
const defs = agents.map(a => `      <builderDefinition>
         <id>${a.properties.id}</id>
         <appId>${APP}</appId>
         <appVersion>1</appVersion>
         <name>${esc(a.properties.name)}</name>
         <type>agent</type>
         <json>${esc(JSON.stringify(a))}</json>
         <description>${esc(a.properties.description)}</description>
         ${dates}
      </builderDefinition>`).join('\n');

// The <!-- ... --> around builderDefinitionList is how Agent Builder apps are exported by Joget: the importer reads the
// builderDefinition elements inside it.
const xml = `<appDefinition>
   <appId>${APP}</appId>
   <id>${APP}</id>
   <version>1</version>
   <name>Acme Tools Demo</name>
   <dateCreated class="java.sql.Timestamp">${ts}</dateCreated>
   <dateModified class="java.sql.Timestamp">${ts}</dateModified>
   <packageDefinitionList/>
  <formDefinitionList/>
   <userviewDefinitionList/>
   <datalistDefinitionList/>
   <!--builderDefinitionList>
${defs}
   </builderDefinitionList-->
   <pluginDefaultPropertiesList/>
   <environmentVariableList/>
   <messageList/>
   <createdBy>admin</createdBy>
</appDefinition>
`;
const zip = new AdmZip();
zip.addFile('appDefinition.xml', Buffer.from(xml, 'utf8'));
const out = path.join(__dirname, APP + '.jwa');
zip.writeZip(out);
console.log('wrote', out, 'agents:', agents.length);

// Also write each agent definition as plain JSON: easy to read, and used by run-agent.js.
const dir = path.join(__dirname, 'agents');
fs.mkdirSync(dir, { recursive: true });
agents.forEach(a => fs.writeFileSync(path.join(dir, a.properties.id.replace('AI-', '') + '.json'), JSON.stringify(a, null, 2)));
