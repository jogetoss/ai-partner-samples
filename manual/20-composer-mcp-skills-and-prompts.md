# Composer MCP: Skills, Prompts and the App Generator

This page explains the knowledge that the Composer MCP server gives to the AI assistant: the skill library inside the plugin jar, the skills you configure in AI Central Config, the prompts and resources the server publishes, and how to add your own skills. It also gives complete example prompts for building apps through MCP and guidance for your project's `CLAUDE.md`. It is for developers and implementers who direct an assistant such as Claude Code, and for anyone who writes conventions that the assistant must follow. The tools themselves are on [Composer MCP: Tool Reference](19-composer-mcp-tools.md).

> **Note on the app generator:** the page title mentions the app generator. The generator is a separate command-line program that is not part of this delivery. It is not in the plugin jar and it is not an MCP tool, so you cannot use it with the jar alone. This page therefore does not cover it. Two bundled skills describe it (`joget-app-generator` and `joget-toolkit`); see "Limitations and things to know". Everything else on this page works with the jar.

## What a skill is

A **skill** is a folder with a guide file named `SKILL.md`, and optionally more files. It tells the assistant what it cannot guess: how Joget stores an app, the exact properties of a plugin, the order in which to build, mistakes to avoid, and the conventions of a customer. The assistant reads a skill when the task matches the skill's description. A skill is knowledge. It does not give the assistant new abilities and it cannot override the safety rules of the tools.

A skill reaches the assistant from three places:

| Source | What it is | Where you manage it |
|---|---|---|
| **Bundled skills** | 46 skills inside the Composer MCP jar. Read-only. They are listed with `source: bundled`. | You cannot edit them. You can read them with `joget_skill_read`. |
| **Skills configured in Joget** | The skills of Agent Builder: the default skills it installs and the skills you add in **AI Central Config**. They are listed with `source: central`. | **Settings > AI Central Config > Skills**, or the skills folder on the server. See "Skills from AI Central Config". |
| **Native Claude Code skills** | Skills stored in your project or home folder. Claude Code loads them itself. | Files on your computer. See "Adding your own skills". |

### How a bundled skill is organised

Each skill folder holds a short guide `SKILL.md` with a header (`name`, `description`, `kind`, sometimes `editions`) and a decision guide that points to deeper files. The deeper files are in folders:

| Folder | Content |
|---|---|
| `references/` | Deep-dive guides, read one at a time. |
| `contracts/` or `schemas/` | The exact property definition of a plugin class, one file per class. |
| `examples/` | Complete working JSON, XPDL or CSS. |
| `evals/`, `scripts/` | Test cases and helper scripts. The assistant can read them but cannot run them through MCP. |

The `kind` is `knowledge` (core Joget build knowledge) or `optional` (extra packs for writing, working style, sample customers and similar). `joget_skills_list` can filter by it.

## How the assistant uses skills

When a client connects, the server sends a short instruction text in its `initialize` reply. It tells the assistant to list the skills, read the one that matches the task, use `joget_skill_search` for keywords, and use `joget_get_contract` for exact property names instead of guessing. It also tells the assistant that the `builder_*` tools are the default way to build and that the browser-connected tools are an optional extra.

A good session therefore looks like this:

1. The assistant calls `builder_status` to see what the connection is allowed to do.
2. It calls `joget_skills_list` (optionally with `query`), or `joget_skill_search`, to find the skills for the task.
3. It reads `joget-start-here`, then `joget-builder-api`, then the skill for each artifact it builds: first the `SKILL.md`, then only the section or file it needs (`section`, `path`).
4. It asks `joget_get_contract` or `builder_introspect` for the exact properties of every plugin it uses.
5. It builds with the `builder_*` tools and checks its work with `builder_lint`, `builder_verify_app` and `builder_data`.

Rules the skills follow:

- **Read the router, then one part.** A whole skill can be hundreds of files. Responses are limited to 40,000 characters and the assistant continues with `offset`.
- **A path is not a read.** The bundled prompts tell the assistant to open the file, not to quote its name.
- **Contracts are law.** For form fields and datalist plugins, the per-class contract files give the property names. The assistant should not invent property names.
- **Short names work.** `joget_skill_read` accepts `forms`, `joget-forms`, `datalists`, `joget-ui`, `joget-theme`, `joget-processes`, `joget-agents`, `joget-plugins`, `joget-api`, `joget-qa` and similar names for the full skill name. A name without the `joget-` prefix, or a part that matches only one skill, also works.

You can ask the assistant which skills it used. The sample project rules at the end of this page make it print that list.

## Bundled skills: building and integrating

These are the skills the assistant reads while it builds. The "Read it when" column is a short form of the `Use when` line in the skill's own description.

