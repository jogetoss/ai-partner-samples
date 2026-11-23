# Writing Good Composer Prompts

The quality of what an AI Composer builds depends mostly on the prompt you give it. A specific prompt with exact ids, fields, options and roles gives a rich result. A one-line prompt gives a generic one. This page teaches the principles, gives a structure for each composer, lists what to do and what to avoid, and then provides a cookbook of ready-to-use prompts grouped by composer and task, including prompts for apps with Bahasa Indonesia labels. It is for implementers who use the composers and the Orchestrator.

Read [AI Composers User Guide](12-composers-user-guide.md) first for what each composer can do, and [The Orchestrator: Building Whole Apps](13-orchestrator.md) for whole-app builds.

## How a composer reads your prompt

A composer sends your message, the last six messages of the conversation, the element you selected in the builder, and the current content of the builder to the AI model. The model then calls functions that change the artifact. Three things follow:

1. **The composer knows only what is in the prompt and in the builder.** It does not know your business. If a field, option or role matters, say it.
2. **It builds what you name.** A detail that you leave out is filled with a sensible default. A default is correct often, but it is not your decision.
3. **It checks the real app.** It reads the forms, lists and processes that exist, and the plugins that are installed. When you refer to something that exists, use its exact id.

## Twelve principles

1. **Name the id and the display name.** Give a snake_case id and a business name: `leave_request` (Leave Request). Without an id, the composer derives one from the name.
2. **State the purpose and the audience.** Who uses it and what they do with it. "Employees submit leave; managers approve."
3. **List the specifics for the artifact type.** See "Structure of a good prompt" below. Every field, column, filter, menu, lane or colour that matters should be in the prompt.
4. **Quote ids exactly from earlier steps.** Use the real form id, `tableName`, field ids and status values. Do not write "the status field" if the id is `leave_status`.
5. **Say which table a second form shares.** A review form, an approver form or a process-step form that shows the same record must use the same table and the same field ids.
6. **Quote your own words for look, labels and behaviour.** "Button label: Ajukan Cuti" is better than "a submit button in Indonesian".
7. **Name the native mechanism when logic is involved.** Status colours: Condition formatter. Email: Email Tool. A side effect on submit: form post-processor. Counts on a dashboard: list size. Ownership: `#currentUser.username#`.
8. **Do not ask for what composers cannot do.** See the limits in [AI Composers User Guide](12-composers-user-guide.md): mapping lanes to people, outside web services without an endpoint and credentials, automatic abort after a time, permissions on a single menu, inbox or calendar templates on a list.
9. **Refer to master data that exists.** "Department, a dropdown from the existing `department` form." Make sure that form exists first.
10. **One change per message in a standalone composer.** Use the Orchestrator for several artifacts.
11. **Select the element first.** Click the field, activity or menu in the builder, then say "make this required". The composer receives the selection.
12. **Ask questions freely.** "What can I do here?" and "What fields are in this form?" cost little and change nothing.

## Structure of a good prompt

A good prompt answers these questions in this order: what artifact (id and name), for whom, what it contains (the specifics), how it connects to other artifacts, and what it looks like.

### Template

```text
<Action> <artifact type> '<id>' (<Display Name>) [on table '<tableName>' / over form '<formId>'].
Purpose: <who uses it and why>.
<Specifics for the type, see below>.
Connections: <exact ids of forms, lists, processes it uses>.
Look and wording: <labels, colours, icons, in the user's own words>.
```

### Specifics by artifact type

| Artifact | Put these in the prompt |
|---|---|
| Form | Sections and columns; every field with id, label, type, options, required, validators, default; read-only fields; icons and placeholders; master-data sources |
| List | The bound form; every column in order; filters; search fields; status colours; date format; row actions; toolbar actions; default sort; page size |
| Userview | Categories by role; the group allowed to see each; every menu with its type and the exact list, form or process id; dashboard tiles (title, form, field and value) and charts (form, group-by field, type); icons |
| Process | Lanes; each activity with its lane and form; Approve and Reject statuses; route conditions and the variable they read; emails; deadlines; sub-processes |
| Theme | Hex colours; sidebar light or dark; font; login page; "with readable contrast" |

