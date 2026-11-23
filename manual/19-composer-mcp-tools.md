# Composer MCP: Tool Reference

This page is the API reference for the headless builder tools of the Composer MCP server: the `builder_*` tools that create and change Joget apps without a browser. For each tool it gives the purpose, access rules, every parameter, the result, the errors, an example call and an example result, and the safety rules built into it. It also has the complete error code reference and recommended call sequences for common jobs. The skill tools (`joget_*`) and the browser-connected composer tools are on [Composer MCP: Tool Reference (continued)](19b-composer-mcp-tools-continued.md). The page is for developers and for anyone who needs to know exactly what the assistant is allowed to do.

> **Note:** Composer MCP is a pre-release (version 9.1.0-ALPHA). If something differs on your server, ask the assistant to run `builder_status` and list the tools.

## How tool calls work

### The tools at a glance

The server offers 16 headless builder tools, 4 skill tools, 2 session tools, and a variable set of composer tools for open builder tabs. The first three families (22 tools) are always listed by `tools/list`. While a Form Builder tab is open, the list has 35 tools (13 more, the `form_*` tools).

In the `tools/list` answer, the tools with an `operation` argument mark only `operation` as required; the server checks `appId`, `appVersion`, `id` and `body` when the call runs (400 if missing). Required arguments of the other tools: `builder_lint` (`kind`, `json`), `builder_verify_app` (`appId`, `appVersion`), `joget_skill_read` (`name`), `joget_skill_search` (`query`), `joget_get_contract` (`className`), `connect_session` (`slot_id`). `builder_status`, `joget_skills_list` and `list_sessions` have none.

| Tool | Family | Operations | Needs the API key | Needs an opt-in flag | `confirm: true` required for |
|---|---|---|---|---|---|
| `builder_status` | Discovery | none | Yes | No | - |
| `builder_introspect` | Discovery | `palette`, `properties` | Yes | No | - |
| `builder_plugin` | Plugins | `check`, `upload` | Yes | `upload`: `plugin-upload` | `upload` |
| `builder_app` | App | `create` | Yes | `app-create` | - |
| `builder_form` | Definitions | `list`, `get`, `create`, `update`, `patch`, `delete` | Yes | No | `delete` |
| `builder_datalist` | Definitions | `list`, `get`, `create`, `update`, `patch`, `delete` | Yes | No | `delete` |
| `builder_theme` | Definitions | `list`, `get`, `create`, `update`, `patch`, `delete` | Yes | No | `delete` |
| `builder_userview` | Definitions | `list`, `get`, `create`, `update`, `patch`, `delete`, `menus`, `page_get`, `page_update`, `menu_add` | Yes | No | `delete`; any call with `params.allowPageWipe` = `true` |
| `builder_process` | Process | `get`, `builder_read`, `validate`, `deploy`, `mappings` | Yes | No | `deploy` |
| `builder_workflow` | Process | `start`, `complete` | Yes | No | both |
| `builder_data` | Data | `rows`, `submit`, `file` | Yes | No | `submit`, `file` |
| `builder_generate_crud` | Generator | `run` | Yes | Needs the Joget Enterprise CRUD Generator plugin | `run` |
| `builder_report` | Definitions (see the continued page) | `list`, `get`, `validate`, `create`, `update` | Yes | Needs the Joget Enterprise Report Builder | - |
| `builder_apibuilder` | Definitions (see the continued page) | `list`, `get`, `validate`, `create`, `update` | Yes | Needs the Joget API Builder | - |
| `builder_lint` | Verify | none | Yes | No | - |
| `builder_verify_app` | Verify | none | Yes | No | - |
| `joget_skills_list`, `joget_skill_read`, `joget_skill_search`, `joget_get_contract` | Skills | none | No | No | - |
| `list_sessions`, `connect_session` | Browser session | none | No | No | - |
| `form_*`, `list_*`, `ui_*`, `theme_*`, `process_*` | Browser composers (Preview) | many | No | An open builder tab | - |

"Needs the API key" means: the call is accepted only with a valid key (`X-Builder-Api-Key` or `Authorization: Bearer`), a Joget administrator session, or anonymous mode. How to set the key and the opt-in flags is on [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md). The security consequences are on [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md).

There is no tool that calls an arbitrary URL, runs SQL, lists apps, publishes an existing app, or exports and imports apps. See "Limitations and things to know".

### The result

Every tool result is one text block that contains JSON. An MCP client calls a tool with `tools/call`, giving the tool `name` and an `arguments` object; the examples on this page show only the `arguments` object.

| Tool type | Success | Failure |
|---|---|---|
| Tools that call a builder route (`builder_app`, `builder_form`, `builder_datalist`, `builder_theme`, `builder_userview`, `builder_process`, `builder_workflow`, `builder_report`, `builder_apibuilder`, `builder_introspect`, `builder_generate_crud`, `builder_data`, `builder_plugin`) | `{"status": 200, "ok": true, "result": <body>}` | `{"status": <code>, "ok": false, "error": <body or message>}`, and the MCP result has `isError: true` for any status of 400 or above |
| `builder_status`, `builder_lint`, `builder_verify_app` | The JSON object described for the tool (no wrapper) | `builder_verify_app` returns `{"error": ...}` with `isError: true`. `builder_lint` reports problems inside its normal result |
| Access refused before the tool runs | - | `{"status": 401, "error": "Unauthorized: ..."}` or `{"status": 403, ...}`, with `isError: true` |
| Unexpected exception | - | `{"status": 500, "error": "<message>"}`, with `isError: true` |

`result` and `error` hold the parsed JSON body when the body is JSON, and plain text otherwise.

The `status` in these results is a code inside the result text. The HTTP response that carries the MCP result is HTTP 200 in all of these cases, including a refused key, a lint failure and a missing `confirm`. A client must read `isError` and the `status` field, not the HTTP status line. Only a request that is not valid JSON gets an HTTP error (400), and a request with an unknown session gets HTTP 404.

### Common arguments

