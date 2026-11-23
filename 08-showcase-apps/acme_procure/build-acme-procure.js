// Generates acme_procure.jwa: the "Acme Procurement Portal" showcase app for Joget DX 9.1 (app id acme_procure, v1).
//
//   npm install                 (once, in the repository root; this script only needs adm-zip)
//   node build-acme-procure.js
//
// Output, all next to this file:
//   acme_procure.jwa            the importable app (appDefinition.xml + package.xpdl in one zip)
//   json/*.json                 every form, datalist, userview and theme as readable, pretty-printed JSON
//   json/package.xpdl           the approval process, the same file that is inside the .jwa
//   LANDING-PAGE-PREVIEW.html   a static preview of the landing page and theme with sample numbers
//
// The script is deterministic: every generated id is derived from a fixed seed string (sha1), and the timestamps are
// fixed, so running it twice gives byte-identical JSON and XML.
//
// What is inside (Acme standards: 04-custom-skills/*/SKILL.md and the acme-standards.json in 03-composer-custom-tool):
//   forms      purchase_request (Permintaan Pembelian), purchase_approval (Persetujuan Pembelian), both on
//              table tbl_purchase_request
//   datalists  purchase_requests, my_requests, approval_inbox, recent_requests
//   userview   purchase_portal with a custom landing page (HTML page "home")
//   theme      theme_acme_procure (Builder Theme, dark sidebar, custom CSS and login page)
//   process    purchase_request: submit > review_manager > [review_head] > [review_finance] > done / rejected,
//              the optional steps follow the amount-based approval matrix
//
// The shapes of the XML, the builder definitions (theme, internal userview pages), the process package mapping and
// the plugin properties copy a real Joget DX 9 export (the stock Expenses Claim app). See README.md.
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
let AdmZip;
try {
  AdmZip = require('adm-zip');
} catch (e) {
  console.error('adm-zip is not installed. Run "npm install" in the repository root first.');
  process.exit(2);
}

// ---------------------------------------------------------------------------------------------------------------------
// Identifiers and standards
// ---------------------------------------------------------------------------------------------------------------------
const APP = 'acme_procure';
const APP_NAME = 'Acme Procurement Portal';
const UV = 'purchase_portal';
const THEME = 'theme_acme_procure';
const PROC = 'purchase_request';               // APP-3: process id equals the entity id
const F_REQ = 'purchase_request';
const F_APR = 'purchase_approval';
const TABLE = 'tbl_purchase_request';          // APP-2 / naming standard: tbl_<first form id>
const DL_ALL = 'purchase_requests';
const DL_MY = 'my_requests';
const DL_INBOX = 'approval_inbox';
const DL_RECENT = 'recent_requests';
const COUNTER = 'acme_request_counter';         // environment variable used by the ID generator
const TS = '2026-11-23 09:00:00.0 IST';         // fixed timestamp, same notation as a Joget export

// Deterministic 32-char upper-case hex id from a seed (Joget uses 32-char hex ids for menus and categories).
const did = seed => crypto.createHash('sha1').update(APP + ':' + seed).digest('hex').slice(0, 32).toUpperCase();

// displaytag ParamEncoder, the scheme Joget uses for datalist request parameters ("d-<hash>-fn_<filter>").
// Verified against the stock app: "ExpensesInbox" gives d-944469-, "list_ExpensesClaimEntry" gives d-1246756-.
function paramPrefix(datalistId) {
  let c = 17;
  for (const ch of 'x-' + datalistId) c = (3 * c + ch.charCodeAt(0)) | 0;
  return 'd-' + (c & 0x7fffff) + '-';
}
const fp = (dl, name) => paramPrefix(dl) + 'fn_' + name;

// Values from acme-standards.json (stored value English code, label Bahasa Indonesia).
const STD = {
  costCentres: [
    { value: 'CC-1000', label: 'CC-1000 - Kantor Pusat Jakarta' },
    { value: 'CC-2000', label: 'CC-2000 - Cabang Surabaya' },
    { value: 'CC-3000', label: 'CC-3000 - Cabang Bandung' },
    { value: 'CC-4100', label: 'CC-4100 - Pusat Data' }
  ],
  departments: [
    { value: 'finance', label: 'Keuangan' },
    { value: 'it', label: 'Teknologi Informasi' },
    { value: 'hr', label: 'Sumber Daya Manusia' },
    { value: 'operations', label: 'Operasional' },
    { value: 'procurement', label: 'Pengadaan' }
  ],
  status: [
    { value: 'draft', label: 'Draf' },
    { value: 'submitted', label: 'Diajukan' },
    { value: 'approved', label: 'Disetujui' },
    { value: 'rejected', label: 'Ditolak' }
  ],
  // approval_matrix: first rule whose maxAmount is not exceeded decides who approves last.
  matrix: [
    { maxAmount: 5000000, approvers: ['line_manager'] },
    { maxAmount: 50000000, approvers: ['line_manager', 'department_head'] },
    { maxAmount: 999999999999, approvers: ['line_manager', 'department_head', 'finance_director'] }
  ]
};
const LIMIT_HEAD = STD.matrix[0].maxAmount;     // above this the department head also approves
const LIMIT_FINANCE = STD.matrix[1].maxAmount;  // above this the finance director also approves

// ---------------------------------------------------------------------------------------------------------------------
// Brand palette. Only rgb()/rgba() is used inside HTML page content and form CustomHTML: a "#" followed later by "."
// could be read as a Joget hash variable. Hex is used only in theme variables, as in the stock export.
// ---------------------------------------------------------------------------------------------------------------------
const C = {
  ink: 'rgb(11,31,51)', ink2: 'rgb(16,46,66)', teal: 'rgb(13,148,136)', tealDark: 'rgb(15,118,110)',
  mint: 'rgb(94,234,212)', amber: 'rgb(245,158,11)', bg: 'rgb(243,246,248)', text: 'rgb(15,23,42)',
  muted: 'rgb(100,116,139)', line: 'rgba(15,23,42,0.08)'
};
const HEX = { ink: '#0B1F33', teal: '#0D9488', tealDark: '#0F766E', tealDarker: '#115E59', bg: '#F3F6F8', mint: '#5EEAD4' };

// Status pill: draft grey, submitted blue, approved green, rejected red (APP-8). Inline style so it renders under any
// theme; the class lets the theme refine it.
const PILL = {
  draft: ['rgb(241,245,249)', 'rgb(71,85,105)', 'rgb(148,163,184)'],
  submitted: ['rgb(219,234,254)', 'rgb(30,64,175)', 'rgb(59,130,246)'],
  approved: ['rgb(220,252,231)', 'rgb(22,101,52)', 'rgb(34,197,94)'],
  rejected: ['rgb(254,226,226)', 'rgb(153,27,27)', 'rgb(239,68,68)']
};
const pill = (value, label) => {
  const [bg, fg, dot] = PILL[value];
  return `<span class="acme-pill acme-pill-${value}" style="display:inline-flex;align-items:center;gap:6px;padding:3px 10px;` +
    `border-radius:999px;font-size:12px;font-weight:600;line-height:18px;white-space:nowrap;background:${bg};color:${fg};">` +
    `<span style="width:6px;height:6px;border-radius:50%;background:${dot};display:inline-block;"></span>${label}</span>`;
};

// ---------------------------------------------------------------------------------------------------------------------
// Form helpers (property sets follow the stock export and the joget-form-builder schemas)
// ---------------------------------------------------------------------------------------------------------------------
const noPlugin = () => ({ className: '', properties: {} });
const mandatory = (type = '') => ({ className: 'org.joget.apps.form.lib.DefaultValidator',
  properties: { mandatory: 'true', type, message: '', 'custom-regex': '' } });
const opts = list => list.map(o => ({ value: o.value, label: o.label, grouping: '' }));

const form = (id, name, description, elements) => ({
  className: 'org.joget.apps.form.model.Form',
  properties: {
    id, name, tableName: TABLE, description,
    loadBinder: { className: 'org.joget.apps.form.lib.WorkflowFormBinder', properties: {} },
    storeBinder: { className: 'org.joget.apps.form.lib.WorkflowFormBinder', properties: {} },
    permission: noPlugin(), noPermissionMessage: '', postProcessor: noPlugin(), postProcessorRunOn: 'both'
  },
  elements
});
const section = (id, label, columns, extra = {}) => ({
  className: 'org.joget.apps.form.model.Section',
  properties: Object.assign({ id, label, readonly: '', readonlyLabel: '', comment: '', visibilityControl: '',
    visibilityValue: '', regex: '', reverse: '', join: '', permissionReadonly: '', permission: noPlugin(),
    loadBinder: noPlugin(), storeBinder: noPlugin() }, extra),
  elements: columns
});
const column = (width, elements) => ({ className: 'org.joget.apps.form.model.Column', properties: { width }, elements });
const textField = (id, label, p = {}) => ({ className: 'org.joget.apps.form.lib.TextField', properties: Object.assign({
  id, label, value: '', placeholder: '', maxlength: '', size: '', readonly: '', readonlyLabel: '', workflowVariable: '',
  validator: noPlugin(), encryption: '', style: '', storeNumeric: '', requiredSanitize: '', disableIncrementDecrementArrow: '' }, p) });
const hidden = (id, value, p = {}) => ({ className: 'org.joget.apps.form.lib.HiddenField',
  properties: Object.assign({ id, value, useDefaultWhenEmpty: '', workflowVariable: '' }, p) });
const selectBox = (id, label, options, p = {}) => ({ className: 'org.joget.apps.form.lib.SelectBox', properties: Object.assign({
  id, label, options: [{ value: '', label: '', grouping: '' }].concat(opts(options)), optionsBinder: noPlugin(), value: '',
  multiple: '', size: '', controlField: '', readonly: '', readonlyLabel: '', workflowVariable: '', validator: noPlugin() }, p) });
const radio = (id, label, options, p = {}) => ({ className: 'org.joget.apps.form.lib.Radio', properties: Object.assign({
  id, label, options: opts(options), optionsBinder: noPlugin(), value: '', fullWidth: '', controlField: '', readonly: '',
  readonlyLabel: '', workflowVariable: '', validator: noPlugin() }, p) });
const datePicker = (id, label, p = {}) => ({ className: 'org.joget.apps.form.lib.DatePicker', properties: Object.assign({
  id, label, value: '', placeholder: '', dataFormat: 'yyyy-MM-dd', format: 'dd/mm/yy', yearRange: 'c-10:c+10',
  allowManual: '', datePickerType: '', currentDateAs: '', disableWeekends: '', startDateFieldId: '', endDateFieldId: '',
  readonly: '', readonlyLabel: '', workflowVariable: '', validator: noPlugin() }, p) });
const textArea = (id, label, p = {}) => ({ className: 'org.joget.apps.form.lib.TextArea', properties: Object.assign({
  id, label, value: '', placeholder: '', rows: '4', cols: '50', readonly: '', readonlyLabel: '', workflowVariable: '',
  validator: noPlugin(), requiredSanitize: '' }, p) });
// CustomHTML: only "value", never "html" (joget-form-builder §5: the extra key is the confirmed "System Error" cause).
const customHtml = (id, value) => ({ className: 'org.joget.apps.form.lib.CustomHTML',
  properties: { id, label: '', value, autoPopulate: '', requiredSanitize: '' } });
// Plain in-flow section heading (replaces a native section header bar, see joget-form-builder §5).
const heading = (id, text, note) => customHtml(id,
  `<div style="display:flex;align-items:baseline;gap:10px;margin:2px 0 12px 0;">` +
  `<span style="font-weight:700;font-size:15px;color:${C.text};">${text}</span>` +
  (note ? `<span style="font-size:12.5px;color:${C.muted};">${note}</span>` : '') + `</div>`);

