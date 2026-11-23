# 04-custom-skills: teach the AI Composers your standards

A skill is a folder with one file, `SKILL.md`: a short header (front matter), then Markdown sections. The AI
Composers see only the name and the description of each skill. They read the body when the description matches the
task. So a skill costs almost nothing until it is needed.

This folder contains four example skills with example standards for a fictional company, Acme (sample data): field ids start with `acme_`,
labels are in Bahasa Indonesia, stored values are English codes, and option lists come from the sample data.

## What the sample shows

| Skill | Available for (`composers:`) | Purpose |
|---|---|---|
| `acme-naming-conventions` | Form Composer | Field ids `acme_*`, Bahasa Indonesia labels, English stored values, date, currency and request-number formats |
| `acme-app-standards` | Orchestrator, List Composer, Process Composer, UI Composer | Artifact ids, process and activity names, approval step rules, list column order, menu names |
| `acme-standards-lookup` | Form Composer, Orchestrator, Process Composer | Links the custom tool of [03-composer-custom-tool](../03-composer-custom-tool/README.md) through `tools: [lookup_acme_standard]` |
| `acme-standards-reference` | none (`composers: []`) | The same data as that tool, as static text. It is for clients that connect through Composer MCP, which cannot call the tool |

What you learn:

1. The `SKILL.md` format and how to write sections that the model reads one at a time.
2. How to scope a skill to composers and how to link a tool template.
3. Two ways to install a skill: the AI Central Config page, or copying the folder.
4. How the same skills reach an MCP client such as Claude Code.
5. How to prove that a skill has an effect (with and without).

## Files

| Path | Content |
|---|---|
| `acme-naming-conventions/SKILL.md` | Sections `FieldIds`, `Labels`, `Formats`, `Checks` |
| `acme-app-standards/SKILL.md` | Sections `ArtifactIds`, `Process`, `Datalists`, `Userview`, `Checks` |
| `acme-standards-lookup/SKILL.md` | Sections `WhenToCall`, `UsingTheResult`, `Topics` |
| `acme-standards-reference/SKILL.md` | One section per list: `CostCentres`, `Departments`, `LeaveTypes`, `RequestStatus`, `ApprovalMatrix`, `Formats` |
| `expected-result/with-skills.fields.txt` | Fields of a form generated with the skills active |
| `expected-result/without-skills.fields.txt` | Fields of a form generated from the same prompt without the skills |

## The SKILL.md format

```markdown
---
id: 1efae2c1-084b-4819-b4c8-5cf8e2d9840c
name: acme-naming-conventions
description: "Use when building or editing any form for an Acme app: field id prefix acme_, ..."
status: active
builtinVersion: 1
createdAt: 2026-11-05T00:00:00Z
composers:
  - org.joget.ai.agent.lib.composer.FormComposerChat
tools: []
---

# acme-naming-conventions
**Nav:** FieldIds - Labels - Formats - Checks

Preamble: one or two sentences that always apply.

---

## FieldIds

When you add any field, its id starts with acme_ ...
```

| Key | Meaning |
|---|---|
| `id` | A UUID. Keep it identical in all your environments. |
| `name` | The unique name. The model passes it to read the skill. Use lower-case letters, digits and hyphens. |
| `description` | One line in double quotes. This is the only text the model sees before it decides to read the skill, so write "Use when ...". A folded value (`description: >`) is not supported. |
| `status` | `active`, `draft` or `deprecated`, in lower case. Only `active` skills are offered. |
| `builtinVersion`, `createdAt` | Housekeeping values. Keep them as in the examples. |
| `composers` | The composers that may use the skill, as full class names (table below). An empty list means no composer is offered the skill. |
| `tools` | Names of tool templates that become available when the skill is read. `[]` for none. |

Composer class names for `composers:`:

