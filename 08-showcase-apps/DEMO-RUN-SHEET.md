# Demo run sheet: the 45-minute live segment

The live part of the session "What's new in Joget AI" (deck `docs/What's new in Joget AI - Moxha session.pptx`).
It covers the four live slides: **18** (write a skill, watch it fire), **19** (the end-to-end prompt: specific
beats short), **20** (what it does not do, and tells you so) and **26** (same prompt with and without the skill and
tool), and shows the finished **Acme Procurement Portal** in between.

All prompts are in [PROMPTS.md](PROMPTS.md); this sheet refers to them by section number (for example "P 4.1").
Acme is a fictional company and all data is sample data.

## The three apps on the demo server

| App id | What it is | Used in |
|---|---|---|
| `acme_scratch` | Empty app for single-form demos. Forms `pr_demo_skill`, `pr_demo_tool` and `pr_demo_noskill` are created empty in it beforehand | Slides 18 and 26 |
| `acme_live` | Empty app for the live Orchestrator slice | Slide 19 |
| `acme_procure` | The finished app, imported from `acme_procure/`, seeded with the 12 rows of `seed/` | Slide 20 tour, every fallback |

Say once, at the start: "Where you see 'Pre-built', it was made earlier from the same prompts; long builds are
staged" (deck slide 3).

## Pre-flight checklist

### The day before (rehearsal)

- [ ] Joget DX 9.1 or later, Enterprise, starts cleanly; you can log in as the presenter user.
- [ ] Agent Builder installed. **Settings > AI Central Config**: AI credential and AI service saved, service set
      as current, **Test connection** passes. Spending limit set on the credential.
- [ ] Composer MCP jar installed; the Joget log shows `Builder API/MCP auth posture:`.
- [ ] The jar of `03-composer-custom-tool` uploaded in **Settings > Manage Plugins**.
- [ ] Tool template `lookup_acme_standard` (Display Name `Acme Standards Lookup`) exists on the **Tool Templates**
      tab, available for Form Composer, Orchestrator and Process Composer.
- [ ] Skills copied to `<JOGET_HOME>/wflow/app_ai_skills/`: `acme-app-standards`, `acme-standards-lookup`,
      `acme-standards-reference`. Keep **`acme-naming-conventions` outside the folder** (for example on the
      desktop) so you can copy it in live on slide 18.
- [ ] **Skills** tab: the three skills show **Active**; `acme-standards-lookup` shows the tool
      `lookup_acme_standard`.
- [ ] `JOGET_BUILDER_API_KEY` set in the server's `setenv` and in the presenter's shell; `-Djoget.builder.api.enable=app-create` set if the MCP prompt should create apps. Joget restarted after the change.
- [ ] `bash 05-composer-mcp/smoke-test.sh` prints PASS for every step; `claude mcp list` shows
      `joget-composer: connected`.
- [ ] `acme_procure.jwa` imported (**Settings > Import App**) and **published**.
- [ ] Seed loaded: `seed/seed-purchase-requests.sql` run against the Joget database (see its header). The list
      **Semua Permintaan** shows 12 rows.
- [ ] ID generator counter of `acme_request_no` set to 12 in `acme_procure`, so a new request gets
      `REQ/2026/00013`.
- [ ] Participants of process `purchase_request` in `acme_procure` mapped to the presenter user (demo shortcut, so
      one login can play requester and every approver). Say on stage that real apps map them to groups.
- [ ] Userview `purchase_portal` uses theme `acme_theme`; the landing page **Beranda** renders with hero, cards,
      four tiles and two charts; tile numbers match the list (3 Draf, 4 Diajukan, 3 Disetujui, 2 Ditolak).
- [ ] Full rehearsal of P 9.1 in **Autopilot** in a throw-away app. Note the time it took: __________ min.
- [ ] Rehearse slides 18 and 26 once each, then delete the fields so the forms are empty again.
- [ ] Capture the fallback screenshots listed at the end of this sheet.

### 60 minutes before

