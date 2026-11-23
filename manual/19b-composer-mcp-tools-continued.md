# Composer MCP: Tool Reference (continued)

This page continues [Composer MCP: Tool Reference](19-composer-mcp-tools.md). It describes the Report Builder and API Builder tools, the four skill tools (`joget_skills_list`, `joget_skill_read`, `joget_skill_search`, `joget_get_contract`), the two session tools (`list_sessions`, `connect_session`) and the optional browser-connected composer tools (`form_*`, `list_*`, `ui_*`, `theme_*`, `process_*`). It is for developers and for anyone who needs the exact arguments and results of these tools.

## Report and API definition tools

Both tools need an Enterprise plugin on the Joget server. They check the license of the app version first, and they write inside one database transaction: a failed write leaves nothing behind. They take the common arguments from [Composer MCP: Tool Reference](19-composer-mcp-tools.md). Unlike the form, datalist and userview tools, the tool maps `id` to the item id only for `get` and `update`. For `validate` and `create`, pass the item id in `params` (`reportId` or `apiId`).

### builder_report

Reads and writes Report Builder definitions.

**Access:** API key. Needs the Joget Enterprise Report Builder plugin (409 `Native Report Builder plugin unavailable` otherwise). No `confirm`.

| Operation | Item id | `body` | Purpose |
|---|---|---|---|
| `list` | not used | not used | `[{"id", "name"}]` of the app version. |
| `get` | `id` (required) | not used | The stored report definition. |
| `validate` | `params.reportId` (required) | Required | Checks the envelope; saves nothing. |
| `create` | `params.reportId` (required) | Required | Creates a report. |
| `update` | `id` (required) | Required | Replaces a report. |

The report id must match `rp_[A-Za-z0-9_-]+`. The body is the report JSON: `properties.id` must equal the id, `properties.name` is required, and `header`, `body` and `footer`, when present, must be objects whose `elements` is an array. The body can be at most 2 MB.

**Returns:** `validate`: `{"valid": true, "scope": "..."}`. `create` and `update`: `{"status": "ok", "reportId", "data"}`.

**Errors:** 400 `reportId must be a native rp_ identifier`; 400 `properties.id must match reportId`; 400 `properties.name is required`; 404 `Report not found`; 409 `Report exists`; 409 `Identifier belongs to another builder type`; 403 `App version/license verification failed`; 413 `Report definition too large`.

Example call:

```json
{"operation": "validate", "appId": "leave_mgmt", "appVersion": "1", "params": {"reportId": "rp_leave_summary"}, "body": {"properties": {"id": "rp_leave_summary", "name": "Leave Summary"}, "body": {"elements": []}}}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"valid": true, "scope": "Native envelope only; element properties and rendering require installed-plugin verification"}}
```

**Safety:** `validate` checks the envelope only. Element properties and rendering are not checked.

### builder_apibuilder

Reads and writes API Builder definitions (REST APIs defined inside Joget).

**Access:** API key. Needs the Joget API Builder plugin (409 `Native API Builder plugin unavailable` otherwise). No `confirm`.

The operations, arguments and errors match `builder_report`, with these differences: the item id is `apiId` (`params.apiId` for `validate` and `create`), it must match `API-[A-Za-z0-9_-]+`, and the success results use `apiId`. The body's `elements`, when present, must be an array. The value of `internalKey` is shown as `[redacted]` in `get` and in success results, and a body that sends `[redacted]` back keeps the stored key.

Example call:

```json
{"operation": "list", "appId": "leave_mgmt", "appVersion": "1"}
```

**Safety:** secrets (`internalKey`) are never returned; masked values are restored from the stored definition on update.

## Skill tools

The four skill tools give the assistant read access to the skill library that is built into the plugin jar, and to the skills configured in **AI Central Config** (see [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md) for what the skills contain and how to add your own).