// ---------------------------------------------------------------------------------------------------------------------
// Form 1: purchase_request (Permintaan Pembelian). Start form of the process and the read-only view everywhere else.
// ---------------------------------------------------------------------------------------------------------------------
const rupiah = n => 'Rp' + n.toLocaleString('de-DE');   // de-DE gives dot thousands separators: Rp5.000.000
const matrixInfo = customHtml('acme_matrix_info',
  `<div style="border:1px solid ${C.line};border-radius:12px;background:rgba(13,148,136,0.05);padding:12px 14px;margin:4px 0 6px 0;">` +
  `<div style="font-weight:600;font-size:13px;color:${C.tealDark};margin-bottom:8px;">Siapa yang menyetujui permintaan Anda?</div>` +
  `<div style="display:flex;flex-wrap:wrap;gap:8px;font-size:12.5px;color:${C.text};">` +
  `<span style="background:rgb(255,255,255);border:1px solid ${C.line};border-radius:999px;padding:4px 10px;">Sampai ${rupiah(LIMIT_HEAD)}: Atasan Langsung</span>` +
  `<span style="background:rgb(255,255,255);border:1px solid ${C.line};border-radius:999px;padding:4px 10px;">Sampai ${rupiah(LIMIT_FINANCE)}: Atasan + Kepala Departemen</span>` +
  `<span style="background:rgb(255,255,255);border:1px solid ${C.line};border-radius:999px;padding:4px 10px;">Di atas ${rupiah(LIMIT_FINANCE)}: Atasan + Kepala Departemen + Direktur Keuangan</span>` +
  `</div></div>`);

const purchaseRequest = form(F_REQ, 'Permintaan Pembelian',
  'Formulir permintaan pembelian Acme. Formulir awal proses purchase_request dan tampilan baca-saja di daftar.', [
    section('sec_requester', 'Data Pemohon', [
      column('50%', [
        { className: 'org.joget.apps.form.lib.IdGeneratorField', properties: { id: 'acme_request_no', label: 'No. Permintaan',
          envVariable: COUNTER, format: 'REQ/#date.yyyy#/?????', hidden: '', workflowVariable: '', isDistributedGeneration: '' } },
        textField('acme_requester', 'Nama Pemohon', { value: '#currentUser.fullName#', readonly: 'true' }),
        hidden('acme_requester_id', '#currentUser.username#'),
        datePicker('acme_request_date', 'Tanggal Permintaan', { value: '#date.yyyy-MM-dd#', readonly: 'true' })
      ]),
      column('50%', [
        selectBox('acme_department', 'Departemen', STD.departments, { validator: mandatory() }),
        selectBox('acme_cost_centre', 'Pusat Biaya', STD.costCentres, { validator: mandatory() })
      ])
    ]),
    section('sec_details', '', [
      column('100%', [heading('acme_details_head', 'Rincian Pembelian', 'Jelaskan apa yang dibeli dan kapan dibutuhkan.')]),
      column('50%', [
        textField('acme_title', 'Judul Permintaan', { placeholder: 'contoh: Laptop untuk tim analis data', maxlength: '120', validator: mandatory() }),
        selectBox('acme_category', 'Kategori', [
          { value: 'it_hardware', label: 'Perangkat Keras TI' }, { value: 'software', label: 'Perangkat Lunak dan Lisensi' },
          { value: 'office_supplies', label: 'Perlengkapan Kantor' }, { value: 'services', label: 'Jasa' },
          { value: 'other', label: 'Lainnya' }], { validator: mandatory() }),
        textField('acme_vendor', 'Usulan Pemasok', { placeholder: 'Opsional, nama pemasok yang Anda sarankan' })
      ]),
      column('50%', [
        datePicker('acme_needed_date', 'Tanggal Dibutuhkan', { currentDateAs: 'minDate', validator: mandatory() }),
        radio('acme_priority', 'Prioritas', [{ value: 'normal', label: 'Normal' }, { value: 'urgent', label: 'Mendesak' }], { value: 'normal' })
      ]),
      column('100%', [
        { className: 'org.joget.apps.form.lib.Grid', properties: { id: 'acme_items', label: 'Daftar Barang',
          options: [{ value: 'acme_item', label: 'Nama Barang' }, { value: 'acme_qty', label: 'Jumlah' },
            { value: 'acme_unit', label: 'Satuan' }, { value: 'acme_unit_price', label: 'Harga Satuan (Rp)' }],
          readonly: '', validator: noPlugin(), validateMinRow: '1', validateMaxRow: '',
          errorMessage: 'Tambahkan minimal satu barang.', loadBinder: noPlugin(), storeBinder: noPlugin() } }
      ]),
      column('50%', [
        // Naming standard, currency rule: money is a numeric text field, label ends with (Rp), shown as 1.250.000. "euro" style = dot thousands.
        // No decimals so the stored value is a plain integer (the process compares it with the approval matrix).
        textField('acme_total_amount', 'Total Biaya (Rp)', { style: 'euro', storeNumeric: 'true', numOfDecimal: '0',
          useThousandSeparator: 'true', prefix: 'Rp', postfix: '', incrementValue: '', decrementValue: '',
          placeholder: 'Jumlah seluruh barang', workflowVariable: 'acme_total_amount', validator: mandatory('numeric') })
      ]),
      column('50%', [matrixInfo])
    ], { 'header-style-display': 'none' }),
    section('sec_justification', '', [
      column('100%', [heading('acme_just_head', 'Justifikasi dan Lampiran', 'Alasan yang jelas mempercepat persetujuan.')]),
      column('100%', [
        textArea('acme_justification', 'Justifikasi Kebutuhan', { rows: '4',
          placeholder: 'Mengapa pembelian ini diperlukan, dan apa dampaknya bila ditunda?', validator: mandatory() }),
        { className: 'org.joget.apps.form.lib.FileUpload', properties: { id: 'acme_attachment', label: 'Lampiran (penawaran, spesifikasi)',
          validator: noPlugin(), maxSize: '10240', maxSizeMsg: 'Ukuran berkas melebihi batas 10 MB.', fileType: '',
          fileTypeMsg: 'Jenis berkas tidak didukung.', removeFile: '', permissionType: '', size: '', multiple: 'true',
          readonly: '', padding: '', attachment: 'true' } },
        hidden('acme_status', 'submitted')
      ])
    ], { 'header-style-display': 'none' })
  ]);

// ---------------------------------------------------------------------------------------------------------------------
// Form 2: purchase_approval (Persetujuan Pembelian). Same table; shows the request read-only (sub form on the same
// record, the stock-app pattern) and captures the decision. Mapped to every review activity of the process.
// ---------------------------------------------------------------------------------------------------------------------
const remarksValidator = {
  className: 'org.joget.apps.form.lib.BeanShellValidator',
  properties: { decoration: '', script: [
    'import org.joget.apps.form.model.Element;',
    'import org.joget.apps.form.model.Form;',
    'import org.joget.apps.form.service.FormUtil;',
    '',
    '// APP-5: remarks are required when the approver chooses Tolak (rejected).',
    'Form root = FormUtil.findRootForm(element);',
    'Element decisionField = FormUtil.findElement("acme_decision", root, formData);',
    'String decision = "";',
    'if (decisionField != null) {',
    '    String[] d = FormUtil.getElementPropertyValues(decisionField, formData);',
    '    if (d != null && d.length > 0 && d[0] != null) { decision = d[0]; }',
    '}',
    'if ("rejected".equals(decision) && (values == null || values.length == 0 || values[0] == null || values[0].trim().length() == 0)) {',
    '    formData.addFormError(FormUtil.getElementParameterName(element), "Catatan wajib diisi bila permintaan ditolak.");',
    '    return false;',
    '}',
    'return true;'].join('\n') }
};
const purchaseApproval = form(F_APR, 'Persetujuan Pembelian',
  'Formulir persetujuan untuk langkah review_manager, review_head dan review_finance.', [
    section('sec_request_view', 'Ringkasan Permintaan', [
      column('100%', [{ className: 'org.joget.apps.form.lib.SubForm', properties: { id: 'acme_request_view', label: '',
        formDefId: F_REQ, readonly: 'true', readonlyLabel: '', noframe: '', parentSubFormId: '', subFormParentId: '',
        permissionHidden: '',
        loadBinder: { className: 'org.joget.apps.form.lib.WorkflowFormBinder', properties: {} },
        storeBinder: { className: 'org.joget.apps.form.lib.WorkflowFormBinder', properties: {} } } }])
    ]),
    section('sec_decision', 'Keputusan Persetujuan', [
      column('100%', [customHtml('acme_decision_info',
        `<div style="font-size:13px;color:${C.muted};margin:0 0 10px 0;">Pilih <b style="color:${C.text};">Setujui</b> untuk meneruskan ` +
        `permintaan ke langkah berikutnya sesuai matriks persetujuan, atau <b style="color:${C.text};">Tolak</b> untuk menutupnya. ` +
        `Catatan wajib diisi bila Anda menolak.</div>`)]),
      column('100%', [
        radio('acme_decision', 'Keputusan', [{ value: 'approved', label: 'Setujui' }, { value: 'rejected', label: 'Tolak' }],
          { workflowVariable: 'acme_decision', validator: mandatory() }),
        textArea('acme_remarks', 'Catatan Persetujuan', { rows: '3',
          placeholder: 'Alasan keputusan, terutama bila permintaan ditolak', validator: remarksValidator }),
        hidden('acme_approver', '#currentUser.fullName#'),
        hidden('acme_decision_date', '#date.yyyy-MM-dd#')
      ])
    ])
  ]);

// ---------------------------------------------------------------------------------------------------------------------
// Datalists (APP-7 column order, APP-8 filters and pills, newest first, page size 10)
// ---------------------------------------------------------------------------------------------------------------------
const col = (id, name, label, p = {}) => Object.assign({ id, name, label, datalist_type: 'column', hidden: 'false',
  sortable: 'true', filterable: true, renderHtml: '', width: '', alignment: '', headerAlignment: '', style: '',
  exclude_export: '', include_export: '', format: noPlugin(), action: noPlugin() }, p);
const optionsFormat = options => ({ className: 'org.joget.plugin.enterprise.OptionsValueFormatter',
  properties: { options: options.map(o => ({ value: o.value, label: o.label, grouping: '' })), optionsBinder: noPlugin() } });
const RUPIAH_SCRIPT = [
  '// Shows the stored amount as Rp 1.250.000 (dot thousands separator).',
  'if (value == null || value.trim().length() == 0) { return ""; }',
  'try {',
  '    java.text.DecimalFormatSymbols s = new java.text.DecimalFormatSymbols();',
  "    s.setGroupingSeparator('.');",
  "    s.setDecimalSeparator(',');",
  '    java.text.DecimalFormat f = new java.text.DecimalFormat("#,##0", s);',
  '    String raw = value.split(",")[0].replaceAll("[^0-9.]", "");',
  '    return "Rp " + f.format(Double.parseDouble(raw));',
  '} catch (Exception e) {',
  '    return value;',
  '}'].join('\n');