- [ ] Server up, logged in as the presenter. Browser zoom 125 percent, bookmarks bar hidden, notifications off.
- [ ] Forms `pr_demo_skill`, `pr_demo_tool` and `pr_demo_noskill` in `acme_scratch` are empty.
- [ ] `acme_live` is empty (delete artefacts from the rehearsal).
- [ ] `acme-naming-conventions` is NOT in `app_ai_skills` yet; the other three are Active.
- [ ] Send one throw-away prompt to the Form Composer ("What can I do here?") to warm up the AI service.
- [ ] Browser tabs, left to right:
      1. Deck (presenter view).
      2. File explorer or terminal open at `<JOGET_HOME>/wflow/app_ai_skills/`.
      3. **Settings > AI Central Config**, tab **Skills**.
      4. Form Builder: `acme_scratch` / `pr_demo_skill`.
      5. App Composer builders page of `acme_live`.
      6. **Monitor > Agent Execution Audit Trail**.
      7. Userview `acme_procure` / `purchase_portal`, menu **Beranda**.
      8. Process Builder of `acme_procure` / `purchase_request`.
      9. Form Builder: `acme_scratch` / `pr_demo_tool`, and 10. `pr_demo_noskill`.
- [ ] `PROMPTS.md` open in an editor for copy and paste (never type prompts live).
- [ ] Claude Code started in the repository root in a terminal, if you plan the optional MCP slice.

### 10 minutes before

- [ ] AI service answers (send "What can I do here?" once more).
- [ ] Every Composer panel starts with **New chat**.
- [ ] Screenshots folder open in a separate window, ready for a fallback.

## Minute by minute

Times are from the start of the segment. "Say" is one line; keep the rest for questions.

### Block A: slide 18, write a skill and watch it fire (00:00 to 10:00)

| Time | Slide | Do (exact clicks) | Say | Audience sees | Fallback |
|---|---|---|---|---|---|
| 00:00 | 18 | Show slide 18. | "A skill is one Markdown file; let's add one and watch a composer pick it up." | The live steps and the prompt | |
| 01:00 | 18 | Tab 2: show `app_ai_skills` without `acme-naming-conventions`. Copy the folder in (drag, or `Copy-Item -Recurse <desktop>\acme-naming-conventions "$env:JOGET_HOME\wflow\app_ai_skills\"`). Open `SKILL.md` for 20 seconds: front matter, `composers:`, the `## FieldIds` section. | "Files, not rows: you version them in git and copy them between servers. No restart." | The folder appears; the skill file | Folder copy blocked: use **Add Skill** on the Skills tab with the body of the file |
| 02:30 | 18 | Tab 3: **Refresh** on the Skills tab. Point at the card: status **Active**, available for **AI Composer - Form Builder Agent**. | "It's live. Only its name and one-line description go into each request." | The new skill card | Card missing: check the path is `app_ai_skills/acme-naming-conventions/SKILL.md` and that the file starts with `---` |
| 03:30 | 18 | Tab 4: form `pr_demo_skill`. **Ctrl+J**, **New chat**, paste **P 4.1**, Enter. | "A plain prompt; it says nothing about prefixes or Bahasa." | Fields appear on the canvas one by one | Run hangs over 2 minutes: **Stop**, **New chat**, send again once; then fallback screenshot S2 |
| 05:30 | 18 | When the run ends: point at `acme_requester_name`, `Nama Pemohon`, the status options `draft` / `Draf`. Press **Confirm**, then **Save**. | "Ids got the prefix, labels are Bahasa, stored values stayed English: that is the skill." | The finished form | Ids without prefix: say "it did not fire this time", open screenshot S2 or the pre-built form `purchase_request` in `acme_procure` |
| 07:00 | 18 | Tab 6: **Agent Execution Audit Trail**, newest **Form Composer Agent** run, **Agent Node Viewer**. Click the **Skill Invoker (Composer)** node: first the index, then the section `FieldIds`. | "Two-step fetch: index first, then only the section it needs." | The trace with `invoke_skill` | Trace slow to load: screenshot S1, or `manual/screenshots/22-node-viewer-invoke-skill.png` |
| 09:00 | 18 | Back to the deck. | "The proper with-and-without comparison comes on slide 26, with a tool as well." | | Ahead of time? Do step 5 now: set the skill to **Draft**, fresh chat, same prompt in `pr_demo_noskill` |