**Access:** the skill tools do **not** need the API key. They are read-only, they touch no Joget data and no database, and they serve a fixed tree of text files from inside the jar. Every response is limited to 40,000 characters; a longer text is cut at a line end and the result says how to continue.

### joget_skills_list

Lists the available skills.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `kind` | string | No | `knowledge` (core skills), `optional` (extra packs) or `central` (skills configured in Joget). Any other value returns an empty list. | all kinds |
| `query` | string | No | Case-insensitive text that must appear in the skill name or description. | no filter |

**Returns:** pretty-printed JSON `{"count", "skills": [...], "next"}`. A bundled skill has `name`, `description`, `kind`, `source: "bundled"`, `editions` (a list that can be empty) and `files` (the number of files in the skill). A skill from Joget has `name`, `description`, `kind: "central"`, `source: "central"` and `composers` (a list of class names; it is information only). When a configured skill has the same name as a bundled one, the configured skill is listed as `central:<name>` and the plain name stays the bundled skill.

**Errors:** with `isError`: `No skills are available: the bundled knowledge is missing from this build ... and no skills are configured in Joget Central Config.`

The `query` text is matched against the name and the description, so a word such as `datalist` matches many skills. Example call, for the skills configured in Joget only:

```json
{"kind": "central", "query": "datalist"}
```

Example result (shortened to the first skill; the second one in the real answer is `central:joget-known-solutions`). `central:joget-datalist-builder` has the same name as a bundled skill, so it is listed with the `central:` prefix; the bundled skill `joget-datalist-builder` is not part of this answer because `kind` is `central`:

```json
{
  "skills": [
    {"name": "central:joget-datalist-builder", "description": "Joget datalist patterns: column types, filter configuration, formatters (date, badge, file link, ...", "kind": "central", "source": "central", "composers": ["org.joget.ai.agent.lib.composer.DatalistComposerChat", "org.joget.ai.agent.lib.composer.OrchestratorChat"]}
  ],
  "count": 2,
  "next": "joget_skill_read {name} for the matching skill; joget_skill_search {query} to find by keyword. source=central skills are the AI Agent Builder default / Central Config skills configured in Joget."
}
```

**Safety:** read-only. Only skills with status `active` are listed from Joget.

### joget_skill_read

Reads a skill: the short guide first, then one section or file at a time.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `name` | string | Yes | Skill name from `joget_skills_list`. Case-insensitive. Old and short names resolve (for example `joget-forms` and `forms` both give `joget-form-builder`; `joget-ui` gives `joget-userview-composition`). A name without the `joget-` prefix, or a part of a name that matches only one skill, also resolves. Use `central:<name>` to read the configured skill when a bundled skill has the same name. | none |
| `section` | string | No | A heading of the guide, by its text or its slug (the slug appears in the section index). With `path`, the heading is cut out of that file. | whole guide |
| `path` | string | No | A file inside the skill, relative to the skill folder, for example `references/tools.md` or `contracts/textField.json`. Only text files are served. | `SKILL.md` |
| `offset` | integer | No | Character offset, to continue a response that was cut. | `0` |

**Returns:** plain text. The first line is a header such as `[joget-form-builder | chars 0-10021 of 10021]`. For a skill from Joget the header name has the prefix `central:`, for example `[central:joget-form-builder | chars 0-37584 of 37584]`; this also happens for a configured skill that has no bundled twin. If the text was cut, the header says `TRUNCATED: call again with offset=<n>`. With only `name`, the text is the guide, then `## Section index (use section=<slug>)`, then `## Files in this skill (use path=<relative path>)` grouped by folder. A skill that comes from Joget is one document: it has no files, `path` returns an error, and `section` selects a `##` heading.