function standardColumns(withLink) {
  return [
    col('column_0', 'acme_request_no', 'No. Permintaan', { 'style-font-weight': '600' }),
    col('column_1', 'acme_requester', 'Pemohon'),
    col('column_2', 'acme_department', 'Departemen', { format: optionsFormat(STD.departments) }),
    col('column_3', 'acme_total_amount', 'Jumlah (Rp)', { alignment: 'dataListAlignRight', headerAlignment: 'dataListAlignRight',
      format: { className: 'org.joget.plugin.enterprise.BeanShellFormatter', properties: { script: RUPIAH_SCRIPT } } }),
    col('column_4', 'acme_status', 'Status', { renderHtml: 'true', exclude_export: '',
      format: optionsFormat(STD.status.map(s => ({ value: s.value, label: pill(s.value, s.label) }))) }),
    col('column_5', 'dateCreated', 'Tanggal Dibuat')
  ];
}
const textFilter = (dl, id, name, label) => ({ id, name, label, filterParamName: fp(dl, name), hidden: '', datalist_type: 'filter',
  type: { className: 'org.joget.apps.datalist.lib.TextFieldDataListFilterType', properties: { defaultValue: '' } } });
const selectFilter = (dl, id, name, label, options) => ({ id, name, label, filterParamName: fp(dl, name), hidden: '', datalist_type: 'filter',
  type: { className: 'org.joget.plugin.enterprise.SelectBoxDataListFilterType', properties: { size: '', multiple: '', defaultValue: '',
    options: [{ value: '', label: 'Semua', grouping: '' }].concat(opts(options)), optionsBinder: noPlugin() } } });
const standardFilters = dl => [
  textFilter(dl, 'filter_0', 'acme_request_no', 'No. Permintaan'),
  selectFilter(dl, 'filter_1', 'acme_status', 'Status', STD.status),
  selectFilter(dl, 'filter_2', 'acme_department', 'Departemen', STD.departments)
];
const advancedBinder = (extraCondition = '') => ({ className: 'org.joget.plugin.enterprise.AdvancedFormRowDataListBinder',
  properties: { formDefId: F_REQ, joins: [], having: [], aggregateField: [], extraCondition, filters: [], groupBy: '' } });
const datalist = (id, name, description, binder, p = {}) => Object.assign({
  id, name, description, binder, template: noPlugin(), columns: standardColumns(), filters: standardFilters(id),
  rowActions: [], actions: [], pageSize: '10', orderBy: 'dateCreated', order: '1',   // order "1" = descending
  checkboxPosition: 'no', buttonPosition: 'bottomLeft', showPageSizeSelector: 'true',
  pageSizeSelectorOptions: '10,20,30,40,50,100', useSession: '', showDataWhenFilterSet: '', considerFilterWhenGetTotal: '',
  hidePageSize: ''
}, p);
const newRequestAction = { name: 'Data List Hyperlink Action', className: 'org.joget.apps.datalist.lib.HyperlinkDataListAction',
  id: 'action_0', label: 'Hyperlink', type: 'text', properties: { label: 'Buat Permintaan Baru', href: 'new_request',
    target: '_self', hrefParam: '', hrefColumn: '', confirmation: '', visible: '', datalist_type: 'action' } };

const dlAll = datalist(DL_ALL, 'Semua Permintaan Pembelian', 'Semua permintaan pembelian. Juga sumber angka KPI di beranda.',
  advancedBinder(), { actions: [newRequestAction] });
// Only the current user's requests. The core FormRowDataListBinder appends extraCondition to its HQL where clause;
// custom fields are addressed as customProperties.<field id>, exactly as the binder's own getColumnName() does.
const dlMy = datalist(DL_MY, 'Permintaan Saya', 'Permintaan pembelian milik pengguna yang sedang masuk.',
  { className: 'org.joget.apps.datalist.lib.FormRowDataListBinder', properties: { formDefId: F_REQ,
    extraCondition: "customProperties.acme_requester_id = '#currentUser.username#'" } },
  { actions: [newRequestAction] });
const dlInbox = datalist(DL_INBOX, 'Kotak Masuk Persetujuan', 'Tugas persetujuan yang menunggu pengguna (dipakai oleh menu kotak masuk).',
  advancedBinder(), { checkboxPosition: 'left' });
const dlRecent = datalist(DL_RECENT, 'Aktivitas Terbaru', 'Lima permintaan terbaru untuk beranda.', advancedBinder(),
  { pageSize: '5', filters: [], showPageSizeSelector: '' });

// ---------------------------------------------------------------------------------------------------------------------
// Landing page (HTML page "home"). Live numbers come from the datalist hash variable (Joget Enterprise plugin
// DatalistHashVariable): #datalist.total.<id>[<filter param>=<value>]# runs the datalist with that filter and returns
// the total row count; #datalist.html.<id># renders the rows (with column formatters, so the pills show) as a table.
// ---------------------------------------------------------------------------------------------------------------------
const ICON = {
  plus: '<path d="M12 5v14M5 12h14"/>',
  file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>',
  inbox: '<path d="M22 12h-6l-2 3h-4l-2-3H2"/><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z"/>',
  clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
  check: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4 12 14.01l-3-3"/>',
  x: '<circle cx="12" cy="12" r="10"/><path d="m15 9-6 6M9 9l6 6"/>',
  arrow: '<path d="M5 12h14M12 5l7 7-7 7"/>',
  activity: '<path d="M22 12h-4l-3 9L9 3l-3 9H2"/>',
  home: '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"/>',
  route: '<circle cx="6" cy="19" r="3"/><path d="M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15"/><circle cx="18" cy="5" r="3"/>'
};
const svg = (name, size = 22) => `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" ` +
  `stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICON[name]}</svg>`;
const LOGO = size => `<svg width="${size}" height="${size}" viewBox="0 0 48 48" aria-hidden="true">` +
  `<path d="M24 3 42 13.5v21L24 45 6 34.5v-21z" fill="rgba(255,255,255,0.10)" stroke="${C.mint}" stroke-width="2"/>` +
  `<path d="M16.5 33 24 14.5 31.5 33M19.4 26.5h9.2" fill="none" stroke="rgb(255,255,255)" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

const LANDING_CSS = `
.ui-html.ql-editor{white-space:normal;padding:0;overflow:visible}
.acme-home{white-space:normal;color:${C.text};max-width:1240px;margin:0 auto;padding:4px 0 28px 0}
.acme-home *{box-sizing:border-box}
.acme-home a{text-decoration:none}
.acme-hero{position:relative;overflow:hidden;border-radius:22px;padding:34px 36px;color:rgb(255,255,255);
 background:radial-gradient(circle at 88% 12%,rgba(94,234,212,0.30) 0,rgba(94,234,212,0) 38%),
 radial-gradient(circle at 8% 110%,rgba(245,158,11,0.22) 0,rgba(245,158,11,0) 42%),
 linear-gradient(135deg,${C.ink} 0%,${C.ink2} 52%,${C.tealDark} 100%);
 box-shadow:0 18px 40px -18px rgba(11,31,51,0.55);display:flex;gap:28px;align-items:stretch;flex-wrap:wrap}
.acme-hero:before{content:"";position:absolute;inset:0;opacity:0.18;pointer-events:none;
 background-image:radial-gradient(rgba(255,255,255,0.55) 1px,transparent 1.2px);background-size:22px 22px;
 -webkit-mask-image:linear-gradient(90deg,transparent 0,rgba(0,0,0,1) 70%);mask-image:linear-gradient(90deg,transparent 0,rgba(0,0,0,1) 70%)}
.acme-hero-main{position:relative;flex:1 1 320px;min-width:0}
.acme-brand{display:flex;align-items:center;gap:12px;margin-bottom:22px}
.acme-brand-name{font-weight:800;font-size:15px;letter-spacing:0.02em}
.acme-brand-sub{font-size:12px;opacity:0.72}
.acme-eyebrow{display:inline-block;font-size:12px;font-weight:600;letter-spacing:0.04em;color:${C.mint};
 background:rgba(94,234,212,0.12);border:1px solid rgba(94,234,212,0.30);border-radius:999px;padding:4px 12px;margin-bottom:14px}
.acme-hero h1{font-size:34px;line-height:1.15;font-weight:800;margin:0 0 10px 0;color:rgb(255,255,255);letter-spacing:-0.01em}
.acme-hero p.acme-lead{font-size:15.5px;line-height:1.6;margin:0 0 24px 0;max-width:560px;color:rgba(255,255,255,0.82)}
.acme-cta{display:flex;gap:12px;flex-wrap:wrap}
.acme-btn{display:inline-flex;align-items:center;gap:8px;padding:11px 18px;border-radius:12px;font-weight:700;font-size:14px;
 transition:transform .15s ease,box-shadow .15s ease,background-color .15s ease}
.acme-btn-primary{background:${C.mint};color:${C.ink} !important;box-shadow:0 8px 20px -8px rgba(94,234,212,0.7)}
.acme-btn-primary:hover{transform:translateY(-1px);box-shadow:0 12px 24px -8px rgba(94,234,212,0.8)}
.acme-btn-ghost{background:rgba(255,255,255,0.08);color:rgb(255,255,255) !important;border:1px solid rgba(255,255,255,0.22)}
.acme-btn-ghost:hover{background:rgba(255,255,255,0.16)}
.acme-hero-side{position:relative;flex:0 1 300px;display:flex;flex-direction:column;justify-content:space-between;gap:14px;
 background:rgba(255,255,255,0.07);border:1px solid rgba(255,255,255,0.16);border-radius:18px;padding:20px 22px;
 -webkit-backdrop-filter:blur(6px);backdrop-filter:blur(6px)}
.acme-side-label{font-size:12.5px;color:rgba(255,255,255,0.72);font-weight:500}
.acme-side-num{font-size:46px;font-weight:800;line-height:1;margin-top:6px;color:rgb(255,255,255)}
.acme-side-date{font-size:12.5px;color:rgba(255,255,255,0.72);display:flex;align-items:center;gap:8px}
.acme-side-bar{height:8px;border-radius:999px;background:rgba(255,255,255,0.12);overflow:hidden;display:flex}
.acme-side-bar span{display:block;height:100%}
.acme-section-title{display:flex;align-items:center;justify-content:space-between;margin:30px 2px 14px 2px}
.acme-section-title h2{font-size:17px;font-weight:700;margin:0;color:${C.text}}
.acme-section-title span{font-size:12.5px;color:${C.muted}}
.acme-kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(210px,100%),1fr));gap:16px}
.acme-kpi{position:relative;display:block;background:rgb(255,255,255);border:1px solid ${C.line};border-radius:16px;padding:18px 18px 16px 18px;
 box-shadow:0 1px 2px rgba(15,23,42,0.04),0 8px 24px -16px rgba(15,23,42,0.18);transition:transform .15s ease,box-shadow .15s ease,border-color .15s ease;overflow:hidden}
.acme-kpi:hover{transform:translateY(-2px);box-shadow:0 2px 4px rgba(15,23,42,0.05),0 16px 32px -16px rgba(15,23,42,0.28);border-color:rgba(13,148,136,0.30)}
.acme-kpi:after{content:"";position:absolute;left:0;top:0;bottom:0;width:4px;background:var(--acme-kpi)}
.acme-kpi-top{display:flex;align-items:center;justify-content:space-between}
.acme-kpi-icon{width:40px;height:40px;border-radius:12px;display:flex;align-items:center;justify-content:center;color:var(--acme-kpi);background:var(--acme-kpi-soft)}
.acme-kpi-num{font-size:32px;font-weight:800;line-height:1;margin-top:16px;color:${C.text}}
.acme-kpi-label{font-size:13.5px;font-weight:600;color:${C.text};margin-top:6px}
.acme-kpi-hint{font-size:12px;color:${C.muted};margin-top:2px}
.acme-kpi-go{color:${C.muted};opacity:0;transition:opacity .15s ease}
.acme-kpi:hover .acme-kpi-go{opacity:1}
.acme-actions{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(240px,100%),1fr));gap:16px}
.acme-action{display:flex;flex-direction:column;gap:14px;background:rgb(255,255,255);border:1px solid ${C.line};border-radius:18px;padding:22px;
 min-height:170px;color:${C.text} !important;box-shadow:0 1px 2px rgba(15,23,42,0.04);transition:transform .18s ease,box-shadow .18s ease,border-color .18s ease}