### Block B: slide 19, specific beats short (10:00 to 24:00)

| Time | Slide | Do (exact clicks) | Say | Audience sees | Fallback |
|---|---|---|---|---|---|
| 10:00 | 19 | Show slide 19. | "The Orchestrator only knows what is in the prompt and the skills; a thin prompt gets defaults." | Thin and rich prompts side by side | |
| 11:00 | 19 | Tab 5: builders page of `acme_live`. **Ctrl+J**, **New chat**. Mode button: **Create**, then **Plan**. Paste the **thin** prompt of **P 3**, Enter. | "Plan mode: one call, nothing is built." | A generic plan: English ids, one approval step | Plan fails: screenshot S4 |
| 13:00 | 19 | Scroll through the plan; point at the invented statuses and the single approval. Click **No** on the plan card. **New chat**. Still **Plan**: paste **P 9.1**, Enter. | "Now the rich one: ids, tables, Bahasa labels and the approval matrix." | The plan card with Overview, Artifacts to Create, Design Decisions | Plan fails or takes over 3 minutes: screenshot S3 |
| 16:00 | 19 | Point at: `acme_` field ids, `tbl_purchase_request` shared by both forms, three review steps on `acme_total_amount`, the Bahasa menus. Click **No** on the plan card. Switch the mode to **Create > Confirm**. Paste **P 9.2**, Enter. | "We'll build a slice live; the whole app takes many minutes, so the rest is pre-built from the same prompt." | First approval card: "Create form 'purchase_request' ..." | |
| 17:00 | 19 | **Approve & run** on the form card. While it builds, open the run in tab 6 and point at `invoke_skill` and the `lookup_acme_standard` calls. | "The skill handed the Orchestrator the lookup tool; the lists come from Acme's data, not the model." | Result card "form ... created", the trace | Composer times out (300 s): skip to 22:00 and open `purchase_request` in `acme_procure` |
| 20:00 | 19 | **Approve & run** on the datalist card. When the result card shows, click **Open in Datalist Builder**. | "Same table as the pre-built app on this server, so the list fills with the sample data right away." | The list with 12 rows, status pills, Bahasa titles | List not built: open **Semua Permintaan** in tab 7 |
| 23:00 | 19 | Back to the Orchestrator panel; scroll to the completion report. | "And it tells you what it did not do." (leads into slide 20) | Summary, pending items | Report missing: slide 20 covers it |

### Block C: slide 20 and the finished app (24:00 to 36:00)

| Time | Slide | Do (exact clicks) | Say | Audience sees | Fallback |
|---|---|---|---|---|---|
| 24:00 | 20 | Show slide 20. Point at the pending items in the report from 23:00. | "Participants, seed data, SMTP, group ids, per-menu permissions and live matrix routing stay with you, and the report lists them." | The six manual items | |
| 26:00 | 20 | Tab 7: `acme_procure` / `purchase_portal`, menu **Beranda**. Say "Pre-built from the same prompts" as the page opens. | "This is the full app: theme, landing page, lists, process, made from the prompts you take home." | Hero, three quick-action cards, four KPI tiles, two charts, the matrix strip | Theme broken or page blank: switch the userview to the default theme in the Userview Builder and show screenshot S9 |
| 28:00 | 20 | Click the tile **Diajukan**, then the menu **Semua Permintaan**. Filter **Departemen = Teknologi Informasi**. | "Tiles count real rows; pills are a Condition formatter; codes stay English, labels are Bahasa." | The filtered list with pills | Tiles show the total for every status: say "tile filter", show the list directly |
| 29:30 | 20 | Click **Buat Permintaan Baru**. Pick **Teknologi Informasi**, **CC-4100 - Pusat Data**, description "Penggantian 10 unit UPS ruang server", a date, amount `20000000`. Submit. | "Rp 20 million: the matrix says line manager, then department head." | `REQ/2026/00013` generated, status Diajukan | Submit fails: open the draft row `REQ/2026/00011` (same amount) instead |
| 31:30 | 20 | **Kotak Masuk Persetujuan**: open the task (**Persetujuan Atasan**), add a remark, **Setujui**. Refresh the inbox: the next task **Persetujuan Kepala Departemen** is there. **Setujui**. | "Two approvals because of the amount; a request up to Rp 5 million would stop after the first." | Two tasks in sequence, then Disetujui in the list | Inbox empty: participants not mapped; open tab 8 and show **Participants** as the manual step it is |
| 34:00 | 20 | Tab 8: Process Builder. Click the route after `review_manager`: rules on `status`, then on `total_amount <= 5000000`. Click **Participants**. | "Thresholds were written into the routes at build time; mapping people is yours." | The routes and the participant panel | |
| 35:00 | 20 | Optional, only if on time: terminal, Claude Code, paste **P 10.1**. | "Same skills, outside Joget: Claude Code reads them over Composer MCP." | `builder_status`, the skills of kind central, the matrix section | Skip if later than 35:00; MCP not connected: screenshot S10 |