### Thin and rich

Thin (produces generic defaults):

```text
Create leave request form with all fields
```

Rich:

```text
Create form 'leave_request' (Leave Request) with tableName 'tbl_leave_request'.
Section 'Employee' (2 columns): employee_name (text, required, default #currentUser.fullName#),
  department (select: Engineering, Finance, HR, Operations; required).
Section 'Leave Details' (2 columns): leave_type (select: Annual, Sick, Unpaid, Maternity; required),
  start_date (date, required), end_date (date, required, must not be before start_date),
  days (number, read-only), reason (textarea, required, max 500 characters),
  attachment (file upload, optional; for medical certificates).
Section 'Status' (2 columns): status (select: Pending, Approved, Rejected; default Pending;
  read-only), approver_remarks (textarea, read-only).
Add a label icon and a placeholder to every input field.
```

### Free quality boosters

Words that make the composer use a stronger pattern:

| You write | The composer uses |
|---|---|
| "icons and placeholders on every input" | The field UI skill: label icons and placeholders |
| "status with colours: Pending amber, Approved green" | The Condition formatter for badges |
| "visible only to group managers" | A permission on the category, with a real group id |
| "dashboard with pending and approved counts" | Tiles that count list records by status, and charts |
| "same table as the leave_request form" | The same `tableName` and field ids for the approver form |
| "default to the current user" | `#currentUser.fullName#` or `#currentUser.username#` |

## Do and do not

| Do | Do not |
|---|---|
| Give exact ids, field names, option lists and group names | Say "the usual fields", "all fields", "and so on" |
| Describe the outcome ("approved requests go to Finance") | Describe the Joget internals ("set property X to Y") unless you need them |
| Ask for one artifact per message in a standalone composer | Put a form, a list and a process in one message to the Form Composer |
| Select the element before a targeted edit | Rely on "that field" when nothing is selected |
| Say "this already exists in the app" for forms you refer to | Refer to a form that is not in the app yet |
| Check the result in the real userview, not only the builder preview | Judge filters, tiles or permissions from the builder preview |
| Use `undo` or **Revert** when the result is wrong | Keep sending corrections on top of a wrong result without checking |
| State values that must match exactly (status values used by routes and tiles) | Change the wording of a status value in one artifact only |
| Use hex colours when exact colours matter | Use colour names that are ambiguous ("nice blue") |
| Press **New chat** when you change topic | Carry a long chat across unrelated tasks (only the last six messages are used) |

## Iterating

1. Start with a prompt that has the structure above.
2. Check the result on the canvas. Press **Confirm** or **Revert**.
3. Fix one thing per message: "Add a filter on `department` to this list."
4. Use questions to inspect: "Show me the validator on the `end_date` field."
5. Save in the builder when you are happy.

If the composer says "No changes were made.", it did not find an action in your message. Rephrase it as a command that names the target.

## Prompt cookbook

Every prompt below is ready to paste. Replace the ids with the ids of your app. "Typical result" describes what you should expect. Results depend on the AI model, and you should always review the artifact. Prompts are grouped by composer and then by task.

## Cookbook: Form Composer

Open a form in the Form Builder.

### Create

**F1. A simple form.**

```text
Create a customer registration form with fields: full name, email, phone number,
date of birth, and country (dropdown: Indonesia, Malaysia, Singapore, Other).
```

Typical result: five fields; a date picker for date of birth; a select box with four options; an email check on the email field.

**F2. A form with icons, placeholders and layout.**

```text
Create a vendor onboarding form with company name, contact person, email, phone number,
business address, registration date and remarks. Use two columns for the short fields.
Make company name, contact person and email required. Add an icon and a placeholder to every
input field.
```

Typical result: a two-column section, mandatory validators on three fields, label icons and placeholders.

**F3. A form with a reference number.**

```text
Create form 'purchase_request' (Purchase Request). Add a field 'request_no' (Request No) that
is auto-generated with the pattern PR-???? and is read-only. Add item_name (text, required),
quantity (number, required), unit_price (number, required) and total (calculated as
quantity multiplied by unit_price, read-only).
```

Typical result: an ID Generator field, number fields, a calculation field that is read-only.