.acme-action:hover{transform:translateY(-3px);border-color:rgba(13,148,136,0.35);box-shadow:0 20px 36px -20px rgba(13,148,136,0.45)}
.acme-action-icon{width:48px;height:48px;border-radius:14px;display:flex;align-items:center;justify-content:center;color:rgb(255,255,255);
 background:linear-gradient(135deg,${C.teal},${C.tealDark});box-shadow:0 10px 18px -10px rgba(13,148,136,0.8)}
.acme-action-primary{background:linear-gradient(160deg,rgba(13,148,136,0.10),rgb(255,255,255) 60%);border-color:rgba(13,148,136,0.25)}
.acme-action-title{font-size:16px;font-weight:700;margin:0}
.acme-action-desc{font-size:13px;line-height:1.55;color:${C.muted};margin:0;flex:1}
.acme-action-go{display:inline-flex;align-items:center;gap:6px;font-size:13px;font-weight:700;color:${C.tealDark}}
.acme-action:hover .acme-action-go svg{transform:translateX(3px)}
.acme-action-go svg{transition:transform .18s ease}
.acme-split{display:grid;grid-template-columns:1fr;gap:16px;margin-top:30px}
.acme-card{background:rgb(255,255,255);border:1px solid ${C.line};border-radius:18px;box-shadow:0 1px 2px rgba(15,23,42,0.04);overflow:hidden}
.acme-card-head{display:flex;align-items:center;gap:10px;padding:16px 20px;border-bottom:1px solid ${C.line}}
.acme-card-head h3{font-size:15px;font-weight:700;margin:0;flex:1;color:${C.text}}
.acme-card-head a{font-size:12.5px;font-weight:700;color:${C.tealDark}}
.acme-card-head .acme-dot{width:30px;height:30px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:${C.tealDark};background:rgba(13,148,136,0.10)}
.acme-recent{overflow-x:auto}
.acme-recent:empty:before{content:"Belum ada permintaan. Mulai dengan membuat permintaan baru.";display:block;padding:28px 20px;color:${C.muted};font-size:13.5px}
.acme-recent table{width:100%;border-collapse:collapse !important;border:0 !important;font-size:13px}
.acme-recent th{text-align:left;font-size:11.5px;font-weight:700;letter-spacing:0.04em;text-transform:uppercase;color:${C.muted};
 background:rgb(248,250,252);border:0 !important;border-bottom:1px solid ${C.line} !important;padding:10px 16px !important;white-space:nowrap}
.acme-recent td{border:0 !important;border-bottom:1px solid ${C.line} !important;padding:12px 16px !important;vertical-align:middle;white-space:nowrap;color:${C.text}}
.acme-recent tr:last-child td{border-bottom:0 !important}
.acme-recent tbody tr:hover td{background:rgba(13,148,136,0.04)}
.acme-recent td:first-child{font-weight:700}
.acme-matrix{padding:16px 20px 20px 20px;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(240px,100%),1fr));gap:14px}
.acme-step{display:flex;flex-direction:column;gap:10px;padding:14px 16px;border:1px solid ${C.line};border-radius:14px;background:rgb(250,252,252)}
.acme-step-arrow{color:${C.muted};display:inline-flex;align-items:center}
.acme-legend{display:flex;flex-wrap:wrap;gap:10px;font-size:11.5px;color:rgba(255,255,255,0.75);margin-top:8px}
.acme-legend i{display:inline-block;width:8px;height:8px;border-radius:50%;margin-right:5px;vertical-align:middle}
.acme-step-amt{flex:0 0 auto;align-self:flex-start;font-size:12px;font-weight:700;color:${C.tealDark};background:rgba(13,148,136,0.10);border-radius:8px;padding:4px 8px;height:fit-content;white-space:nowrap}
.acme-step-who{font-size:13px;line-height:1.5;color:${C.text}}
.acme-step-who small{display:block;color:${C.muted};font-size:12px}
.acme-chain{display:flex;flex-wrap:wrap;align-items:center;gap:6px;margin-top:8px}
.acme-chain span{font-size:11.5px;font-weight:600;border-radius:999px;padding:2px 9px;background:rgb(241,245,249);color:rgb(51,65,85)}
.acme-foot{margin-top:22px;font-size:12px;color:${C.muted};text-align:center}
@media (max-width:900px){.acme-split{grid-template-columns:1fr}.acme-hero{padding:26px 22px}.acme-hero h1{font-size:27px}}
@media (max-width:560px){.acme-side-num{font-size:38px}.acme-kpi-num{font-size:28px}.acme-hero-side{flex-basis:100%}}
`;

// mode "joget": hash variables (live). mode "preview": sample values for LANDING-PAGE-PREVIEW.html.
function landingHtml(mode, sample) {
  const live = mode === 'joget';
  const statusCount = s => live ? `#datalist.total.${DL_ALL}[${fp(DL_ALL, 'acme_status')}=${s}]#` : String(sample.counts[s]);
  const total = live ? `#datalist.total.${DL_ALL}#` : String(sample.total);
  const name = live ? '#currentUser.firstName#' : sample.firstName;
  const today = live ? '#date.EEEE, d MMMM yyyy#' : sample.date;
  const recent = live ? `#datalist.html.${DL_RECENT}#` : sample.recentHtml;
  const href = target => live ? target : 'javascript:void(0)';
  const statusHref = s => live ? `all_requests?${fp(DL_ALL, 'acme_status')}=${s}` : 'javascript:void(0)';
  const kpis = [
    ['submitted', 'Menunggu Persetujuan', 'Status Diajukan', 'clock', 'rgb(59,130,246)', 'rgba(59,130,246,0.12)'],
    ['approved', 'Disetujui', 'Siap diproses Pengadaan', 'check', 'rgb(34,197,94)', 'rgba(34,197,94,0.12)'],
    ['rejected', 'Ditolak', 'Lihat catatan persetujuan', 'x', 'rgb(239,68,68)', 'rgba(239,68,68,0.12)'],
    ['draft', 'Draf', 'Belum diajukan', 'edit', 'rgb(100,116,139)', 'rgba(100,116,139,0.12)']
  ];
  const actions = [
    ['new_request', 'plus', 'Buat Permintaan Baru', 'Ajukan pembelian barang atau jasa. Nomor permintaan dibuat otomatis.', 'Mulai', true],
    ['my_requests', 'file', 'Permintaan Saya', 'Pantau status setiap permintaan yang Anda ajukan, dari Diajukan sampai Disetujui.', 'Buka daftar', false],
    ['approval_inbox', 'inbox', 'Kotak Masuk Persetujuan', 'Tinjau permintaan yang menunggu keputusan Anda: Setujui atau Tolak.', 'Tinjau', false],
    ['all_requests', 'grid', 'Semua Permintaan', 'Lihat seluruh permintaan pembelian dengan filter status dan departemen.', 'Jelajahi', false]
  ];
  const label = { line_manager: 'Atasan Langsung', department_head: 'Kepala Departemen', finance_director: 'Direktur Keuangan' };
  const tiers = [
    [`s.d. ${rupiah(LIMIT_HEAD)}`, '1 langkah persetujuan', STD.matrix[0].approvers],
    [`s.d. ${rupiah(LIMIT_FINANCE)}`, '2 langkah persetujuan', STD.matrix[1].approvers],
    [`> ${rupiah(LIMIT_FINANCE)}`, '3 langkah persetujuan', STD.matrix[2].approvers]
  ];
  // Proportion bar in the hero card. Live mode cannot compute ratios server side, so it shows the four status colours.
  const bar = live
    ? `<span style="flex:1;background:rgb(59,130,246)"></span><span style="flex:1;background:rgb(34,197,94)"></span><span style="flex:1;background:rgb(239,68,68)"></span><span style="flex:1;background:rgb(148,163,184)"></span>`
    : ['submitted', 'approved', 'rejected', 'draft'].map((s, i) => `<span style="flex:${sample.counts[s]};background:${['rgb(59,130,246)', 'rgb(34,197,94)', 'rgb(239,68,68)', 'rgb(148,163,184)'][i]}"></span>`).join('');

  const html = `
<style>${LANDING_CSS}</style>
<div class="acme-home">
 <section class="acme-hero">
  <div class="acme-hero-main">
   <div class="acme-brand">${LOGO(40)}<div><div class="acme-brand-name">ACME</div><div class="acme-brand-sub">Procurement Portal</div></div></div>
   <span class="acme-eyebrow">Portal Pengadaan Acme</span>
   <h1>Selamat datang, ${name}</h1>
   <p class="acme-lead">Ajukan, pantau, dan setujui permintaan pembelian di satu tempat. Rute persetujuan mengikuti matriks nilai Acme secara otomatis.</p>
   <div class="acme-cta">
    <a class="acme-btn acme-btn-primary" href="${href('new_request')}">${svg('plus', 18)}Buat Permintaan Baru</a>
    <a class="acme-btn acme-btn-ghost" href="${href('approval_inbox')}">${svg('inbox', 18)}Kotak Masuk Persetujuan</a>
   </div>
  </div>
  <div class="acme-hero-side">
   <div><div class="acme-side-label">Total permintaan pembelian</div><div class="acme-side-num">${total}</div></div>
   <div><div class="acme-side-bar">${bar}</div><div class="acme-legend"><span><i style="background:rgb(59,130,246)"></i>Diajukan</span><span><i style="background:rgb(34,197,94)"></i>Disetujui</span><span><i style="background:rgb(239,68,68)"></i>Ditolak</span><span><i style="background:rgb(148,163,184)"></i>Draf</span></div></div>
   <div class="acme-side-date">${svg('clock', 15)}${today}</div>
  </div>
 </section>
 <div class="acme-section-title"><h2>Ringkasan status</h2><span>Klik kartu untuk melihat daftarnya</span></div>
 <section class="acme-kpis">
  ${kpis.map(([s, title, hint, icon, color, soft]) => `<a class="acme-kpi" href="${statusHref(s)}" style="--acme-kpi:${color};--acme-kpi-soft:${soft}">
   <div class="acme-kpi-top"><span class="acme-kpi-icon">${svg(icon, 20)}</span><span class="acme-kpi-go">${svg('arrow', 18)}</span></div>
   <div class="acme-kpi-num">${statusCount(s)}</div><div class="acme-kpi-label">${title}</div><div class="acme-kpi-hint">${hint}</div></a>`).join('\n  ')}
 </section>
 <div class="acme-section-title"><h2>Aksi cepat</h2><span>Semua yang Anda perlukan untuk pengadaan</span></div>
 <section class="acme-actions">
  ${actions.map(([target, icon, title, desc, go, primary]) => `<a class="acme-action${primary ? ' acme-action-primary' : ''}" href="${href(target)}">
   <span class="acme-action-icon">${svg(icon, 24)}</span><h3 class="acme-action-title">${title}</h3><p class="acme-action-desc">${desc}</p>
   <span class="acme-action-go">${go}${svg('arrow', 16)}</span></a>`).join('\n  ')}
 </section>
 <section class="acme-split">
  <div class="acme-card">
   <div class="acme-card-head"><span class="acme-dot">${svg('activity', 17)}</span><h3>Aktivitas terbaru</h3><a href="${href('all_requests')}">Lihat semua</a></div>
   <div class="acme-recent">${recent}</div>
  </div>
  <div class="acme-card">
   <div class="acme-card-head"><span class="acme-dot">${svg('route', 17)}</span><h3>Matriks persetujuan</h3></div>
   <div class="acme-matrix">
    ${tiers.map(([amt, steps, who]) => `<div class="acme-step"><span class="acme-step-amt">${amt}</span><div class="acme-step-who">${steps}<small>Rute otomatis berdasarkan Total Biaya</small><div class="acme-chain">${who.map(w => `<span>${label[w]}</span>`).join(`<em class="acme-step-arrow">${svg('arrow', 12)}</em>`)}</div></div></div>`).join('\n    ')}
   </div>
  </div>
 </section>
 <div class="acme-foot">Acme Procurement Portal &middot; aplikasi demo dibangun di Joget DX 9 &middot; data contoh, perusahaan fiktif</div>
</div>`;
  // Collapse the whitespace between tags: the HTML page renders inside a Quill "ql-editor" box (white-space:pre-wrap).
  return html.replace(/>\s+</g, '><').replace(/\n/g, ' ').replace(/ {2,}/g, ' ').trim();
}

