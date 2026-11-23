# Prompts: build the Acme Procurement Portal

Ready-to-paste prompts that build **Acme Procurement Portal** (Portal Pengadaan Acme), a purchase-request app
with a matrix-driven approval, live with the AI Composers and the Orchestrator in Joget, and the same app from
Claude Code over Composer MCP. Acme is a fictional company; every value below is sample data from
`03-composer-custom-tool/bundle/src/main/resources/standards/acme-standards.json`.

The finished, importable version of this app is `acme_procure/` (app id `acme_procure`, version 1). Use it as the
"pre-built from the same prompts" app and as the fallback when a live run fails. The run sheet for the session is
[DEMO-RUN-SHEET.md](DEMO-RUN-SHEET.md). Sample data is in [seed/](seed/).

## Contents

1. [Before you paste anything](#1-before-you-paste-anything)
2. [The app at a glance (ids, fields, values)](#2-the-app-at-a-glance)
3. [Thin prompt versus rich prompt (deck slide 19)](#3-thin-prompt-versus-rich-prompt)
4. [Form Composer prompts (deck slides 18 and 26)](#4-form-composer-prompts)
5. [List Composer prompts](#5-list-composer-prompts)
6. [Process Composer prompt](#6-process-composer-prompt)
7. [UI Composer prompt: the userview](#7-ui-composer-prompt-the-userview)
8. [Theme and landing page prompts](#8-theme-and-landing-page-prompts)
9. [Orchestrator prompts (whole app)](#9-orchestrator-prompts)
10. [Claude Code over Composer MCP](#10-claude-code-over-composer-mcp)
11. [Tips: failed runs, staging a long build, what stays manual](#11-tips)
12. [Appendix: exact landing-page markup and theme CSS](#12-appendix-exact-markup-and-css)

## 1. Before you paste anything

- Agent Builder is installed and an AI service is active and tested in **Settings > AI Central Config**. Use a
  strong tool-calling model; the quality of a whole-app build depends on it.
- The four skills of `04-custom-skills` are in `<JOGET_HOME>/wflow/app_ai_skills/` and show **Active** on the
  **Skills** tab: `acme-naming-conventions`, `acme-app-standards`, `acme-standards-lookup`,
  `acme-standards-reference`.
- The jar of `03-composer-custom-tool` is uploaded and the tool template `lookup_acme_standard` exists
  (**Settings > AI Central Config > Tool Templates**). The skill card of `acme-standards-lookup` shows it.
- For section 10: Composer MCP is installed, `JOGET_BUILDER_API_KEY` is set on the server and in your shell, and
  `claude mcp list` shows `joget-composer: connected`.
- An app exists to build into. On a server where the pre-built `acme_procure` is already imported, build the live
  version into another app (for example `acme_live`) and replace `acme_procure` in the prompts with that id.

> Table names are shared by all apps on one Joget server. A live form `purchase_request` in `acme_live` uses the
> same table `tbl_purchase_request` as the pre-built app. That is deliberate for the demo: the list you build live
> fills at once with the seed rows. If you want the live build isolated, give the form a different id and table.

## 2. The app at a glance

Every prompt below uses these exact ids and values. Keep them identical across prompts; lists, routes and tiles
match stored values exactly.

### Artefacts

| Type | Id | Display name (Bahasa) | Table |
|---|---|---|---|
| App | `acme_procure` | Portal Pengadaan Acme (Acme Procurement Portal) | |
| Form | `purchase_request` | Permintaan Pembelian | `tbl_purchase_request` |
| Form | `purchase_approval` | Persetujuan Permintaan Pembelian | `tbl_purchase_request` (same table, same field ids) |
| Datalist | `purchase_requests` | Semua Permintaan Pembelian | over `purchase_request` |
| Datalist | `my_requests` | Permintaan Saya | over `purchase_request`, only the current user's rows |
| Process | `purchase_request` | Proses Permintaan Pembelian | |
| Userview | `purchase_portal` | Portal Pengadaan Acme | |
| Theme | `acme_theme` | Tema Acme | |

### Fields of `purchase_request`

| Field id | Label | Type | Rule |
|---|---|---|---|
| `acme_request_no` | No. Permintaan | ID generator | `REQ/{yyyy}/{00000}`, read-only, example `REQ/2026/00017` |
| `acme_requester_name` | Nama Pemohon | Text | default `#currentUser.fullName#`, read-only |
| `acme_department` | Departemen | Select | departments list, required |
| `acme_cost_centre` | Pusat Biaya | Select | cost centres list, required |
| `acme_item_description` | Deskripsi Barang/Jasa | Text area | required, max 1000 characters |
| `acme_needed_date` | Tanggal Dibutuhkan | Date | `dd/MM/yyyy`, required |
| `acme_total_amount` | Total Jumlah (Rp) | Text | numeric validator, required, shown as `1.250.000` |
| `acme_status` | Status | Select | request status list, default `draft`, read-only |
| `acme_remarks` | Catatan Persetujuan | Text area | read-only here; editable on `purchase_approval`, required on Tolak |

### Option lists (stored value = English code, label = Bahasa)

| List | Values and labels |
|---|---|
| Departments | `finance` Keuangan, `it` Teknologi Informasi, `hr` Sumber Daya Manusia, `operations` Operasional, `procurement` Pengadaan |
| Cost centres | `CC-1000` CC-1000 - Kantor Pusat Jakarta, `CC-2000` CC-2000 - Cabang Surabaya, `CC-3000` CC-3000 - Cabang Bandung, `CC-4100` CC-4100 - Pusat Data |
| Request status | `draft` Draf, `submitted` Diajukan, `approved` Disetujui, `rejected` Ditolak |

### Approval matrix and status pills

| Total amount (Rp) | Approvers, in order | Activities |
|---|---|---|
| up to 5.000.000 | line_manager | `review_manager` |
| 5.000.001 to 50.000.000 | line_manager, department_head | `review_manager`, `review_head` |
| above 50.000.000 | line_manager, department_head, finance_director | `review_manager`, `review_head`, `review_finance` |

| Status | Pill text | Pill background | Meaning |
|---|---|---|---|
| `draft` Draf | `#4B5563` | `#EEF1F4` | grey: saved, not submitted |
| `submitted` Diajukan | `#0B5FA5` | `#E6F0FA` | blue: waiting for an approver |
| `approved` Disetujui | `#14653F` | `#E7F5EE` | green: approved by the last required approver |
| `rejected` Ditolak | `#A93226` | `#FBEAEA` | red: rejected, process ended |

All four text and background pairs pass WCAG AA (4.5 to 1 or better) for small text.

## 3. Thin prompt versus rich prompt

The contrast for deck slide 19 ("specific beats short"). Paste each into the Orchestrator in **Create > Plan**
mode so only one model call is spent, and compare the two plans side by side.

**Thin:**

```text
Create a purchase request app with an approval workflow.
```

What you get: generic English field ids (`requester`, `amount`), invented departments and statuses such as
`pending_approval`, one approval step whatever the amount, a list and a userview with English names.

**Rich:**

```text
Build app 'acme_procure' v1 in Bahasa Indonesia. Form 'purchase_request' (Permintaan Pembelian), tableName
'tbl_purchase_request': acme_request_no (ID generator REQ/yyyy/00000, read-only), acme_requester_name (default
#currentUser.fullName#, read-only), acme_department and acme_cost_centre (dropdowns from lookup_acme_standard),
acme_item_description (textarea, required), acme_total_amount (Rp, numeric, required), acme_status (draft,
submitted, approved, rejected; default draft). Approval form 'purchase_approval' on the same table. Process
'purchase_request': approval steps from the Acme approval matrix (up to Rp 5.000.000 line manager; up to
Rp 50.000.000 plus department head; above that plus finance director), Setujui and Tolak on every step.
Datalist 'purchase_requests' with status pills. Userview 'purchase_portal' with categories Permintaan Saya,
Persetujuan and Dasbor. Ids and stored values stay English; every label is Bahasa.
```

What you get: the `acme_` field ids, the real Acme lists, three approval levels driven by
`acme_total_amount`, Bahasa labels with English stored values, and named categories.

Say it out loud: quote ids and tables; say which values stay English and which labels are Bahasa; name the roles
and the approval path. The composer only knows what is in the prompt, the builder and the skills.

## 4. Form Composer prompts

Open the form in the Form Builder, press **Ctrl+J** (**Cmd+J** on macOS) to open **AI Composer - Form Builder
Agent**, press **New chat**, and paste.

### 4.1 Slide 18: write a skill and watch it fire

The deck's own prompt. Use it after you copied `acme-naming-conventions` into `app_ai_skills`, in an empty form
(for example `pr_demo_skill`, table `tbl_pr_demo_skill`, in a scratch app):

```text
Create a purchase request form with requester name (required), department (dropdown: Finance, IT, Operations),
item description (textarea) and status (dropdown: draft, submitted, approved, rejected).
```

Expect: field ids `acme_requester_name`, `acme_department`, `acme_item_description`, `acme_status`; labels
`Nama Pemohon`, `Departemen`, `Deskripsi Barang/Jasa`, `Status`; stored values still `draft`, `approved`. If
`acme-standards-lookup` is active as well, the department list may come back as the five Acme departments instead
of the three you typed: the skill tells the model to use the real list.

### 4.2 Slide 26: the same prompt with and without the skill and the tool

Send exactly this, once with the skills active and once with `acme-naming-conventions` and
`acme-standards-lookup` set to **Draft** (fresh chat, empty form, same AI service each time):

```text
Create a purchase request form with requester name (required), department (dropdown), cost centre (dropdown),
item description (textarea), total amount and status.
```

| | With skill and tool | Without |
|---|---|---|
| Field ids | `acme_requester_name`, `acme_department` ... | `requester_name`, `department` ... |
| Labels | Nama Pemohon, Departemen, Pusat Biaya | Requester Name, Department, Cost Centre |
| Departments | Keuangan, Teknologi Informasi, Sumber Daya Manusia, Operasional, Pengadaan | invented |
| Cost centres | CC-1000 to CC-4100 from the standards | invented, for example `CC001 - Corporate` |
| Status | draft, submitted, approved, rejected (Draf, Diajukan, Disetujui, Ditolak) | for example `pending_approval`, `cancelled` |
| Trace | `invoke_skill`, then `lookup_acme_standard` for departments, cost_centres, request_status | no skill and no tool calls |

Reference outputs: `04-custom-skills/expected-result/with-skills.fields.txt` and `without-skills.fields.txt`.

### 4.3 The showcase form, with the skills and the tool active

The rich form prompt. Open a new form with id `purchase_request` and table `tbl_purchase_request`.

```text
Build form 'purchase_request' (Permintaan Pembelian) on tableName 'tbl_purchase_request' for Acme.
Purpose: Acme employees request goods or services; approvers review the same record later on a separate form.

First read the skills acme-naming-conventions and acme-standards-lookup, then call lookup_acme_standard for
departments, cost_centres, request_status and formats. Copy the returned values and labels exactly, in order.
All labels, placeholders, section titles and help texts in Bahasa Indonesia; ids and stored option values stay
English.

Section 'Data Pemohon' (2 columns):
- acme_request_no, label 'No. Permintaan': ID generator, pattern REQ/{yyyy}/{00000}, read-only.
- acme_requester_name, label 'Nama Pemohon': text, default #currentUser.fullName#, read-only.
- acme_department, label 'Departemen': select from the departments lookup, required, placeholder 'Pilih departemen'.
- acme_cost_centre, label 'Pusat Biaya': select from the cost_centres lookup, required, placeholder 'Pilih pusat biaya'.

Section 'Detail Permintaan' (1 column):
- acme_item_description, label 'Deskripsi Barang/Jasa': textarea, required, max 1000 characters,
  placeholder 'Contoh: 2 unit laptop untuk staf baru tim Keuangan'.
- acme_needed_date, label 'Tanggal Dibutuhkan': date picker, required, display format dd/MM/yyyy.
- acme_total_amount, label 'Total Jumlah (Rp)': text with a numeric validator, required,
  placeholder 'Contoh: 12500000', help text 'Isi angka saja, tanpa titik atau Rp.'
- A Custom HTML block below the amount, a light blue info box (background #E6F0FA, text #0B5FA5, radius 10px,
  padding 12px 16px) with the text: 'Matriks persetujuan: sampai Rp 5.000.000 disetujui Atasan Langsung;
  sampai Rp 50.000.000 ditambah Kepala Departemen; di atas Rp 50.000.000 ditambah Direktur Keuangan.'

Section 'Status' (2 columns):
- acme_status, label 'Status': select from the request_status lookup, default draft, read-only.
- acme_remarks, label 'Catatan Persetujuan': textarea, read-only.

Add a Font Awesome label icon and a Bahasa placeholder to every input field. When you finish, list the field ids
you created, the lookup topics you called and the skills you used.
```

Then the approval view over the same record (new form `purchase_approval`, same table):

```text
Build form 'purchase_approval' (Persetujuan Permintaan Pembelian) on the SAME tableName 'tbl_purchase_request'
as the existing form purchase_request, with the SAME field ids. Read acme-naming-conventions first.
Section 'Ringkasan Permintaan' (2 columns), all read-only: acme_request_no, acme_requester_name, acme_department,
acme_cost_centre, acme_needed_date, acme_total_amount, acme_item_description, acme_status. Use the same options
as purchase_request so the labels show, not the codes.
Section 'Keputusan' (1 column): acme_remarks, label 'Catatan Persetujuan', textarea, editable, placeholder
'Tulis alasan jika Anda menolak permintaan ini'.
Labels in Bahasa Indonesia. Do not add new field ids.
```

## 5. List Composer prompts

Open a new datalist in the Datalist Builder and pick the form `purchase_request` as its source.

```text
Build datalist 'purchase_requests' (Semua Permintaan Pembelian) over the form purchase_request
(table tbl_purchase_request). Read the skill acme-app-standards first and follow its Datalists section.

Columns, in this order, with these Bahasa titles:
1. acme_request_no 'No. Permintaan', shown as a link that opens the record.
2. acme_requester_name 'Pemohon'.
3. acme_department 'Departemen', with an options formatter so the Bahasa label shows
   (finance Keuangan, it Teknologi Informasi, hr Sumber Daya Manusia, operations Operasional, procurement Pengadaan).
4. acme_total_amount 'Jumlah (Rp)', right-aligned, number format with a dot as thousands separator, no decimals.
5. acme_status 'Status' as a coloured pill with a Condition formatter, rounded 999px, 12px font, padding 2px 10px:
   draft 'Draf' text #4B5563 on #EEF1F4; submitted 'Diajukan' text #0B5FA5 on #E6F0FA;
   approved 'Disetujui' text #14653F on #E7F5EE; rejected 'Ditolak' text #A93226 on #FBEAEA.
6. dateCreated 'Tanggal Dibuat', format dd/MM/yyyy.

Filters: acme_status (dropdown, label 'Status'), acme_department (dropdown, label 'Departemen') and a text search
on acme_request_no and acme_item_description (label 'Cari'). Default sort dateCreated descending, 10 rows per page.
Row action 'Lihat' that opens the record read-only. No delete action.
```

The requester's own list:

```text
Build datalist 'my_requests' (Permintaan Saya) over the form purchase_request with exactly the same columns,
formatters and filters as the existing datalist purchase_requests, plus one condition: only rows where createdBy
equals #currentUser.username#. Default sort dateCreated descending, 10 rows per page. Bahasa titles.
```

## 6. Process Composer prompt

Open the Process Builder of the app and paste. It is a long prompt on purpose; the Process Composer builds the
whole process with `create_process`.

```text
Build process 'purchase_request' (Proses Permintaan Pembelian). Read the skills acme-app-standards and
acme-standards-lookup first, and call lookup_acme_standard with topic approval_matrix. Use the thresholds it
returns; if they differ from the ones below, use the tool's values and tell me.

Participants (lanes), ids then Bahasa names: requester 'Pemohon', line_manager 'Atasan Langsung',
department_head 'Kepala Departemen', finance_director 'Direktur Keuangan'.
Workflow variables: status, total_amount. On the start form, map the field acme_total_amount to total_amount.

Activities (id, Bahasa label, lane, form):
- submit 'Pengajuan', requester, form purchase_request. When the requester submits, acme_status becomes submitted.
- review_manager 'Persetujuan Atasan', line_manager, form purchase_approval.
- review_head 'Persetujuan Kepala Departemen', department_head, form purchase_approval.
- review_finance 'Persetujuan Keuangan', finance_director, form purchase_approval.
Every review step has two buttons: 'Setujui' (value approved) and 'Tolak' (value rejected); both write the
workflow variable status. Tolak requires acme_remarks and sets acme_status to rejected.

Routes (Approved branch first, Rejected last, a route reads one variable only, so chain two routes):
- After review_manager: route on status; approved goes to a second route on total_amount: total_amount <= 5000000
  goes to the approved end, otherwise to review_head. rejected goes to the rejected end.
- After review_head: route on status; approved goes to a route on total_amount: total_amount <= 50000000 goes to
  the approved end, otherwise to review_finance. rejected goes to the rejected end.
- After review_finance: route on status; approved to the approved end, rejected to the rejected end.
Just before every approved end, set acme_status to approved. Each finishing branch gets its own End node.

Do not map participants to people; list "map participants" as a manual follow-up. When you finish, list the
activity ids, the route rules and the variables you created.
```

Note: setting `acme_status` on submit and on the approved end needs a tool step (the composer picks an installed
one with `get_tool_plugins`). If the composer chooses the simpler pattern from the skill, where each Setujui also
writes `acme_status = approved`, accept it for the demo and say so: an intermediate approval then shows
"Disetujui" before the last approver has decided.

## 7. UI Composer prompt: the userview

Open a new userview with id `purchase_portal` in the Userview Builder.

```text
Build userview 'purchase_portal' (Portal Pengadaan Acme) for app acme_procure. Read the skill acme-app-standards
first and follow its Userview section. All category, menu and tile names in Bahasa Indonesia; no emoji, use Font
Awesome icons. New userview permission: logged-in users.

Categories and menus, in this order (menu id, label, type, target, icon):
1. Category 'Dasbor':
   - home 'Beranda': a page (I will describe its content in a second message). Make it the first menu, so the
     portal opens on it.
2. Category 'Permintaan Saya':
   - new_request 'Buat Permintaan Baru': run process purchase_request. Icon fa-plus-circle.
   - my_request_list 'Daftar Permintaan': datalist my_requests. Icon fa-list-alt.
3. Category 'Persetujuan':
   - approval_inbox 'Kotak Masuk Persetujuan': inbox for process purchase_request. Icon fa-inbox.
   - all_requests 'Semua Permintaan': datalist purchase_requests, read-only. Icon fa-table.

Welcome message 'Selamat datang di Portal Pengadaan Acme'. Footer 'Acme - contoh data untuk demo'. Report the
menu ids and say which stock Joget texts stay English.
```

## 8. Theme and landing page prompts

### 8.1 The look we are going for

Calm, trustworthy and modern: a finance tool people like opening. Plenty of white space, one strong blue, a deep
navy sidebar, soft shadows instead of heavy borders, 12px rounded cards, one font (Inter). Colour is used for
meaning (status, primary action), never for decoration. Nothing flashy, no gradients except one in the hero.

| Role | Hex | Use |
|---|---|---|
| Primary (Acme blue) | `#0B5FA5` | buttons, links, active menu, focus ring |
| Primary dark | `#084A82` | button hover, hero gradient end |
| Ink | `#0F172A` | headings and body text |
| Muted text | `#5B6675` | help text, captions, table headers |
| Sidebar | `#0A2540` | sidebar background; links white at 72 percent |
| Page background | `#F4F7FA` | behind the content |
| Surface | `#FFFFFF` | cards, forms, tables |
| Border | `#E3E8EF` | hairlines, table rows |
| Accent (sparing) | `#D98A00` | one highlight, for example the "needs your approval" count |
| Status pills | see section 2 | grey, blue, green, red |

White on `#0B5FA5` is 6.6 to 1; white on `#0A2540` is above 14 to 1.

### 8.2 Theme Composer prompt

Open **Theme Builder**, create the theme `acme_theme` (Tema Acme), open the composer and paste:

```text
Style the Acme Procurement Portal theme. It should feel calm, trustworthy and modern, like a well-designed finance
SaaS tool: lots of white space, one strong blue, a deep navy sidebar, soft shadows, rounded corners, no clutter.

Colours (exact hex): primary #0B5FA5; page background #F4F7FA; content background #FFFFFF; border #E3E8EF;
body text #0F172A; footer text #5B6675 on #F4F7FA.
Sidebar mode dark-sidebar: sidebar background #0A2540, link text rgba(255,255,255,0.72), link icons the same,
active link background #0B5FA5 with white text and white icon, hover background rgba(255,255,255,0.08).
Header: white background #FFFFFF, header text #0F172A, height 60px. The app title in the sidebar logo area is the
text 'Acme' in white, weight 700; no logo image.
Typography: font Inter (one CSS import, fallback system-ui, sans-serif), base size 15px, line height 1.55,
headings weight 600, border radius 12px for cards and 8px for buttons and inputs, control height 40px.
Components: primary buttons #0B5FA5 with white text, hover #084A82; inputs with a 1px #E3E8EF border and a
3px rgba(11,95,165,0.25) focus ring; tables with a white background, a #5B6675 uppercase 12px header with letter
spacing 0.04em, 1px #E3E8EF row separators and a #F8FAFC row hover.
Custom CSS: cards and form sections get box-shadow 0 1px 2px rgba(15,23,42,0.06), 0 4px 16px rgba(15,23,42,0.06)
and no hard border.
Login page: a white card with radius 16px on a background gradient from #0A2540 to #0B5FA5, the text 'Acme' above
the fields, a full-width #0B5FA5 login button. Keep the username and password fields.

Check the contrast of white on #0B5FA5, white on #0A2540 and #0F172A on #F4F7FA with check_color_contrast and show
the ratios. List every colour and the font you set.
```

Then, in the Userview Builder of `purchase_portal`, choose `acme_theme` as the userview theme (the Theme Composer
cannot change which theme a userview uses) and **Save**.

### 8.3 UI Composer prompt: the landing page "Beranda"

Open `purchase_portal` in the Userview Builder, select the menu **Beranda** (`home`), open the composer and paste:

```text
Design the page of the menu 'home' (Beranda) of userview purchase_portal as a beautiful landing page. Bahasa
Indonesia only, no emoji, Font Awesome icons. It should look like a modern SaaS home screen: calm, airy, clear.

Top to bottom:
1. A hero, as one HTML block with its own scoped CSS (class names start with acme-): full-width card, radius 16px,
   background linear-gradient(135deg, #0A2540 0%, #0B5FA5 100%), white text, padding 40px 32px.
   Small overline 'PORTAL PENGADAAN' (12px, letter-spacing 0.12em, white at 70 percent).
   Heading 'Selamat datang di Portal Pengadaan Acme' (28px, weight 700).
   Sub-text 'Ajukan, pantau, dan setujui permintaan pembelian dalam satu tempat.' (16px, white at 85 percent).
   Two buttons: 'Buat Permintaan Baru' (white background, #0B5FA5 text, links to the menu new_request) and
   'Kotak Masuk Persetujuan' (transparent, 1px white border, links to approval_inbox). Stack on phones.
2. Three quick-action cards in a row (one HTML block, CSS grid, one column under 768px): white cards, radius 12px,
   shadow 0 1px 2px rgba(15,23,42,0.06), 0 4px 16px rgba(15,23,42,0.06), lift 2px on hover. Each card has a 40px
   round icon chip (#E6F0FA background, #0B5FA5 icon), a title and one line of text, and is a link:
   - fa-plus-circle 'Buat Permintaan Baru' / 'Isi formulir dalam dua menit.' -> new_request
   - fa-list-alt 'Daftar Permintaan' / 'Lihat status permintaan Anda.' -> my_request_list
   - fa-inbox 'Kotak Masuk Persetujuan' / 'Tinjau permintaan yang menunggu keputusan Anda.' -> approval_inbox
3. A heading 'Ringkasan' and one row of four KPI tiles that count real records of the datalist purchase_requests
   by acme_status, each linked to that list with the status filter: 'Draf' (draft, grey icon fa-file-alt),
   'Diajukan' (submitted, blue fa-paper-plane), 'Disetujui' (approved, green fa-check-circle), 'Ditolak'
   (rejected, red fa-times-circle). Use the pill colours #4B5563, #0B5FA5, #14653F, #A93226 for the icons.
4. Two charts side by side: a bar chart 'Nilai Permintaan per Departemen' (sum of acme_total_amount grouped by
   acme_department) and a pie chart 'Permintaan per Status' (count grouped by acme_status, colours as the pills).
5. A slim info strip (HTML block, #FFFFFF card, 3px left border #0B5FA5): 'Matriks persetujuan: sampai
   Rp 5.000.000 Atasan Langsung; sampai Rp 50.000.000 ditambah Kepala Departemen; di atas Rp 50.000.000 ditambah
   Direktur Keuangan.'

Tiles and charts must read real data; if one cannot be calculated, tell me why instead of showing a placeholder.
Keep the menu component placeholder if the page needs one. List the page elements you created.
```

If the composer's HTML is not what you want, paste the exact markup of section 12 into the HTML blocks.

## 9. Orchestrator prompts

Open the app `acme_procure` (or `acme_live`) in the App Composer, on the builders page, and press **Ctrl+J** to
open **AI Orchestrator Agent**. Choose the mode first: **Create > Plan** to show the plan (one call, builds
nothing), **Create > Confirm** in the room (an approval card per artefact), **Create > Autopilot** only for a
rehearsed run.

### 9.1 The whole app

```text
Build the Acme Procurement Portal (Portal Pengadaan Acme) in this app, in Bahasa Indonesia.

First read the skills acme-standards-lookup, acme-naming-conventions and acme-app-standards, and call
lookup_acme_standard for departments, cost_centres, request_status, approval_matrix and formats. Use the returned
values exactly, in order.

Ids (hard rules): snake_case, at most 20 characters, no generic suffix. One record, one table: every form over the
purchase request uses tableName tbl_purchase_request and the same field ids. Field ids start with acme_. Ids and
stored values English; labels, menus, tiles and sample texts Bahasa.

1. Form purchase_request (Permintaan Pembelian), tableName tbl_purchase_request. Section 'Data Pemohon':
   acme_request_no (ID generator REQ/{yyyy}/{00000}, read-only), acme_requester_name (default
   #currentUser.fullName#, read-only), acme_department (departments, required), acme_cost_centre (cost centres,
   required). Section 'Detail Permintaan': acme_item_description (textarea, required), acme_needed_date (date,
   dd/MM/yyyy, required), acme_total_amount (Rp, numeric validator, required). Section 'Status': acme_status
   (request status, default draft, read-only), acme_remarks (textarea, read-only). Icons and Bahasa placeholders
   on every input.
2. Form purchase_approval (Persetujuan Permintaan Pembelian) on tbl_purchase_request: the request fields
   read-only, acme_remarks editable.
3. Datalist purchase_requests: columns acme_request_no 'No. Permintaan', acme_requester_name 'Pemohon',
   acme_department 'Departemen' (label, not code), acme_total_amount 'Jumlah (Rp)', acme_status 'Status' as a pill
   (draft grey #4B5563 on #EEF1F4, submitted blue #0B5FA5 on #E6F0FA, approved green #14653F on #E7F5EE, rejected
   red #A93226 on #FBEAEA), dateCreated 'Tanggal Dibuat' dd/MM/yyyy. Filters acme_status and acme_department,
   newest first, 10 rows. Datalist my_requests: the same, only createdBy = #currentUser.username#.
4. Process purchase_request (Proses Permintaan Pembelian): lanes requester, line_manager, department_head,
   finance_director; activities submit, review_manager, review_head, review_finance with Bahasa labels from the
   skill. Steps follow the approval matrix on acme_total_amount (workflow variable total_amount): up to 5000000
   line manager only; up to 50000000 plus department head; above that plus finance director. Every review offers
   Setujui (approved) and Tolak (rejected, acme_remarks required). acme_status becomes submitted on submit,
   rejected on any Tolak, approved after the last required approval. A rejected request ends the process.
5. Userview purchase_portal (Portal Pengadaan Acme): category Dasbor with the first menu home 'Beranda', a landing
   page with a hero ('Selamat datang di Portal Pengadaan Acme'), three quick-action links (Buat Permintaan Baru,
   Daftar Permintaan, Kotak Masuk Persetujuan) and four KPI tiles, one per status, counting purchase_requests;
   category Permintaan Saya with 'Buat Permintaan Baru' (run process) and 'Daftar Permintaan' (my_requests);
   category Persetujuan with 'Kotak Masuk Persetujuan' (inbox) and 'Semua Permintaan' (purchase_requests).
6. Theme acme_theme: dark-sidebar #0A2540, primary #0B5FA5, page #F4F7FA, white content, font Inter, radius 12px,
   soft shadows, readable contrast. Calm, modern, trustworthy.

Keep chat output short. When finished, report: skills and tools you used, a table of artefact ids (type, id,
table), and the manual follow-ups.
```

### 9.2 The live slice (fits a few minutes)

For the room, build only the first artefacts live, in **Create > Confirm**, and switch to the pre-built app for the
rest. Paste 9.1 in **Plan** first to show the full plan, then send this in **Confirm**:

```text
From that plan, build only the form purchase_request and the datalist purchase_requests now. Skip the rest; I will
show the finished app separately. Report the artefact ids and the lookup topics you called.
```

Approve the two approval cards. Because the table `tbl_purchase_request` already holds the seed rows of the
pre-built app (same server), the new list shows data as soon as you open it.

### 9.3 A follow-up that shows "update in place"

```text
In the existing datalist purchase_requests (id purchase_requests), add a column acme_cost_centre 'Pusat Biaya'
after Departemen, showing the label, not the code. Do not create a new datalist.
```

Then check the builders page for a `_2` copy (there should be none).

## 10. Claude Code over Composer MCP

Start Claude Code in the repository root (so `.mcp.json` and `CLAUDE.md` apply), with `JOGET_BUILDER_API_KEY`
set in that shell. Optionally run `/mcp__joget-composer__joget_global_instructions` and
`/mcp__joget-composer__joget_build_instructions` first.

### 10.1 Warm-up (no changes)

```text
Call builder_status and tell me what this connection may do. Then call joget_skills_list and list the skills with
kind central. Then read the section ApprovalMatrix of acme-standards-reference with joget_skill_read.
```

### 10.2 The whole app

```text
Build the Acme Procurement Portal on the Joget connected through MCP server joget-composer, in app acme_procure,
version 1 (create it with builder_app if it does not exist and app-create is enabled; otherwise stop and tell me).

First, without chatting: call builder_status, then joget_skills_list. Read with joget_skill_read (index first,
then the sections you need) acme-naming-conventions, acme-app-standards and acme-standards-reference, and the
bundled joget-form-builder, joget-datalist-builder, joget-process-builder, joget-userview-builder and
joget-theme-builder. They override your defaults. lookup_acme_standard does not exist over MCP: take every option
list and the approval matrix from acme-standards-reference.

Hard rules: every builder call passes appId acme_procure and appVersion 1. Ids snake_case, at most 20 characters,
no generic suffix. Table tbl_purchase_request for every form over the request, same field ids. Field ids start
with acme_. Ids and stored values English; labels, menus, tile titles and seed texts Bahasa Indonesia. Use
builder_introspect for real class names and builder_lint before every save.

Build, in this order:
1. Form purchase_request (Permintaan Pembelian): acme_request_no (ID generator REQ/{yyyy}/{00000}, read-only),
   acme_requester_name (default #currentUser.fullName#, read-only), acme_department, acme_cost_centre (lists from
   the reference skill, required), acme_item_description (textarea, required), acme_needed_date (date dd/MM/yyyy,
   required), acme_total_amount (Rp, numeric validator, required), acme_status (request status, default draft,
   read-only), acme_remarks (textarea, read-only). Sections Data Pemohon, Detail Permintaan, Status.
2. Form purchase_approval over tbl_purchase_request: request data read-only, acme_remarks editable.
3. Datalists purchase_requests and my_requests (only createdBy = #currentUser.username#): columns per
   acme-app-standards (No. Permintaan, Pemohon, Departemen as label, Jumlah (Rp) with dot thousands, Status pill:
   draft #4B5563 on #EEF1F4, submitted #0B5FA5 on #E6F0FA, approved #14653F on #E7F5EE, rejected #A93226 on
   #FBEAEA, Tanggal Dibuat dd/MM/yyyy); filters status and department; newest first; 10 rows.
4. Process purchase_request: participants requester, line_manager, department_head, finance_director; activities
   submit, review_manager, review_head, review_finance (labels from the skill); routes on acme_total_amount per
   the matrix (<= 5000000 manager only; <= 50000000 plus head; above plus finance director); Setujui and Tolak on
   every review; acme_status submitted on submit, rejected on Tolak, approved after the last required approval.
   Validate, then ask me before deploy (confirm:true).
5. Theme acme_theme: dark-sidebar #0A2540 (links white at 72 percent, active #0B5FA5), primary #0B5FA5, page
   #F4F7FA, content #FFFFFF, border #E3E8EF, text #0F172A, font Inter, radius 12px cards and 8px controls, soft
   card shadow 0 1px 2px rgba(15,23,42,0.06), 0 4px 16px rgba(15,23,42,0.06). Calm, modern, trustworthy.
6. Userview purchase_portal (Portal Pengadaan Acme) using acme_theme: category Dasbor with first menu home
   'Beranda'; category Permintaan Saya with new_request 'Buat Permintaan Baru' (run process) and my_request_list
   'Daftar Permintaan'; category Persetujuan with approval_inbox 'Kotak Masuk Persetujuan' and all_requests
   'Semua Permintaan'. The Beranda page: a hero HTML block (gradient #0A2540 to #0B5FA5, heading 'Selamat datang
   di Portal Pengadaan Acme', two buttons to new_request and approval_inbox), three quick-action cards, four KPI
   tiles counting purchase_requests by acme_status (Draf, Diajukan, Disetujui, Ditolak), a bar chart of
   acme_total_amount by department and a pie chart by status. Use the markup in
   08-showcase-apps/PROMPTS.md section 12 for the HTML blocks.
7. Seed the 12 rows of 08-showcase-apps/seed/seed-purchase-requests.csv into purchase_request with builder_data
   (drop the c_ prefix of the column names to get the field ids; confirm:true is allowed for these seed rows only).

Then run builder_lint on everything and builder_verify_app; fix what they report.

Output rules: do not narrate; one short line between tool calls at most. At the end print exactly: (a) "Skills
used:" with the names you actually read, (b) a table of created artefact ids (type, id, table), (c) "Manual
follow-ups:" (participant mapping, publish the app, ID generator counter after seeding, English stock texts),
(d) the lint and verify result.
```

### 10.3 A short MCP slice for the room

```text
In app acme_live version 1, read acme-app-standards (section Datalists) and joget-datalist-builder, then create
datalist purchase_requests over the existing form purchase_request exactly as the skill's column standard says,
with the status pill colours draft #4B5563 on #EEF1F4, submitted #0B5FA5 on #E6F0FA, approved #14653F on #E7F5EE,
rejected #A93226 on #FBEAEA. Lint before you save. Print the column list and the lint result only.
```

## 11. Tips

### If a run fails

| What you see | What to do |
|---|---|
| The composer answers in text and nothing changes | Press **Revert** if shown, press **New chat**, and resend as a command that names the target id: "Add field acme_needed_date to form purchase_request". |
| Field ids without `acme_`, or English labels | The skill was not read in that run. Check the skill is **Active** and scoped to that composer, then start the prompt with "First read the skill acme-naming-conventions". |
| Invented cost centres or statuses | `lookup_acme_standard` was not called. Check the tool template exists and the skill card lists it; add "call lookup_acme_standard for cost_centres" to the prompt. Over MCP, name `acme-standards-reference`. |
| The Orchestrator times out on one artefact (300 seconds) | The other artefacts continue. Rebuild the missing one with its standalone composer and the prompt from sections 4 to 8. |
| "Built but NOT saved" in the report | Open the artefact's builder, rebuild it with the standalone prompt, and save there. |
| `_2` duplicates after a follow-up | Delete the copy; in follow-ups always name the existing id ("in the existing datalist purchase_requests"). |
| KPI tiles show the total, not the count per status | The tile filter parameter is wrong. Ask: "Point every tile at datalist purchase_requests with the filter on acme_status". |
| The approval goes to the requester | Participants are not mapped. Map them in **Participants** (manual step). |
| A theme change breaks every page | The primary colour is not a six-digit hex. **Revert**, then give `#0B5FA5`. |
| A long prompt is partly ignored | Split it: the Orchestrator for the form and lists, then the standalone composers for process, userview and theme. |
| The model is slow or rate-limited | Switch the AI service in the service picker, or continue with the pre-built app. |

### How to stage a long build

A whole-app Orchestrator build takes many minutes (design brief, one composer run per artefact, a critic review,
refinement rounds, saves, report). Do not run it unrehearsed in front of an audience.

1. **Rehearse the full build** the day before with prompt 9.1 in **Autopilot**. Time it. Export the result.
2. **Live, show the plan**: prompt 9.1 in **Plan** mode. One call, nothing built; the plan shows that the
   Orchestrator understood the matrix, the ids and the Bahasa labels.
3. **Live, build a slice**: prompt 9.2 in **Confirm**, form and list only. The room sees the approval cards, the
   result cards and the trace.
4. **Switch to the pre-built app**: open `acme_procure` (imported from `acme_procure/`), labelled on screen as
   "Pre-built from the same prompts". Tour the landing page, a request, the inbox, the list and the process.
5. **Optional**: one MCP slice (10.3) from Claude Code to show the same skills reaching an outside client.

### What stays manual (deck slide 20)

| Item | Who does it | Where |
|---|---|---|
| Map process participants (lanes) to users, groups or a workflow variable | You | Process Builder > **Participants** |
| Seed data | You | `seed/seed-purchase-requests.sql`, or `builder_data` over MCP |
| Set the ID generator counter after seeding (else the next request reuses `REQ/2026/00001`) | You | App > environment variable used by the `acme_request_no` field, value 12 |
| Configure email (SMTP) if you add notifications | You | General settings |
| Group ids for category permissions | You type them in the prompt | Userview categories |
| Per-menu permissions | Not available; per category only | Userview Builder |
| Routing from a live matrix | Thresholds are written into the routes at build time; a tool computes them only at build time | Process routes |
| Publish the app (menus answer 404 until it is published) | You | App Composer |
| Stock Joget texts (login page, standard buttons such as Save as Draft) | Stay English | |

## 12. Appendix: exact markup and CSS

Use these when you want a pixel-exact result, or as the fallback when a composer's HTML is not good enough. Paste
the HTML into the HTML blocks of the **Beranda** page and the CSS into the theme's custom CSS. The links are
relative to the userview, so `new_request` opens the menu with that id.

### Hero and quick-action cards (one HTML block)

```html
<style>
  .acme-home{font-family:Inter,system-ui,sans-serif;color:#0F172A}
  .acme-hero{background:linear-gradient(135deg,#0A2540 0%,#0B5FA5 100%);color:#fff;border-radius:16px;padding:40px 32px;margin-bottom:24px}
  .acme-hero__over{font-size:12px;letter-spacing:.12em;text-transform:uppercase;opacity:.7;margin:0 0 8px}
  .acme-hero h1{font-size:28px;font-weight:700;line-height:1.25;margin:0 0 8px;color:#fff}
  .acme-hero p{font-size:16px;opacity:.85;margin:0 0 24px;max-width:560px}
  .acme-btn{display:inline-block;padding:10px 18px;border-radius:8px;font-weight:600;text-decoration:none;margin:0 8px 8px 0}
  .acme-btn--light{background:#fff;color:#0B5FA5}
  .acme-btn--ghost{border:1px solid rgba(255,255,255,.8);color:#fff}
  .acme-btn:hover{text-decoration:none;opacity:.92}
  .acme-cards{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:24px}
  .acme-card{display:block;background:#fff;border-radius:12px;padding:20px;text-decoration:none;color:#0F172A;
    box-shadow:0 1px 2px rgba(15,23,42,.06),0 4px 16px rgba(15,23,42,.06);transition:transform .15s,box-shadow .15s}
  .acme-card:hover{transform:translateY(-2px);box-shadow:0 2px 4px rgba(15,23,42,.08),0 8px 24px rgba(15,23,42,.10);text-decoration:none;color:#0F172A}
  .acme-card__icon{width:40px;height:40px;border-radius:50%;background:#E6F0FA;color:#0B5FA5;display:flex;align-items:center;justify-content:center;margin-bottom:12px}
  .acme-card h3{font-size:16px;font-weight:600;margin:0 0 4px}
  .acme-card p{font-size:14px;color:#5B6675;margin:0}
  @media (max-width:768px){.acme-cards{grid-template-columns:1fr}.acme-hero{padding:28px 20px}.acme-hero h1{font-size:22px}}
</style>
<div class="acme-home">
  <section class="acme-hero">
    <p class="acme-hero__over">Portal Pengadaan</p>
    <h1>Selamat datang di Portal Pengadaan Acme</h1>
    <p>Ajukan, pantau, dan setujui permintaan pembelian dalam satu tempat.</p>
    <a class="acme-btn acme-btn--light" href="new_request">Buat Permintaan Baru</a>
    <a class="acme-btn acme-btn--ghost" href="approval_inbox">Kotak Masuk Persetujuan</a>
  </section>
  <nav class="acme-cards">
    <a class="acme-card" href="new_request"><span class="acme-card__icon"><i class="fas fa-plus-circle"></i></span>
      <h3>Buat Permintaan Baru</h3><p>Isi formulir dalam dua menit.</p></a>
    <a class="acme-card" href="my_request_list"><span class="acme-card__icon"><i class="fas fa-list-alt"></i></span>
      <h3>Daftar Permintaan</h3><p>Lihat status permintaan Anda.</p></a>
    <a class="acme-card" href="approval_inbox"><span class="acme-card__icon"><i class="fas fa-inbox"></i></span>
      <h3>Kotak Masuk Persetujuan</h3><p>Tinjau permintaan yang menunggu keputusan Anda.</p></a>
  </nav>
</div>
```

### Approval-matrix strip (a second HTML block, below the charts)

```html
<div style="background:#fff;border-left:3px solid #0B5FA5;border-radius:8px;padding:12px 16px;color:#0F172A;
  font-size:14px;box-shadow:0 1px 2px rgba(15,23,42,.06)">
  <strong>Matriks persetujuan:</strong> sampai Rp 5.000.000 Atasan Langsung; sampai Rp 50.000.000 ditambah
  Kepala Departemen; di atas Rp 50.000.000 ditambah Direktur Keuangan.
</div>
```

### Theme custom CSS

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
body, .form-cell, .dataList { font-family: Inter, system-ui, sans-serif; }
h1, h2, h3, h4 { font-weight: 600; letter-spacing: -0.01em; }
.form-section, .dataList, .card, .ui-tile { border: 0; border-radius: 12px;
  box-shadow: 0 1px 2px rgba(15,23,42,.06), 0 4px 16px rgba(15,23,42,.06); }
.btn, button, input[type=submit] { border-radius: 8px; font-weight: 600; }
input:focus, select:focus, textarea:focus { outline: none; border-color: #0B5FA5;
  box-shadow: 0 0 0 3px rgba(11,95,165,.25); }
.dataList table thead th { color: #5B6675; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }
.dataList table tbody tr:hover { background: #F8FAFC; }
.acme-pill { display: inline-block; border-radius: 999px; padding: 2px 10px; font-size: 12px; font-weight: 600; }
.acme-pill--draft { color: #4B5563; background: #EEF1F4; }
.acme-pill--submitted { color: #0B5FA5; background: #E6F0FA; }
.acme-pill--approved { color: #14653F; background: #E7F5EE; }
.acme-pill--rejected { color: #A93226; background: #FBEAEA; }
```

The selectors are a starting point: Joget theme class names differ between themes and versions, so check them with
the browser's inspector on your server. The font import needs internet access in the user's browser; without it
the page falls back to `system-ui`.