| Skill | Kind | What it teaches | Read it when |
|---|---|---|---|
| `joget-start-here` | knowledge | The base skill. What Joget is (an app is a set of stored definitions), the map of every other skill, and two rules: guides are written for the builder user, and custom components never replace the stock Save, Submit, Approve or other workflow actions. | At the start of any Joget task. |
| `joget-builder-api` | knowledge | How work lands through the `builder_*` tools: procedure, laws (explicit version, confirm gate, lint), tool reference, recipes, build order, verification, gaps, and gotchas found while building apps. | Before the first write, or when a tool answers 400, 409, 412, 415, 422 or 501. |
| `joget-war-room` | knowledge | A method for every build request: intake, one running plan, possibilities, data model, mock screens, one approval, a fixed build order, and a close with evidence. | You ask to build, change, fix or continue an app, or ask what Joget can do. |
| `joget-architect` | knowledge | Maps a plain-language requirement to the right Joget mechanism (native element, setting, hash variable, JavaScript, BeanShell, plugin), with patterns, traps and known mistakes. | "Can Joget do this?", "How should this be built?", or something fails silently. |
| `joget-form-builder` | knowledge | Form JSON: tables, field ids, sections, columns, validators, binders, post processors, grids, sub-forms, visibility rules and seed rows. Per-class contracts for form elements. | Any form work. |
| `joget-datalist-builder` | knowledge | Datalist JSON: binders, columns, filters, status chips, formatters, row and list actions, templates, nested master-detail lists and cascading lookups. | Any list, table view, filter or CRUD list. |
| `joget-userview-composition` | knowledge | Userviews, categories, menus, inboxes, Page Builder components (tiles, charts, columns), permissions and theme settings, with per-class contracts and a userview lint. | Any userview or page. |
| `joget-theme-builder` | knowledge | Native theme JSON, shared CSS, a matching login page and information-tile templates, and how to create or update the theme headlessly. | Generating a theme or applying a brand. |
| `joget-process-builder` | knowledge | Processes: XPDL graph, activities, routes, participants, approval and rework loops, workflow variables, deadlines, tools, and the form, tool and participant bindings. | Any process, approval or workflow. |
| `joget-process-recipes` | knowledge | Recipes for building and testing an approval process through `builder_process` and `builder_workflow`: an XPDL skeleton that deploys, the mappings body, a dry run, start and complete, a test checklist. | You must deploy, map or test a process, or a deploy returned `Error in package`. |
| `joget-known-solutions` | knowledge | The correct Joget solution for recurring calls: charts, business logic, dashboard counts, hash variables, form layout, notifications, approvers, workflow activities, integrations, and fixes for known theme and rendering problems. | Before you build or repair any app, so the native feature is used instead of code. |
| `joget-enterprise-source` | knowledge | Plugin property truth for the Joget DX 9 Enterprise plugins: form elements, grids, binders, validators, datalist binders, formatters, filters, templates, userview menus, process tools, hash variables. | App JSON names an Enterprise plugin and you need its exact properties. |
| `joget-plugin-builder` | knowledge | Java plugins (form element, process tool, userview menu, binder, participant), BeanShell scripts for forms, lists and processes, and jar packaging. | Building or reviewing a plugin or a script. |
| `joget-api-connector` | knowledge | Connecting Joget plugins to a REST API through the API Connector Builder plugin (datalist binder, form load, options and store binders, process tool, datalist action) and the cases that silently give an empty screen. | A connector-backed list or form is empty. |
| `joget-agent-builder` | knowledge | Building AI Agent Builder agents: LLM, prompts, tools, enhancers, decisions, guards, multi-agent orchestration, the agent JSON, running agents, chat UI and evaluation. | The task is about an Agent Builder agent. |
| `joget-ai-composer-prompting` | knowledge | Writing natural-language prompts for the in-product AI Composers and the Orchestrator, with templates and known limits. | You call the in-product composers or the Orchestrator. |
| `joget-composer-tools-live` | knowledge | The optional browser-connected composer tools as they are really named: argument names, slot claiming, unsaved edits, `connect_session` order, and what never to call. | A builder page is open and you want to watch or change it live. |
| `joget-dashboard-recipes` | knowledge | Dashboard pages built headlessly: KPI tiles fed by hash variables over counting lists, branded information tiles, charts, a recent-items list, mobile layout, and how to verify the numbers. | A home or dashboard page with counts or charts. |
| `joget-list-recipes` | knowledge | Production list recipes: role-scoped "all" and "my" lists, status chips, date urgency, search and facet filters, report lists with export, KPI strips. | Building or upgrading a list or report. |
| `joget-insights-pages` | knowledge | An interactive analytics page in a userview: a filter bar kept in the address, KPI tiles, bars, lines, donuts, a heatmap and a ranked table, each drilling into a filtered list. | A dashboard needs filters, click-through or a heatmap. |
| `joget-enterprise-app-patterns` | knowledge | Patterns of a production approval app: several form views per table, decision outcome sections, dynamic approver, record header, row-level security, read-only related grids, history, scoring. | Designing a multi-form, multi-stage app. |
| `joget-translation` | knowledge | Translating an app and proving it: message catalog, locale codes, forms, lists, menus, notifications, bilingual data and options. | Any translation or language-switch task. |
| `joget-ui-localization` | knowledge | Making an app fully local-language, including stock text that has no property: shipped locales, where stock strings live, theme JavaScript and CSS to translate or hide list chrome, login template, `?_lang=` switching. | The app must show no leftover English. |
| `joget-app-identity` | knowledge | The App Center card icon (UI Thumbnail resource, upload, public permission, serve check) and naming rules for app name, app id and builder ids, including why an app id cannot be renamed. | An app has no icon, or you choose names. |

## Bundled skills: design, quality and review

| Skill | Kind | What it teaches | Read it when |
|---|---|---|---|
| `joget-design-expert` | knowledge | Design taste for Joget apps: what an app should look like and how to judge a screen against settled rules (one color ramp plus one accent, light demos, content as design). | You need a design direction or a screen critique. |
| `joget-premium-ui-system` | knowledge | A brand-neutral design system for Joget DX 9 apps: tokens, component recipes, shell, login, role-aware navigation, a QA loop, and a ready stylesheet and script that make generated apps look production-grade. | Generating or upgrading theme CSS, login page, list, form, dashboard or record page look. |
| `joget-qa-checklist` | knowledge | A checklist of past mistakes for reviewing every screen before it is shown: typed KPI numbers, hash-variable table names, stylesheet placement, theme CSS problems, untranslated labels, default chrome, mobile layout, empty forms. | A final visual and content check. |
| `joget-visual-atlas` | knowledge | How to check each change on screen in the order a builder sees it (builder canvas, preview, generated list and form, then runtime as a real user), with the screen, menu path and property label for each kind of change. | Before a build step is reported done; "where is this setting?" |
| `joget-form-lint-gaps` | knowledge | The blind spots of `builder_lint` and `builder_verify_app` for forms and lists, with a pre-seed checklist and a symptom-to-cause table. | Lint says ok but data or screens are wrong. |
| `joget-review` | knowledge | Reviewing an app against its approved plan in four evidence lanes (configuration, runtime, visual, builder), with a result and a receipt for each item. | "Is it ready?", "check the app". |
| `joget-app-review` | knowledge | A whole-app audit for security, design, performance and maintainability findings with severities: severity scale, false-positive rules, plugin risk catalog, pattern scan rules, BeanShell checklist. Works from a live app or an export. | An audit of an app. |
| `joget-app-security-baseline` | knowledge | Security settings every built app needs before it is shown: lock the userview (a new one is public), menu permissions, process start whitelist, no secrets in definitions, safe seed data, publish step, anonymous versus logged-in proof. | Finishing any app, or a page shows data to a visitor who is not logged in. |
| `joget-docs` | knowledge | Writing Joget documents: business requirements, how-tos and handovers, knowledge-base articles, user guides in the voice of a builder, and architecture diagram guidance. | Writing documents about an app. |

## Bundled skills: samples, packs and command-line tools

