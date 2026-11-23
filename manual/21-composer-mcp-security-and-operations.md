# Composer MCP: Security, Clustering and Troubleshooting

This page is for the people who run Composer MCP and decide who may use it. It describes the security model in full (what is protected, what is not, and what a holder of the key can do), the recommended production set-up, how to rotate the key, how the plugin behaves in a cluster, a reference table of all settings and fixed values, the error codes, a complete troubleshooting table and the known limitations. Installation steps are on [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md); the concepts are on [Composer MCP: Overview and How It Works](17-composer-mcp-overview.md).

## Security model

### Summary

The MCP endpoint is `/jw/mcp/composer/mcp`. The plugin protects the part that changes your apps and reads your data, and leaves the read-only knowledge part and the browser-composer part open. In short:

| Surface | Protected by the key? | Notes |
|---|---|---|
| `builder_*` tools (all of them, including `builder_status`) | Yes. Deny-by-default. | Checked on every call. |
| REST routes under `/jw/api/*` (the same Builder API code) | Yes. Same rules. | Reachable with the same key. |
| Skill tools (`joget_skills_list`, `joget_skill_read`, `joget_skill_search`, `joget_get_contract`), prompts, resources | **No.** | Read-only. Includes skills and instructions configured in Agent Builder. |
| `initialize`, `tools/list`, `ping` | **No.** | Anyone who can reach the URL can open a session and list tools. (`tools/list` and `ping` need the `Mcp-Session-Id` header from `initialize`, but no key.) |
| Browser-composer endpoints `/jw/mcp/composer/register` and `/jw/mcp/composer/slot-status`, and the tools `list_sessions`, `connect_session`, `form_*`, `list_*`, `ui_*`, `theme_*`, `process_*` | **No.** | They act on the canvas of an open builder tab; a user must press **Save**. |
| Cross-origin access (CORS) to the MCP endpoint | Open to any origin. | See the CORS section. |

### Authentication

For every `builder_*` call (and every `/jw/api/*` request) the plugin makes one decision, in this order:

1. **Signed-in administrator session.** If the request carries a valid Joget session of a user with the role `ROLE_ADMIN`, the call is allowed and runs under that user's own identity. Because a browser sends session cookies automatically, a web page on another site could try to drive such a session. To prevent this, the plugin refuses the call (403, "Cross-site request refused for session authentication") when the request has `Sec-Fetch-Site: cross-site`, or when its `Origin` header does not match its `Host` header. Non-browser clients send neither header and are not affected. Detection of the administrator session is best-effort.
2. **Shared key.** If a key is configured, the request must present it as `X-Builder-Api-Key: <key>` or as `Authorization: Bearer <key>`. The comparison is constant-time. A missing or wrong key gives 401. The call then runs as the run-as user.
3. **Anonymous mode.** Only if no key is configured and `joget.builder.api.allowAnonymous=true`, the call is allowed without credentials. A request that has an `Origin` header is still refused (403), to block calls made from web pages. The call runs as the run-as user.
4. **Otherwise** the call is refused with 401.

A refused call does not produce an HTTP error. The HTTP response is 200, the MCP result has `isError: true`, and its text is `{"error": "Unauthorized: present the Builder API key as 'X-Builder-Api-Key' or 'Authorization: Bearer <key>', or use an authenticated Joget admin session.", "status": 401}`. A missing key and a wrong key give exactly the same answer. When the server has no key at all, the message says instead `Unauthorized: the Builder API is deny-by-default.` and names the setting to configure.

When a key is configured, anonymous mode is never reached: a request without a valid key is refused. Use `builder_status` to see which mode a connection was accepted under: `api-key`, `admin-session` or `anonymous-optin`.

At start-up the plugin writes one log line that states the active posture, for example `Builder API/MCP auth posture: shared-secret required (joget.builder.api.key set) or admin session; opt-in features: none`. Check it after every configuration change. If anonymous mode is on, the line says `ANONYMOUS ACCESS ENABLED (joget.builder.api.allowAnonymous=true) - DEVELOPMENT ONLY`.

### What is and is not behind the key