**F4. A form that uses master data.**

```text
Build an employee registration form. Fields: Employee Name, Employee ID (auto-generated),
Department (dropdown that lists the records of the existing 'department' form),
Status (dropdown: Active, On Leave, Resigned).
```

Typical result: an ID Generator field, an options binder on the real `department` form, a static status list. The `department` form must exist.

### Change

**F5. Add fields to a section.** Select the section first.

```text
Add a Contact Number text field and an Emergency Contact Name text field to this section.
```

**F6. Make one field required and numeric.** Select the field first.

```text
Make this field required and only accept numbers.
```

**F7. Email validation.**

```text
Add a Head Email field for the department head's email address and make sure only valid
email addresses can be entered.
```

**F8. File upload with limits.**

```text
Add a file upload field 'profile_picture' (Profile Picture). Allow only jpg and png, maximum
2 MB.
```

**F9. Hidden audit fields.**

```text
Add a hidden field 'submitted_by' that records the user name of the person who submitted the
form, and a hidden field 'submitted_on' that records the submission date.
```

Typical result: hidden fields with the defaults `#currentUser.username#` and `#date.yyyy-MM-dd#`.

**F10. Cascading dropdowns.**

```text
Build a project assignment form. Fields: Department (dropdown from the 'department' form) and
Team Member (dropdown from the 'employee' form, showing only the employees of the selected
department).
```

**F11. Show a field only when needed.**

```text
Show the field 'reject_reason' only when 'status' is Rejected.
```

**F12. Embed another form.**

```text
Add a sub-form below the department fields that embeds the existing 'employee' form.
```

**F13. Convert a field type.**

```text
Convert the field 'username' to a password field. Keep its id and label.
```

## Cookbook: List Composer

Open a list in the Datalist Builder. The form it shows must exist.

**L1. A list over a form.**

```text
Create a new datalist for the employee form showing id, full name, department, hire date and
status.
```

Typical result: five columns, a date formatter on the hire date.

**L2. A complete list in one prompt.**

```text
Create a new datalist for the employee form showing id, full name, department, hire date and
status, with filters for department and status and a text search on the name, a row link to
open each employee, a bulk delete action, date formatting on the hire date, default sort by
name and paging of 20 rows.
```

**L3. Add a text filter.**

```text
Add a text search filter for the full_name column.
```

Typical result: the new filter is added and the existing filters stay.

**L4. Add a dropdown filter.**

```text
Add a dropdown filter for the leave_type column.
```

**L5. Set page size and sort.**

```text
Show 20 rows per page and sort by full_name ascending by default.
```

**L6. Status badges.**

```text
On the status column show coloured badges: Pending amber, Approved green, Rejected red.
```

**L7. A row action.**

```text
Add a "View" button on each row that opens the employee form.
```

**L8. A toolbar button.**

```text
Add a "New Employee" button to the list toolbar that opens the employee form.
```

**L9. Link formatters.**

```text
Show the email column as a link that opens the mail program, and show the resume_file column
as a download link. The files are uploaded through the employee form.
```

**L10. Date format.**

```text
Show only the date, without the time, on every date column. Use the format dd/MM/yyyy.
```

**L11. Change columns.**

```text
Rename the column 'dept' to 'Department', move it after 'full_name' and remove the 'notes'
column.
```

**L12. Show only some records.**

```text
Show only the records where status is Pending.
```

Typical result: an extra condition on the list's data. Check it in the userview.

## Cookbook: UI Composer

Open a userview in the Userview Builder.

**U1. Add a menu.**

```text
Add a new menu "Employee Directory" in the Employees category that shows the list_employee
datalist.
```

**U2. Rename a menu and the userview.**

```text
Rename the menu menu_employees to "Staff Directory". Rename the userview to "HR Self-Service
Portal". Do not change any id.
```

**U3. Reorder categories.**

```text
Move the Employees category above the Leave category.
```

**U4. A role category with permission.**

```text
Add a category "Approvals" visible only to the group managers, with an inbox menu
"Pending Approvals" for the process leave_approval_flow.
```

Typical result: the composer looks up the real id of the group `managers` and sets the permission on the category.

**U5. My requests list.**