| Composer | Name under **Available For** in AI Central Config | Class name |
|---|---|---|
| Form Composer | AI Composer - Form Builder Agent | `org.joget.ai.agent.lib.composer.FormComposerChat` |
| List Composer | AI Composer - Datalist Builder Agent | `org.joget.ai.agent.lib.composer.DatalistComposerChat` |
| Process Composer | AI Composer - Process Builder Agent | `org.joget.ai.agent.lib.composer.ProcessComposerChat` |
| UI Composer | AI Composer - UI Builder Agent | `org.joget.ai.agent.lib.composer.UiComposerChat` |
| Theme Composer | AI Composer - Theme Builder Agent | `org.joget.ai.agent.lib.composer.ThemeBuilderComposerChat` |
| Orchestrator | AI Orchestrator Agent | `org.joget.ai.agent.lib.composer.OrchestratorChat` |

Rules for the body:

- The file must start with `---` on the first line, with no byte order mark.
- The model first receives an index: the preamble and the first sentence of each `## ` section. Then it reads one
  section at a time. So make the first line of every section a "When X, do Y" sentence.
- Never write a line that starts with `## ` inside a code block. It would be read as a new section.
- Do not edit the bundled `joget-*` skills. A plugin upgrade can overwrite them. Add your own skills beside them.

## Prerequisites

- Joget DX Enterprise with Agent Builder installed and an active AI service.
- For `acme-standards-lookup`: the plugin jar and the tool template of
  [03-composer-custom-tool](../03-composer-custom-tool/README.md).
- Nothing to build in this folder.

## Install

### Way 1: copy the folder

Copy each skill folder into `<JOGET_HOME>/wflow/app_ai_skills/`, so that the result is
`<JOGET_HOME>/wflow/app_ai_skills/acme-naming-conventions/SKILL.md`, and so on.

```powershell
Copy-Item -Recurse .\acme-naming-conventions, .\acme-app-standards, .\acme-standards-lookup, .\acme-standards-reference "$env:JOGET_HOME\wflow\app_ai_skills\"
```

```bash
cp -r acme-naming-conventions acme-app-standards acme-standards-lookup acme-standards-reference "$JOGET_HOME/wflow/app_ai_skills/"
```

No restart is needed. Joget reads a skill again when the time stamp of its file changes. Keep the skill folders in
your version control and copy them to promote a skill from one environment to the next. In a cluster the folder must
be on the shared storage.

### Way 2: the AI Central Config page

1. Open **Settings > AI Central Config**, tab **Skills**, and click **Add Skill**.
2. Fill in **Skill Name**, **Description**, **Instruction** (the body, from the line `# <name>` down), **Available
   For**, **Associated Tool Templates** and **Status**.
3. Click **Add Skill**. Joget writes the same `SKILL.md` file. Note that the page gives the skill a new `id`.

Optional script for way 2 (needs `npm install` in the repository root):

```bash
node ../03-composer-custom-tool/scripts/central-config.js skill acme-naming-conventions/SKILL.md
node ../03-composer-custom-tool/scripts/central-config.js list-skills
```

## Configure

Nothing more. Check on the **Skills** tab that the four skills are listed with status **Active**, and that
`acme-standards-lookup` shows the tool template `lookup_acme_standard`.

## Try it

Form Composer, in an empty form:

1. ```text
   Create a purchase request form with requester name (required), department (dropdown), cost centre (dropdown), item description (textarea), total amount and status.
   ```
2. ```text
   Add a date needed field and a request number.
   ```

Orchestrator (a question; it builds nothing):

3. ```text
   How many approval steps does a purchase request of Rp 20.000.000 need?
   ```

To see the difference a skill makes: set `acme-naming-conventions` and `acme-standards-lookup` to **Draft**
on the **Skills** tab, start a new chat in an empty form, and send prompt 1 again. Then set them back to **Active**.

## Expected result