Behind the key: all builder tools. These can read and change forms, lists, userviews, themes, processes, reports, API Builder definitions and record data in any app, start and complete process instances, deploy processes, and (when enabled) create apps and upload plugins.

Not behind the key:

- **Skills, prompts and resources.** Anyone who can reach the MCP URL can read the skills in the jar, **and the skills and the per-composer Central Instructions that you configured in Agent Builder**, because the server publishes them as `source: central` skills and as prompts. Do not put secrets, credentials, personal data or confidential client rules in a skill or in Central Instructions if the Joget URL is reachable by people you do not trust.
- **Session creation and the tool list.** Opening a session and listing tools needs no key. This is why `claude mcp list` can show a server as connected when the key is wrong.
- **The browser-composer part.** `register` and `slot-status` do not check any credential, and neither do `list_sessions`, `connect_session` and the composer tools. `list_sessions` reveals the app ids, artifact names and slot ids of builder tabs that users have open. A caller who knows a slot id can connect to a tab that is not already claimed by another session and change its canvas. These changes exist only in the user's browser until the user presses **Save**, but a user who does not read the canvas before saving could save unwanted content.

### Deny-by-default features

Two powerful features are off until an operator switches them on with `joget.builder.api.enable` (or `JOGET_BUILDER_API_ENABLE`). `plugin-upload` also needs `confirm: true` on each call. `app-create` does not.

| Feature | Unlocks | Risk |
|---|---|---|
| `app-create` | `builder_app` `create`: new apps on the server. | Creates apps and fills the app list. Limited damage: it does not change existing apps. |
| `plugin-upload` | `builder_plugin` `upload`: a plugin jar sent as base64. | **Deploys arbitrary code into the Joget server.** Equal to full control of the server and its database. |

A call to a feature that is off returns HTTP 403 with the exact property and environment variable to set. `all` switches on every feature, including any added later; avoid it outside a throwaway test server.

Other deny-by-default safeguards in the tools:

- **Whitelist.** Only the listed tools and operations exist. There is no generic "call any endpoint" tool.
- **Confirm gate.** Deletes, process deployment, workflow start and complete, data and file submit, CRUD generation, plugin upload, and any userview update with `allowPageWipe=true` are refused with 412 until the call carries `confirm: true`. This is a protection against an assistant acting without asking, not an access control: any caller that holds the key can send `confirm: true`.
- **Explicit target.** Every call needs an `appId` and a numeric `appVersion`.
- **Lint gate and page-wipe guard.** Invalid forms and lists are refused. A userview update that would remove existing pages is refused.
- **No SQL.** Writes go through Joget services.

### Run-as identity

Calls accepted by key or anonymous mode run as the Joget user named in `joget.builder.api.runAs` (default `admin`). That user name is written into `createdBy` and `modifiedBy`, so the audit columns of data and definitions are real names and not blank. Calls accepted by administrator session keep the signed-in administrator's name.

Notes:

- The default `admin` is the most powerful user. Create a dedicated user (for example `mcp_builder`) and set `runAs` to it, so that changes made through MCP can be recognised in audit columns and logs.
- Do not assume that a less privileged run-as user limits what the key can do. This manual does not confirm that the Builder API enforces the permissions of the run-as user. Protect the key as if it gave full administrator rights on the apps.
- Set the run-as user to an existing directory user. The manual does not describe what Joget records when the name does not exist.

### CORS

The MCP endpoints answer every request with:

```text
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, Accept, Authorization, X-Builder-Api-Key, Mcp-Session-Id
Access-Control-Expose-Headers: Mcp-Session-Id
```

