# 05-composer-mcp: connect Claude Code to Joget

Composer MCP is a Joget plugin. It offers the Joget builders (forms, lists, userviews, processes, themes, data) and
the skill library as tools over the Model Context Protocol (MCP). The model is not in Joget: it is the MCP client on
the developer's machine, for example Claude Code. Joget only supplies the tools.

The Composer MCP plugin jar is ready to install. Nothing in this folder needs to be built.

## What the sample shows

1. How to configure access on the server and connect an MCP client.
2. A smoke test that checks the connection with `curl`, without any model.
3. That your own skills from AI Central Config (sample 04) are visible to the MCP client.
4. Two prompts that build the same small purchase-request app: one for Claude Code over MCP, one for the
   Orchestrator inside Joget.
5. A text block for a project's `CLAUDE.md` that makes the client follow your standards.

## Files

| File | Content |
|---|---|
| `smoke-test.sh` | Checks Composer MCP over HTTP: `initialize`, `tools/list`, the skill tools, the prompts and the access rule. Needs only `bash` and `curl`. |
| `E2E-PROMPT.md` | A prompt for Claude Code: build the app through Composer MCP. |
| `E2E-PROMPT-orchestrator.md` | A prompt for the Orchestrator in Joget: build the same app. |
| `CLAUDE.md.snippet` | Rules to paste into the `CLAUDE.md` of a project that uses Composer MCP. |
| `../.mcp.json` | Project configuration for the MCP connection. It reads the key from `JOGET_BUILDER_API_KEY`. |

## Prerequisites

- Joget DX 9.1 or later, Enterprise edition. Composer MCP does not run on earlier versions.
- Agent Builder installed. Without it, the skills from AI Central Config are not listed over MCP.
- The Composer MCP plugin jar.
- For the smoke test: `bash` and `curl` (on Windows use Git Bash).
- For the prompts: Claude Code, or another MCP client that supports the HTTP transport.
- For the checks of your own skills: the four skills of [04-custom-skills](../04-custom-skills/README.md) installed.

## Install

1. Open **Settings > Manage Plugins**, click **Upload**, choose the Composer MCP jar in the **Upload Plugin** dialog and
   click **Upload**.
2. The plugin list now contains **Composer MCP Server**. The Joget log shows a line that starts with
   `Builder API/MCP auth posture:`.

## Configure

The `builder_*` tools change your Joget, so they are refused by default. You switch them on with a shared key on the
server. The skill tools and the prompts work without a key.

| JVM property | Environment variable | Meaning | Default |
|---|---|---|---|
| `joget.builder.api.key` | `JOGET_BUILDER_API_KEY` | The shared key. Clients send it in the header `X-Builder-Api-Key` (or `Authorization: Bearer <key>`). | not set: `builder_*` tools are refused |
| `joget.builder.api.runAs` | `JOGET_BUILDER_API_RUNAS` | The Joget user that changes are made as | `admin` |
| `joget.builder.api.enable` | `JOGET_BUILDER_API_ENABLE` | Optional features, comma separated: `app-create`, `plugin-upload`, or `all` | none |

Example for Tomcat on Windows, in `<JOGET_HOME>\apache-tomcat-<version>\bin\setenv.bat`:

```bat
set JOGET_BUILDER_API_KEY=<your key, 32 or more random characters>
set CATALINA_OPTS=%CATALINA_OPTS% -Djoget.builder.api.enable=app-create
```

Example for Linux, in `bin/setenv.sh`:

```bash
export JOGET_BUILDER_API_KEY="<your key, 32 or more random characters>"
export CATALINA_OPTS="$CATALINA_OPTS -Djoget.builder.api.enable=app-create"
```

Restart Joget after you change these values.

- `app-create` lets the tool `builder_app` create new apps. Without it, create the app in Joget first.
- Leave `plugin-upload` off. It lets a client install code on the server.

## Connect Claude Code

Per user, on the command line:

```bash
claude mcp add joget-composer --transport http http://localhost:8080/jw/mcp/composer/mcp --header "X-Builder-Api-Key: <your key>"
claude mcp list
```

Or per project, with the file `.mcp.json` in the project root. This repository contains one:

```json
{
  "mcpServers": {
    "joget-composer": {
      "type": "http",
      "url": "http://localhost:8080/jw/mcp/composer/mcp",
      "headers": {
        "X-Builder-Api-Key": "${JOGET_BUILDER_API_KEY}"
      }
    }
  }
}
```

Each developer sets `JOGET_BUILDER_API_KEY` in the shell before starting Claude Code. The key is never written in
the file.

Notes:

- Use the HTTP transport. If an older registration with the SSE transport exists, remove it:
  `claude mcp remove joget-composer`.
- If a proxy in front of Joget removes custom headers, use `--header "Authorization: Bearer <your key>"`.
- Use one MCP server name per Joget instance (development, test, customer), each with its own address and key.
- Change the address in `.mcp.json` if your Joget is not on `http://localhost:8080/jw`.

## Try it

### 1. Smoke test (no model needed)

```bash
bash smoke-test.sh                                   # http://localhost:8080/jw, or the JW_BASE variable
bash smoke-test.sh http://myserver:8080/jw           # another address
JOGET_BUILDER_API_KEY=<your key> bash smoke-test.sh  # also calls builder_status with the key
```

The same first call by hand:

```bash
curl -s -i -H "Content-Type: application/json" -H "Accept: application/json,text/event-stream" \
  -X POST http://localhost:8080/jw/mcp/composer/mcp \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"smoke","version":"1"}}}'
```

Take the value of the response header `Mcp-Session-Id` and send it as a header on every later call.

### 2. Confirm your skills from Claude Code

After `claude mcp list` shows the server as connected, ask:

```text
Call joget_skills_list and tell me which skills have kind central.
```

```text
Read acme-naming-conventions section FieldIds with joget_skill_read.
```

### 3. Add the rules to a project

Copy the content of `CLAUDE.md.snippet` into the `CLAUDE.md` of the project. It tells the client to check the
connection, read the skills first, pass an explicit `appId` and `appVersion`, lint before saving, ask before
destructive steps, and report which skills it used.

### 4. Build an app

- Over MCP: start a new Claude Code session and paste the prompt from `E2E-PROMPT.md`.
- In Joget: follow `E2E-PROMPT-orchestrator.md`.

## Expected result

Smoke test without a key, with the four skills of sample 04 installed:

```text
PASS  initialize (session 6fa87293-56f3-4fdb-82d5-d33d65bdbbd2)
PASS  tools/list has builder_status
PASS  tools/list has builder_form
PASS  tools/list has joget_skills_list
PASS  tools/list has joget_skill_read
PASS  bundled skill joget-form-builder listed
PASS  custom skill acme-naming-conventions listed
PASS  custom skill acme-app-standards listed
PASS  custom skill acme-standards-lookup listed
PASS  custom skill acme-standards-reference listed
PASS  joget_skill_read acme-naming-conventions#FieldIds
PASS  prompts/list returns the bundled prompts
PASS  prompt skill_acme_naming_conventions
PASS  builder_status refused without a key (expected)
SKIP  keyed builder_* steps (set JOGET_BUILDER_API_KEY, and the same key on the server)
```

The exit code is 0 when every step passes. The session id differs each time. If the skills of sample 04 are not installed, the custom skill lines show `SKIP` instead of `PASS` and the exit code is still 0. With the key set on the server and in
your shell, the last two lines become `PASS  builder_status with key`.

From Claude Code: `joget_skills_list` names `acme-naming-conventions`, `acme-app-standards`,
`acme-standards-lookup` and `acme-standards-reference` with `kind: central`, and the second question returns
the rule about the `acme_` prefix. The `/mcp` panel also lists the prompts `skill_acme_*`.

The build prompts ask for a form `purchase_request`, an approval form, a list `purchase_requests`, a process and a
userview `purchase_portal`, with field ids that start with `acme_` and Bahasa Indonesia labels, and for a closing
table of the created ids and a list of manual follow-ups (participant mapping is always one of them). Check the
result against these points; a whole-app build differs from run to run.

## How to adapt it

- **Another Joget:** change the address in the `claude mcp add` command or in `.mcp.json`, and pass it to
  `smoke-test.sh`.
- **Your own skills in the smoke test:** edit the list of names in step 3 of `smoke-test.sh` and the name in steps 4
  and 5.
- **Your own standards:** replace the skill names and the rules in `CLAUDE.md.snippet` and in the prompts with yours.
- **A smaller first build:** use steps 1, 3, 5 and 6 of `E2E-PROMPT.md` only (form, list, userview, sample rows),
  and add the process later.

## Limitations and things to know

- The key is one shared secret, and changes are made as the user in `joget.builder.api.runAs`. Use Composer MCP on
  development and test servers on a trusted network.
- The `composers:` scope of a skill is not applied over MCP, and a skill's `tools:` link has no effect over MCP.
  See [04-custom-skills](../04-custom-skills/README.md).
- A client can search skills with `joget_skill_search`, but bundled skills can rank above yours. Name your skills in
  `CLAUDE.md` so that the client reads them by name.
- A whole-app build is long and uses many model tokens. The result differs from run to run; always finish with
  `builder_verify_app` and check the app in Joget.
- Mapping process participants to users, mail server settings and other environment settings stay manual.
- Delete, deploy and data submit calls need `confirm:true`. Let the client ask you first.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `FAIL  initialize: no Mcp-Session-Id` | The plugin is not installed or not started, or the address is wrong | Check **Manage Plugins** and the address; the path is `<JW_BASE>/mcp/composer/mcp` |
| `claude mcp list` shows a 404 for `joget-composer` | An old registration with the SSE transport | `claude mcp remove joget-composer`, then add it again with `--transport http` |
| `builder_*` answers 401 | No key on the server, or the client sends no key or a different key | Set `JOGET_BUILDER_API_KEY` on the server and restart; send the same key; call `builder_status` to see the mode |
| `builder_app create` answers 403 | `app-create` is not enabled | Add `-Djoget.builder.api.enable=app-create` and restart, or create the app in Joget first |
| A call answers 412 | A step that changes or deletes data was called without `confirm:true` | Agree, and let the client repeat the call with `confirm:true` |
| `FAIL  custom skill ... NOT listed` | The skill folder is not in `app_ai_skills`, its status is not `active`, or Agent Builder is not installed | Install the skills of sample 04; set `status: active`; install Agent Builder |
| The key works with `curl` but not through a proxy | The proxy removes the header `X-Builder-Api-Key` or `Mcp-Session-Id` | Forward both headers, or use `Authorization: Bearer <key>` |
| `${JOGET_BUILDER_API_KEY}` is sent as text | The variable is not set in the shell that started the client | Set it, then restart the client |