| Skill | Kind | What it teaches | Read it when |
|---|---|---|---|
| `joget-service-desk-reference` | optional | A reference build of a branded Bahasa Indonesia service-desk app (2 forms, 10 lists, a dashboard userview, a themed login, one approval process, seed data) as reusable JSON, with the call shapes that worked. | You build a ticket, request or approval app and want a shape to copy and adapt. |
| `joget-acme` | optional | A **sample** skill: ACME branding (palette, wordmark, tone) and Bahasa Indonesia rules (labels, date and number formats, identifiers stay English). The brand facts are marked where they must be confirmed against the customer's brand guide. | You build or demonstrate an app for ACME, or an app must be in Indonesian. |
| `joget-app-generator` | knowledge | Describes a command-line app generator that expands a compact JSON spec into a large app. **The generator is not in the jar** (see Limitations). | Not usable with the jar alone. |
| `joget-toolkit` | knowledge | Describes command-line tools that are not part of this delivery: a command-line client, console helpers to list, create and publish apps, screenshot checks, a smoke test and IDE rule export. **These tools are not in the jar.** | Not usable with the jar alone. |
| `joget-starter-apps` | optional | Starter app ideas (to-do, leave request, invoice) and the startup sequence for a first ask. | You ask what can be built, or give an ask with no target. |
| `joget-instance-notes` | optional | Facts for people who run a local instance: plugin upload receipts, database location rules, unique session cookie names, JVM agents, header stripping by reverse proxies. | An instance is slow, being upgraded, or MCP calls fail behind a proxy. |
| `joget-orchestration-notes` | optional | Coordinating multi-page app delivery with specialist agents: roles, dependency order, plan template, acceptance checklist, layout rules. | You split a large app across several agents. |
| `joget-working-style` | optional | A pack of 14 general working-discipline skills (calibrate, gather evidence, verify then report, act or ask, checkpoint, when stuck, and others). | The operator wants stricter working habits from the assistant. |
| `joget-content-method` | optional | The method behind marketing and documentation copy: buyer words, positioning card, drafts, tests, link map, prose pass, critic. | Writing a listing, or checking a draft. |
| `joget-listing-writer` | optional | Writing or rewriting a Joget Marketplace app listing from the buyer's own words. | A Marketplace listing. |
| `joget-marketplace-factory` | optional | A release-readiness playbook: builder and reviewer roles, ranked defects, review rounds, human approval stops. | Preparing an app for the Marketplace or a demo-safe handover. |
| `joget-demo-recording-notes` | optional | A method for recording a demo video of a finished app. | A demo video or screen recording. |
| `joget-writing-craft` | optional | Prose craft and an edit pass, plus a catalogue of signs that text is machine-made. | Polishing text that people will read. |

> **Note:** the bundled library is a snapshot that ships with the jar. Skills carry an `editions` tag in some cases and many of them describe Joget DX 9.1 behaviour. Treat a skill as guidance that was written against one build, and check results with `builder_verify_app` and a person looking at the screen.

## Skills from AI Central Config

The first rows of `joget_skills_list` come from the jar. The rest come from Joget itself, if the Agent Builder plugin is installed on the same instance. This is how the server reads them and what the assistant sees.

### How the server finds them

1. Agent Builder stores each skill as one file: `app_ai_skills/<name>/SKILL.md`, in the Joget data folder (`wflow`), next to folders such as `app_forms` and `app_plugins`. The **Skills** tab of **AI Central Config** writes these files. In a cluster the folder must be on storage that all nodes share. With several tenants, each tenant has its own folder.
2. Composer MCP asks the Agent Builder skill store for the list and for the text of a skill. If the store is not available or is empty, it falls back to the single setting `AI_SKILLS`.
3. If Agent Builder is not installed, there are no skills from Joget, and everything else works.

### What the assistant sees

| Topic | Behaviour |
|---|---|
| Which skills are listed | Only skills whose status is `active`. A skill with status `draft` is never served. A `deprecated` skill is not listed, but can be read by its exact name. |
| How a skill is shown | `kind: central`, `source: central`, and a `composers` list. |
| Reading | One document. Use `section` to read one `##` heading. A `path` returns an error, because a central skill has no files. |
| Searching | `joget_skill_search` adds up to 5 hits from these skills after the bundled hits. Words in the skill name count most, then the description, then the text. |
| Same name as a bundled skill | The bundled skill keeps the plain name: `joget_skill_read` with `name: "joget-form-builder"` returns the bundled text, never the configured one. The skill from Joget is listed as `central:<name>`, and is read with `name: "central:<name>"`. Its prompt is `skill_central_<slug>`. Neither hides the other. The header of a read of any skill from Joget starts with `central:`, also when there is no bundled twin. |
| Scope | The **Available For** setting (`composers`) is shown but **not used**. Every active skill is visible to every MCP client, even one that is limited to a single composer in the product. |
| Tool templates | The **Associated Tool Templates** link (`tools`) does nothing over MCP. Those tools exist only inside Agent Builder runs. |
| Changes | Metadata is re-read when the file changes; the text is read on every call. A new or edited skill is visible without a restart. |

### The default skills of Agent Builder

Agent Builder installs these skills with status `active`. Five of them have the same name as a bundled skill, so they appear as `central:<name>`. Your list can be longer than this table: it also shows the skills you add, and skills that other plugins or other Agent Builder builds install.

| Skill in Joget | Listed to the MCP client as | Scope in the product |
|---|---|---|
| `joget-form-builder` | `central:joget-form-builder` | Form Composer, Orchestrator |
| `joget-form-validators-binders` | `joget-form-validators-binders` | Form Composer, Orchestrator |
| `joget-form-field-ui` | `joget-form-field-ui` | Form Composer, Orchestrator |
| `joget-datalist-builder` | `central:joget-datalist-builder` | Datalist Composer, Orchestrator |
| `joget-userview-builder` | `joget-userview-builder` | UI Composer, Orchestrator |
| `joget-theme-builder` | `central:joget-theme-builder` | Theme Composer, Orchestrator |
| `joget-process-builder` | `central:joget-process-builder` | Process Composer, Orchestrator |
| `joget-orchestrator` | `joget-orchestrator` | Orchestrator |
| `joget-app-design` | `joget-app-design` | Orchestrator |
| `joget-known-solutions` | `central:joget-known-solutions` | All composers, Orchestrator |
| `joget-hash-variables` | `joget-hash-variables` | All composers, Orchestrator |

