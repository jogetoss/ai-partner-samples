---
id: 89b560d0-f49f-432b-9784-e6238f02a5ca
name: acme-app-standards
description: "Use when designing or building a whole Acme customer app, or its datalists, process or userview: artifact ids, process and activity naming, approval step rules, datalist column standard and userview menu names."
status: active
builtinVersion: 1
createdAt: 2026-11-05T00:00:00Z
composers:
  - org.joget.ai.agent.lib.composer.OrchestratorChat
  - org.joget.ai.agent.lib.composer.DatalistComposerChat
  - org.joget.ai.agent.lib.composer.ProcessComposerChat
  - org.joget.ai.agent.lib.composer.UiComposerChat
tools: []
---

# acme-app-standards
**Nav:** ArtifactIds - Process - Datalists - Userview - Checks

Acme app-level standards. They sit on top of the platform rules; where both speak, follow the stricter one. Field ids come from the form composer (they start with acme_); never invent a column that is not a real field id.

---

## ArtifactIds

When you name a form, datalist, process or userview, use the core entity in snake_case, at most 20 characters, with no generic suffix.

- Rule APP-1: `purchase_request` is right; `purchase_request_form` and `PurchaseRequestMgmt` are wrong.
- Rule APP-2: one record type is one table. Approver and read-only forms reuse `tbl_<first form id>` and the same acme_ field ids.
- Labels and display names are Bahasa Indonesia ("Permintaan Pembelian"); ids stay English.

---

## Process

When you build a process, name it after the entity and give every activity a verb-first English id and a Bahasa label.

- Rule APP-3: process id equals the entity id (`purchase_request`); process name is Bahasa ("Proses Permintaan Pembelian").
- Rule APP-4: activity ids are `submit`, `review_manager`, `review_head`, `review_finance`, `done`. Labels: `Pengajuan`, `Persetujuan Atasan`, `Persetujuan Kepala Departemen`, `Persetujuan Keuangan`.
- Rule APP-5: every approval step offers `Setujui` and `Tolak`, sets `acme_status` to `approved` or `rejected`, and requires `acme_remarks` on Tolak.
- Rule APP-6: the number of approval steps follows the Acme approval matrix. If the lookup skill is available, ask it for the matrix instead of assuming steps.
- A rejected request ends the process; the requester files a new one.
- You cannot map participants to people. Use the participant ids `requester`, `line_manager`, `department_head`, `finance_director` and list "map participants" as a manual follow-up.

---

## Datalists

When you build a datalist for a request form, show the same columns in the same order.

- Rule APP-7: columns in this order: `acme_request_no`, requester, department, the main amount (if any), `acme_status`, `dateCreated`. Titles are Bahasa: `No. Permintaan`, `Pemohon`, `Departemen`, `Jumlah (Rp)`, `Status`, `Tanggal Dibuat`.
- Rule APP-8: filters on `acme_status` and department; status shown as a coloured pill (draft grey, submitted blue, approved green, rejected red).
- Default sort: newest first. Page size 10.

---

## Userview

When you build the userview, group by audience and name every menu in Bahasa.

- Rule APP-9: categories `Permintaan Saya`, `Persetujuan`, `Dasbor`. Menus `Buat Permintaan Baru`, `Daftar Permintaan`, `Kotak Masuk Persetujuan`.
- A dashboard is only added when the request asks for one; tile titles are Bahasa.
- Stock Joget text (login page, standard buttons) stays English. Do not claim it is translated; mention it in the closing report.

---

## Checks

Before you finish, verify and fix.

1. APP-1 and APP-3: ids are snake_case, at most 20 characters, no generic suffix.
2. APP-5 and APP-6: approval steps match the matrix and every approval has Setujui and Tolak.
3. APP-7: datalist columns follow the standard order and real field ids.
4. APP-9: every category and menu name is Bahasa.
5. In your reply, name this skill as the source of the rules and list the artifact ids you created.