An `OPTIONS` request is answered with 204. This lets any web page that a user opens in a browser send requests to the MCP endpoint, if that browser can reach Joget (for example a Joget on the user's own network). What that page can do:

- It can reach the unprotected parts: open a session, list tools, read skills and prompts, list open builder tabs, and use the browser-composer tools.
- It cannot use the builder tools without the key. It does not know the key, and in anonymous mode a request with an `Origin` header is refused.

The CORS header is not configurable. If it is a concern, restrict who can reach Joget (next section) or terminate the connection at a proxy that removes or overrides the header.

### Network exposure

Treat the Joget host as trusted-network only, as long as the browser-composer part has no authentication:

- Prefer access through a VPN or an IP allow-list for the MCP and `/jw/api/*` paths.
- Serve Joget over HTTPS. The key is sent in a header on every request, so plain HTTP to a remote host exposes it.
- If you do not use the browser-connected composers, you cannot switch them off in the plugin. Limit the paths `/jw/mcp/composer/register` and `/jw/mcp/composer/slot-status` at the proxy to the networks of your developers, or block them if developers do not use the feature. The injected script in the builder pages needs these two paths only for the optional composers.
- Make sure a reverse proxy or ingress forwards `X-Builder-Api-Key` (or `Authorization`) and `Mcp-Session-Id`, passes the request body unchanged, and does not cache the MCP path.

### What an attacker with the key can do

A person or program that holds the key can do everything the builder tools do, in every app on the instance that the run-as user can reach:

| Capability | Tool | Effect |
|---|---|---|
| Read all definitions | `builder_form`, `builder_datalist`, `builder_userview`, `builder_theme`, `builder_process`, `builder_report`, `builder_apibuilder` | Reveals your application design, hash variables, endpoints and anything stored in definitions. |
| Read all record data | `builder_data` `rows` | Reads the rows of any form's table. |
| Change or delete definitions | the same tools, with `confirm: true` | Breaks or replaces forms, lists, userviews, themes and processes. A deleted definition can be recovered only from a backup or from git history, if the instance has git configured. |
| Write data, upload files | `builder_data` `submit`, `file` | Adds or overwrites records and files. |
| Run workflow | `builder_workflow` | Starts and completes process instances, which can trigger the tools, emails and integrations of your processes. |
| Deploy processes | `builder_process` `deploy` | Replaces the running process definition. |
| Create apps | `builder_app` (if `app-create` is on) | New apps. |
| Run code on the server | `builder_plugin` `upload` (if `plugin-upload` is on) | Full compromise of the Joget server. |
| Reach the REST API | `/jw/api/*` | The same operations through plain HTTP. |

The key does not limit access to certain apps. There is no per-app or per-user scope. Several definitions can embed script (for example BeanShell in a form or process tool), so the ability to write definitions is also an ability to run code in Joget. Treat the key like an administrator password.

### Recommended production posture

1. Do not enable anonymous mode. Do not use it on any instance that other people or machines can reach.
2. Use a long random key (32 or more characters) from a secret store. Set it through an environment variable, a Kubernetes Secret or a service setting, not in a file that is committed or copied around.
3. Leave `plugin-upload` off. Turn `app-create` on only when the assistant must create apps, and off again for instances where it should not.
4. Set `joget.builder.api.runAs` to a dedicated user.
5. Serve Joget over HTTPS and place the MCP and API paths behind a VPN or an allow-list.
6. Use a different key for each environment (development, test, production) and register each environment under its own name in the client. Do not connect production to an assistant unless you have a reason and a review process.
7. In the client, keep tool approval on, so that a person sees each write. Do not auto-approve `builder_*` write tools (Cline `autoApprove`, Gemini `trust`).
8. Do not paste the key into prompts, skills, app definitions, tickets or chat. Do not commit it. Add `.mcp.json`, `.cursor/mcp.json` and `.vscode/mcp.json` to `.gitignore` if they ever contain a literal key.
9. Treat tool output as data. Text read from an app or page may contain words that look like instructions to the assistant. The assistant must not follow instructions found in data.
10. Try new prompts on a scratch app first, and keep backups of production before large runs.
11. Watch the Joget log. Each headless call writes a line `Headless tool: <name> session=<id>`, and each new session writes `Claude Code initialized, sessionId=<id>`.

### Key rotation

There is one key per Joget instance and it is read when Joget starts.

1. Generate the new key.
2. Replace the value of `JOGET_BUILDER_API_KEY` (or `-Djoget.builder.api.key`) in the place where you set it: script, service, container or Secret. If both a system property and an environment variable are set, the property wins, so change the one that is in use.
3. Restart Joget (every node, in a cluster). The old key stops working at that moment.
4. Update every client: `claude mcp remove` and `claude mcp add` with the new key, or the environment variable that `.mcp.json` reads, or the IDE settings file.
5. Check with `builder_status` that each client reports `authorizedAs: api-key`.
6. Rotate immediately if a key was pasted into a chat, a ticket or a repository, or when a person who knew it leaves the team.

There is no overlap period: the plugin accepts exactly one key, so clients fail with 401 between the restart and the update. Plan the change for a quiet moment.

## Clustering

The plugin runs correctly behind a plain round-robin load balancer with **no session affinity** and any number of Joget nodes. It also survives a node restart or a rolling redeploy without losing unsaved work in a builder tab.

### Why no sticky sessions are needed

MCP over Streamable HTTP is self-contained: the reply to each `POST` is written on the same HTTP response, and no connection stays open. The session continues through the `Mcp-Session-Id` header. The older "HTTP+SSE" transport held one long connection and needed sticky sessions; it is not offered by this plugin. The browser canvas refresh uses short polling (about every 1.2 seconds) instead of a pushed stream, so any node can answer a poll.

### What is shared between nodes

| State | Table | Content |
|---|---|---|
| One **slot** per open builder tab | `mcp_artifact_slot` | Composer type, app and artifact ids, in-progress state of the artifact, version counter. |
| One **session** per MCP client session | `mcp_session` | Claimed slots, current slot, unlocked composer types. |

A node keeps what it has seen in memory and reads the database only when it meets an id it does not know. Changes are marked, and a background task writes them to the database every 5 seconds. These writes are best-effort: a failure is swallowed and does not fail the request. Up to about five seconds of state can therefore be lost if a node crashes. Idle sessions are removed after 2 hours and idle slots after 4 hours, from memory and from the database; the clean-up runs every 30 minutes.

The tables are created automatically at plug-in start (`hibernate.hbm2ddl.auto=update`). There is no migration step. The database user needs the right to create tables.

### Behaviour in common situations

| Situation | Behaviour |
|---|---|
| Several nodes, no sticky sessions | Works. Any node answers any request. |
| A node is restarted or replaced during a session | The client gets a 404 "Session not found or expired — re-initialize" if the new node cannot find the session, or finds it in the database and continues. Claude Code initialises again and continues. The builder tab keeps polling and resumes with its saved state. |
| A request for an existing slot arrives on a node that never saw it | The node loads the slot from `mcp_artifact_slot` and continues. |
| Rolling redeploy of the plugin | Same as a node restart. |
| The database is unreachable | Requests that need only memory still work. State is not mirrored, and a request that lands on another node cannot find it. |
| The builder tools | Stateless. Each call is independent, so they are not affected by session state. |

### What you must do in a cluster

- Set the same `JOGET_BUILDER_API_KEY`, `JOGET_BUILDER_API_ENABLE` and `JOGET_BUILDER_API_RUNAS` on **every** node. The settings are per process. A node without the key answers 401 to some requests and not to others, which looks random.
- Register the load balancer address once in the client.
- Forward `Mcp-Session-Id` and the key header unchanged at every proxy layer.
- Restart every node after a key change.
- A definition written through one node is visible to the others through the shared Joget database. Cache invalidation across nodes is that of the normal Joget cluster set-up, so use your usual cluster configuration. Open builder pages must be reloaded to show changes made by the headless tools.

## Configuration reference

All settings are read when Joget starts, from system properties first and environment variables second.

| Setting (property / environment variable) | Default | Description |
|---|---|---|
| `joget.builder.api.key` / `JOGET_BUILDER_API_KEY` | Not set | Shared key for `X-Builder-Api-Key` and `Authorization: Bearer`. |
| `joget.builder.api.runAs` / `JOGET_BUILDER_API_RUNAS` | `admin` | Directory user that key and anonymous calls write as. |
| `joget.builder.api.enable` / `JOGET_BUILDER_API_ENABLE` | None | Comma list of opt-in features: `plugin-upload`, `app-create`, `all`. |
| `joget.builder.api.allowAnonymous` / `JOGET_BUILDER_API_ALLOWANONYMOUS` | `false` | `true` removes the key check. Development only. Ignored when a key is set. |
| `joget.mcp.knowledge.dir` (system property only) | Not set | Folder of skills served live instead of the bundled skills. For writing your own skills. See [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md). |
| `AI_SKILLS` (Joget setting) | Not applicable | Older storage of Agent Builder skills in **AI Central Config**. The server reads it as a fallback if the newer skill store is not available. You do not set it by hand. |

The name of the environment variable is the property name in upper case, with dots replaced by underscores. A blank value counts as unset.

Fixed values (not configurable):

| Value | Meaning |
|---|---|
| `/jw/mcp/composer/mcp` | MCP endpoint (POST). Protocol version `2025-03-26`. |
| `/jw/mcp/composer/register` (POST), `/jw/mcp/composer/slot-status?slotId=<id>&sinceVersion=<n>` (GET) | Browser tab registration and polling. |
| `/jw/mcp/composer/inject.js` | The registration script that is injected into builder pages. |
| `/jw/api/*` | Builder REST API. |
| About 1.2 seconds | Poll interval of an open builder tab. |
| 15 seconds | A tab counts as open while it was polled in the last 15 seconds. |
| 5 seconds | Interval of the background write of slot and session state to the database. |
| 2 hours / 4 hours | Idle time after which a session / slot is removed. Clean-up every 30 minutes. |
| 40,000 characters | Maximum size of one skill response. The client continues with `offset`. |
| 50 MB | Largest file that `builder_data` `file` accepts. |

## Error codes

### HTTP status codes in tool results

Tool failures come back as an MCP result with `isError: true` and a JSON text `{"status": <code>, "error": ...}` (or the error body of the Builder API, for example the list `errors[]` of lint findings). The HTTP response that carries the result is HTTP 200: read the `status` in the text.

| Code | Meaning | What to do |
|---|---|---|
| 200 | OK. | |
| 400 | Bad request: missing or invalid `appId`, `appVersion`, parameter or body. Also `operation must be one of ...`, `invalid params key`, and `body_base64 is not valid base64`. A lint failure on a form or list `create`, `update` or `patch` is also 400, with `{"ok": false, "errors": [...], "hint": ...}`. | Fix the arguments. Pass `appId` and `appVersion` as top-level arguments, not in `params`. |
| 401 | Not authorised: no valid key or administrator session; the server is deny-by-default. | Configure and send the key. See the troubleshooting table. |
| 403 | An opt-in feature is disabled (`plugin-upload`, `app-create`); or a browser-originated request in anonymous mode; or a cross-site request with session authentication. | Enable the feature, configure the key, or call from a non-browser client. |
| 404 | App, definition or menu not found. Menus also return 404 until the app is published. | Check the ids; publish the app. |
| 405 | Wrong HTTP method on a `/jw/api/*` route. | Use the method named in the message. |
| 409 | Conflict: `create` on an id that already exists, or an ambiguous patch. | `get` and then `update` or `patch`. |
| 412 | A side-effecting operation was called without `confirm: true`. | Repeat with `confirm: true` after the user agrees. |
| 415 | Body not sent as `application/json`. | Fix the content type. |
| 422 | Form validation failed on `builder_data` `submit` or `builder_workflow` `complete` (`{"status": "invalid", "errors": {"field": "message"}}`). Nothing is stored. | Read `errors` and fix the values. |
| 501 | Capability not available: for example the CRUD Generator plugin is not installed, or the route is disabled (`process/save` is intentionally off; save processes through `builder_process` `deploy`). | Install the missing plugin or use the right tool. |
| 500 | Internal error. | Read the Joget log. |

### MCP protocol errors

These come from the endpoint itself, not from a tool.

| Where | Code | Meaning |
|---|---|---|
| HTTP 404 on a request other than `initialize` | Body `{"error":"Session not found or expired — re-initialize."}` | The `Mcp-Session-Id` is missing, unknown, or belongs to a session that was idle for more than 2 hours. The client must send `initialize` again. |
| HTTP 400 | JSON-RPC `-32700` "Parse error" | The body is not valid JSON. |
| HTTP 200, JSON-RPC error | `-32601` "Method not found" | The method is not supported. The server supports `initialize`, `ping`, `tools/list`, `tools/call`, `prompts/list`, `prompts/get`, `resources/list`, `resources/templates/list`, `resources/read`. |
| HTTP 200, JSON-RPC error | `-32602` | Invalid parameters, for example `name is required` or a prompt that does not exist. |
| HTTP 200, JSON-RPC error | `-32002` "Resource not found" | The `joget-skill://` address does not exist. Call `resources/list`. |
| HTTP 200, JSON-RPC error | `-32603` "Internal error" | An unexpected failure. Read the Joget log. |
| HTTP 202 | none | Answer to a notification such as `notifications/initialized`. No body. |
| HTTP 204 | none | Empty request body, or an `OPTIONS` request. |

### Errors from the browser-connected composers

These are returned as tool text:

| Message | Cause |
|---|---|
| "No artifact connected. Call list_sessions then connect_session first." | A composer tool was called before `connect_session`. |
| "Slot not found: ..." | The `slot_id` is unknown or has expired. Call `list_sessions` again. |
| "... has no recently active browser tab" | The tab was closed, or it has not polled in the last 15 seconds. Reopen the builder page. |
| "Slot ... is already connected to another session." | Another client session has claimed that tab. |

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `builder_*` returns 401 | No key configured on the server, no header sent, or the values differ. | Check the log line `Builder API/MCP auth posture`. Set `JOGET_BUILDER_API_KEY`, restart Joget, send `X-Builder-Api-Key` or `Authorization: Bearer`. Run `builder_status`. |
| 401 although the key is set on the server | The key in the client is different, has a trailing space or line break, or `.mcp.json` shows the literal `${JOGET_BUILDER_API_KEY}` because the variable is not set in the shell that started the client. | Print the variable in that shell. Set it and restart the client. Check `/mcp` for a warning about an unset variable. |
| 401 on one node only (cluster) | Different settings on different nodes. | Set the same key on every node and restart all of them. |
| `claude mcp list` shows connected but builder tools fail | Initialising and listing tools need no key. | Run `builder_status` to test the key. |
| Registration shows failed with 404 after an upgrade, or the URL ends with `/sse` | An old registration uses the `sse` transport. That endpoint no longer exists. | `claude mcp remove joget-composer`, then `claude mcp add joget-composer --transport http <base>/mcp/composer/mcp --header "X-Builder-Api-Key: <key>"`. |
| 404 on `/jw/mcp/composer/mcp` | Plugin not yet active, wrong context path, wrong host. | Wait about two minutes after upload. A browser `GET` on the URL should return `Joget Composer MCP Server`. Check the context path (`/jw`). |
| 404 "Session not found or expired — re-initialize" | The session was idle for more than 2 hours, or the node restarted before the state was written. | Reconnect. The client should send `initialize` again. In Claude Code, run `/mcp` and reconnect. |
| Key header is lost behind a proxy | The proxy or ingress strips custom headers. | Allow `X-Builder-Api-Key` and `Mcp-Session-Id`, or register with `Authorization: Bearer <key>`. |
| Everything works over `http://` but not `https://` | Certificate not trusted by the client, or a proxy terminates TLS and changes the path. | Use a certificate from a trusted authority. Check the path that arrives at Joget. |
| 403 "is disabled" | The opt-in feature is not on. | Set `JOGET_BUILDER_API_ENABLE=app-create` (or `plugin-upload`) and restart. |
| 403 about a browser-originated request | Anonymous mode and the request has an `Origin` header. | Configure the key. Anonymous mode does not accept browser calls. |
| 403 "Cross-site request refused for session authentication" | An administrator session was used from a cross-site page. | Use the key, or call from a non-browser client. |
| `builder_app create` is refused | `app-create` is not enabled, or the app already exists (409). | Enable the feature, or use the existing app. |
| 409 on create | The id already exists. | `get` and then `update` or `patch`. |
| 412 | `confirm: true` missing. | Ask the user, then repeat with `confirm: true`. |
| 400 with `errors[]` on a form or list save | Lint failure. Nothing was saved. | Read and fix the findings. Do not bypass lint. |
| 422 on `builder_data` `submit` or `builder_workflow` `complete` | A form validator failed. | Read `errors` and fix the values. |
| Userview update refused | The page-wipe guard. | Use `patch`, `menu_add` or `page_update`. |
| Menus or CRUD pages show 404 | The app is not published. | Publish from the Joget console, or use `builder_generate_crud` / `builder_app` `create` with `publish=true`. |
| Changes are not visible in an open builder page | The headless tools write to the database; the open page keeps its old copy. | Reload the builder page. |
| Tools are missing: no `builder_*` tools | The connection did not list tools, or the plugin is not active. | Run `tools/list` (see the smoke test). Check the plugin in **Settings > Manage Plugins** and the log. |
| Tools are missing: no `form_*`, `list_*`, `ui_*`, `theme_*` or `process_*` tools | These tools are listed only while a builder tab of that type is open and polling. | Open the builder page, wait a few seconds, then run `list_sessions` and ask the client to refresh its tool list (reconnect if needed). |
| `list_sessions` shows no tabs | The injected script did not register: the page was opened before the plugin was installed, Agent Builder is missing, or the page type is not supported. | Reload the builder page. Install Agent Builder. Look for `[MCP] Registered with server` in the browser console. |
| "No artifact connected" | A composer tool was called before `connect_session`. | Call `list_sessions`, then `connect_session`. |
| Skills from Agent Builder are not listed | Agent Builder is not installed, or the skills are not active. | Install Agent Builder. In **AI Central Config**, set the skill's status to active. Only active skills are listed. |
| A prompt such as `joget_form_instructions` shows only the bundled global rules, not my Central Instructions | No per-composer Central Instructions are entered for that composer, Agent Builder is not installed, or you entered the text in the **Global** box, which MCP does not serve. | Enter the text in the box of the matching composer in **AI Central Config > Skills**, or put the rule in a skill. |
| Skill response ends abruptly | Responses are capped at 40,000 characters. | Continue with the `offset` argument, or read a section. |
| Plugin does not appear after upload | The bundle did not resolve, or the server is not DX 9. | Upload again and read the Joget log. The plugin requires DX 9. |
| Session or slot state lost after redeploy | State is written every 5 seconds and the tables may be missing. | Check that `mcp_session` and `mcp_artifact_slot` exist in the Joget database and that the database user may create tables. |
| `claude mcp list` is slow or times out | Network, proxy buffering or an overloaded server. | Test with the curl smoke test. Check the proxy does not buffer the MCP path. |

## Limitations and things to know

- **Alpha build.** `9.1.0-ALPHA`. It has not been tested on every Joget environment. Use a test instance first.
- **Joget DX 9.1 or later only.** It will not load on Joget 8.x.
- **One shared key.** No per-user keys, no per-app scope, no expiry, no overlap period during rotation. Everyone with the key has the same power.
- **Parts of the endpoint are open.** The browser-composer endpoints and tools, the skill tools, prompts, resources and the tool list do not use the key. The CORS header allows any origin and cannot be changed. Restrict network access.
- **Agent Builder skills are readable without the key.** Do not store confidential material in skills or Central Instructions.
- **The `confirm` flag is not an access control.** It is a prompt to the assistant.
- **The effect of the run-as user's permissions is not confirmed.** Treat the key as an administrator credential.
- **No tool to list apps, publish an existing app, or export and import apps.** The Builder API has no such routes. Publish from the Joget console.
- **No programmatic save in the browser-connected composers.** The user presses **Save**.
- **`builder_verify_app` does not render pages.** It checks references, lint and the published flag.
- **List lint is partial.** Lists are not checked against the full element property schemas that forms are checked against.
- **State mirroring is best-effort.** The latest few seconds of canvas state can be lost if a node crashes.
- **Cloud-hosted clients may not reach a private Joget.** A client that connects from a cloud service (for example the connector flow of Claude Desktop) is likely unable to reach `localhost` or a private network address. Use a client that runs on your own machine, or expose Joget through a secure gateway with HTTPS.

## Where to go next

- [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md) for the steps to set the key and connect clients.
- [Composer MCP: Tool Reference](19-composer-mcp-tools.md) for the tools and their confirm gates.
- [Security and Governance](11-security-and-governance.md) for the security features of Agent Builder itself.
- [Troubleshooting](30-troubleshooting.md) for problems outside Composer MCP.