### Block D: slides 25 and 26, with and without the skill and tool (36:00 to 43:00)

| Time | Slide | Do (exact clicks) | Say | Audience sees | Fallback |
|---|---|---|---|---|---|
| 36:00 | 25, 26 | Show slide 25 for 30 seconds, then slide 26. | "Tool plugin, tool template, skill: the skill hands the tool to the composer for that run." | The chain, then the table | |
| 37:00 | 26 | Tab 9: form `pr_demo_tool`. **Ctrl+J**, **New chat**, paste **P 4.2**, Enter. | "With the skill and the tool." | Fields with Bahasa labels and the real Acme lists | Screenshot S6 and S7 |
| 39:00 | 26 | Tab 3: set `acme-naming-conventions` and `acme-standards-lookup` to **Draft**. Tab 10: form `pr_demo_noskill`, **Ctrl+J**, **New chat**, same prompt, Enter. | "Same prompt, same model, skills off." | English ids, invented cost centres and statuses | Screenshot S8 |
| 41:00 | 26 | Put the two forms side by side (two windows). Tab 6: the trace of the first run: `invoke_skill`, then `lookup_acme_standard` three times. | "One run each on a beta: an example, not a benchmark." | Both forms and the trace | Compare with `04-custom-skills/expected-result/*.fields.txt` |
| 42:30 | 26 | Tab 3: set both skills back to **Active**. | (No line; do it quietly.) | | Do not skip this step |

### Wrap (43:00 to 45:00)

| Time | Do | Say |
|---|---|---|
| 43:00 | Show the repository folder `08-showcase-apps/`. | "Every prompt you saw, the pre-built app and the seed data are in the repo; replace Acme with your own standards." |
| 44:00 | Questions, or hand over to the next segment. | |

If you run late, cut in this order: the MCP slice (35:00), the second approval step (31:30, describe it instead),
the thin-prompt plan (11:00, show screenshot S4), the trace on slide 18 (07:00, show screenshot S1).

## If something breaks