```text
In the category "My Leave" add a menu "My Requests" that lists the processes submitted by the
current user for leave_approval_flow.
```

**U6. A start-process menu.**

```text
In the category "My Leave" add a menu "New Request" that starts the process
leave_approval_flow.
```

**U7. A dashboard.**

```text
Build a dashboard from the leave_request form: tiles for total requests, pending requests
(status = Pending) and approved requests (status = Approved), a bar chart of requests by
leave_type and a pie chart by status.
```

Typical result: one row of three tiles and two charts. The form's main list needs a filter on `status`.

**U8. A full multi-role userview.**

```text
Create a userview 'ops_portal' (Operations Portal) with a dashboard menu, an Employee
Self-Service category with the leave request form and the leave request list, and an HR Admin
category with the employee and department lists. The HR Admin category is visible only to
the group hr.
```

**U9. A calendar.**

```text
Add a calendar menu "Team Leave" based on the list leave_requests, using start_date and
end_date.
```

**U10. An information page and a link.**

```text
Add an information page "Leave Policy" with a heading and two paragraphs, and a link menu
"Company Intranet" to https://intranet.example.com.
```

**U11. Change the welcome and footer texts.**

```text
Set the welcome message to "Welcome to the HR portal" and the footer message to "Contact HR
for help".
```

## Cookbook: Process Composer

Open the process in the Process Builder.

**P1. Three-step approval.**

```text
Create a simple three-step approval workflow: Submit, Review, and Approve, with lanes
Requester, Manager and Finance. Use the form leave_request for Submit and the form
leave_review for Review and Approve. After Review, a route sends Approved to Approve and
Rejected back to Submit.
```

**P2. Add an email step.**

```text
Add an email step after the Approve activity that sends the requester an email with the
subject "Your request was approved". Send it to the requester lane.
```

Typical result: an email tool step. The email tool needs the SMTP server settings and a recipient.

**P3. A conditional route.**

```text
Add a route after Review: if status is Approved go to Approve, if Rejected go back to Submit.
```

**P4. Route by amount.**

```text
Create a purchase approval process: the requester submits a request, the manager reviews it,
a route sends approved requests with amount above 1000 to Finance approval and the others to
the end. Rejected requests go back to the requester. The requester is notified at the end.
Use separate lanes for requester, manager and finance.
```

**P5. Add a lane and move a step.**

```text
Add a swimlane for the Finance team and move the Approve step into it.
```

**P6. Rename.** Select the activity first.

```text
Rename this activity to "Initial Request".
```

**P7. A deadline with escalation.**

```text
Add a deadline of 48 hours to the Review step. If it is not completed, send it to the
activity "Escalate to Director".
```

**P8. A clarification loop.**

```text
Add a clarification loop: the manager can send the request back to the requester, who
resubmits.
```

**P9. An AI step.**

```text
After Submit, add an AI step that calls the agent 'request_classifier' with the input
'description' set to the reason field, and stores the result in the workflow variable
'category'. Run it synchronously.
```

Typical result: an agent process tool step. You must give the real agent id.

**P10. Edit another process of the app.**

```text
In the process leave_approval_2, add a HR approval step after Manager Review, in a lane
named HR.
```

Typical result: the composer works on the process you name, not the process on the canvas.

## Cookbook: Theme Composer

Open a theme in the Theme Builder.

**T1. Brand colours.**

```text
Set the primary colour to #2563EB (blue) and use #F59E0B (amber) as the accent for badges
and highlights.
```

**T2. Dark sidebar.**

```text
Set the sidebar background to dark navy #0F172A with white link text, and make the active
menu item amber #F59E0B on a darker navy background.
```

**T3. Accessibility.**

```text
Apply a theme that meets WCAG AA contrast for all text. Show me the contrast ratio of each
colour pair you checked.
```

**T4. Typography and spacing.**

```text
Use the font Inter for body text, a base font size of 16px, and rounded buttons with a 6px
radius.
```

**T5. A complete look.**

```text
Give the app a modern teal and amber look: set the primary, header, navigation, button, link
and background colours with readable contrast, and add a little custom CSS for rounded
buttons.
```

**T6. Login page.**

