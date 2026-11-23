---
id: 5d0c7a1e-3b84-4f26-9a6e-0c2f8d1b7e45
name: acme-standards-reference
description: "Use when you build an Acme app from outside Joget (Claude Code over Composer MCP) and need the Acme option lists: cost centres, departments, leave types, request status, approval matrix and formats. Same data as the in-product tool lookup_acme_standard."
status: active
builtinVersion: 1
createdAt: 2026-11-05T00:00:00Z
composers: []
tools: []
---

# acme-standards-reference
**Nav:** HowToUse - CostCentres - Departments - LeaveTypes - RequestStatus - ApprovalMatrix - Formats

Static copy of the data behind the Agent Builder tool lookup_acme_standard, for clients that cannot call that tool. The in-product composers never see this skill (its composers list is empty on purpose); Composer MCP lists every active skill whatever its scope. Keep it identical to `standards/acme-standards.json` of the tool bundle; a unit test checks the values.

---

## HowToUse

When a form needs one of these lists, copy it exactly: the first column is the stored value, the second the option label.

- Do not translate, reorder, add or drop items.
- In your reply, say you used this skill.

---

## CostCentres

Stored value, then label.

| Value | Label |
|---|---|
| CC-1000 | CC-1000 - Kantor Pusat Jakarta |
| CC-2000 | CC-2000 - Cabang Surabaya |
| CC-3000 | CC-3000 - Cabang Bandung |
| CC-4100 | CC-4100 - Pusat Data |

---

## Departments

Stored value, then label.

| Value | Label |
|---|---|
| finance | Keuangan |
| it | Teknologi Informasi |
| hr | Sumber Daya Manusia |
| operations | Operasional |
| procurement | Pengadaan |

---

## LeaveTypes

Stored value, then label.

| Value | Label |
|---|---|
| annual | Cuti Tahunan |
| sick | Cuti Sakit |
| maternity | Cuti Melahirkan |
| unpaid | Cuti di Luar Tanggungan |
| special | Cuti Khusus |

---

## RequestStatus

Stored value, then label, in lifecycle order.

| Value | Label |
|---|---|
| draft | Draf |
| submitted | Diajukan |
| approved | Disetujui |
| rejected | Ditolak |

---

## ApprovalMatrix

Purchase approval matrix, amounts in Rupiah. The first rule whose maximum amount is not below the request amount decides; build one approval step per approver, in order.

| Up to (Rp) | Approvers in order |
|---|---|
| 5.000.000 | line_manager |
| 50.000.000 | line_manager, department_head |
| above 50.000.000 | line_manager, department_head, finance_director |

---

## Formats

Use these when you set a field's data format, display format or default value.

| Item | Pattern | Note |
|---|---|---|
| date | dd/MM/yyyy | Day first. Show the same pattern the user types. |
| currency | Rp 1.250.000,50 | Rp before the amount, dot as thousands separator, comma as decimal separator. |
| request_number | REQ/{yyyy}/{00000} | Example REQ/2026/00017. Generate it with an ID generator field. |