Agent Builder refreshes a default skill from its own plugin when its built-in version is higher. Do not edit a default skill; add your own skill next to it. Edits to a default skill can be overwritten on an upgrade.

### What this means for your own skills

- **Use a unique name prefix** such as `acme-`. A name that equals a bundled skill name is listed as `central:<name>` and is easy to confuse with the bundled one.
- **Write for an outside assistant.** The assistant that reads your skill over MCP is not the in-product composer. Do not tell it to "call the composer function". If your skill depends on a tool template, add a second skill with the same data as plain text and set its **Available For** list to empty. The sample skill `acme-standards-reference` is built this way.
- **Phrases that refer to one composer are visible to everyone.** Because the scope is ignored, a skill meant for the Form Composer is also visible to an assistant that builds a datalist. Write it so that it makes sense there.
- **Use status `draft` while you write.** A draft is not served to anyone.
- **Search is not exact.** The bundled hits come first. Tell the assistant to read your skills by name (your project rules can say so).
- **Global Central Instructions are not served over MCP.** In **AI Central Config > Skills**, the **Central Instructions** section has a **Global** box and one box for each composer. The prompts `joget_form_instructions`, `joget_list_instructions`, `joget_ui_instructions`, `joget_theme_instructions` and `joget_process_instructions` return the bundled global rules plus the text of the matching composer box (**AI Composer - Form Builder Agent**, **AI Composer - Datalist Builder Agent**, **AI Composer - UI Builder Agent**, **AI Composer - Theme Builder Agent**, **AI Composer - Process Builder Agent**). The text you entered in the **Global** box is not included, and neither are the **Agent Builder** and **AI Orchestrator Agent** boxes. `joget_global_instructions` returns the bundled global rules only. Put rules that must reach MCP clients in a skill, or in the project's `CLAUDE.md`.

## Using the skill tools

The arguments of the four skill tools are in [Composer MCP: Tool Reference (continued)](19b-composer-mcp-tools-continued.md). These are the usual patterns.

| Goal | Call |
|---|---|
| See everything that exists | `joget_skills_list` with `{}` |
| Only your own skills | `joget_skills_list` with `{"kind": "central"}` |
| Find a skill for a task | `joget_skills_list` with `{"query": "approval"}` or `joget_skill_search` with `{"query": "approval rework loop"}` |
| Read the guide of a skill | `joget_skill_read` with `{"name": "joget-process-builder"}` |
| Jump to one section | `joget_skill_read` with `{"name": "joget-process-builder", "section": "<slug from the section index>"}` |
| Read a file | `joget_skill_read` with `{"name": "joget-builder-api", "path": "references/recipes.md"}` |
| Read a file part | `joget_skill_read` with `{"name": "joget-builder-api", "path": "references/tools.md", "section": "<heading>"}` |
| Continue a long text | the same call with `"offset": <number from the TRUNCATED line>` |
| Read your own skill with a bundled twin | `{"name": "central:joget-form-builder"}` |
| Exact properties of a plugin class | `joget_get_contract` with `{"className": "SelectBox"}` |
| Live properties on your server | `builder_introspect` `properties` with the full class name |

Contract files describe a class from the Joget Enterprise or Community plugins. A plugin that is not installed on your server will show "Missing Plugin" in the builder, so check the class with `builder_plugin` `check` before you rely on it.

## Adding your own skills

Add a skill when the assistant must follow rules it cannot guess: your naming rules, language and wording, standard lists (departments, cost centres, statuses), approval rules, and what the customer's demo must show. Keep a skill short and specific. Link to a bundled skill for general mechanics instead of repeating it (for example, point to `joget-translation` for how message catalogs work).

### The file format

A skill is a file `SKILL.md` in a folder with the skill's name. It starts with a header between two lines that contain only `---`.

```markdown
---
id: 5d0c7a1e-3b84-4f26-9a6e-0c2f8d1b7e45
name: acme-standards-reference
description: "Use when you build an Acme app from outside Joget (Claude Code over Composer MCP) and need the Acme option lists: cost centres, departments, leave types, request status, approval matrix and formats."
status: active
builtinVersion: 1
createdAt: 2026-11-05T00:00:00Z
composers: []
tools: []
---

# acme-standards-reference
**Nav:** HowToUse - CostCentres - Departments

Static data for clients that cannot call the in-product tool.

---

 ## HowToUse

When a form needs one of these lists, copy it exactly: the first column is the stored value, the second the option label.

---

 ## CostCentres

Stored value, then label.

| Value | Label |
|---|---|
| CC-1000 | CC-1000 - Kantor Pusat Jakarta |
```

In the real file, the lines that start with `##` begin in the first column. They have one leading space in this example only.

| Header key | Rule |
|---|---|
| (first line) | The file must start with `---`, with no byte-order mark. A file that does not is skipped. The header must be closed by a second `---`. |
| `id` | A UUID. Keep the same value in every environment. |
| `name` | The skill name. Lowercase letters, digits and hyphens are safest. The folder name follows the name. |
| `description` | One line, in quotes. This is the only text the assistant sees before it decides to open the skill, so say when to use it. The form `description: >` is not supported. |
| `status` | `active`, `draft` or `deprecated`, in lowercase. Only `active` skills are listed over MCP. |
| `builtinVersion` | A number. Use `1` for your own skills. |
| `createdAt` | A date and time. |
| `composers` | A list of full class names that name the in-product composers that may use the skill (for example `org.joget.ai.agent.lib.composer.FormComposerChat`). Over MCP it is shown but not used. An empty list `[]` keeps the skill out of the in-product composers. |
| `tools` | A list of tool template names. Not used over MCP. |

The composer class names are `org.joget.ai.agent.lib.composer.` plus `FormComposerChat`, `DatalistComposerChat`, `ProcessComposerChat`, `UiComposerChat`, `ThemeBuilderComposerChat` or `OrchestratorChat`.

**The body.** Write a short introduction, then sections that start with `## `. The assistant first gets an index (the introduction and the first sentence of each section) and then reads one section at a time. So:

- Begin every section with one sentence in the form "when X, do Y".
- Never put a line that starts with `## ` inside a code block. It would be read as a new section.
- Keep a `**Nav:**` line under the title that lists the sections. The **Skills** tab can rebuild it for you.

### Route 1: AI Central Config (recommended)

1. Open **Settings > AI Central Config** and select the **Skills** tab.
2. Click **Add Skill**.
3. Fill in the fields.