**Errors** (with `isError`): `name is required. Call joget_skills_list.`; `Unknown skill '<name>'. Did you mean: ... ? Call joget_skills_list for all names.`; `No file '<path>' in skill <name>. Files: ...`; `'<path>' is not a text file`; `Section '<section>' not found in ... Headings: ...`; `No Central Config skill named '<name>'`; `'<name>' is a Central Config skill: one instructions document, no files. Omit path; use section=<heading> for a part.`

Example call:

```json
{"name": "joget-form-builder", "section": "binders"}
```

Example result (shortened):

```text
[joget-form-builder#binders | chars 0-1432 of 1432]

(the text of the Binders section follows)
```

Example call (a file):

```json
{"name": "joget-builder-api", "path": "references/tools.md"}
```

**Safety:** file names are resolved only against the list of files built into the jar. There is no access to the file system, and `..` is refused.

### joget_skill_search

Keyword search across the skills.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `query` | string | Yes | Keywords, a class name or a property name. Longer than 300 characters is cut. | none |
| `limit` | integer | No | Maximum number of bundled hits, from 1 to 25. | `8` |

**Returns:** pretty-printed JSON `{"query", "count", "hits": [{"skill", "path", "kind", "score", "excerpt"}], "next"}`. The bundled hits come first, ranked by score. `kind` is the file kind: `skill`, `reference`, `contract`, `example`, `eval`, `template`, `script` or `other`. Up to 5 more hits come from the skills configured in Joget; they have `path: "(central)"` and `kind: "central"`. The scores of the two groups cannot be compared. Open a hit with `joget_skill_read` using `name` and `path`.

**Errors:** an empty `query` returns the text `query is required`.

Example call:

```json
{"query": "WorkflowFormBinder store nothing", "limit": 3}
```

Example result (shortened; the real answer has up to 3 bundled hits, then up to 5 hits from skills configured in Joget):

```json
{"query": "WorkflowFormBinder store nothing", "count": 5, "hits": [{"skill": "joget-form-lint-gaps", "path": "SKILL.md", "kind": "skill", "score": 21.8, "excerpt": "L3: description: Known blind spots of builder_lint and builder_verify_app ... missing WorkflowFormBinder pair stores nothing ..."}, {"skill": "central:joget-form-builder", "path": "(central)", "kind": "central", "score": 3, "excerpt": "..."}], "next": "Open a hit: joget_skill_read {name:<skill>, path:<path>}"}
```

**Safety:** read-only.

### joget_get_contract

Returns the exact property contract of a plugin class.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `className` | string | Yes | A simple or full class name, any case: `SelectBox`, `org.joget.apps.form.lib.TextField`, `DatalistBinder`. | none |
| `offset` | integer | No | To continue a response that was cut. | `0` |

**Returns:** plain text with a header `[contract <className> (<n> file(s)) | chars ...]`, then each matching file after a line `=== <skill>/<path> ===`. Contract files are the JSON schemas and contract notes in the `contracts`, `dtb-contracts` and `schemas` folders of the skills. JSON files come first.

**Errors** (with `isError`): `className is required`; `No contract file named '<className>'. Closest search hits: ...`.

Example call:

```json
{"className": "SelectBox"}
```

The answer starts with the header `[contract SelectBox (1 file) | chars 0-3793 of 3793]`, then `=== joget-form-builder/contracts/selectBox.json ===` and the JSON schema of the class.

**Safety:** read-only. A contract describes a class that may not be installed on your server; check with `builder_plugin` `check` or `builder_introspect` `palette`. For the live property definition of an installed plugin use `builder_introspect` `properties`.

## Browser-connected composers (Preview)

> **Preview:** the browser-connected composer tools are optional and are not needed for building apps. They need the Agent Builder plugin installed on the same Joget instance (it supplies the composer tools and the skill store), and a Joget builder page open in a browser. The headless `builder_*` tools do the same jobs with no browser.

### How a browser session works

