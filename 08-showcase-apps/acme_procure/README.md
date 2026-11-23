# Acme Procurement Portal (`acme_procure`)

A demo app for Joget DX 9.1 that shows what a finished purchase-request app looks like: a request form, an approval form, four lists, an approval process driven by the amount of the request, a custom landing page and a custom theme. Acme is a fictional company. All labels are in Bahasa Indonesia, and every stored value is an English code.

Open `LANDING-PAGE-PREVIEW.html` in a browser to see the landing page and theme without a Joget server.

## Files

| File | What it is |
|---|---|
| `acme_procure.jwa` | The app to import. It is a zip that holds `appDefinition.xml` and `package.xpdl`. |
| `build-acme-procure.js` | The script that writes everything in this folder. Run it again after you change it: `node build-acme-procure.js` (it needs `adm-zip` from the repository root `npm install`). The output is deterministic: the ids come from fixed seeds and the timestamps are fixed. Only the zip entry dates inside the `.jwa` change between runs. |
| `json/` | Every artefact as readable JSON: `form_*.json`, `datalist_*.json`, `userview_purchase_portal.json`, `theme_acme_procure.json`, plus `package.xpdl` (the process), `package_mapping.json` (forms, tools and participants mapped to activities) and `landing_page_home.html` (the HTML of the landing page as stored in the app). |
| `LANDING-PAGE-PREVIEW.html` | A static preview of the landing page with sample numbers. The landing HTML and CSS are the same strings that the app contains. The sidebar and top bar around them are a simplified copy of the Joget dark-sidebar layout. The page works offline. With internet it also loads the theme font. |

## What is inside the app

The ids follow the Acme standards: snake_case, at most 20 characters, field ids `acme_*`, and the table is `tbl_<first form id>`.

**Forms** (both use table `tbl_purchase_request`, which Joget creates as `app_fd_tbl_purchase_request`)

- `purchase_request` (Permintaan Pembelian). It has three parts:
  - **Data Pemohon**: request number from an ID generator (`REQ/<year>/00001`, environment variable `acme_request_counter`), the requester (read-only, defaults to `#currentUser.fullName#`), a hidden user name, and the request date.
  - **Rincian Pembelian**: department and cost centre (the lists from the standards file), title, category, vendor, needed-by date, priority, an item grid (`acme_items`, at least one row), and the total amount in Rupiah. The total is a numeric field shown as `Rp12.500.000` and stored as a plain integer. A note next to the total shows who will approve.
  - **Justifikasi dan Lampiran**: justification, attachments, and a hidden `acme_status`.
- `purchase_approval` (Persetujuan Pembelian). It shows the whole request read-only (a sub form on the same record) and adds the decision `acme_decision` (Setujui = `approved`, Tolak = `rejected`) and the remarks `acme_remarks`. Remarks are required when the approver chooses Tolak. A BeanShell validator enforces this. The form also records the approver's name and the date in hidden fields.

**Lists.** Each has the standard columns in this order: No. Permintaan, Pemohon, Departemen, Jumlah (Rp), Status, Tanggal Dibuat. Status shows as a coloured pill: Draf grey, Diajukan blue, Disetujui green, Ditolak red. Lists are newest first, 10 rows per page.

- `purchase_requests`: all requests. Filters: request number, status, department. It also supplies the counts on the landing page.
- `my_requests`: only the requests of the signed-in user (`customProperties.acme_requester_id = '#currentUser.username#'`).
- `approval_inbox`: the list used by the approval inbox menu.
- `recent_requests`: the five newest requests, shown on the landing page.

**Process** `purchase_request` (Proses Permintaan Pembelian). The number of approval steps follows the approval matrix in `acme-standards.json`:

```
submit (Pengajuan, sets acme_status=submitted)
  -> review_manager (Persetujuan Atasan)
       Tolak                  -> rejected (Ditolak, sets acme_status=rejected)
       amount <= 5.000.000    -> done (sets acme_status=approved)
       amount >  5.000.000    -> review_head (Persetujuan Kepala Departemen)
            Tolak                  -> rejected
            amount <= 50.000.000   -> done
            amount >  50.000.000   -> review_finance (Persetujuan Keuangan)
                 Tolak  -> rejected
                 Setujui -> done
```

- The start form is `purchase_request`. All three review steps use `purchase_approval`.
- `submit`, `done` and `rejected` use the Form Data Update Tool to set `acme_status`.
- The workflow variables are `acme_total_amount` and `acme_decision`.
- Participant mapping: `requester` is the person who started the process. `line_manager`, `department_head` and `finance_director` are all mapped to the user **admin**, so the whole route can be demonstrated with one login.

**Userview** `purchase_portal`

| Category | Menus |
|---|---|
| Dasbor | Beranda (landing page), Semua Permintaan |
| Permintaan Saya | Buat Permintaan Baru (starts the process), Daftar Permintaan |
| Persetujuan | Kotak Masuk Persetujuan (approval tasks for review_manager, review_head and review_finance) |

The landing page has:

- a hero banner with the Acme mark and "Selamat datang, #currentUser.firstName#"
- the total number of requests
- four status tiles with live counts. Each tile opens the filtered list.
- four quick-action cards
- a "recent activity" table
- a card that explains the approval matrix

**Theme** `theme_acme_procure`. A Builder Theme with:

- a dark ink sidebar (`#0B1F33`) and a teal accent (`#0D9488`)
- the Plus Jakarta Sans font, with system fonts as fallback
- rounded cards and subtle shadows
- hover and active states, with the active menu item marked by a left border and a tint
- a matching custom login page

The userview already uses this theme.

## How to import

1. Sign in to Joget DX 9.1 as an administrator.
2. Open **Settings > Manage Apps** (in some versions this is the **Design New App** dialog) and choose **Import App**.
3. Select `acme_procure.jwa` and confirm. Leave "overwrite" off on a server that does not have the app yet.
4. Open the app and **publish** version 1 if Joget does not do it for you.
5. Open the userview: `/jw/web/userview/acme_procure/purchase_portal/_/home`
6. Optional: in the process builder, use **Map Participants** to map `line_manager`, `department_head` and `finance_director` to real users or groups. They are mapped to `admin` by default.

Demo flow:

1. As admin, choose **Buat Permintaan Baru** and enter a total of, for example, `12500000`.
2. Open **Kotak Masuk Persetujuan** twice: the first time as the line manager, the second time as the department head, because 12.5 million is above 5 million.
3. The status moves from Diajukan to Disetujui.

A total above 50 million adds a third step. Choosing Tolak at any step ends the request with status Ditolak.

## What was verified (no Joget server was available)

Every check below was done offline against the generated files:

- The script runs. The JSON, XPDL, landing HTML and preview are byte-identical between two runs.
- `appDefinition.xml` and `package.xpdl` are well-formed XML. This was checked with `@xmldom/xmldom`, installed in a scratch folder outside the repository. `appDefinition.xml` was checked twice: as exported, and after the same un-commenting of the builder definitions that the Joget importer does.
- Every JSON blob inside the XML parses: 2 forms, 4 lists, 1 userview, the theme, 5 internal userview pages, and 3 tool configurations.
- The ids follow the rules:
  - the app, form, list, userview and theme ids are snake_case and at most 20 characters
  - all 25 element ids in the two forms match `^acme_[a-z0-9_]+$` and are unique within each form
  - both forms use table `tbl_purchase_request`
  - menu, category and `customId` values are unique
- Form content:
  - no CustomHTML element has an `html` key
  - field labels contain no HTML
  - there are no empty columns, and no em or en dashes
  - the form properties were checked against the joget-form-builder component schemas. The one difference is `SubForm.formDefId`, which a real Joget export also uses.
- Lists:
  - each list column and filter is a real field or a system field, and the columns are in the APP-7 order
  - each filter parameter name equals the encoding Joget computes at run time. The algorithm was checked against two filter names from a real export.