| Symptom | Likely cause | Do this on stage | Fix afterwards |
|---|---|---|---|
| Panel shows **No AI Service Configured** | No current service | Switch to the pre-built app and screenshots | Set the service as current in AI Central Config, **New chat** |
| A run never ends | Model slow or rate-limited | **Stop**, **New chat**, send once more; otherwise fallback | Choose another service in the service picker |
| "Maximum Sessions Reached" or "Session Duration Exceeded" | Session limits | **New chat**; if it repeats, fallback | Raise the limits in **AI Session Monitor** |
| Composer replies in text and changes nothing | The model answered instead of calling functions | **Revert**, **New chat**, resend with the target id | Use a stronger model |
| Field ids without `acme_` | Skill not read in that run | Show screenshot S2 or the pre-built form | Check status Active, `composers:` and the description |
| Invented cost centres despite the skill | Tool template missing or not linked | Show screenshot S6 | Check the template name `lookup_acme_standard` and the skill card |
| Skill card does not appear after copying | Wrong folder level, BOM or empty first line | Use **Add Skill** with the file body | Path `app_ai_skills/<name>/SKILL.md`, first line `---` |
| Orchestrator result card says "Built but NOT saved" | Save failed after repairs | Say so; it is the honest behaviour; open the pre-built artefact | Rebuild with the standalone composer prompt |
| A `_2` copy appears | Follow-up without the existing id | Ignore on stage | Delete it; name existing ids in follow-ups |
| Datalist shows no rows | Seed not loaded or other table | Open **Semua Permintaan** in `acme_procure` | Run the seed SQL; check the table name |
| Tiles all show 12 | Tile filter parameter wrong | Show the list instead | Point the tiles at `purchase_requests` with the filter on `acme_status` |
| Menus answer 404 | App not published | Publish it (App Composer, one click) | |
| Inbox empty after submit | Participants not mapped | Show **Participants**: "this is the manual step from slide 20" | Map lanes to the presenter user for the demo |
| New request number repeats `REQ/2026/00001` | ID counter not set after seeding | Carry on; mention it is a seed-data step | Set the counter to 12 |
| Every page errors after a theme change | Primary colour not six-digit hex | Userview Builder: switch to the default theme | **Revert** in the Theme Builder, set `#0B5FA5` |
| Fonts look plain | No internet for the Inter import | Carry on; it falls back to `system-ui` | Host the font locally or accept the fallback |
| `claude mcp list` shows failed | Key unset in the shell, or server down | Skip the MCP slice, show screenshot S10 | Export `JOGET_BUILDER_API_KEY`, rerun the smoke test |
| Projector cuts off the panel | Resolution | Dock the panel, zoom the browser out | |
| `acme_procure` itself is broken | Import or data problem | Import `acme_procure/acme_procure.jwa` again (Settings > Import App, overwrite), publish, rerun the seed SQL (it is re-runnable) | |

## Fallback screenshots to capture at the rehearsal

Save them in `08-showcase-apps/screenshots/` (create the folder) and keep the window open during the session.
Until they exist, the placeholders below mark where each one is used.

| Id | File | Shows | Used at |
|---|---|---|---|
| S1 | `[SCREENSHOT: s1-trace-invoke-skill.png]` | Agent Node Viewer: **Skill Invoker (Composer)** with the index and section `FieldIds` | 07:00 |
| S2 | `[SCREENSHOT: s2-slide18-form-result.png]` | Form with `acme_` ids and Bahasa labels after P 4.1 | 03:30, 05:30 |
| S3 | `[SCREENSHOT: s3-plan-rich.png]` | Orchestrator plan card for P 9.1 | 13:00 |
| S4 | `[SCREENSHOT: s4-plan-thin.png]` | Orchestrator plan card for the thin prompt | 11:00 |
| S5 | `[SCREENSHOT: s5-completion-report.png]` | Completion report with pending items | 23:00, 24:00 |
| S6 | `[SCREENSHOT: s6-trace-lookup-tool.png]` | Trace: `invoke_skill`, then `lookup_acme_standard` x3 | 37:00, 41:00 |
| S7 | `[SCREENSHOT: s7-slide26-with.png]` | Form fields with skill and tool | 37:00 |
| S8 | `[SCREENSHOT: s8-slide26-without.png]` | Form fields without | 39:00 |
| S9 | `[SCREENSHOT: s9-landing-page.png]` | The Beranda landing page of `acme_procure` | 26:00 |
| S10 | `[SCREENSHOT: s10-mcp-warmup.png]` | Claude Code output of P 10.1 | 35:00 |

Already in the repository as a last resort: `manual/screenshots/22-node-viewer-invoke-skill.png`,
`manual/screenshots/13-plan-card.png`, `manual/screenshots/13-approval-card.png` and
`manual/screenshots/12-form-composer-result.png`.
