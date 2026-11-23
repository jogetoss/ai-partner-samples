---
id: 1efae2c1-084b-4819-b4c8-5cf8e2d9840c
name: acme-naming-conventions
description: "Use when building or editing any form for an Acme app: field id prefix acme_, Bahasa Indonesia labels with English stored values, and date, currency and request-number formats."
status: active
builtinVersion: 1
createdAt: 2026-11-05T00:00:00Z
composers:
  - org.joget.ai.agent.lib.composer.FormComposerChat
tools: []
---

# acme-naming-conventions
**Nav:** FieldIds - Labels - Formats - Checks

Acme naming rules for forms. Apply them to every field you add or change. They govern field ids, labels and formats; the form id and table name keep the platform rules (snake_case, at most 20 characters, table tbl_ plus the form id).

---

## FieldIds

When you add any field, its id starts with acme_ followed by short snake_case English words.

- Rule ITAS-1: every field id matches `^acme_[a-z0-9_]+$`. `acme_requester_name` is right. `nama_pemohon` (no prefix, Bahasa) and `acme_namaPemohon` (camelCase) are wrong.
- Keep the part after the prefix to two or three words: `acme_cost_centre`, `acme_total_amount`.
- Section ids and the form id do NOT take the prefix.
- Never rename a field that already exists in the form.
- A status field is always called `acme_status`, so lists and processes can rely on it.

---

## Labels

When you set a label, placeholder, help text, section title or option label, write it in Bahasa Indonesia; the stored option value stays English.

- Rule ITAS-2: label `Nama Pemohon`, never `Requester Name`. Section title `Data Pemohon`, not `Requester`.
- Rule ITAS-3: an option's stored value is an English snake_case code and only its label is Bahasa. Value `approved`, label `Disetujui`. Never translate the value.
- Use the polite neutral register ("Anda"), no slang. Buttons you control read `Simpan`, `Ajukan`, `Batal`.
- Mark mandatory fields with the validator, not by adding "(wajib)" to the label.

---

## Formats

When a field holds a date, an amount or a request number, use the Acme formats.

- Rule ITAS-4: dates are dd/MM/yyyy in the field's display format. Do not mix the Java pattern for the data format with the jQuery pattern for display; follow the joget-form-builder DatePicker section for each.
- Rule ITAS-5: money is a text field with a numeric validator, label ends with `(Rp)`; amounts display as `1.250.000,50`.
- Rule ITAS-6: a request number looks like `REQ/2026/00017`. Add it as an ID generator field, read-only, named `acme_request_no`. The user never types it.
- Default the requester name to `#currentUser.fullName#` and make it read-only.

---

## Checks

Before you finish, verify each rule and fix the form if one fails.

1. ITAS-1: every new field id starts with acme_ and is snake_case.
2. ITAS-2: every visible text is Bahasa Indonesia.
3. ITAS-3: option values are English codes, option labels are Bahasa.
4. ITAS-4 to ITAS-6: date, currency and request number follow the formats above.
5. In your reply, list the ids you created and name this skill as the source of the rules.
