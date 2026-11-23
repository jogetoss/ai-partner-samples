# Agent Builder Examples Repository

Working examples for Joget Agent Builder 8.2.5-ALPHA, the AI Composers and Composer MCP. Each folder is one
self-contained sample that you can build, install, try and then change into your own extension. The Java packages
are `org.joget.samples.*`.

You do not need the source code of Agent Builder or Composer MCP. Everything here works with the two plugin jars (Agent Builder and Composer MCP),
this repository and a Joget DX Enterprise installation.

The full manual (38 pages with screenshots) is in the [manual](manual/README.md) folder. Start with [Welcome and What Is Included](manual/01-welcome.md).

> **Note:** Agent Builder 8.2.5-ALPHA is an early build, two releases ahead of the current stable release. Its features are subject to change and can be unstable. Use it on development and test servers only, not in production.

## Contents

| Folder | What it shows | You build | Needs an AI provider to try |
|---|---|---|---|
| [00-setup](00-setup/README.md) | One-time setup: the Agent Builder API jar, the Joget platform jars, optional install scripts | nothing | no |
| [01-agent-builder-tools](01-agent-builder-tools/README.md) | A custom Tool, Guard and Enhancer for the Agent Builder palette (Indonesian NIK and NPWP numbers) | `indonesian-id-tools-1.0-SNAPSHOT.jar` | yes |
| [02-system-agent](02-system-agent/README.md) | A system agent written in Java, started from its own page in **Settings** (form naming audit) | `form-naming-auditor-1.0-SNAPSHOT.jar` | yes |
| [03-composer-custom-tool](03-composer-custom-tool/README.md) | A custom tool that the AI Composers can call, through a tool template and a skill | `composer-tool-1.0.0.jar` | yes |
| [04-custom-skills](04-custom-skills/README.md) | Four custom skills (`SKILL.md` files) that teach the AI Composers your standards | nothing | yes |
| [05-composer-mcp](05-composer-mcp/README.md) | Connecting Claude Code to Joget through Composer MCP: configuration, smoke test, example prompts | nothing | no for the smoke test; the MCP client brings its own model |
| [06-custom-llm-provider](06-custom-llm-provider/README.md) | A custom LLM provider ("PrivateGateway") for a model behind your own OpenAI-compatible gateway: a Model Class for AI Services and an LLM element for the palette | `private-gateway-llm-1.0-SNAPSHOT.jar` | no for the unit tests and the form; a chat-completions server to send a real message |
| [07-custom-composer](07-custom-composer/README.md) | A composer of your own for a new artifact type ("checklist"): a class that extends `AbstractComposerChat`, its tool, and the build route for the `jakarta` packages | `checklist-composer-1.0-SNAPSHOT.jar` | no for the unit tests; yes to send a message |

Other files:

| File or folder | Purpose |
|---|---|
| `.mcp.json` | Project configuration that connects an MCP client (for example Claude Code) to Composer MCP on `http://localhost:8080/jw`. The key comes from the `JOGET_BUILDER_API_KEY` environment variable. See [05-composer-mcp](05-composer-mcp/README.md). |
| `.claude/skills/` | Three developer cheat sheets in skill format: Agent Builder extensions, system agents, and plugin rules for Joget DX 9, clusters and Joget Cloud. Claude Code loads them automatically in this folder; they are also plain Markdown you can read. |
| `docs/partner-session-deck.pptx` | Slide deck for partner sessions (16 slides with speaker notes). The facilitator guide is [manual page 33](manual/33-running-a-partner-session.md). |
| `package.json`, `scripts/` | Dependencies and a shared helper for the optional Node scripts. |

## Prerequisites