```text
Style the login page with a white card on a teal background, a centred logo area and a
teal login button. Keep the username and password fields.
```

**T7. Start over.**

```text
Reset all theme values to the default.
```

## Cookbook: Orchestrator

Open the app's builders page. Use **Plan** first for large requests.

**O1. A form and its list.**

```text
Build a contact directory: form 'contact' (Contact) with name, company, email and phone, and
a list 'contacts' to browse them with a search on name and company.
```

**O2. A form, a list and a userview.**

```text
Create a bug tracker. Form 'bug' (Bug): title (text, required), description (textarea),
severity (select: Low, Medium, High), status (select: Open, In Progress, Closed; default
Open). List 'bugs' with columns title, severity, status (coloured badges) and a filter on
status. Userview 'bug_portal' (Bug Portal) with a category 'Bugs' with the list 'bugs'
and a form menu to report a bug.
```

**O3. A two-step approval process.**

```text
Create a two-step expense claim approval workflow: the employee submits a claim with the
form 'expense_claim', then the finance team approves it. Rejected claims go back to the
employee.
```

**O4. A module with approval.**

```text
Create a full leave-request module: a leave request form, a list of all leave requests, a
two-step approval workflow (manager approval, then HR approval) where both approval steps use
the leave request form's table, and a userview with a menu for the list and for the form.
```

**O5. A change to one existing artifact.**

```text
Add a Phone Number text field to the employee form.
```

Typical result: only the Form Composer is called, and the existing form is edited.

**O6. A theme only.**

```text
Set the button colour to red.
```

**O7. Everything in one message.**

Use **Plan** mode, then run the plan. The leave-request prompt in [The Orchestrator: Building Whole Apps](13-orchestrator.md) is a complete example for forms, a list, a process and a userview.

## Cookbook: questions that change nothing

**Q1.** In any composer:

```text
What can I do here?
```

**Q2.** In the Form Composer:

```text
Which field types are available on this server?
```

**Q3.** In the Orchestrator:

```text
What are hash variables? Give three examples I can use in a form default value.
```

## Prompts for Bahasa Indonesia

### What the composers do with other languages

The composers have no language setting. The panel text (buttons, status messages) is shown in English. The labels, option texts, menu names, messages and sample texts that the composers write are whatever language your prompt asks for, and the AI model decides the quality of the wording. The bundled skills contain no rules about Bahasa Indonesia. Therefore:

- **Say the language you want, and give the exact wording.** The composers pass your words through. A prompt in English that says "all labels in Bahasa Indonesia" is accepted, but the model chooses the wording. For exact terms, quote them.
- **Keep ids in plain ASCII snake_case.** `pengajuan_cuti` is a valid id. Do not use spaces, accents or punctuation in ids. Labels may contain any text.
- **Keep one spelling for values that other artifacts read.** A route rule, a dashboard tile or a list filter matches the stored option value exactly. If the status value is `Disetujui`, say `Disetujui` in the form, the list, the process route and the tile.
- **Do not use long dashes in labels.** The composers avoid em and en dashes in labels and placeholders. Write labels with plain hyphens or none.
- **Check dates and numbers.** Ask for a display format such as `dd/MM/yyyy`. Check the result in the list.
- **For an app that switches languages**, Joget's own app translation feature (the `#i18n.key#` hash variable) is the right tool. The composers write labels as plain text in one language. Add and manage translations in Joget yourself.
- **Replies.** You may write the request in Bahasa Indonesia. Whether the explanation comes back in Bahasa depends on the model. Add "Jawab dalam Bahasa Indonesia" ("Reply in Bahasa Indonesia") if you want it.
- Review all generated texts with a native speaker before you release the app.

### Vocabulary for prompts

| English | Bahasa Indonesia |
|---|---|
| Leave request | Pengajuan Cuti |
| Employee | Karyawan |
| Department | Departemen |
| Start date / End date | Tanggal Mulai / Tanggal Selesai |
| Number of days | Jumlah Hari |
| Reason | Alasan |
| Attachment | Lampiran |
| Status: Pending / Approved / Rejected | Status: Menunggu / Disetujui / Ditolak |
| Approver remarks | Catatan Atasan |
| Submit | Ajukan |
| Approve / Reject | Setujui / Tolak |
| My requests | Pengajuan Saya |
| Pending approvals | Menunggu Persetujuan |
| Dashboard | Beranda |
| Search | Cari |