// ---------------------------------------------------------------------------------------------------------------------
// Theme (Builder Theme). Variables paint, CSS garnishes (joget-theme-builder doctrine). Key set copied from the stock
// export's theme; values are the Acme palette.
// ---------------------------------------------------------------------------------------------------------------------
const FONT = "'Plus Jakarta Sans','Segoe UI',system-ui,-apple-system,Roboto,'Helvetica Neue',Arial,sans-serif";
const THEME_CSS = `@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
/* Acme Procurement theme. Font: Plus Jakarta Sans (an Indonesian typeface), system fonts as fallback. */
* { font-family: ${FONT}; }
body { background: rgb(243,246,248); color: rgba(15,23,42,0.86); }
.dataList table, .dataList table th, .dataList table td, .dataList .filter_form, .dataList .filter_form input,
.dataList .filter_form select, .dataList .pagination, .dataList .pagination a { font-family: ${FONT} !important; }

/* Sidebar: left border + tint active state */
#navigation li.menu > a.menu-link span { flex: 1 !important; min-width: 0 !important; overflow: hidden !important; text-overflow: ellipsis !important; white-space: nowrap !important; }
#navigation li.category > a.dropdown span { font-weight: 600 !important; letter-spacing: 0.01em; }
#navigation li.category.active > a.dropdown span, #navigation li.category.current-category > a.dropdown span,
#navigation li.category.active > a.dropdown span i, #navigation li.category.current-category > a.dropdown span i { color: rgb(94,234,212) !important; }
#navigation li.menu > a.menu-link { border-radius: 10px !important; transition: background-color .15s ease; }
#navigation li.menu > a.menu-link:hover { background-color: rgba(255,255,255,0.06) !important; }
#navigation li.menu > a.menu-link:hover span, #navigation li.menu > a.menu-link:hover span i { color: rgb(255,255,255) !important; }
#navigation li.menu.current > a.menu-link, #navigation li.menu.active > a.menu-link {
  background-color: rgba(13,148,136,0.22) !important; border-left: 3px solid rgb(94,234,212) !important; border-radius: 0 10px 10px 0 !important; }
#navigation li.menu.current > a.menu-link span, #navigation li.menu.active > a.menu-link span,
#navigation li.menu.current > a.menu-link span i, #navigation li.menu.active > a.menu-link span i { color: rgb(255,255,255) !important; font-weight: 600 !important; }
#navigation li.menu.current > a.menu-link .badge, #navigation li.menu.active > a.menu-link .badge {
  background-color: rgb(11,31,51) !important; color: rgb(94,234,212) !important; border: 1px solid rgba(94,234,212,0.45) !important; }
#navigation::-webkit-scrollbar { width: 4px !important; }
#navigation::-webkit-scrollbar-track { background-color: rgb(11,31,51) !important; }
.sidebar_brand { border-bottom: 1px solid rgba(255,255,255,0.08); }
.sidebar_brand a { font-weight: 800 !important; letter-spacing: 0.02em; }

/* Content cards: forms, lists, inbox */
.form-section, .dataList, .datalist-inbox, .userview-body-content > .card {
  border-radius: 16px !important; }
.form-section { background: rgb(255,255,255); border: 1px solid rgba(15,23,42,0.08) !important; box-shadow: 0 1px 2px rgba(15,23,42,0.04), 0 10px 28px -20px rgba(15,23,42,0.25); margin-bottom: 16px !important; overflow: hidden; }
.form-section-title { background: rgba(13,148,136,0.06) !important; border-bottom: 1px solid rgba(15,23,42,0.06) !important; }
.form-section-title span { color: rgb(15,23,42) !important; font-weight: 700 !important; }
.form-cell input[type=text], .form-cell textarea, .form-cell select, .subform-cell input[type=text] { border-radius: 10px !important; transition: border-color .15s ease, box-shadow .15s ease; }
.form-cell input[type=text]:focus, .form-cell textarea:focus, .form-cell select:focus { border-color: rgb(13,148,136) !important; box-shadow: 0 0 0 3px rgba(13,148,136,0.15) !important; outline: none; }
.dataList table thead th { background: rgb(248,250,252) !important; color: rgb(71,85,105) !important; font-size: 12px !important; font-weight: 700 !important; text-transform: uppercase; letter-spacing: 0.04em; }
.dataList table tbody tr { transition: background-color .12s ease; }
.dataList table tbody tr:hover td { background: rgba(13,148,136,0.045) !important; }
.dataList .filter_form { background: rgb(255,255,255); border: 1px solid rgba(15,23,42,0.08); border-radius: 14px; padding: 12px 14px; margin-bottom: 14px; }

/* Buttons */
.btn, .form-button, button.btn { border-radius: 10px !important; font-weight: 600 !important; transition: transform .12s ease, box-shadow .15s ease, filter .15s ease; }
.btn-primary:hover, .form-button.btn-primary:hover, input.form-button:hover { filter: brightness(1.06); box-shadow: 0 8px 18px -10px rgba(13,148,136,0.9); transform: translateY(-1px); }

/* Status pills (the inline style already paints them; this only refines) */
.acme-pill { box-shadow: inset 0 0 0 1px rgba(15,23,42,0.04); }
`;
const LOGIN_TEMPLATE = `<!--{sampleWidth: '100%', tags: 'columns'}-->
<style>
  .login-container-4 { display: flex !important; height: 100% !important; background: radial-gradient(circle at 85% 15%, rgba(94,234,212,0.28) 0, rgba(94,234,212,0) 40%), linear-gradient(135deg, rgb(11,31,51) 0%, rgb(16,46,66) 55%, rgb(15,118,110) 100%) !important; }
  .login-form-4 { width: 80% !important; max-width: 980px; position: absolute !important; top: 50% !important; left: 50% !important; transform: translate(-50%, -50%) !important; background: rgb(255,255,255) !important; border-radius: 22px; overflow: hidden; box-shadow: 0 30px 60px -20px rgba(0,0,0,0.45) !important; }
  .input-container-4 { position: relative !important; }
  .login-form-4 input { border: 1px solid rgba(15,23,42,0.14) !important; border-radius: 12px !important; background: rgb(255,255,255) !important; height: 48px !important; float: unset !important; text-align: left !important; }
  .login-form-4 input:focus { border-color: rgb(13,148,136) !important; box-shadow: 0 0 0 3px rgba(13,148,136,0.15) !important; }
  .login-form-4 button { color: rgb(255,255,255) !important; background-color: rgb(13,148,136) !important; border: none; border-radius: 12px !important; height: 48px; font-weight: 700; }
  .login-form-4 .img { width: 50%; display: flex; flex-direction: column; justify-content: flex-end; padding: 40px; color: rgb(255,255,255); background: radial-gradient(circle at 80% 20%, rgba(94,234,212,0.35) 0, rgba(94,234,212,0) 45%), linear-gradient(160deg, rgb(11,31,51), rgb(15,118,110)); }
  .login-form-4 .img h3 { font-size: 26px; font-weight: 800; margin: 18px 0 8px 0; color: rgb(255,255,255); }
  .login-form-4 .img p { font-size: 14px; line-height: 1.6; opacity: 0.85; margin: 0; }
  .login-form-4 form { padding: 3rem !important; width: 50% !important; float: unset !important; text-align: left !important; max-width: 480px; margin: 0 auto; }
  .login-form-4 input#customUsername, .login-form-4 input#customUsername::placeholder, .login-form-4 input#customPassword, .login-form-4 input#customPassword::placeholder { color: rgb(15,23,42) !important; }
  @media only screen and (max-width: 768px) { .login-form-4 form { width: 100% !important; } .login-form-4 .img { display: none; } }
</style>
<div class="container-fluid login-container-4">
  <div class="login-form-4">
    <div class="row h-100" style="margin:0;">
      <div class="col" style="padding:0;">
        <div class="wrap d-md-flex h-100">
          <div class="img">${LOGO(56)}<h3>Acme Procurement Portal</h3><p>Ajukan, pantau, dan setujui permintaan pembelian di satu tempat.</p></div>
          <form style="padding: 3rem;">
            <h2 class="mb-1" style="color:rgb(15,23,42);font-weight:800">Masuk</h2>
            <p class="mb-4" style="color:rgb(100,116,139);font-size:14px">Gunakan akun Joget Anda.</p>
            <label style="color:rgb(15,23,42);font-weight:600">Username</label>
            <div class="mb-3 input-container-4"><input type="text" class="form-control w-100" id="customUsername" placeholder="Username Anda"></div>
            <label style="color:rgb(15,23,42);font-weight:600">Password</label>
            <div class="mb-5 input-container-4"><input type="password" class="form-control w-100" id="customPassword" placeholder="Password Anda"></div>
            <div class="input-container-4"><button type="button" class="btn btn-primary w-100" id="loginButton">Masuk</button></div>
          </form>
        </div>
      </div>
    </div>
  </div>
</div>
<!---->`;
const loginProps = { template: LOGIN_TEMPLATE, image: '', btnText: 'Masuk', btnTextColor: '#FFFFFF', customLogin: 'true',
  passwordPlaceholder: 'Password Anda', cardColor: '#FFFFFF', title: 'Masuk', usernameFieldColor: '#0F172A',
  passwordFieldColor: '#0F172A', customPasswordColor: '#0F172A', customUsername: 'Username', titleColor: '#0F172A',
  bgColor: '#0B1F33', customPassword: 'Password', btnColor: HEX.teal, customUsernameColor: '#0F172A', usernamePlaceholder: 'Username Anda' };