| Field | What it does | Default | Notes |
|---|---|---|---|
| **Skill Name** | The name of the skill. | empty | Required. A short name without spaces, for example `acme-naming-conventions`. A skill with the same name cannot be created twice. |
| **Description** | One or two sentences that say when the assistant should use the skill. | empty | This is what the assistant reads before it opens the skill. |
| **Instruction** | The full text, in Markdown, with `##` sections. | a template | A live preview shows the section list the assistant will receive. |
| **Available For** | The in-product composers and builders that may use the skill. | none selected | Not used over MCP. |
| **Status** | **Active**, **Draft** or **Deprecated**. | | Only **Active** skills are discoverable. **Draft** is hidden from all agents and composers. **Deprecated** is not discoverable but can still run by exact name. |

4. Save. The server writes the `SKILL.md` file. The interface gives the skill a new `id`.

### Route 2: copy the file

1. Put the folder `<name>/SKILL.md` into the skills folder of the server: `app_ai_skills`, in the Joget data folder next to `app_forms`.
2. There is no restart. The file is read when its time stamp changes.
3. Use git for versions, and copy the folder to promote a skill between environments.
4. In a cluster, the folder must be on shared storage.

### The Acme sample skills

The examples repository folder `04-custom-skills` has four skills in this format. Install them with either route.

| Skill | Scope | What it does |
|---|---|---|
| `acme-naming-conventions` | Form Composer | Field ids start with `acme_`; labels in Bahasa Indonesia; stored values in English; date, currency and request-number formats. |
| `acme-app-standards` | Orchestrator, Datalist, Process and UI composers | Artifact ids, process and activity naming, approval-step rules, datalist column order, menu names. |
| `acme-standards-lookup` | Form, Orchestrator, Process | Links the custom tool of the example `03-composer-custom-tool` through `tools`. The link works only inside Agent Builder. |
| `acme-standards-reference` | none (`composers: []`) | A plain-text copy of the same data (departments, cost centres, leave types, request status, approval matrix, formats) for assistants that cannot call the in-product tool. |

Do not edit the bundled `joget-*` skills; add your own next to them.

### Confirm the skills from Claude Code

1. Make sure the MCP server is connected (`claude mcp list` shows it as connected; see [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md)). Start a fresh Claude Code session.
2. Ask: `Call joget_skills_list and tell me which skills have kind central.` The answer must contain your skills, for example the four `acme-*` skills, and the default skills of Agent Builder.
3. Ask: `Read acme-naming-conventions section FieldIds with joget_skill_read.` The answer must show the rule text.
4. Open the `/mcp` panel. The prompts `skill_acme_naming_conventions` and its three siblings are listed.
5. Optional, without Claude: run `smoke-test.sh` from the examples repository folder `05-composer-mcp`. It checks the tool list, the bundled and custom skills, one section read and the prompts.

If a skill is missing, see "Troubleshooting".

### Use a skill without the MCP server

Claude Code can load a skill on its own. Copy the skill folder to `.claude/skills/<name>/SKILL.md` in the project (or to `.claude/skills/` in your home folder for all projects), keep only `name` and `description` in the header, and start a new session. Claude Code opens the skill when the task matches its description. The `builder_*` tools are not available then, so such a skill acts as knowledge only.

### Advanced: a folder of skills for development

If the Joget server is started with `-Djoget.mcp.knowledge.dir=<folder>`, the skill tools, prompts and resources serve that folder instead of the skills inside the jar. The folder is scanned again every 2 seconds, so a new or edited skill appears with no restart. The folder **replaces** the bundled library; it does not add to it. Use it only on a development server, with a copy of the whole library plus your skills.

## Prompts

An MCP **prompt** is a ready-made block of text that the server offers. Your client shows prompts to you; you choose one and the client puts its text into the conversation. The assistant does not fetch prompts by itself. The server's prompts have no arguments.

### The prompts of this server

| Prompt | What it contains |
|---|---|
| `joget_global_instructions` | The global rules for building: define the problem first, headless first, list and inspect before writing, never invent ids or class names, native before code, no secrets in definitions, one writer per app. |
| `joget_build_instructions` | How to run a build or a small job on an existing app: confirm the target, read before write, native before code, idempotent seeding, acceptance states, security checks. |
| `joget_review_instructions` | How to review an app against its brief in four lanes (configuration, runtime, visual, builder), and how to review an export. Review is read-only. |
| `joget_builder_agent` | A role prompt for the single writer of one app: build order, standards and report. |
| `joget_app_expert_agent` | A role prompt for the single writer of a complex app: several processes, role-based form views, configuration-driven approvals, in-app agents or integrations. |
| `joget_code_expert_agent` | A role prompt for source-level work: plugin development and fixes, reviewing jars, reading Joget source. It never touches app definitions. |
| `joget_checker_agent` | A role prompt for a read-only verifier: structural checks, data checks, leftover scans, and a list of visual checks to ask a person for. |
| `joget_form_instructions`, `joget_list_instructions`, `joget_ui_instructions`, `joget_theme_instructions`, `joget_process_instructions` | The bundled global rules plus the per-composer **Central Instructions** that you entered in **AI Central Config** for the Form, Datalist, UI, Theme or Process Composer. The **Global** box of the Central Instructions is not included. Without Agent Builder, or when nothing is entered, they return the bundled global rules only. |
| `skill_<slug>` | One prompt per skill. For a bundled skill the text is its `SKILL.md`. For a skill configured in Joget it is the full instruction text. The slug is the skill name in lowercase with every run of other characters replaced by `_`: `joget-form-builder` gives `skill_joget_form_builder`. |
| `skill_central_<slug>` | The same for a skill from Joget whose name equals a bundled skill. |

The role prompts are for a coordinator that starts helper assistants: give each helper the prompt for its role, and use one writer per app.

### Use a prompt in Claude Code

Claude Code shows server prompts as slash commands in the form `/mcp__<server name>__<prompt name>`. If you registered the server as `joget-composer`:

```text
/mcp__joget-composer__joget_global_instructions
/mcp__joget-composer__joget_build_instructions
/mcp__joget-composer__skill_joget_form_builder
```

Start a build session by running the first two. You can also list the commands by typing `/mcp__joget-composer__` and reading the suggestions. The `/mcp` panel shows the connected server.

