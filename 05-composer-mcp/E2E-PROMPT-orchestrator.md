# Prompt: build the same app with the Orchestrator in Joget

Before you start:

- Agent Builder is installed and an AI service is active in **Settings > AI Central Config**.
- The skills `acme-naming-conventions`, `acme-app-standards` and `acme-standards-lookup` of
  `04-custom-skills` are installed and active.
- The plugin jar of `03-composer-custom-tool` is installed and the tool template `lookup_acme_standard` exists.

Steps:

1. Create an empty app with the id `purchase_mgmt`.
2. Open the app in the App Composer and open the Orchestrator chat panel (Ctrl+J).
3. Paste the prompt below the line. Review the plan the Orchestrator proposes before you let it build.

This prompt asks for a whole app. Expect a run of many minutes. Do not start a second Orchestrator run in the same
app while the first one is running.

---

Build a small purchase-request app in Bahasa Indonesia.

First read the skills acme-standards-lookup, acme-naming-conventions and acme-app-standards and call lookup_acme_standard for cost_centres, departments, request_status and approval_matrix. Use the returned values exactly.

Ids: all artefact ids snake_case, at most 20 characters, no generic suffix. Form `purchase_request` (table tbl_purchase_request); every other form over the same record, such as `purchase_approval`, must set tableName to tbl_purchase_request and reuse the same field ids. Field ids start with acme_. Ids and stored values English; labels, menus and sample values Bahasa.

Form purchase_request: acme_request_no (ID generator REQ/yyyy/00000, read-only), acme_requester_name (default #currentUser.fullName#, read-only), acme_department, acme_cost_centre, acme_item_description (required textarea), acme_total_amount (Rp, required), acme_status (default draft).
Datalist purchase_requests: request no, requester, department, amount, status pill, created date; filters status and department.
Process purchase_request: approval steps follow the approval matrix (Rp 20.000.000 needs line manager and department head); each approval offers Setujui and Tolak and sets acme_status.
Userview purchase_portal: categories Permintaan Saya, Persetujuan, Dasbor; menus per the standards skill; a dashboard with one tile per status.

Keep chat output short. When finished, report: skills and tools you used, a table of artefact ids (type, id, table), and the manual follow-ups.

---

Where to look afterwards:

- **Monitor > Agent Execution Audit Trail**: the runs show the calls of `invoke_skill` and of **Acme Standards Lookup**.
- The Orchestrator's completion message lists the artefacts it created and the pending manual steps. Mapping the
  process participants to users is always one of them.