| | Without the skills | With the skills |
|---|---|---|
| Field ids | `requester_name`, `department` | `acme_requester_name`, `acme_department` |
| Labels | English | Bahasa Indonesia (`Nama Pemohon`, `Pusat Biaya`) |
| Cost centres | invented, for example `cc001 = CC001 - Corporate` | the list from the sample data: `CC-1000 - Kantor Pusat Jakarta`, `CC-2000 - Cabang Surabaya`, `CC-3000 - Cabang Bandung`, `CC-4100 - Pusat Data` |
| Status values | invented, for example `draft`, `pending_approval`, `approved`, `rejected`, `cancelled` | `draft`, `submitted`, `approved`, `rejected`, with Bahasa Indonesia labels |

The two files in `expected-result/` show one complete result of each kind. Prompt 2 should add a date field with the
format `dd/MM/yyyy` and an ID generator field `acme_request_no`. Prompt 3 should answer 2 steps.

The model's output differs from run to run. Judge the result by the rules (prefix, language, exact option lists),
not by an exact match with the files.

## The same skills over Composer MCP

Composer MCP reads the same folder `app_ai_skills` and offers the skills to MCP clients, together with its own
bundled skills. See [05-composer-mcp](../05-composer-mcp/README.md).

| Topic | Behaviour |
|---|---|
| Tools | `joget_skills_list`, `joget_skill_read {name, section}`, `joget_skill_search {query}`. Skills from AI Central Config are listed with `kind: central`. |
| Prompts | Each active skill is also a prompt `skill_<name with underscores>`, for example `skill_acme_naming_conventions`. |
| Status | Only `active` skills are listed. |
| Scope | `composers:` is not applied over MCP. A skill for the Form Composer is visible to every MCP client, so its text must also make sense to an outside assistant. |
| Linked tools | `tools:` has no effect over MCP: the linked tool exists only inside Agent Builder runs. This is why `acme-standards-reference` exists. |
| Search | `joget_skill_search` can rank bundled skills above yours. Tell the client to read your skills by name. |

You can also install a skill as a native skill of your AI coding assistant: copy the folder to
`.claude/skills/<name>/SKILL.md` in your project and keep only `name` and `description` in the front matter.

## How to adapt it

1. Copy a skill folder and rename it. Change `name`, give it a new `id` (any UUID), and write a new `description`
   that starts with "Use when".
2. Replace the rules with your own. Give each rule a short code (`ITAS-1`), a right example and a wrong example.
3. Keep a `Checks` section at the end: a numbered list the model verifies before it finishes.
4. Set `composers:` to the composers that should use it.
5. Keep skills short. A section is read as a whole; split long sections.

## Limitations and things to know

- A skill is guidance, not enforcement. The model usually follows it, but check the result. Rules that must never
  be broken belong in code (a tool, a guard or a validator).
- The values in these skills are example data, and the Bahasa Indonesia terms are examples too. Replace them with
  your own standard and your own reviewed terms.
- `acme-app-standards` affects a whole Orchestrator build. Such a build takes much longer than a single form;
  the prompts in `05-composer-mcp` are meant for that test.
- Skills are files on the server. They are not part of an app export (`.jwa`).

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| The skill is not on the **Skills** tab after copying | Wrong folder level, or the file does not start with `---` | The path must be `app_ai_skills/<name>/SKILL.md`; remove a byte order mark or empty first line |
| The composer ignores the skill | Status is `draft`, or the composer is not in `composers:`, or the description does not match the task | Set `active`; add the composer; rewrite the description as a trigger |
| Only part of the skill seems to be used | The model read one section only | Put the essential rule in the preamble; make each section's first sentence say when it applies |
| A section is cut in a strange place | A line starting with `## ` inside a code block | Indent it or remove it |
| `A skill with this name already exists` | The name is taken | Choose another name, or edit the existing skill |
| The field ids have no `acme_` prefix | `acme-naming-conventions` was not read in that run | Name the skill in your prompt, or check its scope and status |
