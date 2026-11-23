---
id: a17b14ca-8bdb-47fc-94bf-2266b38f6ee2
name: acme-standards-lookup
description: "Use when a form needs a dropdown, radio or checkbox for cost centres, departments, leave types or request status, or when a process needs the Acme approval matrix. Gives you the tool lookup_acme_standard, which returns the real Acme lists."
status: active
builtinVersion: 1
createdAt: 2026-11-05T00:00:00Z
composers:
  - org.joget.ai.agent.lib.composer.FormComposerChat
  - org.joget.ai.agent.lib.composer.OrchestratorChat
  - org.joget.ai.agent.lib.composer.ProcessComposerChat
tools:
  - lookup_acme_standard
---

# acme-standards-lookup
**Nav:** WhenToCall - UsingTheResult - Topics

Reading this skill adds the function lookup_acme_standard(topic) to your tools for the rest of this run. Call it BEFORE you add any option list or approval step; never invent cost centres, departments, leave types or approval thresholds.

---

## WhenToCall

When the request needs a list of choices or an approval rule, call lookup_acme_standard with the matching topic first, then build.

- Dropdown or radio for cost centre: topic `cost_centres`.
- Dropdown for department: topic `departments`.
- Leave type: topic `leave_types`. Request status: topic `request_status`.
- Number of approval steps or who approves last: topic `approval_matrix`.
- Date, currency or request number format: topic `formats`.
- One call per topic. Do not call it for fields that are free text.

---

## UsingTheResult

When the tool returns an option list, copy it into the field exactly as returned.

- Every item is `{value, label}`. Put `value` in the option's stored value and `label` in the option's label. Do not translate, reorder, add or drop items.
- Keep the order of the list.
- If the tool answers with an `error`, say so in your reply and fall back to what the request itself listed; do not make up values.
- In your reply, mention which topics you looked up.

---

## Topics

Topics the tool knows: cost_centres, departments, leave_types, request_status, approval_matrix, formats.

- `approval_matrix` returns `rules`: the first rule whose `maxAmount` is not below the request amount lists the approvers in order. Build one approval step per approver.
- An unknown topic returns an error that lists the valid topics; retry once with a valid one.
