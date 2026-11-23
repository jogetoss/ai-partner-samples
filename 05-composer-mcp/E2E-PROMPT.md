# Prompt: build the app with Claude Code over Composer MCP

Before you start:

- Composer MCP is connected and the key is set (see the [README](README.md)).
- The four skills of `04-custom-skills` are installed and active.
- The app `purchase_mgmt`, version 1, exists in Joget, or `app-create` is enabled on the server.
- Start a new Claude Code session.

This prompt asks for a whole app. Expect a long run. For a shorter first test, keep steps 1, 3, 5 and 6 only.

Paste everything below the line.

---

Build a complete small Indonesian purchase-request app on the Joget connected through MCP server `joget-composer`. Work in app `purchase_mgmt`, version `1` (create it with builder_app if it does not exist).

First, without chatting: call builder_status, then joget_skills_list. Read the skills `acme-naming-conventions`, `acme-app-standards` and `acme-standards-reference` (index first, then the sections you need) and the bundled joget-form-builder, joget-datalist-builder, joget-process-builder and joget-userview-builder skills. Follow them; they override your defaults.

Artefact id rules (hard): every form, datalist, process and userview id is snake_case, at most 20 characters, no suffix such as form/list/management. Table name is tbl_<formId> (the form id `purchase_request` gives table `tbl_purchase_request`). All approval forms reuse that same table and the same field ids. Field ids start with `acme_`. Ids and stored values are English; every label, section title, menu and seed value is Bahasa Indonesia.

Build, in this order:
1. Form `purchase_request` (Permintaan Pembelian): `acme_request_no` (ID generator, read-only, REQ/yyyy/00000), `acme_requester_name` (default #currentUser.fullName#, read-only), `acme_department` (use the departments from the reference skill), `acme_cost_centre` (cost centres from the reference skill), `acme_item_description` (textarea, required), `acme_total_amount` (Rp, numeric, required), `acme_status` (request status list from the reference skill, default draft).
2. Approval form over the same table: `purchase_approval` (read-only request data plus `acme_remarks` and Setujui/Tolak).
3. Datalist `purchase_requests` with the column standard from acme-app-standards, filters on status and department, status as coloured pill.
4. Process `purchase_request` using the approval matrix: Rp 20.000.000 must resolve to line manager then department head; build the steps for the whole matrix with conditions on `acme_total_amount`. Set `acme_status` on every outcome. Participants cannot be mapped to people: use the participant ids from the skill and list that as a manual follow-up.
5. Userview `purchase_portal` with categories and menus named as in the skill: create-request, request list, approval inbox, and a dashboard with tiles for each status (tile titles in Bahasa).
6. Seed 5 realistic Indonesian sample rows with builder_data (confirm:true is allowed for seed rows only).

Then run builder_lint on everything and builder_verify_app; fix what they report.

Output rules: do not narrate; no more than one short line between tool calls. At the end print exactly: (a) "Skills used:" with the names you actually read, (b) a table of created artefact ids (type, id, table), (c) "Manual follow-ups:" (participant mapping, SMTP if email is used, English stock texts), (d) lint/verify result.
