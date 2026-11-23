# Running a Session for a New Partner

This is the facilitator guide for a partner enablement session on Joget Agent Builder, the AI Composers and Composer MCP. The repository is generic: its samples use a fictional company, **Acme**, so you can reuse the same material for every partner. This page covers what to keep and what to swap, how to prepare, a full-day agenda with a script for each module, exercises, troubleshooting and follow-up.

## 1. Goals and audience

By the end of the session a partner developer or implementer should be able to:

1. Install Agent Builder and Composer MCP and connect an AI service.
2. Build and run an agent that uses a custom tool, guard and enhancer.
3. Teach the AI Composers the partner's own standards with skills and a custom tool.
4. Drive Joget from Claude Code through Composer MCP.
5. Know which extension point to use for a new need (tool, guard, enhancer, LLM provider, system agent, composer).

Audience: implementers and plugin developers who already know Joget DX. Group size of 3 to 8 works best. Plan for one facilitator and, for groups above 5, one helper.

## 2. What is generic and what is an example

| Part | Status | Action for a new partner |
|---|---|---|
| Manual pages 01 to 32 | Generic | Keep as is |
| `00-setup`, `02-system-agent`, `05-composer-mcp`, `06-custom-llm-provider`, `07-custom-composer` | Generic | Keep as is |
| `01-agent-builder-tools` | Worked example with Indonesian NIK and NPWP numbers | Keep, or swap the ID rules for the partner's market |
| `03-composer-custom-tool`, `04-custom-skills` | Example company standards for **Acme** (cost centres, naming rules, formats) | Replace with the partner's own standards, or keep Acme for a neutral demo |
| `01-agent-builder-tools/demo-app/acme_tools.jwa` | Generated demo app | Regenerate with `node build-demo-app.js` after any change |

## 3. Before the session (one week out)

1. **Version.** Confirm the partner's Joget version. Everything here needs Joget DX 9.1 or later and an Enterprise licence with Agent Builder.
2. **Scope.** Agree which samples to cover (see the agendas in section 4).
3. **Server.** Prepare a development Joget server for each participant, or one shared server with one app per participant. Install the Agent Builder plugin and set up an AI credential and AI service in **Settings > AI Central Config**. Test one agent run yourself.
4. **Machines.** Each developer needs JDK 17, Maven 3.8 or later, Git, and Node 18 or later for the optional scripts. Ask them to run `00-setup/check-install.js` before the day.
5. **Models and cost.** Agree who pays for model usage. A full day with 5 participants is a small amount, but set a spending limit on the credential.
6. **Build once yourself** (`mvn clean package "-Djoget.version=9.1.2"` in each sample folder) so you know the build works on the same network.
7. **Access.** Make sure the partner can reach the repository and the two plugin jars.

## 4. Agendas

### Full day (about 6 hours of content)

| Time | Module | Samples and manual pages | Format |
|---|---|---|---|
| 0:00 | Welcome, goals, how Agent Builder fits Joget | Manual 01, 04 | Talk, 20 min |
| 0:20 | Setup and first agent | `00-setup`, manual 02, 03, 05 | Hands-on, 40 min |
| 1:00 | Custom Tool, Guard and Enhancer | Sample 01, manual 24, 25 | Hands-on, 60 min |
| 2:00 | Break | | 15 min |
| 2:15 | System agent | Sample 02, manual 27 | Demo plus exercise, 40 min |
| 2:55 | AI Composers and the Orchestrator | Manual 12, 13, 14 | Demo, 30 min |
| 3:25 | Lunch | | 45 min |
| 4:10 | Teach the Composers your standards | Samples 03 and 04, manual 22, 28 | Hands-on, 60 min |
| 5:10 | Composer MCP with Claude Code | Sample 05, manual 17 to 21 | Hands-on, 45 min |
| 5:55 | Choosing an extension point, Q and A, next steps | Manual 23, 29, 31 | Discussion, 30 min |

### Half day (3 hours)

Setup and first agent (40 min), sample 01 (60 min), samples 03 and 04 (60 min), wrap-up (20 min).

### Composer-focused (2 hours)

Composers and Orchestrator demo (30 min), samples 03 and 04 (60 min), Composer MCP smoke test and one prompt (30 min).

## 5. Module notes

Each note gives the point to make, what to do on screen and how to know it worked.

### Setup and first agent
- **Point.** An agent is a set of blocks (prompt, LLM, tool, guard, enhancer) drawn on a canvas. Everything runs through one engine with tracing.
- **Do.** Upload the plugin, add the AI credential and service, import `acme_tools.jwa`, open **ID Check** and click **Preview**.
- **Works when.** The answer appears under **Execution Output** and the node viewer shows every node green.