All tools in the table above that have operations take these arguments. The tool-specific sections list only what is different.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `operation` | string (one of the tool's operations) | Yes | What to do. A missing or unknown value returns 400 `operation must be one of: ...`. | none |
| `appId` | string | Yes, except for `builder_introspect` and `builder_plugin` | Target app id. Letters, digits, underscore and hyphen; it cannot start with a hyphen. | none |
| `appVersion` | string of digits, for example `"1"` | Yes, except for `builder_introspect` and `builder_plugin` | Target app version. There is never a default. A missing value returns 400. (`builder_app` `create` assumes `"1"`.) | none |
| `id` | string | Depends on the operation | The id of the item to read or change. The server maps it to the right query parameter for the tool: form id, datalist id, userview id, theme id, report id, API id, form id (for data and CRUD generation) or class name (for `builder_introspect` and `builder_plugin`). | none |
| `params` | object of strings | No | Extra query parameters for the operation, for example `{"menuId": "home"}`. Values are sent as strings. Keys must match `[A-Za-z][A-Za-z0-9_]{0,40}`. `appId` and `appVersion` are not allowed here; pass them as top-level arguments. | none |
| `body` | object, array or string | Depends on the operation | The request body: a definition as a JSON object, or a string (XPDL for `builder_process` `deploy`). | none |
| `body_base64` | string | Only for file or jar upload | Raw bytes, base64 encoded. Only `builder_data` and `builder_plugin` have this argument. | none |
| `confirm` | boolean | Only for the operations marked in the table | Must be `true` for operations that delete data, deploy, run workflow, write records, generate artifacts or upload code. Without it the call returns 412 and does nothing. | `false` |

### Rules that apply to every builder tool

1. **Explicit target.** Every write names an `appId` and a numeric `appVersion`. A missing version returns 400. This stops a write from silently landing on the wrong version of an app.
2. **Calls are in-process.** The tools call the Builder API inside the Joget JVM. All of its guards run: access check, explicit target, one transaction per write, lint gate, page-wipe guard, cache refresh. Changes are in the database at once. Builder pages that are already open in a browser must be reloaded to show them.
3. **Run-as user.** Calls authorised with the key (or anonymous mode) write as the Joget user set by `joget.builder.api.runAs` (default `admin`). Calls authorised with an administrator session keep that administrator's own name.
4. **Confirm gate.** The assistant must set `confirm: true` only after you agreed to that exact action. If the instance has git configured for app definitions, each changed definition is committed after the call.

## Discovery tools

### builder_status

Reports how this call was accepted, which opt-in features are enabled and which builder tools exist. Call it first in every session.

**Access:** API key (or admin session). No opt-in. No `confirm`.

**Parameters:** none.

**Returns:** a JSON object.

| Field | Meaning |
|---|---|
| `mode` | Always `headless`. |
| `authorizedAs` | `api-key`, `admin-session` or `anonymous-optin`. |
| `optInFeatures` | `{"plugin-upload": true or false, "app-create": true or false}`. |
| `tools` | The names of the 16 builder tools. |
| `note` | A reminder of the write rules. |

**Errors:** 401 or 403 in the form `{"status": 401, "error": "Unauthorized: ..."}`.

Example call:

```json
{}
```

Example result:

```json
{
  "mode": "headless",
  "authorizedAs": "api-key",
  "optInFeatures": {"plugin-upload": false, "app-create": true},
  "tools": ["builder_app", "builder_form", "builder_datalist", "builder_userview", "builder_theme", "builder_process", "builder_workflow", "builder_report", "builder_apibuilder", "builder_introspect", "builder_generate_crud", "builder_data", "builder_plugin", "builder_lint", "builder_verify_app", "builder_status"],
  "note": "Writes need an explicit appId + numeric appVersion. Destructive operations need confirm:true. ..."
}
```

**Safety:** read-only.

### builder_introspect

Finds the real class names and property names of installed plugins, so that nobody has to guess them.

**Access:** API key. No opt-in. No `appId` needed.

| Operation | Purpose | Parameters (in `params` unless marked) |
|---|---|---|
| `palette` | Lists installed plugins that can be used in a builder. | `builder` (required): `form`, `datalist` or `userview`. |
| `properties` | Returns the property definition of one plugin. | `className` (required; or pass it as top-level `id`): the full class name. `refreshCache` (optional): `"true"` clears the lint schema cache. |

**Returns:**

- `palette`: an array of `{"className", "label"}`. For `form` these are the form elements. For `userview` these are the page components of the Page Builder (tiles, charts, and so on), not the menu plugins. For `datalist` each entry also has `kind`: `binder` (data binders), `column` (column formatters) or `action` (row and list actions). Datalist filter plugins are not in the palette; ask for them with `properties` or `joget_get_contract`.
- `properties`: the plugin's property options, as the plugin defines them (a JSON array of property pages). An empty array `[]` if the plugin defines none.

**Errors:** 400 `builder is required (form|datalist|userview)`; 400 `unknown builder: ...`; 400 `className is required`; 404 `Plugin not found: <className>`; 400 `plugin has no property options: <className>`.

Example call:

```json
{"operation": "properties", "id": "org.joget.apps.form.lib.DatePicker"}
```

Example result (shortened to the first property of the first page):

```json
{"status": 200, "ok": true, "result": [{"title": "Configure Date Picker", "properties": [{"name": "id", "label": "ID", "type": "autocomplete", "required": "True", "regex_validation": "^[a-zA-Z0-9_]+$", "validation_message": "Only alpha-numeric and underscore characters allowed"}]}]}
```

**Safety:** read-only.

### builder_plugin

Checks whether a plugin class is registered, and optionally uploads a plugin jar.

**Access:** API key. `check` needs nothing else. `upload` needs the server opt-in `plugin-upload` **and** `confirm: true`. No `appId` needed.

| Operation | Purpose | Parameters |
|---|---|---|
| `check` | Is the class registered right now? | `className` (required; in `params`, or as top-level `id`): one class name, or several separated by commas. |
| `upload` | Deploys a plugin jar into Joget. | `params.fileName` (required): a name of the form `name.jar` (letters, digits, `.`, `_`, `-`). `body_base64` (required): the jar bytes, base64 encoded; at most 50 MB. `params.className` (optional): a class that must appear in the result. `confirm: true`. |

**Returns:**

- `check`: `{"classes": [{"className", "registered", "name", "version"}]}`. `name` and `version` appear only when registered.
- `upload`: `{"fileName", "bytes", "uploaded", "classes": [{"className", "registered", ...}]}`. HTTP 200 when uploaded, 500 when the upload failed.

**Errors:** 400 `className is required`; 403 `'plugin-upload' is disabled...` (names the flag to set); 400 `fileName=<name>.jar is required`; 400 `empty body`; 413 `jar larger than 52428800 bytes`; 412 without `confirm`.

Example call:

```json
{"operation": "check", "params": {"className": "org.joget.plugin.enterprise.CrudMenu,org.joget.plugin.enterprise.EChartMenu"}}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"classes": [{"className": "org.joget.plugin.enterprise.CrudMenu", "registered": true, "name": "CRUD Menu", "version": "5.0.0"}, {"className": "org.joget.plugin.enterprise.EChartMenu", "registered": false}]}}
```

**Safety:** `upload` deploys arbitrary code into Joget. It is off unless the operator sets `joget.builder.api.enable=plugin-upload`, and it needs `confirm: true`. Keep it off in production.

## App tool

### builder_app

Creates a new, empty Joget app.

**Access:** API key **and** the server opt-in `app-create` (`joget.builder.api.enable=app-create`). No `confirm`.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `operation` | string | Yes | `create`. | none |
| `appId` | string | Yes | Id of the new app. | none |
| `appVersion` | string | No | Only checked for being numeric. A new app always starts at version 1. | `"1"` |
| `params.name` | string | No | Display name of the app. | the `appId` |
| `params.description` | string | No | Description. | empty |
| `params.publish` | string | No | `"true"` publishes the new app at once. Any other value leaves it unpublished. | unpublished |
| `params.as` | string | No | Joget user recorded as the creator. Must exist in the directory. | `admin` |

**Returns:** `{"status": "ok", "appId", "appVersion", "name", "published", "createdBy"}`.

**Errors:** 403 `'app-create' is disabled...`; 409 `App already exists: <appId> - pick a fresh appId`; 400 `as=<user>: no such user in the directory`; 400 with the validation messages of Joget joined by `; `.

Example call:

```json
{"operation": "create", "appId": "leave_mgmt", "params": {"name": "Leave Management", "publish": "true"}}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"status": "ok", "appId": "leave_mgmt", "appVersion": 1, "name": "Leave Management", "published": true, "createdBy": "admin"}}
```

**Safety:** refuses an id that exists, so it cannot overwrite an app. There is no tool to list, delete, export, import or publish an existing app. If `app-create` is off, ask an administrator to create the app in the Joget console and give you its id and version.

## Definition tools: forms, datalists and themes

`builder_form`, `builder_datalist` and `builder_theme` have the same six operations. They differ in the body and in the checks that run before a save.

### Operations shared by the three tools

| Operation | `id` | `body` | `confirm` | What it does |
|---|---|---|---|---|
| `list` | not used | not used | No | Lists the items of the app version: an array of `{id, name}` (forms also have `tableName`). |
| `get` | Required | not used | No | Returns the stored definition as JSON. |
| `create` | not used | Required: the full definition | No | Creates an item. The id comes from the body. 409 if the id already exists. |
| `update` | Required | Required: the full definition | No | Replaces the stored definition. 404 if the item does not exist. |
| `patch` | Required | Required: a patch (below) | No | Changes one or more elements without sending the whole definition. |
| `delete` | Required | not used | Yes | Deletes the item. 404 if it does not exist. |

**Patch bodies.** A patch is `{"elementId": "<id>", "set": {...}, "unset": ["key", ...]}`, or `{"ops": [<patch>, <patch>, ...]}` for several edits. `elementId` matches an element's `properties.id`, its `properties.customId`, or (for datalist columns and filters, which have no `properties`) its `id`. Use `_root` or leave it out to change the root. `set` and `unset` act on the element's `properties` object (or on the element itself if it has none). With `ops`, every edit applies or none is saved. No match returns 404; more than one match returns 409 with the matching paths; an edit without `set` or `unset` returns 400. On success the result is the normal update result plus `patched`: a list of `{elementId, path, before}` with the previous values.

**Bypassing lint.** `params: {"lint": "false"}` skips the lint gate on `create`, `update` and `patch` of forms and datalists. Use it only when the lint is wrong about a property that Joget accepts, and tell the user that you did. A bypass never fixes a real error.

### builder_form

Reads and writes form definitions.

**Body of `create` and `update`:** the full form JSON. `properties.id` is required. `properties.name` defaults to the id. `properties.tableName` defaults to the id. The form needs its load binder and store binder (`org.joget.apps.form.lib.WorkflowFormBinder`) in its top-level `properties`, or `builder_data` `submit` will report success and store nothing.

**Checks before a save (create, update, patch):** the form is linted (see `builder_lint`). Any error returns 400 and nothing is saved. `update` also parses the form with Joget's own form parser and refuses what it cannot parse. `update` keeps protected values (masked secrets) from the stored definition.

**Returns:**

| Operation | Result |
|---|---|
| `list` | `[{"id", "name", "tableName"}]` |
| `get` | The stored form JSON. |
| `create` | `{"status": "ok", "formId", "name", "tableName"}` |
| `update`, `patch`, `delete` | `{"status": "ok", "formId", "appId", "appVersion"}` (`patch` adds `patched`) |

**Errors:** 400 `form JSON must contain properties.id`; 400 lint failure `{"ok": false, "errors": [{"path", "problem", "expected"}], "hint": "nothing was saved..."}` (a lint failure is always 400, never 422); 400 `invalid form JSON`; 404 `Form not found`; 409 the id exists (create; the message can be a key such as `console.form.error.label.idExists`) or the patch matches several elements.

Example call (create, shortened):

```json
{
  "operation": "create",
  "appId": "leave_mgmt",
  "appVersion": "1",
  "body": {
    "className": "org.joget.apps.form.model.Form",
    "properties": {
      "id": "leave_request",
      "name": "Leave Request",
      "tableName": "leave_request",
      "loadBinder": {"className": "org.joget.apps.form.lib.WorkflowFormBinder", "properties": {}},
      "storeBinder": {"className": "org.joget.apps.form.lib.WorkflowFormBinder", "properties": {}}
    },
    "elements": [
      {"className": "org.joget.apps.form.model.Section", "properties": {"id": "section1", "label": "Request"},
       "elements": [{"className": "org.joget.apps.form.model.Column", "properties": {"width": "100%"},
         "elements": [{"className": "org.joget.apps.form.lib.TextField", "properties": {"id": "employee_name", "label": "Employee Name"}}]}]}
    ]
  }
}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"status": "ok", "formId": "leave_request", "name": "Leave Request", "tableName": "leave_request"}}
```

Example result when lint blocks the save (the same form, with `"tableName": "leave request"`). The MCP result has `isError: true`, and the status inside it is 400:

```json
{"status": 400, "ok": false, "error": {"ok": false, "errors": [{"path": "$.properties.tableName", "problem": "value does not match the declared format: leave request", "expected": "^[a-zA-Z0-9_]{1,20}$"}, {"path": "$", "problem": "tableName 'leave request' invalid - [a-zA-Z0-9_], max 57 chars (MySQL's 64 minus the app_fd_ prefix)"}], "hint": "nothing was saved. Fix the errors, or repost with &lint=false to force."}}
```

A second `create` with the same form id returns `{"status": 409, "ok": false, "error": {"error": "console.form.error.label.idExists"}}`.

Example call (patch):

```json
{"operation": "patch", "appId": "leave_mgmt", "appVersion": "1", "id": "leave_request", "body": {"elementId": "employee_name", "set": {"label": "Employee"}}}
```

**Safety:** lint gate; `update` replaces, so read with `get` first or use `patch`; `delete` needs `confirm: true` and does not drop the data table of the form.

### builder_datalist

Reads and writes datalist (list) definitions.

**Body of `create` and `update`:** the full datalist JSON with `id` and `name` at the top level (or in `properties`), a `binder` with a `className` (usually `org.joget.apps.datalist.lib.FormRowDataListBinder` with `properties.formDefId` set to the form id), `columns` and optional `filters`, `actions` and `rowActions`.

**Checks before a save:** structural lint (see `builder_lint`). Errors return 400 and nothing is saved. Warnings do not block; they come back in the success result as `lintWarnings`. A column that uses `OptionsValueFormatter` must carry an `options` key in the formatter properties, otherwise the save is refused (the formatter would fail on the first row). `update` parses the list with Joget's own parser.

**Returns:** the same shapes as `builder_form`, with `datalistId` instead of `formId`; `list` returns `[{"id", "name"}]`. A `create` or `update` with warnings adds `"lintWarnings": [{"severity": "WARN", "path", "problem"}]`.

**Errors:** 400 `datalist JSON must contain an id`; 400 lint failure with `errors` and `warnings`; 404 `Datalist not found`; 409 `Datalist already exists`.

Example call:

```json
{
  "operation": "create",
  "appId": "leave_mgmt",
  "appVersion": "1",
  "body": {
    "id": "leave_request_list",
    "name": "Leave Requests",
    "binder": {"className": "org.joget.apps.datalist.lib.FormRowDataListBinder", "properties": {"formDefId": "leave_request"}},
    "columns": [{"id": "column_0", "name": "employee_name", "label": "Employee Name"}]
  }
}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"status": "ok", "datalistId": "leave_request_list", "name": "Leave Requests", "appId": "leave_mgmt", "appVersion": 1}}
```

**Safety:** lint gate; `delete` needs `confirm: true`.

### builder_theme

Reads and writes theme definitions (the app themes of Joget DX 9).

**Body of `create` and `update`:** the theme JSON. `id` (top level, or `properties.id`) is required on `create`. `name` falls back to the id. `update` replaces the theme and keeps protected values (masked secrets) from the stored one.

**Returns:** `list` gives `[{"id", "name"}]`; `get` gives the stored theme JSON; `create`, `update`, `patch` and `delete` give `{"status": "ok", "themeId", ("name"), "appId", "appVersion"}`.

**Errors:** 400 `theme JSON must contain an id`; 404 `Theme not found`; 409 `Theme already exists`.

Example call:

```json
{"operation": "get", "appId": "leave_mgmt", "appVersion": "1", "id": "main_theme"}
```

**Safety:** no lint is run on themes; the specialist skill `joget-theme-builder` explains what a valid theme contains. `delete` needs `confirm: true`.

## Userview tool

### builder_userview

Reads and writes userviews (the app user interface). A userview stores its shell (categories and menus) in one record and the content of each menu page in its own record. Because of that, whole-userview writes are guarded and page edits have their own operations.

| Operation | `id` | `params` | `body` | `confirm` | What it does |
|---|---|---|---|---|---|
| `list` | not used | | | No | `[{"id", "name"}]` of the app version. |
| `get` | Required | `raw`: `"true"` returns the stored shell without page content (never post it back) | | No | The full userview including every page. |
| `create` | not used | | Required: full userview JSON; `id` at top level or in `properties` | No | Creates a userview and its page records. 409 if the id exists. |
| `update` | Required | `allowPageWipe`: `"true"` allows removing pages | Required: full userview JSON including every menu's `referencePage` | Yes if `allowPageWipe` is `"true"` | Replaces the userview. |
| `patch` | Required | | Patch as for forms | No | Edits one element of the userview. |
| `delete` | Required | | | Yes | Deletes the userview and its page records. |
| `menus` | Required | | | No | Cheap tree of categories and menus. |
| `page_get` | Required | `menuId` (required) | | No | The page JSON of one menu. |
| `page_update` | Required | `menuId` (required) | Required: page JSON | No | Replaces the page of one menu. |
| `menu_add` | Required | | Required: `{"categoryId" or "categoryLabel", "menu": {"className", "properties"}, "position"}` | No | Adds one menu to a category. |

**Details.**

- `get` returns the combined definition, with all pages inlined. If Joget cannot assemble the pages, the call returns 500 `{"ok": false, "problem": ..., "hint": ...}` instead of a misleading shell.
- `menus` returns `{"userviewId", "categories": [{"id", "label", "menus": [{"id", "label", "className", "customId", "hasPage", "menus"}]}]}`. `id` is the internal menu id (`properties.id`); `customId` is only the address alias. `hasPage` says whether the menu has a page record.
- `page_get` and `page_update` use the internal menu id from `menus`, never the `customId`. They return 404 if the menu does not exist, or if it exists but has no page record. These operations cannot create a page: the first page of a menu has to be created with a whole-userview `update` whose menu carries a `referencePage`.
- `menu_add`: `categoryId` selects a category by its id; `categoryLabel` selects it by label and creates it if it does not exist. `menu.properties.id` is generated if you leave it out. `position` is 0-based; the default is the end. A `customId` that is already used in the userview is refused with 409.
- `update` and `create` accept the whole document. Protected values (masked secrets) of the old definition are kept.

**Returns:** `create`, `update`, `patch` give `{"status": "ok", "userviewId", ("name"), "appId", "appVersion"}`. `delete` also returns `inlinePagesRemoved` (and a `note` if the page records could not be counted). `page_update` returns `{"status": "ok", "userviewId", "menuId", "pageId", "appId", "appVersion", "note"}`. `menu_add` adds `menuId`, `categoryId`, `categoryCreated` and `position` to the update result.

**Errors:** 400 `userviewId is required`, `menuId is required`, `menu with className is required`, `categoryId or categoryLabel is required`; 400 page-wipe refusal (below); 404 `Userview not found`, `Menu not found: no menu with properties.id ...`, `Menu ... has no inline page`; 409 id exists or `customId ... is already used`; 500 `saveUserviewPages failed`.

**Page-wipe refusal.** If an `update` body carries fewer `referencePage` entries than the userview has pages, the call returns 400:

```json
{"status": 400, "ok": false, "error": {"ok": false, "errors": [{"path": "categories[].menus[].referencePage", "problem": "the posted userview JSON carries 1 referencePage entry, but userview 'portal' currently has 6 inline page definition(s) ...", "wouldDelete": ["up-home", "up-requests"], "expected": "..."}], "hint": "nothing was saved. Repost with &allowPageWipe=true only if you really do mean to delete the page(s) listed in wouldDelete."}}
```

Only `params.allowPageWipe: "true"` together with `confirm: true` overrides it, and only for a deletion that was counted. Use `patch`, `menu_add` or `page_update` instead.

Example call (add a CRUD menu):

```json
{
  "operation": "menu_add",
  "appId": "leave_mgmt",
  "appVersion": "1",
  "id": "portal",
  "body": {
    "categoryLabel": "Requests",
    "menu": {
      "className": "org.joget.plugin.enterprise.CrudMenu",
      "properties": {"customId": "requests", "datalistId": "leave_request_list", "addFormId": "leave_request", "editFormId": "leave_request"}
    }
  }
}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"status": "ok", "userviewId": "portal", "appId": "leave_mgmt", "appVersion": 1, "menuId": "6f1c2a52-0d7f-4d0e-9a41-3c2f4b0a9c11", "categoryId": "category-8A1F0B7E6C3D4E5F9A2B1C0D3E4F5A6B", "categoryCreated": true, "position": 0}}
```

**Safety:** the page-wipe guard; `delete` needs `confirm: true`. A new userview is public until it has a permission setting; set a logged-in-user permission in `setting.properties.permission` (see the skill `joget-userview-composition`).

## Process and workflow tools

### builder_process

Reads, checks, deploys and wires processes. A deployed process has two parts with two write paths: the graph (XPDL), written only by `deploy`, and the bindings (which form, plugin and participant each activity uses), written by `mappings`.

**Access:** API key. `deploy` needs `confirm: true`. The Process Builder "save" route is switched off on purpose; `deploy` is the only way to change a graph.

| Operation | `body` | `confirm` | Purpose |
|---|---|---|---|
| `get` | not used | No | The deployed process package: graph and bindings. |
| `builder_read` | not used | No | The same content as the Process Builder loads it, with `packageVersion` as a string. Start of any `validate`. |
| `validate` | JSON (below) | No | A structural dry run. Saves nothing. |
| `deploy` | The whole XPDL document as a string | Yes | Deploys the package. Raises the package version and moves running cases onto it. |
| `mappings` | JSON (below) | No | Adds, changes or removes bindings. Does not change the version. |

**Returns:**

- `get`: `{"xpdl": <graph as JSON>, "activityForms": {...}, "activityPlugins": {...}, "participants": {...}, "packageId", "packageVersion", "appId", "appVersion"}`. The keys of the three binding maps look like `<processId>::<activityId>` (or `<processId>::<participantId>`). Copy them exactly.
- `builder_read`: the same, with `packageVersion` as a string (empty if nothing is deployed).
- `validate` success: `{"valid": true, "scope": "structural-only", "saveEnabled": false, "runtimeVerified": false, "unchecked": [...]}`. It does not check that forms and plugins exist, that participants resolve to people, or that Joget accepts the XPDL.
- `deploy`: `{"success": true, "appId", "appVersion", "packageId", "packageVersion", "processMigration"}`.
- `mappings`: `{"success": true, "appId", "appVersion", "packageVersion", "applied": {"activityForms": {...}, "activityPlugins": {...}, "participants": {...}}}` where each entry says `created`, `updated` or `removed`.

**`validate` body.** `{"definition": {"xpdl": {...}, "activityForms": {...}, "activityPlugins": {...}, "participants": {...}}, "diff": {"xpdl": ..., ...}, "expectedPackageVersion": "<packageVersion from builder_read>", "allowMigration": true}`. `definition` is the object from `builder_read` after your edit. `diff` is required and must not be empty; it may contain only the keys `xpdl`, `activityForms`, `activityPlugins`, `participants`, and `xpdl` must be present exactly when the graph changed. `allowMigration` must be `true` when the graph changed and a package is already deployed. The check covers: the package id equals `appId`; process, activity and transition ids are present and unique; transitions point to known activities; every tool activity has a plugin binding; every performer has a participant binding; binding keys match real activities; every binding has its required field (`formId`, `className` or `type`).

**`deploy` body.** The XPDL XML as a string. It must start with `<`. The `Package Id` must equal `appId`.

**`mappings` body.** Any subset of the three sections. For each key: an object adds or updates the binding, `null` removes it, and a key you leave out is not touched.

```json
{
  "activityForms": {"leave_approval::approve": {"formId": "leave_request", "type": "SINGLE"}},
  "activityPlugins": {"leave_approval::notify": {"className": "org.joget.apps.app.lib.EmailTool", "properties": {}}},
  "participants": {"leave_approval::manager": {"type": "role", "value": "manager"}}
}
```

A form binding needs `formId` (or `type: "EXTERNAL"` with `formUrl`); `disableSaveAsDraft` and `autoContinue` are optional booleans. A plugin binding needs a non-empty `className` and optional `properties`. A participant binding needs a `type` and a `value`.

**Errors:** 400 `request body must be the XPDL XML document`; 400 `XPDL deploy rejected: <message>` (the message comes from Joget's XPDL validation); 409 `a process migration is already running for app ...` (retry later); 409 `app ... has no deployed process - POST the XPDL to /api/process/deploy first` (for `mappings`); 409 `Package version changed; read process before saving` (for `validate`); 400 `... key '...' must be "<processDefId>::<id>"`; 412 without `confirm` on `deploy`.

Example call (deploy, XPDL shortened):

```json
{"operation": "deploy", "appId": "leave_mgmt", "appVersion": "1", "confirm": true, "body": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Package Id=\"leave_mgmt\" ...>...</Package>"}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"success": true, "appId": "leave_mgmt", "appVersion": 1, "packageId": "leave_mgmt", "packageVersion": 2, "processMigration": false}}
```

**Safety:** `deploy` needs `confirm: true` because it creates a new package version and migrates running cases, even when the XPDL did not change. Joget allows one migration at a time per app.

### builder_workflow

Starts and completes cases of a deployed process, to seed or test a flow. Both operations change live data.

**Access:** API key, `confirm: true` for both operations. The call runs in the context of a directory user (`params.as`), so participant mapping, tools and route conditions run as they would for that user.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `operation` | string | Yes | `start` or `complete`. | none |
| `appId`, `appVersion` | string | Yes | Target app version. | none |
| `confirm` | boolean | Yes | Must be `true`. | `false` |
| `params.processDefId` | string | `start`: Yes | The process id inside the deployed package (the short id such as `leave_approval`). | none |
| `params.recordId` | string | No | `start`: the record id to link the case to. | a new id |
| `params.activityId` | string | `complete`: Yes, unless you give `processId` and `activityDefId` | Id of the open activity instance. | none |
| `params.processId` | string | `complete`: with `activityDefId` | The id of the running case, as returned by `start`. | none |
| `params.activityDefId` | string | `complete`: with `processId` | Definition id of the open activity. | none |
| `params.as` | string | No | Directory user the call runs as. For `complete` the default is the first assignee of the activity. | `admin` |
| `body` | object | No | `{"variables": {"name": "value"}}` sets workflow variables. For `complete` you can also send `{"formData": {"field": "value"}}`: the form mapped to the activity is submitted through its own binders and validators, and the activity completes only if the form has no errors. | empty |

**Returns:**

- `start`: `{"status": "started", "processDefId", "processId", "recordId", "startedBy", "open": [{"activityId", "activityDefId", "name", "state", "assignees"}]}`.
- `complete`: `{"status": "completed", "processId", "activityId", "activityDefId", "completedBy", ("formId", "recordId", "row"), "open", "processState"}`.

**Errors:** 400 `processDefId is required`; 400 `send activityId, or processId and activityDefId`; 404 `no process '<id>' in the deployed package of this app version`; 404 `no open activity for that id; open now: ...`; 409 `process did not start (whitelist, or start-participant mapping refused <user>)`; 409 `activity ... has no assignee; pass as=`; 409 `activity ... has no form mapped; complete it with variables only`; 403 `<user> holds no assignment for <activity>`; 422 `{"status": "invalid", "errors": {...}, "note": "form not stored, activity left open"}`; 412 without `confirm`.

Example call (start):

```json
{"operation": "start", "appId": "leave_mgmt", "appVersion": "1", "confirm": true, "params": {"processDefId": "leave_approval", "as": "admin"}, "body": {"variables": {"status": "submitted"}}}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"status": "started", "processDefId": "leave_mgmt#1#leave_approval", "processId": "412", "recordId": "412", "startedBy": "admin", "open": [{"activityId": "412_leave_mgmt_leave_approval_approve", "activityDefId": "approve", "name": "Approve", "state": "open.not_running.not_started", "assignees": ["admin"]}]}}
```

**Safety:** `confirm: true` on both operations. When you complete with `formData`, send the full row of the form, not only the changed field: fields you leave out can be blanked. Read the row with `builder_data` `rows`, change the field, and send all of it.

## Data tool

### builder_data

Reads, creates and updates form records, and attaches files to records. Records go through the same path as a user submitting the form: validators, the form's store binder and post-form-submission processing all run.

**Access:** API key. `submit` and `file` need `confirm: true`. `id` is the **form id**.

| Operation | Purpose | Parameters |
|---|---|---|
| `rows` | Reads records. | `id` (required): form id. `params.start` (default `0`), `params.rows` (default `50`, at most `1000`), `params.sort` (a field id), `params.desc` (`"true"` for descending), `params.field` and `params.value` (one equality filter). `params.id`: return one record by its record id (if you get a list back, pick the record from the list yourself). |
| `submit` | Creates or updates a record. | `id` (required): form id. `body` (required): `{"fieldId": "value"}`; a list value (`["a","b"]`) sends several values for a multi-value field. `params.id`: the record id to update; if it matches an existing record, that record is updated. `params.ignoreValidation`: `"true"` skips the form validators. `params.as`: the directory user (default `admin`). `confirm: true`. |
| `file` | Stores a file in the File Upload field of a record. | `id` (required): form id. `params.recordId`, `params.fieldId`, `params.fileName` (all required; `fileName` is a bare name without `/`, `\`, `..` or `;`). `params.append`: `"true"` adds to existing files (separated by `;`). `body_base64` (required): the bytes, at most 50 MB. `confirm: true`. |

**Returns:**

- `rows`: `{"formId", "tableName", "total", "start", "rows": [<record>]}` where a record holds the field values plus `id`, `dateCreated`, `dateModified` and `createdBy`. With `params.id`: the single record.
- `submit` success: `{"formId", "id", "status": "ok", "row": <record>}`.
- `file`: `{"status": "ok", "formId", "recordId", "fieldId", "value", "bytes", "stored", "downloadUrl"}`.

**Errors:** 400 `formId is required`, `bad field name`, `bad sort field`, `recordId, fieldId and fileName are required`, `bad fieldId`, `fileName must be a bare name`, `empty body`; 404 `Form not found`, `No row <id> in form <formId>`; 413 `file larger than ...`; 422 on `submit` when validation fails: `{"formId", "id", "status": "invalid", "errors": {"field": "message"}}`, and nothing is stored; 412 without `confirm`.

Example call (submit):

```json
{"operation": "submit", "appId": "leave_mgmt", "appVersion": "1", "id": "leave_request", "confirm": true, "body": {"employee_name": "Siti Rahma", "leave_type": "annual", "status": "submitted"}}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"formId": "leave_request", "id": "5b1e5d1c-2f0a-4c0b-8f3e-61d0c3a8d9b2", "status": "ok", "row": {"employee_name": "Siti Rahma", "leave_type": "annual", "status": "submitted", "id": "5b1e5d1c-2f0a-4c0b-8f3e-61d0c3a8d9b2", "createdBy": "admin"}}}
```

Example call (read back):

```json
{"operation": "rows", "appId": "leave_mgmt", "appVersion": "1", "id": "leave_request", "params": {"rows": "20", "sort": "dateCreated", "desc": "true"}}
```

**Safety:** `submit` and `file` need `confirm: true`. `rows` reads the table of the form, not what a datalist shows: a list filter or join can show different data. After the first `submit` for a form, read it back with `rows` and check `total`. If `total` is `0` the form probably lacks its `WorkflowFormBinder` load and store binders. Use fictional data; never real personal data. Seed e-mail fields with a real-looking domain such as `.com`; the e-mail validator refuses `.example`.

## Generator tool

### builder_generate_crud

Runs the Joget Enterprise **CRUD Generator** for an existing form. It creates a datalist, adds a CRUD menu to a userview (creating the userview if needed), and **publishes the app**.

**Access:** API key, `confirm: true`. Returns 501 if the Enterprise CRUD Generator plugin is not installed. `id` is the form id; the options go in `body`.

| Option (in `body`) | Type | Required | Description | Default |
|---|---|---|---|---|
| `newListColumns` | string or array of strings | Yes | The field ids to show as columns. A list (`["a","b"]`) or a string separated by `,` or `;`. There is no default: the generator does not derive columns from the form. | none |
| `menuId` | string | No | Id of the menu to create. | `<formId>_crud` |
| `menuLabel` | string | No | Label of the menu. | the form name |
| `categoryLabel` | string | No | Label of the category that holds the menu. | the form name |
| `newListId` | string | No | Id of the new datalist. | `list_<formId>` |
| `newListName` | string | No | Name of the new datalist. | the form name |
| `listId` | string | No | Id of an existing datalist to reuse; empty creates a new one. | empty |
| `userviewNewId` | string | No | Id of a new userview. | `<formId>_uv` |
| `userviewName` | string | No | Name of the new userview. | the form name |
| `userviewId` | string | No | Id of an existing userview to add the menu to; empty creates a new one. | empty |
| `userviewDesc` | string | No | Description of the new userview. | empty |

The options `menuId`, `menuLabel`, `newListId`, `newListName` and `newListColumns` must not end up empty, and a new userview needs an id and a name. All of these have defaults except `newListColumns`.

**Returns:** `{"message", "itemId", "datalistId", "datalistColumns", "status", "appId", "appVersion"}`. `status` is `ok` only when the list was read back with the requested number of columns. It is `incomplete` when the list has no columns or fewer than requested, and `unverified` when it could not be read back. HTTP stays 200 in these cases because the artifacts exist. Other fields can appear: `requestedColumns`, `warning`, `normalisedToSemicolonSeparated`, `iconCopiedToMenuLabel` (the category's icon tag was copied onto the menu label) and `repairedOptionsBinder`.

**Errors:** 501 `CRUD Generator not available (enterprise plugin not installed)`; 404 `form not found`; 400 `{"ok": false, "errors": [{"problem": "required CRUD option(s) resolved to empty: ...", "missing": [...]}]}`; 400 `CRUD option '<key>' must be a string or an array of strings, not an object`; 500 `CRUD generation failed`; 412 without `confirm`.

Example call:

```json
{"operation": "run", "appId": "leave_mgmt", "appVersion": "1", "id": "leave_request", "confirm": true, "body": {"newListId": "leave_request_list", "newListName": "Leave Requests", "newListColumns": ["employee_name", "leave_type", "status"], "userviewNewId": "portal", "userviewName": "Leave Portal", "menuId": "requests", "menuLabel": "Requests", "categoryLabel": "Leave"}}
```

Example result:

```json
{"status": 200, "ok": true, "result": {"message": "CRUD generated. Click <a href=\"/jw/web/console/app/leave_mgmt/1/userview/builder/portal?menuId=...\" target=\"_blank\">here</a> to continue editing the CRUD in UI Builder. Click <a href=\"/jw/web/userview/leave_mgmt/portal/_/requests\" target=\"_blank\">here</a> to launch UI.", "itemId": "", "datalistId": "leave_request_list", "datalistColumns": 3, "requestedColumns": 3, "normalisedToSemicolonSeparated": ["newListColumns"], "status": "ok", "appId": "leave_mgmt", "appVersion": 1}}
```

**Safety:** it changes several artifacts and publishes the app, so `confirm: true` is required and the assistant should say so before it runs. Treat any `status` other than `ok` as a failed build even when the HTTP status is 200.

## Lint and verify tools

### builder_lint

Checks a form or datalist definition without saving anything. Use it before `create` or `update`.

**Access:** API key. No `confirm`. No `appId` is needed.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `kind` | string | Yes | `form` or `datalist`. | none |
| `json` | object or string | Yes | The definition to check. | none |

**Returns:** `{"kind", "ok", "errors": [...], "warnings": [...]}`. `ok` is `false` when there is at least one error. Warnings never block a save. Each finding has `severity`, `path` and `problem` (form schema findings also have `expected`). A definition that does not parse returns one error with `path` `$`. A missing `json` returns `{"ok": false, "error": "json is required"}`; a wrong `kind` returns `{"ok": false, "error": "kind must be 'form' or 'datalist'"}` (neither sets `isError`).

**What it checks.**

| Kind | Errors (block a save) | Warnings |
|---|---|---|
| Form | Unknown class name or property, missing required property, wrong enum value (checked against the property definitions of the plugins installed on your server). A selection field (select box, radio, check box, multi-select) without `options` or `optionsBinder` (read-only display fields are exempt). A `PopupSelectBox` without `listId`. A BeanShell script with a wildcard import (`import x.*;`). A `tableName` that is not `[a-zA-Z0-9_]`, or longer than 20 characters (the form's own property definition allows 1 to 20 characters; the lint also checks the database limit of 57). | A property set to `null` (Joget expects `""`). A `tableName` longer than 20 characters also gets a warning (the form builder refuses it on a manual save). |
| Datalist | Missing `id` or `name`. Missing `binder.className`. A column without `name`. A column without `id`, with a duplicate `id`, or with an `id` that does not start with `column_`. A filter without `name` or `label`. A special column whose inner and outer ids differ. A `OptionsValueFormatter` without `options`. | A list with no columns. A column without a label. A filter without a `type`. A non-boolean `filterable`. `NestedDatalistFormatter` without `renderHtml`. A BeanShell formatter that looks up data for every row, has an empty `catch` block, or puts a record value into HTML without escaping. |

Example call:

```json
{"kind": "datalist", "json": {"id": "leave_request_list", "name": "Leave Requests", "columns": [{"name": "employee_name", "label": "Employee Name"}]}}
```

Example result:

```json
{"kind": "datalist", "ok": false, "errors": [{"severity": "FAIL", "path": "root.binder", "problem": "missing binder {className, properties} - a datalist without a binder shows nothing"}, {"severity": "FAIL", "path": "columns[0]", "problem": "column 'employee_name' has NO 'id' - every column needs id 'column_<n>'. Without it the List Builder canvas renders an empty table body (the palette still lists the fields, so it looks like a data-source fault and is not one)."}], "warnings": []}
```

**Safety:** saves nothing. The lint covers structure and plugin properties, not whether a list points at a form that exists; use `builder_verify_app` for that.

### builder_verify_app

A read-only audit of a built app.

**Access:** API key. No `confirm`.

| Name | Type | Required | Description | Default |
|---|---|---|---|---|
| `appId` | string | Yes | App id. | none |
| `appVersion` | string | Yes | App version, digits only. | none |

**Returns:** `{"appId", "appVersion", "published", "forms", "datalists", "userviews": [{"userviewId", "categories": [...]}], "problems": [{"where", "path", "problem"}], "warnings": [...], "ok", ("note")}`. `ok` is `true` when `problems` is empty. It checks:

- whether the app version is published (and adds a `note` if not);
- every datalist, re-linted, and whether its binder form exists in the app;
- every userview: the menu tree with `hasPage`, and every `datalistId`, `listId`, `datalist`, `formId`, `addFormId`, `editFormId`, `viewFormId` and `formDefId` found in the userview, resolved against the forms and datalists of the app (values that contain `#` or `{` are skipped).

**Errors:** `{"error": "appId and numeric appVersion are required"}`; `{"error": "cannot read app: HTTP 404 ..."}`. Both set `isError`.

Example call:

```json
{"appId": "leave_mgmt", "appVersion": "1"}
```

Example result:

```json
{"appId": "leave_mgmt", "appVersion": "1", "published": true, "forms": 1, "datalists": 1, "userviews": [{"userviewId": "portal", "categories": [{"id": "category-8A1F0B7E6C3D4E5F9A2B1C0D3E4F5A6B", "label": "Requests", "menus": [{"id": "6f1c2a52-0d7f-4d0e-9a41-3c2f4b0a9c11", "label": "Requests", "className": "org.joget.plugin.enterprise.CrudMenu", "customId": "requests", "hasPage": false}]}]}], "problems": [], "warnings": [], "ok": true}
```

**Safety:** read-only. It resolves references; it does not open pages over HTTP, so a menu can still fail at run time. Ask a person to open the main pages.

## Error code reference

Tool errors come back as `{"status": <code>, "ok": false, "error": ...}` with `isError: true`. The tool layer produces 400, 412, 401, 403 and 500; the other codes come from the Builder API behind it.

| Code | Where it comes from | Meaning | What to do |
|---|---|---|---|
| 200 | All | Done. For `builder_generate_crud` read `status` in the body too. | - |
| 400 | Tool layer | `operation must be one of: ...`; `invalid params key: <key>`; `pass appId as a top-level argument, not in params`; `body_base64 is not valid base64`. | Fix the arguments. |
| 400 | Builder API | `appId is required`; `appVersion is required (no default)`; `Explicit appId and numeric appVersion required`; a required id or body is missing; JSON does not parse. | Pass the missing value. |
| 400 | Lint and guards | `{"ok": false, "errors": [...], "hint": "nothing was saved..."}`. Lint errors on `create`, `update` and `patch`, the page-wipe refusal (`wouldDelete`) and missing CRUD options (`missing`) all use this shape and status 400. | Read `errors[]` and fix the definition. Do not bypass lint with `lint=false`. |
| 400 | Process | `request body must be the XPDL XML document`; `XPDL deploy rejected: ...`; a binding key not in the form `<processDefId>::<id>`; a binding without `className`, `formId` or `type`. | Fix the XPDL or the binding. |
| 401 | Access check | No valid key or administrator session. | Send the key; see [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md). |
| 403 | Access check, opt-in | `'app-create' is disabled` or `'plugin-upload' is disabled`. | Ask the operator to enable the flag, or do the step by hand. |
| 403 | Access check | A cross-site browser request with session authentication, or a browser-originated request (an `Origin` header) in anonymous mode. | Call from a non-browser client; use the key. |
| 403 | Report and API | `App version/license verification failed`. | The Joget license does not cover this app version. |
| 403 | Workflow | `<user> holds no assignment for <activity>`. | Pass `params.as` with an assignee shown in `open`. |
| 404 | Builder API | App version, item, menu, page, process, activity, plugin or record not found. Menus also fail with a 404 page at run time until the app is published. | Check the ids with the `list` and `menus` operations. |
| 409 | Builder API | `create` on an id that exists; a patch that matches several elements (`paths`); a `customId` already used; an app that exists; a migration already running; no deployed process for `mappings`; the package version changed; an Enterprise plugin that is unavailable (reports and APIs); `process did not start`. | `get` and then `update` or `patch`; make the patch id unique; retry later; read the message. |
| 412 | Tool layer | A side-effecting operation without `confirm: true`. | Ask the user, then repeat with `confirm: true`. Do not retry blindly. |
| 413 | Builder API | A file or jar over 50 MB, or a report or API definition over 2 MB. | Send a smaller body. |
| 415 | Builder API | A body sent with a content type that browsers can send without a preflight. | The tools always send a valid type. If you see this, you are calling the REST API by hand; send `application/json`. |
| 422 | Data and workflow only | Form validation failed on `builder_data` `submit` or `builder_workflow` `complete`: `{"status": "invalid", "errors": {"field": "message"}}`. The record or activity is unchanged. Lint failures are not 422. | Fix the values and repeat. |
| 500 | Any | An unexpected exception, or a failed delete or save. | Read the Joget log. |
| 501 | Builder API | The Enterprise CRUD Generator plugin is not installed, or the route is switched off (`process/save`). | Install the plugin; use `deploy` for processes. |
| 503 | Report and API | The Joget console verifier is unavailable. | Check the Joget log. |

Protocol-level errors (a missing session, JSON-RPC codes, tool errors of the browser composers) are in [Composer MCP: Tool Reference (continued)](19b-composer-mcp-tools-continued.md) and [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md).

## Recommended call sequences

Always start with `builder_status`. Always give the assistant the `appId` and `appVersion`. The assistant should read the matching skills with `joget_skill_read` before it writes each artifact (see [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md)).

### Create an app

1. `builder_status`: check `optInFeatures.app-create`.
2. If `app-create` is `true`: `builder_app` `create` with `params.name` and, if you want pages to work at once, `params.publish: "true"`.
3. If it is `false`: ask an administrator to create the app in the Joget console (App Center) and to give you the `appId` and version.
4. Confirm the target: `builder_form` `list` for the app (an empty list is a valid answer; a 404 means the id or version is wrong).

### Add a form and a list

1. `builder_introspect` `palette` (`builder: form`) and `properties` for the classes you plan to use.
2. Author the form JSON. Include `properties.id`, `tableName` (letters, digits, underscore; at most 20 characters) and both `WorkflowFormBinder` binders.
3. `builder_lint` `kind: form`. Fix every error.
4. `builder_form` `create`. On 409, `get` and then `update`.
5. Author the datalist (`binder.formDefId` = the form id; columns `column_0`, `column_1`, ...). `builder_lint` `kind: datalist`, then `builder_datalist` `create`. Read `lintWarnings`.
6. Check the table name is not used by another app (table names are shared by all apps on the server): `builder_data` `rows` for the new form must show `total: 0` before you seed.
7. Seed one row with `builder_data` `submit` (`confirm: true`) and read it back with `rows`. `total` must be 1. If it is 0, the binders are missing.
8. `builder_verify_app`.

### Build a userview

1. `builder_userview` `list`. Do not create a second userview by accident.
2. Author the userview JSON with its categories and menus, a `LoggedInUserPermission` in `setting.properties.permission`, and the theme if you have one (`builder_theme` `create` first).
3. `builder_userview` `create`. For later changes use `menus` to see the tree, `menu_add` to add a menu, `page_get` and `page_update` to change one page, and `patch` for one property. Avoid whole-document `update`.
4. Publish the app (ask the user, or use `builder_generate_crud`, or create the app with `publish`). Menus return 404 until the app is published.
5. `builder_verify_app`. Ask a person to open the login page and one menu.

Alternative for a plain CRUD screen: `builder_generate_crud` `run` for a form (it creates the list, the menu and the userview, and publishes). Check `status` is `ok` and `datalistColumns` is what you asked for.

### Create and run a process

1. `builder_process` `get`: copy the default package and the binding keys.
2. Author the XPDL. The `Package Id` equals the `appId`. Participants and tool activities are referenced by id.
3. `builder_process` `deploy` with the XPDL string and `confirm: true`. A bare `Error in package` usually means the XPDL is missing the default `Script` element; copy it from `get`.
4. `builder_process` `mappings`: bind forms to activities, plugins to tool activities, and participants to users, roles or groups. Use the keys from `get`.
5. `builder_workflow` `start` with `confirm: true` and `params.processDefId`. Read `open` for the next activity and its assignees.
6. `builder_workflow` `complete` with `confirm: true`; send the full row in `formData` when the activity has a form. Repeat until `processState` is closed.
7. `builder_data` `rows` to check the stored values.

## Limitations and things to know

- **No app management.** There is no tool to list, publish, unpublish, delete, export or import an existing app. Do not import an app file over an existing app: it creates a new version.
- **Process validation is structural.** `builder_process` `validate` does not run Joget's own XPDL validation. A failing `deploy` is the real test; do it on a scratch app version first.
- **`builder_data` reads tables, not lists.** `rows` returns what is stored in the form's table. A datalist with filters or joins can show something else.
- **Lint does not prove correctness.** Lint checks structure and plugin properties. It does not know that a list points at a column that does not exist, or that a form lacks its binders. The bundled skill `joget-form-lint-gaps` lists these blind spots.
- **Table names are global.** Two apps that use the same table name share rows. Use a prefix that is unique to the app.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `builder_data` `submit` returns `ok` but `rows` is empty | The form has no `WorkflowFormBinder` as `loadBinder` and `storeBinder`; or the table belongs to another app. | Add both binders; check the table name is unique. |
| `builder_process` `deploy` returns `Error in package` or `XPDL deploy rejected` | The XPDL is not valid for Joget (for example the `Script` element is missing, or the `Package Id` differs from the `appId`). | Start from the package returned by `get`. |

## Where to go next

- [Composer MCP: Tool Reference (continued)](19b-composer-mcp-tools-continued.md): the skill tools, the session tools and the browser-connected composer tools.
- [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md): the skill library, prompts and example prompts.
- [Composer MCP: Overview and How It Works](17-composer-mcp-overview.md): how the pieces fit together.
- [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md): the security model and operations.