const themeProps = Object.assign({
  css: THEME_CSS, js: '', primaryColor: HEX.teal, horizontal_menu: 'dark-sidebar', hideSidebarUserMenu: '',
  disablePush: '', removeAssignmentTitle: '', shortcutLinkLabel: 'Shortcut', disableHelpGuide: '', logo: '', fav_icon: '',
  enableResponsiveSwitch: 'true', profile: '', homeAttractBanner: '', homeUrl: '', disablePwa: '', subheader: '', subfooter: '',
  userMenu: [], urlsToCache: '', userImage: '', shortcut: [], fontControl: '', inbox: 'current',
  'global-sidebarWidth': '272px', 'global-contentWidth': '1600px', 'global-contentPaddingInline': '36px',
  'global-contentPaddingInlineSM': '16px', 'global-footerHeight': '50px', 'global-footerheight': '50px', 'global-footerPadding': '16px 36px',
  'global-headerHeight': '64px', 'global-headerPadding': '', 'global-padding': '16px', 'global-fontSize': '14px',
  'global-lineHeight': '1.5714285714285714', 'global-iconMarginInline': '10px', 'global-iconSize': '15px', 'global-itemHeight': '42px',
  'global-itemPaddingBlock': '', 'global-itemPaddingInline': '20px', 'global-itemMarginBlock': '3px', 'global-itemMarginInline': '12px',
  'global-controlHeight': '40px', 'global-borderRadius': '10px', 'global-borderRadiusLG': '16px',
  'global-boxShadowSecondary': '0 6px 16px 0 rgba(15,23,42,0.08), 0 3px 6px -4px rgba(15,23,42,0.12), 0 9px 28px 8px rgba(15,23,42,0.05)',
  dx8background: HEX.bg, dx8backgroundImage: '', dx8contentbackground: '#FFFFFF', dx8footerBackground: '#FFFFFF',
  dx8footerColor: '#475569', dx8borderColor: 'rgba(15, 23, 42, 0.08)',
  'dx8-dark-sidebar-navBackground': HEX.ink, 'dx8-dark-sidebar-navLinkBackground': HEX.ink,
  'dx8-dark-sidebar-navLinkColor': 'rgba(255, 255, 255, 0.70)', 'dx8-dark-sidebar-navLinkIcon': 'rgba(255, 255, 255, 0.50)',
  'dx8-dark-sidebar-navActiveLinkColor': '#FFFFFF', 'dx8-dark-sidebar-navActiveLinkBackground': '',
  'dx8-dark-sidebar-navActiveIconColor': '#FFFFFF', 'dx8-dark-sidebar-navBadge': HEX.teal, 'dx8-dark-sidebar-navBadgeText': '#FFFFFF',
  'dx8-dark-sidebar-navScrollbarThumb': '#2A4A63', 'dx8-dark-sidebar-headerColor': '#FFFFFF', 'dx8-dark-sidebar-headerFontColor': HEX.ink,
  'dx8-dark-subMenuBgColor': HEX.ink, 'dx8-dark-subMenuColor': 'rgba(255, 255, 255, 0.70)',
  'org.joget.theme.lib.LoginPageComponent': { prefix: 'jgt-login-', className: 'org.joget.theme.lib.LoginPageComponent', properties: loginProps },
  'org.joget.theme.lib.ButtonComponent': { prefix: 'jgt-btn-', className: 'org.joget.theme.lib.ButtonComponent', properties: {
    colorPrimary: HEX.teal, colorPrimaryHover: HEX.tealDark, colorPrimaryActive: HEX.tealDarker, primaryColor: '#FFFFFF',
    primaryShadow: '0 2px 0 rgba(13, 148, 136, 0.20)', defaultBg: '#FFFFFF', defaultColor: 'rgba(15, 23, 42, 0.88)',
    defaultBorderColor: '#D5DCE3', defaultHoverBg: '#FFFFFF', defaultHoverColor: HEX.teal, defaultHoverBorderColor: HEX.teal,
    defaultActiveBg: '#FFFFFF', defaultActiveColor: HEX.tealDark, defaultActiveBorderColor: HEX.tealDark,
    defaultShadow: '0 2px 0 rgba(0, 0, 0, 0.02)', colorLink: HEX.teal, colorLinkHover: HEX.tealDark, colorLinkActive: HEX.tealDarker,
    linkHoverBg: 'transparent', colorError: '#EF4444', colorErrorHover: '#F87171', colorErrorActive: '#DC2626', dangerColor: '#FFFFFF',
    dangerShadow: '0 2px 0 rgba(239, 68, 68, 0.06)', colorBorder: '#D5DCE3', colorBgContainerDisabled: 'rgba(0, 0, 0, 0.04)',
    colorTextDisabled: 'rgba(0, 0, 0, 0.25)', borderColorDisabled: '#D5DCE3', contentFontSize: '14px', contentLineHeight: '1',
    paddingBlock: '8px', paddingInline: '16px', controlHeight: '38px', borderRadius: '10px', fontWeight: '600' } },
  'org.joget.theme.lib.FormComponent': { prefix: 'jgt-form-', className: 'org.joget.theme.lib.FormComponent', properties: {
    fieldWidth: '70%', labelFloat: 'left', borderRadiusLG: '16px', colorTextDescription: 'rgba(15, 23, 42, 0.50)', labelWidth: '30%',
    lineWidth: '1px', headerFontWeight: '700', labelRequiredMarkColor: '#EF4444', headerFontSize: '15px',
    controlOutline: 'rgba(13, 148, 136, 0.15)', lineType: 'solid', controlOutlineWidth: '3px', fontWeight: '400',
    colorText: 'rgba(15, 23, 42, 0.88)', labelMarginInlineEnd: '15px', labelFontSize: '14px', colorBgContainer: '#FFFFFF',
    headerFontHeight: '22', colorBorder: '#E2E8F0', labelHeight: 'auto', headerBg: '#F2FAF9', itemMarginBottom: '22px', labelAlign: 'end',
    sectionPaddingBottom: '20px', marginBottom: '10px', lineHeight: '1.5714285714285714', colorTextHeading: '#0F172A',
    headerHeight: '54px', headerPaddingInline: '24px', fieldFloat: 'left' } }
}, loginProps);   // the stock export keeps the login properties flat as well as inside the component

const themeDef = { theme: { className: 'org.joget.plugin.enterprise.BuilderTheme.' + THEME, properties: themeProps },
  properties: { id: THEME, name: 'Acme Procurement Theme', description: 'Tema Acme: sidebar gelap, aksen teal, kartu membulat.', userviewId: UV } };

// ---------------------------------------------------------------------------------------------------------------------
// Userview purchase_portal (APP-9 categories and menu names)
// ---------------------------------------------------------------------------------------------------------------------
const loggedIn = { className: 'org.joget.apps.userview.lib.LoggedInUserPermission', properties: {} };
const category = (seed, label, menus) => ({ className: 'org.joget.apps.userview.model.UserviewCategory',
  properties: { id: 'category-' + did('category:' + seed), label, hide: '', enableMegaMenu: '', comment: '', permission: loggedIn },
  menus });
const crud = (customId, label, datalistId, p = {}) => ({ className: 'org.joget.plugin.enterprise.CrudMenu', properties: Object.assign({
  id: did('menu:' + customId), customId, label, datalistId, addFormId: '', editFormId: F_REQ, keyName: '',
  'list-newButtonLabel': '', 'list-newLinkTarget': '', 'list-editLinkLabel': 'Lihat', 'list-editLinkTarget': '',
  'list-showDeleteButton': '', 'list-deleteButtonLabel': '', 'list-customHeader': '', 'list-customFooter': '', 'list-moreActions': [],
  'edit-readonly': 'yes', 'edit-readonlyLabel': '', 'edit-showEditButton': '', 'edit-saveButtonLabel': '', 'edit-backButtonLabel': 'Kembali',
  'edit-customHeader': '', 'edit-customFooter': '', 'edit-afterSaved': 'list', 'edit-afterSavedRedirectUrl': '',
  'edit-afterSavedRedirectParamName': '', 'edit-afterSavedRedirectParamvalue': '', 'edit-messageShowAfterComplete': '',
  'edit-allowRecordTraveling': '', 'edit-firstButtonLabel': '', 'edit-prevButtonLabel': '', 'edit-nextButtonLabel': '',
  'edit-lastButtonLabel': '', 'edit-moreActions': [], 'add-saveButtonLabel': '', 'add-cancelButtonLabel': '', 'add-customHeader': '',
  'add-customFooter': '', 'add-afterSaved': 'list', 'add-afterSavedRedirectUrl': '', 'add-afterSavedRedirectParamName': '',
  'add-afterSavedRedirectParamvalue': '', 'add-messageShowAfterComplete': '', buttonPosition: 'topLeft', selectionType: 'multiple',
  checkboxPosition: 'no', rowCount: 'true', cacheAllLinks: '', cacheListAction: '', enableOffline: '',
  userviewCacheScope: '', userviewCacheDuration: '' }, p) });
const pageHeader = (title, subtitle) => `<div style="margin:0 0 16px 0;"><div style="font-size:22px;font-weight:800;color:${C.text};">${title}</div>` +
  `<div style="font-size:13.5px;color:${C.muted};margin-top:2px;">${subtitle}</div></div>`;

const menus = {
  home: { className: 'org.joget.apps.userview.lib.HtmlPage', properties: { id: did('menu:home'), customId: 'home',
    label: '<i class="fas fa-home" aria-hidden="true"></i> Beranda', iconIncluded: false, content: landingHtml('joget'),
    userviewCacheScope: '', userviewCacheDuration: '', enableOffline: '' } },
  all: crud('all_requests', '<i class="fas fa-th-large" aria-hidden="true"></i> Semua Permintaan', DL_ALL,
    { 'list-customHeader': pageHeader('Semua Permintaan', 'Seluruh permintaan pembelian Acme. Saring berdasarkan status atau departemen.') }),
  newReq: { className: 'org.joget.apps.userview.lib.RunProcess', properties: { id: did('menu:new_request'), customId: 'new_request',
    label: '<i class="fas fa-plus-circle" aria-hidden="true"></i> Buat Permintaan Baru', processDefId: PROC, runProcessDirectly: 'Yes',
    runProcessSubmitLabel: 'Ajukan', showInPopupDialog: '', keyName: '', paramName: '', fieldPassover: '', fieldPassoverMethod: 'append',
    messageShowAfterComplete: 'Permintaan Anda telah diajukan dan dikirim untuk persetujuan.', redirectUrlAfterComplete: 'my_requests',
    redirectTarget: '', enableOffline: '', userviewCacheScope: '', userviewCacheDuration: '' } },
  my: crud('my_requests', '<i class="fas fa-file-alt" aria-hidden="true"></i> Daftar Permintaan', DL_MY,
    { 'list-customHeader': pageHeader('Permintaan Saya', 'Status terbaru setiap permintaan yang Anda ajukan.') }),
  inbox: { className: 'org.joget.plugin.enterprise.DataListInboxMenu', properties: { id: did('menu:approval_inbox'), customId: 'approval_inbox',
    label: '<i class="fas fa-inbox" aria-hidden="true"></i> Kotak Masuk Persetujuan', datalistId: DL_INBOX, appFilter: 'activity',
    processId: PROC, activityDefId: 'review_manager;review_head;review_finance', rowCount: 'true', showPopup: '', showDetail: '', keyName: '',
    'list-customHeader': pageHeader('Kotak Masuk Persetujuan', 'Permintaan yang menunggu keputusan Anda. Buka satu baris untuk Setujui atau Tolak.'),
    'list-customFooter': '', 'assignment-customHeader': '', 'assignment-customFooter': '', cacheAllLinks: '', cacheListAction: '',
    enableOffline: '', userviewCacheScope: '', userviewCacheDuration: '' } }
};
const userview = {
  className: 'org.joget.apps.userview.model.Userview',
  properties: { id: UV, name: APP_NAME, description: 'Portal permintaan pembelian Acme', welcomeMessage: '#date.EEE, d MMM yyyy#',
    logoutText: 'Keluar', footerMessage: 'Acme Procurement Portal &middot; demo Joget DX' },
  setting: { properties: { theme: { className: themeDef.theme.className, properties: themeProps }, permission: loggedIn,
    userviewId: UV, userviewDescription: 'Portal permintaan pembelian Acme', hideThisUserviewInAppCenter: '', userview_thumbnail: '',
    userview_category: '', __: '', userviewName: APP_NAME } },
  categories: [
    category('dashboard', '<i class="fas fa-chart-pie" aria-hidden="true"></i> Dasbor', [menus.home, menus.all]),
    category('my_requests', '<i class="fas fa-user" aria-hidden="true"></i> Permintaan Saya', [menus.newReq, menus.my]),
    category('approvals', '<i class="fas fa-check-double" aria-hidden="true"></i> Persetujuan', [menus.inbox])
  ]
};
// Internal userview pages: one trivial page per menu, exactly what the DX 9 builder exports for a plain menu.
const menuList = [menus.home, menus.all, menus.newReq, menus.my, menus.inbox];
const pages = menuList.map(m => ({ id: 'up-' + m.properties.id, name: m.properties.label.replace(/<[^>]+>/g, '').trim(),
  json: { elements: [{ className: 'menu-component' }], className: 'org.joget.apps.userview.model.UserviewPage',
    properties: { id: 'up-' + m.properties.id } } }));