1. You open a builder page in Joget: the Form Builder, Datalist Builder, Userview (UI) Builder, Process Builder or Theme Builder. An injected script registers the open page with the server as a **slot** and then asks the server for changes about every 1.2 seconds.
2. The assistant calls `list_sessions` to see the open pages, then `connect_session` with the `slot_id` of the page it should work on.
3. The assistant calls the composer tools for that page type. Each change updates the slot, and the page in your browser applies the change at its next poll, so the canvas changes field by field.
4. You save in the builder. Except for `form_save`, none of the composer tools saves to the database.

Rules that follow from this:

- **Open the page before you start the session.** The server lists the composer tools only for page types that have an open tab (a tab counts as open if it contacted the server in the last 15 seconds). Claude Code reads the tool list once when it connects. If you open a builder page later, reconnect the MCP server (the `/mcp` command in Claude Code) so that the tools appear.
- **One client per page.** A page that another session has claimed answers `already connected to another session`.
- **Idle limits.** A session is removed after 2 hours without activity and a slot after 4 hours.
- **Unsaved edits belong to the user.** The tools change the state of the open canvas. A user who edits the same canvas by hand at the same time can lose changes. Tell the assistant when you are done and then save.

The browser-connected tools are **not** protected by the API key. See [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md) before you enable them on a shared server.

### Session tools

#### list_sessions

Lists the builder pages that are open and registered.

**Access:** no key. No parameters.

**Returns:** plain text. For each open page: `slot_id`, `type` (`form`, `list`, `ui`, `theme` or `process`), `name` (the title of the builder page, for example `Form Builder : Leave Request`), `id` (if the page has one), `app` and `status`: `available`, `connected`, `active (current)` or `in use by another session`. If no page is open: `No builder tabs registered yet.` followed by the instruction to open a builder page.

Example call: `{}`

Example result:

```text
Open builder tabs:

slot_id: 3f0d6c1e-8a2b-4c57-9d0e-7a1b2c3d4e5f
  type:   form
  name:   Form Builder : Leave Request
  id:     leave_request
  app:    leave_mgmt
  status: available

Call connect_session(slot_id) to start working on an artifact.
```

#### connect_session

Connects the assistant to one open builder page. Call it again with another `slot_id` to switch.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `slot_id` | string | Yes | The `slot_id` from `list_sessions`. | none |

**Returns:** JSON `{"status": "ok", "slot_id", "type", "artifact_name", "artifact_id", "app_id", "message"}`. The existing definition of the page is loaded into the session when the page was opened on an existing item.

**Errors:** returned as text `{"status": "error", "message": "..."}` (the MCP result is not flagged as an error): `slot_id is required. Call list_sessions to see available slots.`; `Slot not found: <id>. Call list_sessions to refresh.`; `'<name>' has no recently active browser tab - it was likely closed or navigated away from. Reopen the builder page and call list_sessions again to get its current slot_id.`; `Slot '<name>' is already connected to another session.`

Example call:

```json
{"slot_id": "3f0d6c1e-8a2b-4c57-9d0e-7a1b2c3d4e5f"}
```

Example result:

```json
{"status": "ok", "slot_id": "3f0d6c1e-8a2b-4c57-9d0e-7a1b2c3d4e5f", "type": "form", "artifact_name": "Form Builder : Leave Request", "artifact_id": "leave_request", "app_id": "leave_mgmt", "message": "Connected to 'Form Builder : Leave Request' (form, app: leave_mgmt). Use form_* tools to make changes. Call connect_session again with a different slot_id to switch artifacts."}
```

### Where the composer tools come from

Each composer type has a name prefix, and most of its tools are the tools of the matching in-product AI Composer in Agent Builder, offered under a prefixed name. The tool list therefore depends on the Agent Builder build that is installed. Check the real list and the argument definitions with `tools/list` after you connect; they are always right for your server.