Other clients show prompts in a prompt picker or not at all. The same text is always reachable through the skill tools, except for the role prompts and the instruction prompts, which exist only as prompts.

## Resources

A **resource** is a file that the server lets the client read by address. Composer MCP publishes every text file of the bundled skills (up to 200,000 bytes each, about a thousand files) as `joget-skill://<skill>/<path>`.

| Address | File |
|---|---|
| `joget-skill://joget-form-builder/SKILL.md` | The guide of a skill. |
| `joget-skill://joget-form-builder/contracts/textField.json` | A contract file (the real file names are in the skill's file list). |
| `joget-skill://{skill}/{path}` | The address pattern (a resource template). |

The skills configured in Joget are not published as resources. Binary files are not published.

In Claude Code you can add a resource to a message by typing `@` and the server name, for example `@joget-composer:joget-skill://joget-form-builder/SKILL.md`. Support for resources differs between clients. Where it is missing, use `joget_skill_read`, which returns the same text.

## Example prompts

All prompts below are for an assistant connected to the Composer MCP server. Replace the values in angle brackets. Never paste the API key into a prompt.

Before you use them: the server needs the key and (to create the app from the prompt) the `app-create` flag; or create an empty app in the Joget console first and give its id. See [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md). Table names are shared by all apps on a Joget server, so each prompt asks for a prefix that is unique to the app.

### Check the connection

```text
Call builder_status and tell me what this connection is allowed to do.
```

```text
Call joget_skills_list and tell me which skills have kind central.
```

```text
Read the section FieldIds of the skill acme-naming-conventions with joget_skill_read and show me the rules.
```

Expect a short answer: the access mode and the opt-ins; the list of skills configured in Joget; the rule text of the section.

### Build a small app

This prompt builds a leave-request app. It asks for the target explicitly, and it asks the assistant to ask before every action that needs `confirm`.

```text
Build a leave-request app in Joget app "<APP_ID>" version <VERSION>:
- a Leave Request form (employee name, leave type, start date, end date, reason, status)
- a list of requests with filters on status and leave type
- a userview with a "Requests" menu and a manager approval process
- add 5 realistic sample rows and verify the app.

Rules: start with builder_status and joget_skills_list. Read the skills that match each
artifact before you write it. Pass appId and appVersion on every call. Use builder_lint
before you create a form or a list. Ask me before any action that needs confirm:true.
Finish with builder_verify_app and tell me what you could not check.
```

What to expect:

1. `builder_status`, then the skill tools. The assistant reads `joget-start-here`, `joget-builder-api` and the skills for forms, lists, userviews and processes.
2. It builds in this order: form, list, theme, userview, process (`validate`, then `deploy`, then `mappings`).
3. It asks you before it deploys the process, seeds the rows or starts a case. It seeds the rows with `builder_data` `submit` and checks them with `rows`.
4. It runs `builder_verify_app` and reports the result and what is still unchecked.

What to check yourself: open the app in the App Composer and look at the form, the list, the userview and the process. The app must be **published** before the userview menus open; ask an administrator to publish it, or have the app created with `publish`.

### Change an existing app

Use short, targeted prompts for changes. Give the target and the artifact ids.

```text
In app "<APP_ID>" version <VERSION>, add a "Manager comment" text area to the form
"leave_request" after the field "reason". Read joget-form-builder first. Use
builder_form get, then patch (not update). Lint before you save. Tell me the field id.
```

```text
In app "<APP_ID>" version <VERSION>, add a status filter and a date range filter to the
list "leave_request_list". Read joget-datalist-builder. Use builder_datalist patch. Show me
any lintWarnings.
```

```text
Review app "<APP_ID>" version <VERSION> against these requirements: <paste your requirements>.
Use the prompt joget_review_instructions and the skills joget-review and joget-app-review.
Do not change anything. Report the findings by severity with a receipt for each.
```

### Build a service-desk app (Bahasa Indonesia)

This prompt reproduces a branded service-desk app with two forms, ten lists, a dashboard with four KPI tiles and two charts, a themed login page, role chips, an approval process with Approve and Reject, and seed rows. It copies the JSON of the bundled skill `joget-service-desk-reference`, so the assistant adapts a finished example instead of inventing everything. Replace `<APP_ID>`, `<COMPANY NAME>`, the colors, the tagline and `<APPROVER_USERNAME>`.

```text
Build a complete, polished service-desk app on my Joget server using ONLY the joget-composer MCP tools.

TARGET: appId "<APP_ID>", appVersion "1" (the empty app already exists; do not touch any other app).
BRAND: <COMPANY NAME>. Primary blue <#2367A0>, gold <#D0A030> used ONLY on the main call-to-action button, navy for
headings. Light, clean, generous spacing. Tagline for the login page: "<your tagline in your language>".
LANGUAGE: everything a user reads is in Bahasa Indonesia (labels, menus, buttons, chips, messages, seed data).
Technical ids (appId, form/field ids, table names, status codes) stay English snake_case.

HOW TO WORK
1. Start with builder_status and joget_skills_list. Read with joget_skill_read: joget-builder-api (references/tools.md, "Live-tested gotchas")
   and joget-service-desk-reference (SKILL.md first, then its examples/ files). Open joget-theme-builder, joget-form-builder,
   joget-datalist-builder, joget-userview-composition, joget-process-recipes and joget-acme only when you change something
   the reference does not already cover.
2. Reuse the examples in joget-service-desk-reference: replace APP_ID, replace TABLE_PREFIX with a prefix unique to this app
   (table names are shared by ALL apps on the server; before seeding, builder_data rows for each form must show total 0), change the brand and labels, then land each
   piece with builder_form / builder_datalist / builder_theme / builder_userview / builder_process / builder_data.
   Follow its numbered build order exactly. Lint every form and list with builder_lint before creating it.
3. Never invent ids, class names or property names; check them with builder_introspect or joget_get_contract.
   Check builder_plugin before relying on a chart plugin (fall back to the one that is installed).
4. Ask me before any action that needs confirm:true (data seeding, process deploy, starting or completing workflow
   instances) unless I already said yes in this message: I say yes to seeding and to deploying the approval process.
5. There is no publish tool. When the userview exists, tell me to press Publish in the Joget console, then continue.

BUILD
- Forms: customers (company, contact person, email, phone, city, service tier) and service requests (auto ticket
  number, title, description, customer lookup, category [Incident, Service Request, Change, Consultation], priority
  [low, medium, high, critical], status [new, in_progress, pending_approval, approved, rejected, resolved, closed],
  assignee, created date, target date, cost estimate in Rupiah, attachment, notes). Two-lane sections, Multi Select Box
  dropdowns, BOTH loadBinder and storeBinder set to WorkflowFormBinder.
- Lists with filters (search, status, priority, category, date range), coloured status and priority chips, dd/MM/yyyy
  dates and Rupiah amounts.
- Userview "Beranda" dashboard: four KPI tiles (open tickets, critical, waiting for approval, resolved this month), two
  charts (status distribution, tickets per category), and a recent-tickets list. Menus: Requests, Customers, Approvals.
  The app must require login (LoggedInUserPermission).
- Theme and login page in my brand, built from the reference theme; hide or translate the stock English list chrome
  (export bar, page-size selector, Save/Back/Print) with theme CSS/JS.
- Approval process: requests with a cost estimate go to a manager (Approve / Reject); the approver is the user <APPROVER_USERNAME, e.g. admin>. Deploy and map it, then run two
  cases through (one approved, one rejected) and leave two waiting.
- Seed 8 fictional customers and 28 requests across all statuses, priorities and categories over the last 60 days.
  Fictional company names only, e-mail domains ending in .co.id (the validator rejects .example).

CHECK BEFORE YOU TELL ME IT IS DONE
- BEFORE seeding, builder_data rows for each form must show total 0 (otherwise the table name is shared with another app: stop and
  choose another TABLE_PREFIX). After the FIRST seed row of each form, confirm total is 1 (a form without both binders silently stores nothing).
- When completing a workflow activity send the FULL row, not only the changed field (omitted fields are blanked).
- builder_verify_app returns no problems; the app is published.
- Screenshots (desktop 1440 px and mobile 390 px) of login, dashboard, a list, an open form and the approvals inbox; look
  at them and fix what is ugly (raw ids as labels, English text, misaligned tiles, empty charts).
- Finish with: what exists (ids and URLs), what is imperfect, and every tool error you hit.
```

What to expect: the assistant builds from the reference JSON in the order of the reference skill, asks you to publish the app after the userview exists, and reports the artifacts it created. The prompt contains a screenshot step; if your assistant has no screenshot tool, it will ask you to look at the pages, and you should do so.

Things to know about the result:

- A ticket that a user creates from the form does **not** start the approval process by itself. The assistant starts the cases it needs with `builder_workflow`. If you want every new ticket to start the process, add a start step to the form's post-submission processing in the builder.
- The approval e-mails do nothing until SMTP is configured on the server.
- "Resolved this month" counts by creation date. Add a completion-date field if you need the true figure.
- Stock text with no property (export bar, page-size menu, some button texts) is hidden or translated with theme CSS and JavaScript written for the class names of Joget DX 9.1. Check it again after a Joget upgrade.
- If the chart plugin is not installed, the assistant falls back to the SQL chart plugin.
- Only the cases that the assistant starts with `builder_workflow` have process state. The other seeded approved and rejected rows are plain data.
- A form builder page that is open on the same form shows the old version until you reload it.

### Build a purchase-request app with your own conventions

This prompt shows how custom skills change the result. It needs the four `acme-*` skills active (see "The Acme sample skills") and the app `purchase_mgmt` version 1, created in the console or by `builder_app` `create`. Start a fresh session. `joget-userview-builder` is one of the default skills of Agent Builder, so it appears only if Agent Builder is installed.

```text
Build a complete small Indonesian purchase-request app on the Joget connected through MCP server `joget-composer`. Work in app `purchase_mgmt`, version `1` (create it with builder_app if it does not exist).

First, without chatting: call builder_status, then joget_skills_list. Read the skills `acme-naming-conventions`, `acme-app-standards` and `acme-standards-reference` (index first, then the sections you need) and the bundled joget-form-builder, joget-datalist-builder, joget-process-builder and joget-userview-builder skills. Follow them; they override your defaults.

Artefact id rules (hard): every form, datalist, process and userview id is snake_case, at most 20 characters, no suffix such as form/list/management. Table name is tbl_<formId> (the form id `purchase_request` gives table `tbl_purchase_request`). All approval forms reuse that same table and the same field ids. Field ids start with `acme_`. Ids and stored values are English; every label, section title, menu and seed value is Bahasa Indonesia.

Build, in this order:
1. Form `purchase_request` (Permintaan Pembelian): `acme_request_no` (ID generator, read-only, REQ/yyyy/00000), `acme_requester_name` (default #currentUser.fullName#, read-only), `acme_department` (use the departments from the reference skill), `acme_cost_centre` (cost centres from the reference skill), `acme_item_description` (textarea, required), `acme_total_amount` (Rp, numeric, required), `acme_status` (request status list from the reference skill, default draft).
2. Approval forms over the same table: `purchase_approval` (read-only request data plus `acme_remarks` and Setujui/Tolak) .
3. Datalist `purchase_requests` with the column standard from acme-app-standards, filters on status and department, status as coloured pill.
4. Process `purchase_request` using the approval matrix: Rp 20.000.000 must resolve to line manager then department head; build the steps for the whole matrix with conditions on `acme_total_amount`. Set `acme_status` on every outcome. Participants cannot be mapped to people: use the participant ids from the skill and list that as a manual follow-up.
5. Userview `purchase_portal` with categories and menus named as in the skill: create-request, request list, approval inbox, and a dashboard with tiles for each status (tile titles in Bahasa).
6. Seed 5 realistic Indonesian sample rows with builder_data (confirm:true is allowed for seed rows only).

Then run builder_lint on everything and builder_verify_app; fix what they report.

Output rules: do not narrate; no more than one short line between tool calls. At the end print exactly: (a) "Skills used:" with the names you actually read, (b) a table of created artefact ids (type, id, table), (c) "Manual follow-ups:" (participant mapping, SMTP if email is used, English stock texts), (d) lint/verify result.
```

What to expect: the final report lists the skills that the assistant actually read, the artifact ids that follow your naming rules, and the manual follow-ups. What to check: that the ids and field ids follow your rules, that the lists in the form come from the reference skill and are not invented, and that the participants of the process are mapped by a person.

## Guidance for your project's CLAUDE.md

The server already tells the assistant to read skills first. Rules in your own project file make the result consistent and repeatable. In Claude Code the file is `CLAUDE.md` in the project folder. Other clients have equivalent files (for example `AGENTS.md`, `GEMINI.md`, `.cursor/rules`, or custom instructions). The Acme version of this snippet is also in [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md). A general version:

```markdown
### Joget via Composer MCP

The MCP server `joget-composer` builds Joget apps. Rules:

1. Before building, call `builder_status`, then `joget_skills_list`. Read the matching skills with
   `joget_skill_read` (the guide first, then only the sections you need): `joget-start-here`,
   `joget-builder-api`, and the skill for each artifact. Also read our own skills: <list your skills>.
2. Every builder call passes an explicit `appId` and numeric `appVersion`. Target: app `<APP_ID>`,
   version `<VERSION>`. Never use another app or version.
3. Naming: <your id rules, for example snake_case, at most 20 characters, table name rule>.
   Language: <your language rule>. Stored values and ids stay in English.
4. Look ids, class names and property names up: `builder_introspect`, `joget_get_contract`.
   Never invent them.
5. Run `builder_lint` before saving a form or list. Finish with `builder_verify_app`. Read the
   body of every result, not only the status.
6. Deleting, deploying a process, starting or completing workflow, submitting data and generating
   CRUD need `confirm:true`. Ask me first.
7. Do not turn lint off. Do not pass `allowPageWipe`. Prefer `patch`, `menu_add` and `page_update`
   to rewriting a userview.
8. There is no publish tool. When the app must be published, tell me.
9. Keep replies short. At the end print: the skills you used, a table of created ids (type, id,
   table), what you could not verify, and the manual follow-ups.
10. Never print or store the Builder API key.
```

Why these rules help:

| Rule | Reason |
|---|---|
| Name the skills | Search ranks bundled skills first. Naming your skills makes sure they are read. |
| Pin the target | The tools have no default version. A pinned target prevents a write to the wrong app or version. |
| Naming and language | The assistant cannot guess your conventions. Put the short rules here, and the long lists in a skill. |
| No invented ids | Most silent failures are name mismatches between a form field, a list column and a filter. |
| Lint, verify, read the body | A 200 is not proof. The tools return `lintWarnings`, `incomplete` and `unverified` inside a successful result. |
| Confirm gate | The assistant must ask before it changes live data or deploys. |
| Report format | You get an audit trail for each build: which skills, which ids, what is open. |

Keep the key out of the file. If your team shares the file, use one server name per environment, as described in [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md).

## Limitations and things to know

- **The app generator is not part of the jar.** The skills `joget-app-generator` and `joget-toolkit` describe command-line programs (the generator, a command-line client, console helpers to list, create and publish apps, screenshot checks) that are not part of this delivery. With the jar alone, the assistant cannot run them. It builds apps one artifact at a time with the `builder_*` tools, and it will tell you to create or publish the app in the console. If the assistant offers to run one of these programs, say that they are not available.
- **Skills are guidance, not enforcement.** A skill cannot give the assistant a permission that the tools do not have, and it cannot switch off the confirm gate, the lint gate or the page-wipe guard.
- **Skills from Joget ignore their scope.** The **Available For** setting and the associated tool templates have no effect over MCP. All active skills are visible to all MCP clients.
- **Global Central Instructions do not reach MCP.** Only per-composer Central Instructions are returned, by the prompts for each composer. Put global rules in a skill or in `CLAUDE.md`.
- **The composer skill tools read a different store.** `form_list_skills`, `form_invoke_skill` and their siblings do not reliably show the skills you add in **AI Central Config**. Use `joget_skills_list` and `joget_skill_read`.
- **Search ranks bundled skills first.** A search for one of your own terms can return bundled files before your skill. Name your skills in your project rules.
- **Prompts are started by you.** The assistant does not fetch a prompt. The same knowledge is in the skills, except the role prompts and instruction prompts.
- **Responses are cut at 40,000 characters.** The text says how to continue.
- **The skills describe one build of Joget.** Many were written against Joget DX 9.1. A property or class can differ on your version. Check with `builder_introspect` and `builder_plugin`.
- **The sample Acme skills are samples.** The brand values in `joget-acme` are marked where they must be confirmed against the customer's brand guide before a customer-facing release.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `joget_skills_list` shows only bundled skills | Agent Builder is not installed on this instance, or no skill is active in Joget. | Install Agent Builder, or add a skill in **AI Central Config** with status **Active**. |
| My skill is not listed | Its status is `draft` or `deprecated`; the file does not start with `---` or its header is not closed; the folder is in the wrong place. | Check the status and the first line of the file. Check the folder: `app_ai_skills/<name>/SKILL.md` in the Joget data folder. Look for a "Skipping ... frontmatter" line in the Joget log. |
| I added a skill file, and it is still not listed | The file is on another node, or in the folder of another tenant. | In a cluster use shared storage. With several tenants, use the folder of the tenant you connect to. |
| I read `joget-form-builder` and got the bundled text, not mine | The bundled skill keeps the plain name. | Read `central:joget-form-builder`. Better: give your skill a unique name. |
| `Unknown skill '<name>'` | The name is wrong, or the skill is not active. | Run `joget_skills_list`, copy the name. |
| `path` returns an error for my skill | A skill from Joget has no files. | Omit `path`; use `section`. |
| The assistant does not use my skill | The description does not say when to use it, or the assistant never searched. | Rewrite the `description` (start with "Use when ..."), and name the skill in `CLAUDE.md`. |
| A skill response ends with `TRUNCATED` | The text is longer than 40,000 characters. | Repeat with the `offset` shown, or read one section. |
| The prompts do not appear in the client | The client does not support prompts, or the server connected before the plugin finished starting. | Use the skill tools; reconnect the server; check the plugin is active. |
| My Central Instructions are not in `joget_global_instructions` | Global instructions are not served over MCP (see Limitations). | Put the rules in a skill or in `CLAUDE.md`. |
| The assistant tries to run `node tools/...` | A bundled skill describes a program that is not in the jar. | Tell it these tools are not available and to use the `builder_*` tools. |

## Where to go next

- [Composer MCP: Tool Reference](19-composer-mcp-tools.md): every builder tool, the error codes and call sequences.
- [Composer MCP: Tool Reference (continued)](19b-composer-mcp-tools-continued.md): the arguments of the four skill tools and the browser-connected tools.
- [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md): how skills and Central Instructions work inside Agent Builder.
- [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md): connect a client and verify the connection.
- [The Examples Repository](29-examples-repository.md): the sample skills, prompts and smoke test.