### Sample 01: tool, guard, enhancer
- **Point.** Three extension points, three jobs. The tool does a deterministic check, the guard blocks input, the enhancer reshapes output.
- **Do.** Build the jar, upload it, open the three demo agents. Send a valid NIK, an invalid NIK and an NPWP. Show how the guard stops a raw NIK that is sent as plain text.
- **Exercise.** Add a rule: reject a NIK whose province code is not in the list. Rebuild, upload, retest.

### Sample 02: system agent
- **Point.** When the logic is mostly code and the model only writes things up, write a system agent in Java.
- **Do.** Open the Form Naming Auditor page in **Settings**, run it on a demo app, open the report and the trace in **Agent Execution Audit Trail**.
- **Exercise.** Change the naming rule in `NamingRules` and re-run the unit test.

### Composers and the Orchestrator
- **Point.** Composers generate Joget artifacts from a prompt. A good prompt names the fields, the roles and the approvals.
- **Do.** Run the Form Composer with and without a skill (compare the two files in `04-custom-skills/expected-result`). Then run the Orchestrator on a small purchase-request app and approve its plan.

### Samples 03 and 04: standards for the Composers
- **Point.** Skills teach rules. A tool supplies data. Use both: the skill says "use the lookup tool for cost centres", the tool returns them.
- **Do.** Upload the sample 03 jar, add the tool template, add the four skills, then ask the Form Composer for a purchase-request form. Check that field ids start with `acme_` and the cost centre list matches `acme-standards.json`.
- **Exercise.** Replace the Acme standards with the partner's own: edit the JSON, keep the skills in step, run the tests.

### Sample 05: Composer MCP
- **Point.** The model lives in Claude Code. Joget only supplies tools and skills over MCP.
- **Do.** Set `JOGET_BUILDER_API_KEY`, run `05-composer-mcp/smoke-test.sh`, start Claude Code in the repository, run the end-to-end prompt.
- **Works when.** The smoke test prints PASS for every step and the prompt creates the purchase-request app.

### Choosing an extension point (wrap-up)

| Need | Use |
|---|---|
| A deterministic check or lookup the agent can call | Custom tool (sample 01) |
| Block or rewrite what goes in | Guard |
| Reshape what comes out | Enhancer |
| A model behind your own gateway | Custom LLM provider (sample 06) |
| Mostly code, model writes it up | System agent (sample 02) |
| Rules or data for the Composers | Skill and tool template (samples 03, 04) |
| A new kind of artifact with its own builder | Custom composer (sample 07) |

## 6. Tailoring the Acme standards for a partner (optional)

1. Edit `03-composer-custom-tool/bundle/src/main/resources/standards/acme-standards.json` (cost centres, statuses, formats).
2. Edit the four skills in `04-custom-skills` so the field-id prefix (`acme_`), naming rules and terminology match the partner.
3. Run the tests in `03-composer-custom-tool/bundle`. `ReferenceSkillInSyncTest` fails if the skill and the standards file drift apart.
4. Update `expected-result` in `04-custom-skills` if the generated field ids change.
5. Do not put real partner data in this repository. Keep partner-specific changes in a private fork or branch.

## 7. Common problems on the day

| Symptom | Likely cause | Fix |
|---|---|---|
| Plugin does not appear after upload | Wrong Joget version or jar built with another `joget.version` | Rebuild with `-Djoget.version=` set to the partner's version |
| Agent run fails with a model error | Credential or service not tested | Use **Test connection** in AI Central Config |
| Build fails on missing Joget or API jar | `00-setup` not run | Run the install scripts in `00-setup` |
| Composer ignores the skill | Skill not enabled for that composer, or description does not match the task | Check `composers:` in the skill header and sharpen the description |
| Smoke test fails at the key step | `JOGET_BUILDER_API_KEY` unset or wrong | Export it in the same shell and rerun |
| Slow or timed-out calls | Shared model rate limit | Stagger participants, or use a separate key each |

See also manual 30 (Troubleshooting) and 31 (FAQ).

## 8. Checklist for the day

- [ ] Server reachable, Agent Builder installed, AI service tested
- [ ] Sample jars built and uploaded in **Settings > Manage Plugins**
- [ ] Demo app imported (sample 01)
- [ ] Composer MCP connected, if used (run `05-composer-mcp/smoke-test.sh`)
- [ ] Spending limit set on the AI credential
- [ ] Partner has a way to keep the repository afterwards

## 9. After the session

- Send the partner the repository link, the manual and the slide deck.
- Ask each participant for one thing they will build in the next two weeks, and schedule a short check-in.
- Note which samples they plan to adapt, and what they asked for that is missing. Feed that back into this repository.