| Page type | Prefix | Source of the tools | Local tools added by Composer MCP |
|---|---|---|---|
| Form Builder | `form_` | Form Composer tools | `form_save`, `form_list_skills`, `form_invoke_skill`, `form_save_skill` |
| Datalist Builder | `list_` | Datalist Composer tools | `list_get_current`, `list_get_context`, `list_list_skills`, `list_invoke_skill` |
| Userview (UI) Builder | `ui_` | UI Composer tools | `ui_get_current`, `ui_list_skills`, `ui_invoke_skill` |
| Theme Builder | `theme_` | Theme Composer tools | `theme_get_properties`, `theme_patch_json`, `theme_get_current`, `theme_list_skills`, `theme_invoke_skill` |
| Process Builder | `process_` | Process Composer tools | `process_get_current`, `process_patch_json`, `process_list_skills`, `process_invoke_skill` |

- A tool that Agent Builder marks as for the Orchestrator only (for example the tools that create a brand-new form, datalist or userview in the database) is never offered, and a call to it by name is refused.
- All arguments of the Agent Builder tools are strings. A structured argument (for example `properties`) is a JSON text inside the string.
- A tool name that does not exist returns the same text as a call before `connect_session`. A call before `connect_session` returns `{"status": "error", "message": "No artifact connected. Call list_sessions then connect_session first."}`. If the matching Agent Builder tool is not available the call returns a message such as `FormComposerAgentTool not available - ensure the AI Agent plugin is installed.`
- Errors of these tools come back as text JSON `{"status": "error", "message": ...}`; the MCP result is not flagged as an error.
- After a call that changed the canvas the page updates at its next poll. If the page has been closed, the change stays in the session only.

### Local skill tools of every composer

`<prefix>list_skills` and `<prefix>invoke_skill` exist for each composer.

| Tool | Parameters | What it does |
|---|---|---|
| `<prefix>list_skills` | none | Lists the skills for this composer type, followed by the built-in skills that fit the type (form: `joget-form-builder`; list: `joget-datalist-builder`; ui: `joget-userview-composition` and `joget-theme-builder`; theme: `joget-theme-builder`; process: `joget-process-builder`). |
| `<prefix>invoke_skill` | `name` (string, required): exact skill name. `section` (string, optional): a heading; leave it out to get the section index. | Returns the section index or one section of a skill. If the composer has no such skill, the built-in skill of that name answers with the same text as `joget_skill_read`. |

> **Note:** the composer skill tools read skills from a single stored setting (`AI_SKILLS`), not from the skill files of Agent Builder, and only skills that are scoped to that composer. The skills you add in **Settings > AI Central Config > Skills** are stored as files and are not reliably visible through these tools. Use `joget_skills_list` and `joget_skill_read` to reach every skill. For the same reason, `form_save_skill` writes to that setting: do not use it to add skills.

## Form and Datalist composer tools

The tables in this section and the next list the tools of the Agent Builder build described on this page. The argument tables show the arguments of the form tools; for the other composers, read the argument definitions in `tools/list`.

### Form Composer tools (`form_`)

Local tools:

| Tool | Parameters | Required | What it does |
|---|---|---|---|
| `form_save` | none | - | Saves the form that is in progress to the Joget database (a new form is created, an existing one is replaced). The builder page must be closed and reopened to show the saved form. The only composer tool that writes to the database: call it only when the user asks you to save. |
| `form_list_skills`, `form_invoke_skill` | see above | | |
| `form_save_skill` | `name`, `description`, `instructions` (all strings) | All three | Stores a skill in the `AI_SKILLS` setting, which the skill tools of Agent Builder do not read. Do not use. |

Tools from the Form Composer (names as listed by `tools/list`):