| Requirement | Version | Needed for |
|---|---|---|
| Joget DX Enterprise with the Agent Builder plugin installed | Agent Builder 8.2.5-ALPHA | everything |
| An AI credential and an active AI service in **Settings > AI Central Config** | any supported provider (sample 01's demo app expects an Anthropic credential) | trying samples 01 to 04 |
| JDK | 17 | building samples 01, 02, 03, 06 and 07 |
| Apache Maven | 3.8 or later | building samples 01, 02, 03, 06 and 07 |
| Composer MCP plugin | the plugin jar; Joget DX 9.1 or later | sample 05 |
| `bash` and `curl` | any | `05-composer-mcp/smoke-test.sh` (on Windows use Git Bash) |
| Node.js and Chrome, Chromium or Edge | Node 18 or later | only the optional scripts |

Agent Builder is a Joget DX Enterprise feature. The samples were built and run on Joget DX 9.1.2
Enterprise. Use Joget DX 9.1 or later for everything in this repository; Composer MCP requires it.

## Quick start

1. Install Agent Builder on your Joget: open **Settings > Manage Plugins**, click **Upload**, choose
   `agent-builder-plugin-8.2.5-ALPHA.jar` in the **Upload Plugin** dialog and click **Upload**. Then add an AI credential and an AI service in
   **Settings > AI Central Config**.

2. Install the two compile-time prerequisites into your local Maven repository (once per machine). Details and the
   reasons are in [00-setup](00-setup/README.md).

   Windows (PowerShell):

   ```powershell
   cd 00-setup
   .\install-api-jar.ps1
   .\install-joget-jars.ps1 -JogetHome "C:\Joget-DX9-Enterprise"
   ```

   Linux, macOS or Git Bash:

   ```bash
   cd 00-setup
   ./install-api-jar.sh
   ./install-joget-jars.sh /opt/joget
   ```

   The second script prints the Joget version it found, for example `9.1.2`. Use that value in the next step.

3. Build a sample. Replace `9.1.2` with the version from step 2.

   ```bash
   cd 01-agent-builder-tools
   mvn clean package "-Djoget.version=9.1.2"
   ```

   The jar is in `target/`. The unit tests run without Joget.

4. Install the jar in the same way: **Settings > Manage Plugins**, **Upload**. No restart is needed.

5. Follow the "Try it" section of the sample's README.

## Which jars to install, and in which order

| Order | Jar | Notes |
|---|---|---|
| 1 | `agent-builder-plugin-8.2.5-ALPHA.jar` | Always first. The sample bundles start without it, but they register nothing until it is there. |
| 2 | Composer MCP plugin jar | Only for sample 05, and only on Joget DX 9. Install it after Agent Builder, so that the skills from AI Central Config are also visible over MCP. |
| 3 | `indonesian-id-tools-1.0-SNAPSHOT.jar`, `form-naming-auditor-1.0-SNAPSHOT.jar`, `composer-tool-1.0.0.jar`, `private-gateway-llm-1.0-SNAPSHOT.jar`, `checklist-composer-1.0-SNAPSHOT.jar` | Any order, after Agent Builder. They are independent of each other. |

If you replace the Agent Builder jar later, upload the sample jars again when their elements are missing from the
palette, from **Tool Type** or from **Model Class** in AI Central Config.

## Joget versions

The poms take the platform version from the property `joget.version`. Its default is `9.1.2`. Pass the version of
your own Joget installation on the command line (`install-joget-jars` prints it):

```bash
mvn clean package "-Djoget.version=9.1.2"
```

| `joget.version` | Where the platform jars came from | Samples 01, 02, 03, 07 | Sample 06 |
|---|---|---|---|
| `9.1.2` (the default in the poms) | a Joget DX 9.1.2 Enterprise installation, installed with `00-setup/install-joget-jars` | build and unit tests pass; the jars were installed and started on Joget DX 9.1.2 Enterprise | build and unit tests pass; the jar was installed and started on Joget DX 9.1.2 Enterprise |

Things to know:

- Build with the version of the Joget you install the jar on. When the version you pass is not in your local Maven
  repository, the build stops with `Could not find artifact org.joget:wflow-core:jar:<version>`.
- `joget.version` is the version of the Joget platform, for example `9.1.2`. It is not the version of the Agent
  Builder plugin (`8.2.5-ALPHA`).
- Joget DX 9 uses `jakarta.servlet` and `jakarta.websocket`. Samples 02 and 07 are the only samples that name
  servlet or websocket classes; their builds handle the namespace by themselves. Sample 07 extends an Agent Builder
  API class that has such types in its method signatures, so its build first makes a `jakarta` copy of the API jar
  (see [07-custom-composer](07-custom-composer/README.md)).

## Optional scripts

Every step in this repository can be done by hand in the Joget console. The scripts only save time. The Node scripts
drive a headless browser, because the Joget console protects its forms with a CSRF token and refuses a plain HTTP
login.

Setup, once, in the repository root:

```bash
npm install
```

| Script | What it does | Manual route |
|---|---|---|
| `00-setup/install-api-jar.ps1` / `.sh` | Installs the Agent Builder API jar into the local Maven repository | none (required for building) |
| `00-setup/install-joget-jars.ps1` / `.sh` | Installs the Joget platform jars of your installation into the local Maven repository | build Joget from source, or use your own Maven repository login |
| `00-setup/deploy-plugin.ps1` / `.sh` | Backs up an installed jar of the same name and installs a new one | **Settings > Manage Plugins > Upload** |
| `00-setup/check-install.js` | Lists which plugins of this repository are installed | **Settings > Manage Plugins** (lists the jars) |
| `00-setup/deploy-upload.js` | The upload step of `deploy-plugin` | the same |
| `01-agent-builder-tools/demo-app/build-demo-app.js` | Regenerates the demo app `acme_tools.jwa` | edit the agents in the builder |
| `01-agent-builder-tools/demo-app/run-agent.js` | Runs a demo agent and prints the events | the **Preview** tab of the agent builder |
| `02-system-agent/trigger-audit.js` | Runs the naming audit and saves the report | **Settings > Form Naming Auditor** |
| `03-composer-custom-tool/scripts/central-config.js` | Creates a tool template or a skill from a file, or lists them | **Settings > AI Central Config** |
| `05-composer-mcp/smoke-test.sh` | Checks Composer MCP with `curl` (no Node needed) | none |

Environment variables used by the scripts (all optional):

| Variable | Meaning | Default |
|---|---|---|
| `JOGET_HOME` | Joget installation folder | none |
| `JW_BASE` | Joget address with context path | `http://localhost:8080/jw` |
| `JW_USER` | A Joget administrator | `admin` |
| `JW_PASS` | Password of that user | `admin` |
| `CHROME_PATH` | Full path of Chrome, Chromium or Edge | found automatically in the usual places |
| `JOGET_BUILDER_API_KEY` | The Composer MCP key (sample 05 and `.mcp.json`) | none |

Never put a real key or password in a file of this repository. Use environment variables.

## Limitations and things to know

- Agent Builder 8.2.5-ALPHA is an alpha build. The API your extensions compile against can change in a later build;
  install the API jar that belongs to the Agent Builder jar you run, and rebuild.
- The business data in the samples (cost centres, departments, approval matrix, naming rules, Bahasa Indonesia
  labels) is example data. Replace it with your own.
- The samples call your AI provider when you try them. That uses tokens on your account.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Could not find artifact org.joget.ai.agent:agent-builder-api:jar:8.2.5-ALPHA` | The API jar is not in the local Maven repository | Run `00-setup/install-api-jar` |
| `Could not find artifact org.joget:wflow-core:jar:9.1.2` (or another version) | The Joget platform jars of that version are not in the local Maven repository | Run `00-setup/install-joget-jars`, then build with `-Djoget.version=<the version it printed>` |
| The plugin is listed in **Manage Plugins**, but its elements are not in the palette or in **Model Class** | Agent Builder was not installed or not started when the sample bundle started | Install Agent Builder, then upload the sample jar again |
| A Node script says `puppeteer-core is not installed` | `npm install` was not run | Run `npm install` in the repository root |
| A Node script says `Chrome was not found` | No browser in the usual places | Set `CHROME_PATH` |

More troubleshooting is in each sample's README.