// ---------------------------------------------------------------------------------------------------------------------
// Process package (XPDL 1.0, the shape the Joget process builder writes) + Joget package mapping
// ---------------------------------------------------------------------------------------------------------------------
const P = { requester: 'Pemohon', line_manager: 'Atasan Langsung', department_head: 'Kepala Departemen', finance_director: 'Direktur Keuangan' };
// Amount condition (JavaScript, evaluated by the workflow engine). Robust to "12500000", "12.500.000" and "12500000,00".
const amountAbove = n => `parseFloat(String(acme_total_amount).split(',')[0].replace(/[^0-9]/g,'')) &gt; ${n}`;
const REJECTED = `acme_decision==='rejected'`;
const ACT = [
  // id, name, kind (tool|form|route), performer, x, y, join, split transitions
  ['submit', 'Pengajuan', 'tool', 'requester', 120, 40],
  ['review_manager', 'Persetujuan Atasan', 'form', 'line_manager', 120, 30],
  ['route_manager', null, 'route', 'line_manager', 330, 40, null, ['t_mgr_reject', 't_mgr_head', 't_mgr_done']],
  ['review_head', 'Persetujuan Kepala Departemen', 'form', 'department_head', 330, 30],
  ['route_head', null, 'route', 'department_head', 540, 40, null, ['t_head_reject', 't_head_finance', 't_head_done']],
  ['review_finance', 'Persetujuan Keuangan', 'form', 'finance_director', 540, 30],
  ['route_finance', null, 'route', 'finance_director', 750, 40, null, ['t_fin_reject', 't_fin_done']],
  ['done', 'Selesai (Disetujui)', 'tool', 'requester', 960, 30, 'XOR'],
  ['rejected', 'Ditolak', 'tool', 'requester', 960, 110, 'XOR']
];
const TR = [
  ['t_submit_mgr', 'submit', 'review_manager'],
  ['t_mgr_route', 'review_manager', 'route_manager'],
  ['t_mgr_reject', 'route_manager', 'rejected', REJECTED, 'rejected'],
  ['t_mgr_head', 'route_manager', 'review_head', amountAbove(LIMIT_HEAD), 'above ' + LIMIT_HEAD],
  ['t_mgr_done', 'route_manager', 'done', 'OTHERWISE', 'approved'],
  ['t_head_route', 'review_head', 'route_head'],
  ['t_head_reject', 'route_head', 'rejected', REJECTED, 'rejected'],
  ['t_head_finance', 'route_head', 'review_finance', amountAbove(LIMIT_FINANCE), 'above ' + LIMIT_FINANCE],
  ['t_head_done', 'route_head', 'done', 'OTHERWISE', 'approved'],
  ['t_fin_route', 'review_finance', 'route_finance'],
  ['t_fin_reject', 'route_finance', 'rejected', REJECTED, 'rejected'],
  ['t_fin_done', 'route_finance', 'done', 'OTHERWISE', 'approved']
];
const xa = (n, v) => `<ExtendedAttribute Name="${n}" Value="${v}"/>`;
function activityXml([id, name, kind, perf, x, y, join, split]) {
  const impl = kind === 'route' ? '<Route/>' : kind === 'tool'
    ? '<Implementation>\n                        <Tool Id="default_application"/>\n                    </Implementation>'
    : '<Implementation>\n                        <No/>\n                    </Implementation>';
  let restr = '';
  if (join || split) {
    restr = '\n                    <TransitionRestrictions>\n                        <TransitionRestriction>' +
      (join ? `\n                            <Join Type="${join}"/>` : '') +
      (split ? `\n                            <Split Type="XOR">\n                                <TransitionRefs>\n` +
        split.map(t => `                                    <TransitionRef Id="${t}"/>`).join('\n') +
        `\n                                </TransitionRefs>\n                            </Split>` : '') +
      '\n                        </TransitionRestriction>\n                    </TransitionRestrictions>';
  }
  return `                <Activity Id="${id}"${name ? ` Name="${name}"` : ''}>
                    ${impl}
                    <Performer>${perf}</Performer>${restr}
                    <ExtendedAttributes>
                        ${xa('JaWE_GRAPH_PARTICIPANT_ID', perf)}
                        ${xa('JaWE_GRAPH_OFFSET', x + ',' + y)}
                    </ExtendedAttributes>
                </Activity>`;
}
function transitionXml([id, from, to, cond, name]) {
  const c = !cond ? '' : cond === 'OTHERWISE' ? '\n                    <Condition Type="OTHERWISE"/>'
    : `\n                    <Condition Type="CONDITION">${cond}</Condition>`;
  return `                <Transition From="${from}" Id="${id}"${name ? ` Name="${name}"` : ''} To="${to}">${c}
                    <ExtendedAttributes>
                        ${xa('JaWE_GRAPH_TRANSITION_STYLE', 'NO_ROUTING_ORTHOGONAL')}
                    </ExtendedAttributes>
                </Transition>`;
}
const marker = (type, perf, act, x, y) => xa(type === 'START' ? 'JaWE_GRAPH_START_OF_WORKFLOW' : 'JaWE_GRAPH_END_OF_WORKFLOW',
  `JaWE_GRAPH_PARTICIPANT_ID=${perf},CONNECTING_ACTIVITY_ID=${act},X_OFFSET=${x},Y_OFFSET=${y},JaWE_GRAPH_TRANSITION_STYLE=NO_ROUTING_ORTHOGONAL,TYPE=${type}_DEFAULT`);
const XPDL = `<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<Package xmlns="http://www.wfmc.org/2002/XPDL1.0" xmlns:xpdl="http://www.wfmc.org/2002/XPDL1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" Id="${APP}" Name="${APP_NAME}" xsi:schemaLocation="http://www.wfmc.org/2002/XPDL1.0 http://wfmc.org/standards/docs/TC-1025_schema_10_xpdl.xsd">
    <PackageHeader>
        <XPDLVersion>1.0</XPDLVersion>
        <Vendor/>
        <Created/>
    </PackageHeader>
    <Script Type="text/javascript"/>
    <Participants>
${Object.entries(P).map(([id, n]) => `        <Participant Id="${id}" Name="${n}">\n            <ParticipantType Type="ROLE"/>\n        </Participant>`).join('\n')}
    </Participants>
    <Applications>
        <Application Id="default_application"/>
    </Applications>
    <WorkflowProcesses>
        <WorkflowProcess Id="${PROC}" Name="Proses Permintaan Pembelian">
            <ProcessHeader DurationUnit="h"/>
            <DataFields>
${['acme_total_amount', 'acme_decision'].map(d => `                <DataField Id="${d}" IsArray="FALSE">\n                    <DataType>\n                        <BasicType Type="STRING"/>\n                    </DataType>\n                </DataField>`).join('\n')}
            </DataFields>
            <Activities>
${ACT.map(activityXml).join('\n')}
            </Activities>
            <Transitions>
${TR.map(transitionXml).join('\n')}
            </Transitions>
            <ExtendedAttributes>
                ${xa('JaWE_GRAPH_WORKFLOW_PARTICIPANT_ORDER', Object.keys(P).join(';'))}
                ${marker('START', 'requester', 'submit', 40, 50)}
                ${marker('END', 'requester', 'done', 1130, 40)}
                ${marker('END', 'requester', 'rejected', 1130, 120)}
            </ExtendedAttributes>
        </WorkflowProcess>
    </WorkflowProcesses>
    <ExtendedAttributes>
        ${xa('EDITING_TOOL', 'Web Workflow Designer')}
        ${xa('EDITING_TOOL_VERSION', '5.0-pre-alpha')}
    </ExtendedAttributes>
</Package>
`;
const setStatus = value => JSON.stringify({ formDefId: F_REQ, fields: [{ field: 'acme_status', value }] });
const packageMap = {
  forms: [['runProcess', F_REQ], ['review_manager', F_APR], ['review_head', F_APR], ['review_finance', F_APR]],
  tools: [['submit', 'submitted'], ['done', 'approved'], ['rejected', 'rejected']],
  // Participants: requester = the person who started the process; approvers default to the user "admin" so the demo
  // runs on a fresh server. Remap them in the process builder (Map Participants) for a real organisation.
  participants: [['requester', 'requester', 'runProcess'], ['line_manager', 'user', 'admin'], ['department_head', 'user', 'admin'],
    ['finance_director', 'user', 'admin'], ['processStartWhiteList', 'role', 'loggedInUser']]
};

// ---------------------------------------------------------------------------------------------------------------------
// appDefinition.xml
// ---------------------------------------------------------------------------------------------------------------------
const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
const dates = `<dateCreated class="java.sql.Timestamp">${TS}</dateCreated>\n         <dateModified class="java.sql.Timestamp">${TS}</dateModified>`;
const forms = [purchaseRequest, purchaseApproval];
const datalists = [dlAll, dlMy, dlInbox, dlRecent];
const formXml = forms.map(f => `      <formDefinition>
         <id>${f.properties.id}</id>
         <appId>${APP}</appId>
         <appVersion>1</appVersion>
         <name>${esc(f.properties.name)}</name>
         <tableName>${TABLE}</tableName>
         <json>${esc(JSON.stringify(f))}</json>
         <!--description>${esc(f.properties.description)}</description-->
         ${dates}
      </formDefinition>`).join('\n');
const dlXml = datalists.map(d => `      <datalistDefinition>
         <id>${d.id}</id>
         <appId>${APP}</appId>
         <appVersion>1</appVersion>
         <name>${esc(d.name)}</name>
         <json>${esc(JSON.stringify(d))}</json>
         <!--description>${esc(d.description)}</description-->
         ${dates}
      </datalistDefinition>`).join('\n');
const uvXml = `      <userviewDefinition>
         <id>${UV}</id>
         <appId>${APP}</appId>
         <appVersion>1</appVersion>
         <name>${esc(APP_NAME)}</name>
         <json>${esc(JSON.stringify(userview))}</json>
         <!--description>${esc(userview.properties.description)}</description-->
         ${dates}
      </userviewDefinition>`;
const builderXml = [{ id: THEME, name: themeDef.properties.name, type: 'theme', json: themeDef, description: '' }]
  .concat(pages.map(p => ({ id: p.id, name: p.name, type: 'INTERNAL_USERVIEW_PAGE', json: p.json, description: UV })))
  .map(b => `      <builderDefinition>
         <id>${b.id}</id>
         <appId>${APP}</appId>
         <appVersion>1</appVersion>
         <name>${esc(b.name)}</name>
         <type>${b.type}</type>
         <json>${esc(JSON.stringify(b.json))}</json>
         ${b.description ? `<description>${esc(b.description)}</description>\n         ` : ''}${dates}
      </builderDefinition>`).join('\n');