- Userview:
  - every menu points to an existing list or form, and every menu has its internal page
  - the userview's theme class matches the theme id
  - the theme's properties are an exact copy of the userview's theme properties
  - the login template keeps `customUsername`, `customPassword` and `loginButton`
- Process:
  - every transition and split reference points to an existing activity, and every activity is reachable
  - each split has exactly one OTHERWISE path, and `done` and `rejected` have XOR joins
  - the form, tool and participant mappings point to existing activities, forms and participants
  - the routing conditions were run in JavaScript for 9 cases, including the boundaries 5,000,000 / 5,000,001 / 50,000,000 / 50,000,001, the formats "12.500.000" and "72500000,00", and a rejection at each step. All took the expected path.
- The landing page uses only the hash variables `currentUser`, `date` and `datalist`, and every `datalist` variable names an existing list and filter. The page uses no hex colours, because a `#` could be read as the start of a hash variable.
- No text in this folder or inside the `.jwa` contains the old partner name.
- The preview was rendered with headless Microsoft Edge at 1440 px and 600 px wide and checked by eye.

How these shapes were chosen: the XML layout, the package mapping (activity forms, Form Data Update Tool, participant types), the Builder Theme and its copy in the userview, the internal userview pages, the status pills and the sub form pattern all copy a real DX 9 export of the stock Expenses Claim app. The behaviour of the ID generator, the list filters, the `extraCondition` syntax, the datalist hash variable, the HTML page and the numeric text field was checked in the Joget community and enterprise source code.

## What is NOT verified

These need a Joget server:

- **The import itself**, and that DX 9.1 accepts every property as written.
- **The process at run time.** The workflow engine evaluates the amount conditions. They were tested in Node's JavaScript, not in Joget's engine. If the amount ever reached the engine in a format the conditions do not expect, the request would take the shorter route.
- **The landing page counts.** They use the datalist hash variable from Joget DX Enterprise, so they work on Enterprise and not on Community. The "recent activity" table depends on the same plugin.
- **That the Builder Theme renders exactly like the preview.** The sidebar selectors follow the theme-builder skill, and a few content classes (form sections, list filters) are reasonable guesses at DX 9 class names. If one does not match, that rule has no effect and nothing breaks.
- **The custom login page.** It keeps the standard element ids. If it ever fails, `/jw/web/login` still works.

## Known limits

- **Counts are global.** The status tiles count all requests, not only the user's own. "Draf" is never produced by the process (new requests start as Diajukan), so it counts only records loaded as drafts, for example seed data.
- **Seed data and the inbox.** Records inserted straight into `app_fd_tbl_purchase_request` appear in the lists and counts, but not in the approval inbox, because they have no process. Use the stored codes `draft`/`submitted`/`approved`/`rejected`, the department codes and the cost-centre codes.
- **The total is typed in.** The item grid does not calculate the total automatically.
- **Editing the landing page.** Change it in `build-acme-procure.js` (or in JSON), not with the HTML page's visual editor in the Userview Builder. That editor can strip the `<style>` block and the inline SVG icons.
- **Some text stays English.** Joget's own buttons and messages (for example Submit and the login error) are not translated.
- **The font needs internet.** It loads from Google Fonts. Without internet the theme falls back to system fonts.

## What is left for the live Composer run

Nothing is needed for the app to run. The process, the routes, the forms and the inbox are all included.

For a real organisation, one step remains: map the participants `line_manager`, `department_head` and `finance_director` to real people or groups. The Composers cannot do this (see the acme-app-standards skill, "map participants"), and in this demo they point to `admin`.

If you want to show the Process Composer building the route live, you can delete the process in the process builder and ask the Composer to rebuild it. Use process id `purchase_request`, activity ids `submit`, `review_manager`, `review_head`, `review_finance`, `done`, and the matrix limits 5,000,000 and 50,000,000. The forms and lists in this app already match those ids.