### Bahasa prompts

**B1. A form with Bahasa labels (prompt in English).**

```text
Create form 'pengajuan_cuti' (Pengajuan Cuti) with tableName 'tbl_pengajuan_cuti'.
All labels, placeholders, section titles and option texts must be in Bahasa Indonesia, exactly as written here.
Section 'Data Karyawan' (2 columns): nama_karyawan (label 'Nama Karyawan', text, required,
default #currentUser.fullName#), departemen (label 'Departemen', select: Teknik, Keuangan,
SDM, Operasional; required).
Section 'Detail Cuti' (2 columns): jenis_cuti (label 'Jenis Cuti', select: Tahunan, Sakit,
Tanpa Gaji, Melahirkan; required), tanggal_mulai (label 'Tanggal Mulai', date, required),
tanggal_selesai (label 'Tanggal Selesai', date, required, not before tanggal_mulai),
jumlah_hari (label 'Jumlah Hari', number, required), alasan (label 'Alasan', textarea,
required), lampiran (label 'Lampiran', file upload, optional).
Section 'Status' (2 columns): status (label 'Status', select: Menunggu, Disetujui, Ditolak;
default Menunggu; read-only), catatan_atasan (label 'Catatan Atasan', textarea, read-only).
Add an icon and a placeholder in Bahasa Indonesia to every input field.
```

**B2. The same form, requested in Bahasa.**

```text
Buat form 'pengajuan_cuti' (Pengajuan Cuti) dengan tableName 'tbl_pengajuan_cuti'.
Semua label, placeholder, judul bagian dan pilihan harus dalam Bahasa Indonesia.
Bagian 'Data Karyawan' (2 kolom): nama_karyawan (teks, wajib, isi awal #currentUser.fullName#),
departemen (pilihan: Teknik, Keuangan, SDM, Operasional; wajib).
Bagian 'Detail Cuti' (2 kolom): jenis_cuti (pilihan: Tahunan, Sakit, Tanpa Gaji, Melahirkan;
wajib), tanggal_mulai (tanggal, wajib), tanggal_selesai (tanggal, wajib), jumlah_hari (angka,
wajib), alasan (area teks, wajib), lampiran (unggah file, opsional).
Bagian 'Status' (2 kolom): status (pilihan: Menunggu, Disetujui, Ditolak; isi awal Menunggu;
hanya baca), catatan_atasan (area teks, hanya baca).
Tambahkan ikon dan placeholder pada setiap kolom isian.
```

**B3. A list with Bahasa headers and badges.**

```text
Create list 'daftar_cuti' (Daftar Cuti) over form pengajuan_cuti.
Column headers in Bahasa Indonesia: nama_karyawan 'Nama Karyawan', jenis_cuti 'Jenis Cuti',
tanggal_mulai 'Tanggal Mulai', tanggal_selesai 'Tanggal Selesai', jumlah_hari 'Hari',
status 'Status'. Show dates as dd/MM/yyyy. Status badges: Menunggu amber, Disetujui green,
Ditolak red. Filters: jenis_cuti, status. Search on nama_karyawan, label 'Cari'.
Default sort tanggal_mulai descending. Row action label 'Lihat'.
```

**B4. A process with Bahasa names.**

```text
Create process 'alur_cuti' (Alur Persetujuan Cuti). Lanes: 'Karyawan' and 'Atasan'.
Start activity 'Ajukan Cuti' in lane Karyawan with form pengajuan_cuti. Activity
'Tinjau Pengajuan' in lane Atasan with form tinjau_cuti, buttons 'Setujui' (value Disetujui)
and 'Tolak' (value Ditolak) that set the workflow variable status. A route: Disetujui goes to
the email step 'Beri Tahu Disetujui', Ditolak goes to the email step 'Beri Tahu Ditolak'. The
emails are in Bahasa Indonesia and go to the employee. Then an End node.
```

Typical result: the status values in the route match the button values exactly. Check the button captions in the Process Builder.