const entry = (key, body) => `            <entry>\n               <string>${key}</string>\n${body}\n            </entry>`;
const packageXml = `      <packageDefinition>
         <id>${APP}</id>
         <version>1</version>
         <name>${esc(APP_NAME)}</name>
         ${dates}
         <packageActivityFormMap>
${packageMap.forms.map(([act, formId]) => entry(`${PROC}::${act}`, `               <packageActivityForm>
                  <processDefId>${PROC}</processDefId>
                  <activityDefId>${act}</activityDefId>
                  <formId>${formId}</formId>
                  <type>SINGLE</type>
                  <autoContinue>false</autoContinue>
                  <!--disableSaveAsDraft>${act === 'runProcess' ? 'false' : 'true'}</disableSaveAsDraft-->
               </packageActivityForm>`)).join('\n')}
         </packageActivityFormMap>
         <packageActivityPluginMap>
${packageMap.tools.map(([act, value]) => entry(`${PROC}::${act}`, `               <packageActivityPlugin>
                  <processDefId>${PROC}</processDefId>
                  <activityDefId>${act}</activityDefId>
                  <pluginName>org.joget.plugin.enterprise.FormDataUpdateTool</pluginName>
                  <pluginProperties>${esc(setStatus(value))}</pluginProperties>
               </packageActivityPlugin>`)).join('\n')}
         </packageActivityPluginMap>
         <packageParticipantMap>
${packageMap.participants.map(([pid, type, value]) => entry(`${PROC}::${pid}`, `               <packageParticipant>
                  <processDefId>${PROC}</processDefId>
                  <participantId>${pid}</participantId>
                  <type>${type}</type>
                  <value>${value}</value>
               </packageParticipant>`)).join('\n')}
         </packageParticipantMap>
      </packageDefinition>`;

// The <!-- ... --> around builderDefinitionList is how Joget exports it; the importer un-comments it before parsing.
const XML = `<appDefinition>
   <appId>${APP}</appId>
   <id>${APP}</id>
   <version>1</version>
   <name>${esc(APP_NAME)}</name>
   <dateCreated class="java.sql.Timestamp">${TS}</dateCreated>
   <dateModified class="java.sql.Timestamp">${TS}</dateModified>
   <packageDefinitionList>
${packageXml}
   </packageDefinitionList>
   <formDefinitionList>
${formXml}
   </formDefinitionList>
   <userviewDefinitionList>
${uvXml}
   </userviewDefinitionList>
   <datalistDefinitionList>
${dlXml}
   </datalistDefinitionList>
   <!--builderDefinitionList>
${builderXml}
   </builderDefinitionList-->
   <pluginDefaultPropertiesList/>
   <environmentVariableList>
      <environmentVariable>
         <id>${COUNTER}</id>
         <appId>${APP}</appId>
         <appVersion>1</appVersion>
         <value>0</value>
         <remarks>Used for plugin: Id Generator Field (acme_request_no)</remarks>
      </environmentVariable>
   </environmentVariableList>
   <messageList/>
   <createdBy>admin</createdBy>
</appDefinition>
`;

// ---------------------------------------------------------------------------------------------------------------------
// Static preview (sample numbers). Same landing HTML and CSS; a simplified Joget shell around it with the theme colours.
// ---------------------------------------------------------------------------------------------------------------------
const SAMPLE_ROWS = [
  ['REQ/2026/00048', 'Rina Wulandari', 'it', 18750000, 'submitted', '23/11/2026 09:12'],
  ['REQ/2026/00047', 'Budi Santoso', 'operations', 3450000, 'approved', '22/11/2026 16:40'],
  ['REQ/2026/00046', 'Dewi Lestari', 'finance', 72500000, 'submitted', '22/11/2026 11:05'],
  ['REQ/2026/00045', 'Agus Pratama', 'hr', 1250000, 'rejected', '21/11/2026 14:22'],
  ['REQ/2026/00044', 'Sari Kusuma', 'procurement', 9800000, 'draft', '21/11/2026 08:57']
];
const deptLabel = v => STD.departments.find(d => d.value === v).label;
const statusLabel = v => STD.status.find(s => s.value === v).label;
// Same shape as DatalistHashVariable.getTable(): inline borders that the landing CSS overrides.
const sampleTable = '<table border="1" style="border-collapse:collapse;"><thead><tr>' +
  standardColumns().map(c => `<th style="padding:0px 5px;border:1px solid rgb(0,0,0);">${c.label}</th>`).join('') + '</tr></thead><tbody>' +
  SAMPLE_ROWS.map(r => '<tr>' + [r[0], r[1], deptLabel(r[2]), 'Rp ' + r[3].toLocaleString('de-DE'), pill(r[4], statusLabel(r[4])), r[5]]
    .map(v => `<td style="padding:0px 5px;border:1px solid rgb(0,0,0);">${v}</td>`).join('') + '</tr>').join('') + '</tbody></table>';
const SAMPLE = { firstName: 'Rina', date: 'Senin, 23 November 2026', total: 48,
  counts: { submitted: 9, approved: 27, rejected: 5, draft: 7 }, recentHtml: sampleTable };
const navItem = (icon, label, active) => `<li class="${active ? 'active' : ''}"><i class="pv-ic">${icon}</i><span>${label}</span></li>`;
const PREVIEW = `<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Acme Portal Preview</title>
<!-- Static preview of the purchase_portal landing page and theme_acme_procure, with sample numbers.
     Generated by build-acme-procure.js: the landing HTML and CSS are the same strings that go into the .jwa;
     the sidebar and top bar around it are a simplified imitation of the Joget DX 9 dark-sidebar layout.
     It works offline; with internet it also loads the Plus Jakarta Sans font, exactly like the theme. -->
<style>
${THEME_CSS.split('\n').filter(l => l.startsWith('@import')).join('\n')}
html,body{margin:0;padding:0}
body{background:rgb(243,246,248);font-family:${FONT};color:rgba(15,23,42,0.86)}
* { font-family: ${FONT}; }
.pv-wrap{display:flex;min-height:100vh}
.pv-side{width:272px;flex:0 0 272px;background:rgb(11,31,51);color:rgba(255,255,255,0.70);display:flex;flex-direction:column}
.pv-brand{display:flex;align-items:center;gap:10px;padding:18px 20px;border-bottom:1px solid rgba(255,255,255,0.08);color:rgb(255,255,255);font-weight:800;letter-spacing:0.02em}
.pv-cat{padding:18px 20px 6px 20px;font-size:12.5px;font-weight:600;color:rgba(255,255,255,0.55)}
.pv-side ul{list-style:none;margin:0;padding:0 12px}
.pv-side li{display:flex;align-items:center;gap:10px;height:42px;padding:0 14px;margin:3px 0;border-radius:10px;font-size:14px;cursor:pointer;border-left:3px solid transparent}
.pv-side li:hover{background:rgba(255,255,255,0.06);color:rgb(255,255,255)}
.pv-side li.active{background:rgba(13,148,136,0.22);border-left-color:rgb(94,234,212);border-radius:0 10px 10px 0;color:rgb(255,255,255);font-weight:600}
.pv-ic{display:inline-flex;width:18px;justify-content:center;font-style:normal}
.pv-badge{margin-left:auto;background:rgb(13,148,136);color:rgb(255,255,255);font-size:11px;font-weight:700;border-radius:999px;padding:1px 8px}
.pv-main{flex:1;min-width:0;display:flex;flex-direction:column}
.pv-top{height:64px;display:flex;align-items:center;justify-content:space-between;padding:0 36px;background:rgba(255,255,255,0.6);-webkit-backdrop-filter:blur(8px);backdrop-filter:blur(8px);border-bottom:1px solid rgba(15,23,42,0.08);position:sticky;top:0;z-index:2}
.pv-crumb{font-size:13px;color:rgb(100,116,139)}
.pv-user{display:flex;align-items:center;gap:10px;font-size:13.5px;font-weight:600}
.pv-avatar{width:34px;height:34px;border-radius:50%;background:linear-gradient(135deg,rgb(13,148,136),rgb(15,118,110));color:rgb(255,255,255);display:flex;align-items:center;justify-content:center;font-weight:700}
.pv-content{padding:24px 36px}
.pv-ribbon{position:fixed;right:12px;bottom:12px;background:rgba(11,31,51,0.88);color:rgb(255,255,255);font-size:12px;padding:6px 10px;border-radius:8px;z-index:5}
@media (max-width:900px){.pv-side{display:none}.pv-content{padding:16px}.pv-top{padding:0 16px}}
</style>
</head>
<body>
<div class="pv-wrap">
 <aside class="pv-side">
  <div class="pv-brand">${LOGO(30)}<span>Acme Procurement Portal</span></div>
  <div class="pv-cat">Dasbor</div>
  <ul>${navItem(svg('home', 16), 'Beranda', true)}${navItem(svg('grid', 16), 'Semua Permintaan')}</ul>
  <div class="pv-cat">Permintaan Saya</div>
  <ul>${navItem(svg('plus', 16), 'Buat Permintaan Baru')}${navItem(svg('file', 16), 'Daftar Permintaan')}</ul>
  <div class="pv-cat">Persetujuan</div>
  <ul><li><i class="pv-ic">${svg('inbox', 16)}</i><span>Kotak Masuk Persetujuan</span><span class="pv-badge">3</span></li></ul>
 </aside>
 <main class="pv-main">
  <div class="pv-top"><div class="pv-crumb">Dasbor / Beranda</div><div class="pv-user"><span>Senin, 23 Nov 2026</span><span class="pv-avatar">R</span></div></div>
  <div class="pv-content"><div class="ui-html ql-editor">${landingHtml('preview', SAMPLE)}</div></div>
 </main>
</div>
<div class="pv-ribbon">Pratinjau statis, angka contoh</div>
</body>
</html>
`;

// ---------------------------------------------------------------------------------------------------------------------
// Write everything
// ---------------------------------------------------------------------------------------------------------------------
const outDir = __dirname;
const jsonDir = path.join(outDir, 'json');
fs.mkdirSync(jsonDir, { recursive: true });
const writeJson = (name, obj) => fs.writeFileSync(path.join(jsonDir, name), JSON.stringify(obj, null, 2) + '\n');
forms.forEach(f => writeJson(`form_${f.properties.id}.json`, f));
datalists.forEach(d => writeJson(`datalist_${d.id}.json`, d));
writeJson(`userview_${UV}.json`, userview);
writeJson(`${THEME}.json`, themeDef);
writeJson('package_mapping.json', packageMap);
fs.writeFileSync(path.join(jsonDir, 'package.xpdl'), XPDL);
fs.writeFileSync(path.join(jsonDir, 'landing_page_home.html'), landingHtml('joget') + '\n');
fs.writeFileSync(path.join(outDir, 'LANDING-PAGE-PREVIEW.html'), PREVIEW);

const zip = new AdmZip();
zip.addFile('appDefinition.xml', Buffer.from(XML, 'utf8'));
zip.addFile('package.xpdl', Buffer.from(XPDL, 'utf8'));
const jwa = path.join(outDir, APP + '.jwa');
zip.writeZip(jwa);
console.log('wrote', jwa);
console.log('forms:', forms.map(f => f.properties.id).join(', '));
console.log('datalists:', datalists.map(d => d.id).join(', '));
console.log('userview:', UV, '| theme:', THEME, '| process:', PROC);
console.log('KPI filter param:', fp(DL_ALL, 'acme_status'));