| Tool | Parameters (all strings) | What it does |
|---|---|---|
| `form_add_section` | `label` (required), `numberOfColumns` (`1`, `2` or `3`; default `1`), `totalSections` (optional progress hint) | Adds a section. Returns a `sectionId` for `form_add_field`. |
| `form_add_field` | `sectionId` (required), `fieldType` (required: a simple or full class name such as `TextField`), `id` (required, unique, snake_case), `label` (required), `columnIndex` (0-based, default `0`), `properties` (optional JSON text), `afterElementId` (optional) | Adds a field to a column of a section. The reply lists the properties that the field type has. |
| `form_update_element` | `elementId` (required), `properties` (required, JSON text) | Merges properties into a section or field. A `className` or `fieldType` key converts the field to another type. The form's own id targets the form root. |
| `form_remove_element` | `elementId` (required) | Removes a section or field. |
| `form_get_current_form` | none | Returns the form JSON in the session. |
| `form_apply_form_json` | `form` (required, complete form JSON text) | Replaces the whole form. Destructive: only when the user asks to rebuild the whole form. |
| `form_get_form_context` | none | Lists the forms of the app with ids, names, table names, column names and stored option values. |
| `form_get_palette_tools` | none | Lists the installed field types (simple name, label, class name). |
| `form_get_element_definition` | `simpleNames` (required): comma-separated simple class names | Returns the property definitions of those field types. |

Example call:

```json
{"sectionId": "section_1", "fieldType": "TextField", "id": "employee_name", "label": "Employee Name", "columnIndex": "0"}
```

(for tool `form_add_field`).

### Datalist Composer tools (`list_`)

Local tools: `list_get_current` (returns the datalist JSON in the session) and `list_get_context` (lists the forms of the app with ids, names, table names and field ids; use it before you choose a form id for the binder). The skill tools are described above.

Tools from the Datalist Composer: `list_create_enhanced_datalist` (creates the list with default columns, filters and an Edit row action, wired to a form), `list_add_smart_columns`, `list_configure_datalist_filters`, `list_add_column_formatters` (automatic formatters), the formatter tools `list_add_formatter_date`, `list_add_formatter_file_link`, `list_add_formatter_email_link`, `list_add_formatter_font`, `list_add_formatter_image`, `list_add_formatter_condition`, `list_add_formatter_options_static`, `list_add_formatter_options_binder_beanshell`, `list_add_formatter_options_binder_jdbc`, `list_add_formatter_options_binder_database_wizard`, `list_add_formatter_options_binder_form`, `list_add_formatter_options_binder_org_hierarchy` and `list_add_formatter_options_binder_user`, `list_configure_table_joins`, `list_add_datalist_actions`, `list_configure_column_visibility`, `list_configure_datalist_template`, `list_mutate_datalist` (remove, rename or reorder columns, replace formatters or actions, update filters, patch binder properties), `list_get_datalist_context`, `list_get_palette_tools`, `list_get_element_definition` and `list_patch_element` (set arbitrary properties on any element).

## UI, Theme and Process composer tools

### UI (Userview) Composer tools (`ui_`)

Local tools: `ui_get_current` (returns the userview JSON in the session). The skill tools are described above.

Tools from the UI Composer, in groups (the exact names and arguments are in `tools/list`):