**B5. A userview with Bahasa menus and a dashboard.**

```text
Create userview 'portal_cuti' (Portal Cuti). Category 'Cuti Saya' for all logged-in users
with menus 'Pengajuan Saya' and 'Ajukan Cuti'. Category 'Persetujuan' only for the group
managers with the inbox 'Menunggu Persetujuan'. Category 'Admin SDM' only for the group hr
with the menu 'Semua Cuti'. A dashboard 'Beranda' with the tiles 'Total Pengajuan',
'Menunggu' (status = Menunggu) and 'Disetujui' (status = Disetujui), a bar chart 'Pengajuan
per Jenis Cuti' by jenis_cuti and a pie chart 'Status Pengajuan'. Welcome message
'Selamat datang di Portal Cuti'. Footer 'Hubungi SDM jika butuh bantuan'.
```

**B6. Translate the menu labels of an existing userview.**

```text
Translate the menu and category labels of this userview into Bahasa Indonesia. Do not change
any id or address. Keep the product names unchanged.
```

Typical result: only labels in the userview change. The labels inside forms and lists are not changed by this request; ask the Form Composer and the List Composer separately.

**B7. Messages and validation texts.**

```text
For the field tanggal_selesai set the validation message to "Tanggal selesai tidak boleh
sebelum tanggal mulai", and for nama_karyawan set the required message to "Nama karyawan
wajib diisi".
```

**B8. A whole app in Bahasa with the Orchestrator.**

```text
Buat aplikasi pengajuan cuti. Gunakan Bahasa Indonesia untuk semua label, pilihan, nama menu
dan email. Id memakai snake_case Inggris atau Indonesia tanpa spasi. Form 'pengajuan_cuti'
dan form tinjauan 'tinjau_cuti' memakai tabel yang sama 'tbl_pengajuan_cuti' dengan id kolom
yang sama. Daftar 'daftar_cuti'. Proses 'alur_cuti' dengan dua jalur: Karyawan dan Atasan;
Atasan menyetujui atau menolak. Userview 'portal_cuti' dengan kategori Cuti Saya, Persetujuan
(grup managers) dan Admin SDM (grup hr), serta beranda dengan angka dan grafik. Nilai status:
Menunggu, Disetujui, Ditolak.
```

Run it in **Plan** mode first. Check that the status values are the same in the form, the list, the route and the dashboard tiles.

## Limitations and things to know

- **Prompts are not guarantees.** The same prompt can give different results with different models, and sometimes with the same model. Check the artifact.
- **Long prompts.** A very long prompt may be partly ignored by some models. If an item is missing, ask for it in a short follow-up message, or build in stages.
- **Exact wording of labels.** The composers keep the text you quote. If you only describe the label, the model chooses the words.
- **Things a prompt cannot change.** No wording makes a composer map lanes to people, call an outside service without endpoint and credentials, or add a template that the List Composer does not support. Ask for a note or a placeholder and finish the step by hand.
- **Questions with a build word.** A message such as "Can you add a status field?" can be read as a question. Use a command: "Add a status field."
- **Images.** A picture is a guide, not a specification. Add the field names in the text.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| The result is generic | The prompt was thin | Use the structure above; add ids, fields, options and roles |
| A field is in the wrong section | The target section was not selected or named | Select the section first, or name it in the prompt |
| Filters or columns have wrong names | The prompt used labels instead of field ids | Use the exact field ids of the form |
| A dropdown from another form is empty | The form does not exist or has no records | Create the form first; use its exact id |
| Tiles show the wrong number | The status value in the tile is not the stored option value | Use the same status text everywhere |
| A route always goes one way | The route reads a variable that nothing writes | Ask for an approval button or a form field that sets the variable |
| Labels came out in English | The prompt did not say the language | Add "All labels ... in Bahasa Indonesia" and quote the labels |
| The reply asks a question and builds nothing | A change request lacked a needed value | Answer with the exact column, id or name |

## Where to go next

- [AI Composers User Guide](12-composers-user-guide.md)
- [The Orchestrator: Building Whole Apps](13-orchestrator.md)
- [How the Composers Work](15-how-composers-work.md)
- [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md)
