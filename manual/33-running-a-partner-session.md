# Running a Session for a New Partner

This repository is a generic starting kit for partner enablement sessions. It uses a fictional company, **Acme**, so you can reuse it for any partner. This page explains what to keep, what to swap and how to prepare a session.

## What is generic and what is an example

| Part | Status | Action for a new partner |
|---|---|---|
| Manual pages 01 to 32 | Generic | Keep as is |
| `00-setup`, `02-system-agent`, `05-composer-mcp`, `06-custom-llm-provider`, `07-custom-composer` | Generic | Keep as is |
| `01-agent-builder-tools` | Worked example with Indonesian NIK and NPWP numbers | Keep, or swap the ID rules for the partner's market |
| `03-composer-custom-tool`, `04-custom-skills` | Example company standards for **Acme** (cost centres, naming rules, formats) | Replace with the partner's own standards, or keep Acme for a neutral demo |
| `01-agent-builder-tools/demo-app/acme_tools.jwa` | Generated demo app | Regenerate with `node build-demo-app.js` after any change |

## Before the session

1. Confirm the partner's Joget version. Everything here needs Joget DX 9.1 or later.
2. Agree which samples to cover. A half-day session usually covers 00, 01, 03 and 04. Add 05 if the partner uses Claude Code.
3. Prepare a development Joget server with the Agent Builder plugin installed and an AI credential and service set up in **Settings > AI Central Config**.
4. Build the samples once on your own machine, so you know the build works (`mvn clean package "-Djoget.version=9.1.2"` in each sample folder).

## Tailoring the Acme standards for a partner (optional)

1. Edit `03-composer-custom-tool/bundle/src/main/resources/standards/acme-standards.json` (cost centres, statuses, formats).
2. Edit the four skills in `04-custom-skills` so the field-id prefix (`acme_`), naming rules and terminology match the partner.
3. Run the tests in `03-composer-custom-tool/bundle`. `ReferenceSkillInSyncTest` fails if the skill and the standards file drift apart.
4. Update `expected-result` in `04-custom-skills` if the generated field ids change.
5. Do not put real partner data in this repository. Keep partner-specific changes in a private fork or branch.

## Checklist for the day

- [ ] Server reachable, Agent Builder installed, AI service tested
- [ ] Sample jars built and uploaded in **Settings > Manage Plugins**
- [ ] Demo app imported (sample 01)
- [ ] Composer MCP connected, if used (run `05-composer-mcp/smoke-test.sh`)
- [ ] Partner has a way to keep the repository afterwards

## After the session

- Send the partner the repository link and the manual.
- Note which samples they plan to adapt, and what they asked for that is missing. Feed that back into this repository.