| Group | Tools |
|---|---|
| Whole userview | `ui_generate_dashboard`, `ui_mutate_element`, `ui_set_element_visibility`, `ui_patch_userview` |
| Navigation | `ui_add_category`, `ui_add_crud_menu`, `ui_add_datalist_menu`, `ui_add_form_menu`, `ui_add_inbox_menu`, `ui_add_run_process_menu`, `ui_add_process_list_menu`, `ui_add_calendar_menu`, `ui_add_html_page_menu`, `ui_add_link_menu`, `ui_add_simple_menu`, `ui_list_navigation_structure`, `ui_edit_category`, `ui_set_category_permission`, `ui_edit_navigation_menu`, `ui_edit_html_page` |
| Page content: add | `ui_add_information_tile`, `ui_add_chart`, `ui_add_heading`, `ui_add_paragraph`, `ui_add_html_block`, `ui_add_button`, `ui_add_accordion`, `ui_add_quick_navigation`, `ui_add_crud`, `ui_add_calendar`, `ui_add_container`, `ui_add_dashboard`, `ui_add_form`, `ui_add_image`, `ui_add_import`, `ui_add_inbox`, `ui_add_static_tile`, `ui_add_jasper_reports`, `ui_add_label`, `ui_add_link`, `ui_add_list`, `ui_add_list_inbox`, `ui_add_manage_api_key`, `ui_add_manage_permission`, `ui_add_process_status`, `ui_add_report`, `ui_add_rich_text`, `ui_add_run_process`, `ui_add_sla_report`, `ui_add_script`, `ui_add_task_delegation`, `ui_add_universal_inbox`, `ui_add_user_profile` |
| Page content: read and edit | `ui_list_page_elements`, `ui_edit_information_tile`, `ui_edit_chart_display`, `ui_edit_chart_datasource`, `ui_edit_heading`, `ui_edit_paragraph`, `ui_edit_html_block`, `ui_edit_button`, `ui_edit_accordion_panels`, `ui_edit_quick_navigation_items`, and further `ui_edit_*` tools for the other components |
| Look and feel | `ui_update_header_theme`, `ui_update_nav_theme`, `ui_update_content_theme`, `ui_update_typography`, `ui_update_sidebar_mode` |
| Settings | `ui_update_userview_identity`, `ui_update_userview_messages`, `ui_set_userview_permission`, `ui_list_directory` |
| App context | `ui_get_app_context`, `ui_get_element_json`, `ui_get_userview_structure`, `ui_get_palette_tools` |

### Theme Composer tools (`theme_`)

Local tools: `theme_get_properties` (returns `theme.properties` as JSON, to see the property names and values before a change), `theme_get_current` (the full theme JSON), and `theme_patch_json` (parameter `theme`, required: a complete theme JSON text with the keys `properties`, `theme` and `components`; the last resort for a full rebuild).

Tools from the Theme Composer:

| Tool | What it does |
|---|---|
| `theme_update_header_theme` | Header bar colors and height. |
| `theme_update_nav_theme` | Sidebar and navigation colors (give all colors together). |
| `theme_update_content_theme` | Primary brand color, page and content background, borders, footer. |
| `theme_update_typography` | Global typography, spacing and layout dimensions. |
| `theme_update_sidebar_mode` | Sidebar layout mode and feature flags (then update the navigation and header colors). |
| `theme_apply_component_changes` | Property changes of a theme component (Card, Table, Button, Form, Input, and so on). |
| `theme_get_theme_components` | Index of the theme component plugins. |
| `theme_get_component_definition` | Full property definition of components by class name. |
| `theme_patch_theme_properties` | Sets raw key-value pairs in `theme.properties` (fallback). |
| `theme_check_color_contrast` | The WCAG contrast ratio of two hex colors. |

### Process Composer tools (`process_`)

Local tools: `process_get_current` (the process JSON of the builder: `xpdl`, `activityForms`, `activityPlugins`, `participants`; returns `null` until the builder page has sent its state) and `process_patch_json` (parameter `process`, required: the complete process JSON text with all four keys; replaces everything and does not deploy; a last resort).

Tools from the Process Composer:

| Tool | What it does |
|---|---|
| `process_create_process` | Creates a process with participants, activities, routes, transitions, variables, activity forms and plugins. |
| `process_add_participant` | Adds a swim lane. Participants are shared by all processes of the app. |
| `process_add_activity` | Adds a user task under a participant. |
| `process_add_route` | Adds a decision (gateway) route. |
| `process_add_tool_activity` | Adds an automated tool activity; use the exact class name from the tool list. |
| `process_add_subprocess_activity` | Adds a sub-process activity that calls another process by id. |
| `process_add_transition` | Connects two nodes. |
| `process_add_end_node` | Adds an End node. Each finishing branch needs its own. |
| `process_configure_activity_form` | Assigns a form to an activity. |
| `process_configure_activity_plugin` | Configures the assignment form actions of a user task. |
| `process_configure_route_rules` | Sets the conditions of a route. |
| `process_add_workflow_variable` | Adds a workflow variable. |
| `process_mutate_process` | A generic patch operation on the process JSON. |
| `process_patch_element` | Changes simple properties of an element by id. |
| `process_set_activity_deadline` | Sets a deadline on a user task. |
| `process_get_tool_plugins`, `process_get_tool_details` | Index and property definitions of the workflow tool plugins. |
| `process_get_app_context` | Forms, datalists and processes of the app. |

The Process Composer builds the graph on the canvas. It does not deploy and it does not map participants to people: do both in the builder, or with `builder_process`.

## Errors of the MCP endpoint

These errors come from the endpoint itself, not from a tool. The full list is in [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md).

| Where | Meaning |
|---|---|
| HTTP 404 `Session not found or expired — re-initialize.` | The `Mcp-Session-Id` header is missing, unknown, or belongs to a session that was idle for more than 2 hours. The client sends `initialize` again. |
| JSON-RPC `-32601` | Method not supported. |
| JSON-RPC `-32602` | Invalid parameters, for example `Prompt not found`. |
| JSON-RPC `-32002` | Resource not found: the `joget-skill://` address does not exist. |
| JSON-RPC `-32603` | Internal error. Read the Joget log. |

## Limitations and things to know

- **The list of composer tools depends on your Agent Builder build.** The tables above name the tools of the Agent Builder release that ships with this manual. Newer builds can add or rename tools; `tools/list` is the authority.
- **Only the form composer can save.** All other composer tools change the open canvas only. Save in the builder.
- **No key protection.** The session tools and composer tools do not check the API key. Use them on a trusted network.
- **Central skills and the composer skill tools.** See the note above: use `joget_skills_list` and `joget_skill_read` to reach skills configured in **AI Central Config**.
- **Tabs must stay open.** A builder page that has not contacted the server for 15 seconds is no longer listed and cannot be connected.
- **Skill responses are cut at 40,000 characters.** Continue with `offset`, or read one section or file at a time.
- **Skills from Joget need Agent Builder.** Without it, `joget_skills_list` shows only the bundled skills.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `form_*` tools are missing from the tool list | No Form Builder page was open when the client connected, or Agent Builder is not installed. | Open the builder page, reconnect the MCP server, and check Agent Builder is installed. |
| `list_sessions` says `No builder tabs registered yet.` | The page has not contacted the server in the last 15 seconds (for example the browser tab is in the background and throttled), or the builder type is not one of the five. | Bring the tab to the front, reload it, and call `list_sessions` again. |
| `already connected to another session` | Another client session holds the page. | Close that session, or wait until it has been idle for 2 hours and is removed, or open the page again and use the new `slot_id`. |
| A composer call returns `No artifact connected` | `connect_session` was not called. | Call `list_sessions`, then `connect_session`. |
| The canvas does not change after a tool call | The page is not polling (tab closed or asleep), or the browser blocks the script. | Reload the builder page and connect again. |
| A saved form does not show the change | The builder page still shows the earlier version. | Close and reopen the form in the builder. |
| `joget_skill_read` returns `Unknown skill` | The name is wrong or the skill is not `active` in Joget. | Run `joget_skills_list` and use a name from it. |
| A skill response ends with `TRUNCATED` | The text is longer than 40,000 characters. | Call again with the `offset` shown, or read one `section`. |

## Where to go next

- [Composer MCP: Tool Reference](19-composer-mcp-tools.md): the headless builder tools, error codes and call sequences.
- [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md): what the skills contain, prompts, resources and example prompts.
- [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md): the security model and the full error list.
- [Writing Good Composer Prompts](14-composer-prompt-guide.md): prompts for the in-product AI Composers.
