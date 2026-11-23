# Glossary and Reference Tables

This page is the lookup page of the manual. It has a glossary of the terms used in the manual, and reference tables for console pages and URLs, endpoints, configuration keys, system properties, environment variables, database tables, file locations, default values, limits, supported providers and default models. It is written for implementers and administrators who need an exact name or value quickly. Values are those of Agent Builder `8.2.5-ALPHA` and Composer MCP `9.1.0-ALPHA`.

> **Note:** A default in this page is the value a new object receives or the value the code uses when nothing is set. A few help texts in the product show other numbers. Where that happens the table says so.

## Glossary: A to L

| Term | Meaning |
|---|---|
| **Abort** | A request to stop a run. Abort is cooperative: it stops the run at the next check and does not interrupt a model call or a tool call that is already running. Requests are stored in the table `ai_agent_abort_request`. |
| **Active AI Service** | The AI service that the AI Composers and the Orchestrator use. You choose it on the **AI Services** tab of AI Central Config (**Current AI Service**) and press **Save**. A new service is not active until you do. |
| **Agent** | A configured AI worker built in the AI Agent Builder: a model, a persona and an ordered list of tasks. An agent lives in a Joget app. |
| **Agent Builder** | The Joget plugin that contains the AI Agent Builder, AI Central Config, the AI Composers, the Orchestrator, the monitoring pages and the Joget AI Assistant. |
| **Agent Eval Builder** | The builder for evaluation suites. |
| **Agent Eval Dashboard** | The **Monitor** page that lists the results of evaluation runs (table `ai_eval_run`). Its page heading is "Agent Eval Runs". |
| **Agent Execution Audit Trail** | The **Monitor** page that lists every run of every agent, composer and system agent (table `ai_agent_execution`). |
| **Agent Mode** | A mode of the Joget AI Assistant in which a saved agent answers the chat. Also a mode of the **Run AI Agent** datalist action (**direct** or **chat**). |
| **Agent Process Tool** | The process tool that runs an agent from a workflow activity. It appears as **AI Agent** in the Process Builder. It runs in **Synchronous** or **Asynchronous** mode. |
| **Agent Variable** | A named value that an agent keeps during one run so that later tasks can read it. |
| **Agent Versions** | A dialog in the AI Agent Builder to create, rename, check out, activate and delete versions of an agent (table `ai_agent_version`). |
| **AI Central Config** | The console page at **Settings > AI Central Config** where you set up credentials, services, plugins, skills, tool templates and voice. |
| **AI Composers** | The chat panels inside the builders: Form Composer, List (datalist) Composer, UI Composer, Process Composer and Theme Composer. The Session Monitor lists them as "AI Composer - Form Builder Agent" and so on. |
| **AI Configuration** | The field of an LLM node (and other blocks) that selects a saved AI credential. |
| **AI Credentials** | The tab of AI Central Config that stores provider keys and base URLs (setting `AI_CONFIGS`). |
| **AI Decision (Multiple Choice)** | A process tool that picks one of a fixed list of options and writes the choice and confidence to workflow variables. The same function exists as an agent tool. Preview. |
| **AI Service** | A saved model choice: provider (model class), model name and tunable settings (setting `MY_AI_SERVICES`). |
| **AI Session Monitor** | The **Monitor** page with active composer sessions, session limits, **Kill** buttons, composer availability, and heap and GC tiles. |
| **AI Usage Dashboard** | The **Monitor** page with token use by model class, plugin and other views. |
| **Ask mode** | A composer or Orchestrator mode in which you talk and preview. Nothing is saved. |
| **Async job** | A row in `ai_agent_async_job` created by an asynchronous Agent Process Tool. Status values: `PENDING`, `RUNNING`, `DONE`, `FAILED`. |
| **Asynchronous (fire and forget)** | A **Run Mode** of the Agent Process Tool. The process continues at once and a background worker runs the agent. |
| **Autopilot** | An Orchestrator mode that builds and saves everything without stopping to ask. |
| **Base directory** | The Joget `wflow` directory. Skills are stored in `<wflow>/app_ai_skills/`. |
| **BeanShell** | A Java-syntax scripting language used by the BeanShell tool, prompt, enhancer and decision. |
| **builder_* tools** | The headless tools of Composer MCP (`builder_form`, `builder_datalist`, `builder_app` and others) that call the Builder API inside Joget. |
| **Builder API** | The REST API under `/jw/api/*` that Composer MCP tools call in the same JVM. It is deny-by-default. |
| **Builder API key** | The shared secret for Composer MCP (`joget.builder.api.key` or `JOGET_BUILDER_API_KEY`). Clients send it as `X-Builder-Api-Key` or `Authorization: Bearer`. |
| **builtinVersion** | A number in the front matter of a bundled skill. When a new plugin jar carries a higher number, the skill is refreshed and local edits to it are overwritten. |
| **Bundled skill** | A skill that ships inside the jar and is created in `app_ai_skills` when the plugin starts. |
| **Central Instructions** | Rules that are added to the system prompt of every agent and composer: a **Global** text and optional text per target. |
| **Chat Mode** | A mode of the Joget AI Assistant that sends the question to the server in its **Domain** field with an **API Server Key**. |
| **Composer** | A chat panel inside a Joget builder that edits one kind of artifact. |
| **Composer MCP** | A separate plugin (`composer-mcp`) that exposes Joget building tools to an outside AI client over the Model Context Protocol. |
| **Confirm mode** | The Orchestrator mode that saves after you approve. It is the default when you choose **Create**. |
| **Context window** | The amount of text a model can read in one call. The agent also has a context window manager that trims old messages by character count. |
| **Contains** | An evaluation scorer: the share of expected phrases found in the answer. |
| **CORS** | Cross-origin resource sharing. Composer MCP answers with `Access-Control-Allow-Origin: *`. |
| **Credential** | See **AI Credentials**. |
| **Custom** | A credential type and model class for any endpoint that accepts the OpenAI chat format. |
| **Datalist** | A Joget list over records. The List Composer builds datalists. |
| **Datalist action** | A button on a list. **Run AI Agent** runs an agent on one selected row. |
| **Debug Mode** | An agent setting that writes every trace event, with request and reply, to the server log. Default off. |
| **Decision** | A block after a task that decides what runs next. Types: **Response Condition**, **BeanShell**. |
| **Decision provider** | A plugin that answers a multiple-choice question. **TypeSafe** and an LLM provider are supplied. |
| **Deny-by-default** | Composer MCP builder tools refuse every call until a key or administrator session is present. |
| **Display tool** | A tool that shows UI in the chat: **Ajax Display**, **HTML Markup Display**, **URL Display**, **HTML UI**, **File Viewer**. |
| **Dictation** | The microphone button of the composer panel. |
| **DX 9** | The Joget DX generation that this manual covers. Both plugins need Joget DX 9.1 or later, Enterprise edition. |
| **Enhancer** | A block that runs after a task and acts on its output: **Store to Form**, **Store to Process Variable**, **Store to Agent Variable**, **JSON Extractor**, **Code Extractor**, **BeanShell**. |
| **Enterprise** | The Joget edition required by the plugins. |
| **Evaluation (eval)** | A suite of test cases scored against expected output. |
| **Exact Match** | An evaluation scorer: score 1 when the answer equals the expected text, otherwise 0. With **Strict Matching** off, case is ignored. |
| **Fail open** | A guard behaviour: when the guard cannot do its check because of an error, the content passes. |
| **Function** | A named operation that a tool offers to the model, with parameters. Also called a tool function. |
| **Function Description** | The text that tells the model when to use a tool function. |
| **Group tool** | A tool that bundles other tools. A batch that contains it runs sequentially. |
| **Guard** | A policy check on input or output: **Rule-Based Guard**, **LLM-Based Guard**, **Webhook Guard**. Actions: **Block**, **Sanitize**, **Redirect**. |
| **Hands-free voice (beta)** | A mode in which builders talk to the composer without pressing a button. It needs a TypeSafe credential and an OpenAI key. |
| **Hash variable** | A Joget runtime substitution written between `#` signs, for example `#form.table.field#`. |
| **Headless** | Working without an open builder page. The Composer MCP `builder_*` tools are headless. |
| **Hibernate** | The database mapping library of Joget. The plugins create their tables with `hbm2ddl.auto=update`. |
| **Idle timeout** | The time after which an inactive composer session is closed. Default 10 minutes, always on. |
| **invoke_skill** | The function a composer or agent calls to read a skill or one section of it. |
| **Iteration** | One task execution in a run. See **Max Iterations**. |
| **JSON Extractor** | An enhancer that extracts JSON from a model reply. It must come before **Store to Form**. |
| **JSON-RPC 2.0** | The message format of MCP. Error codes `-32700`, `-32601`, `-32602`, `-32603`. |
| **Kill** | The **AI Session Monitor** action that closes a composer session. |
| **List Composer** | The composer of the datalist builder. |
| **LLM** | Large language model. |
| **LLM Judge** | An evaluation scorer that asks a judge agent to rate the answer from 0 to 10. |
| **LLM node** | The block of an agent that selects the model. |
| **LLM Service** | A connector that links an agent to a model provider. |

## Glossary: M to Z

| Term | Meaning |
|---|---|
| **Marketplace** | The category of the userview menu elements where **Joget AI Assistant** is listed. |
| **Max Iterations** | The largest number of task executions in one run. Default 50. |
| **Max Tool Call Depth** | The largest number of tool-call rounds in one task. Default 100. |
| **MCP** | Model Context Protocol, an open standard for connecting tools to an AI client. |
| **MCP Client tool** | An agent tool that uses the tools of an outside MCP server. |
| **Mcp-Session-Id** | The HTTP header that carries the session id of a Composer MCP client. |
| **Model Class** | The provider selector in the **Add AI Service** popup: OpenAI, Anthropic, Google Gemini, Custom. |
| **Node viewer** | The run graph of an agent. Yellow is running, green is success, red is failed. |
| **Nonce** | A one-time token that binds a datalist action request to an agent. |
| **Orchestrator** | The composer that plans and drives the other composers to build several artifacts. |
| **orchestratorOnly** | A flag on a tool function. External bridges such as Composer MCP hide it. The Agent Builder runtime does not enforce it. |
| **OSGi** | The module system of Joget plugins. `Import-Package`, `Export-Package` and `DynamicImport-Package` decide which classes a bundle sees. |
| **Palette** | The list of elements (form fields, list columns, menus) that a builder offers and a composer may use. |
| **Parallel tool calls** | Running several tool calls of one model reply at the same time. Off by default, 4 threads. |
| **Persona** | A prompt that gives the agent a role and rules. |
| **Pill** | The round launcher button of a composer panel. |
| **Plan mode** | An Orchestrator mode that answers with a plan and builds nothing. |
| **PII** | Personally identifiable information. |
| **Preview** | Two meanings. A **Preview** tab runs an agent by hand. A feature labelled "Preview" is a pre-release feature that may change. |
| **Process Composer** | The composer of the process builder. |
| **Process Mode** | A mode of the Joget AI Assistant in which a process answers the chat. |
| **Prompt** | A block that instructs the model or gives it data. Types include **Text**, **Persona**, **Form Data**, **List Data**, **File**, **JSON API**, **Agent Variable**, **Output Format**, **BeanShell**. |
| **Prompt injection** | Hostile instructions hidden in text that an agent reads. |
| **Rate limit** | A provider limit on requests or tokens. The provider answers with HTTP 429. |
| **Reasoning Effort** | A model setting for how much the model thinks before it answers. Used by newer OpenAI, Claude and Gemini models. |
| **Retry** | A repeated model call after HTTP 429, a 5xx error or a network error. Default 3 attempts. |
| **Rollback journal** | A record, kept in memory, of what a synchronous Agent Process Tool wrote, used to undo the writes on failure. |
| **Rule-Based Guard** | A guard that checks Java regular expressions. |
| **Run** | One execution of an agent, composer or system agent. It has a `run_id`. |
| **Run Mode** | **Synchronous** or **Asynchronous** on the Agent Process Tool. |
| **Sanitize** | A guard action that replaces matched or flagged content. |
| **Scorer** | The method that scores an evaluation case: **Exact Match**, **Contains**, **LLM Judge**, **Decision**. |
| **Session (composer)** | One WebSocket connection of a composer panel. |
| **Skill** | A `SKILL.md` instruction set that the model reads on demand with `invoke_skill`. |
| **Slot** | In Composer MCP, one open builder tab registered with the server. |
| **SSE** | Server-Sent Events. Agent token streaming uses SSE. |
| **Sticky session** | A load balancer setting that sends one browser always to the same node. |
| **Streamable HTTP** | The MCP transport (version 2025-03-26) that Composer MCP uses. |
| **Streaming (token)** | Showing the model reply word by word. Off by default. |
| **Sub-agent** | An agent started by another agent with the **Run Agent** tool. |
| **System agent** | An agent defined in Java by a plugin, without a canvas. The composers use them. Table `ai_system_agent`. |
| **System prompt** | The instruction text sent first to the model. |
| **Task** | One step of an agent. It has prompts, tools, guards, enhancers and a decision. |
| **Task Timeout (seconds)** | The longest time a task may run. Blank means no timeout. |
| **Temperature** | A model setting for randomness. Some models ignore or reject it. |
| **Theme Composer** | The composer of the theme builder. |
| **Token** | The unit of text a model reads and writes. About 4 characters. |
| **Tool** | A capability the model can call during a run. |
| **Tool Template** | A pre-configured tool saved in AI Central Config and linked to a skill or offered in the palette. |
| **Trace** | The record of the steps of a run (table `ai_agent_trace_event`). |
| **Trigger Source** | The origin of a run in the history: Sync Process Tool, Async Process Tool, Datalist Action, AI Assistant, Builder Preview, Sub Agent, Agent Evaluation and others. |
| **TypeSafe (System One)** | A credential type for the TypeSafe service. It is used by hands-free voice and decision providers. It cannot be an AI service. |
| **UI Composer** | The composer of the userview builder. |
| **User Input tool** | A tool that pauses a run for a human answer (default wait 300 seconds). It works in the datalist chat window and the assistant agent mode. |
| **Userview** | A Joget app front end made of categories and menus. |
| **Voice provider** | A speech-to-text provider for dictation: **OpenAI Whisper API** or **Google Web Speech API**. |
| **Web Speech API** | The browser's own speech recognition. Chromium browsers only. |
| **Webhook Guard** | A guard that sends content to your HTTP endpoint. |
| **WebSocket** | The connection a composer panel keeps with the server. |
| **Whisper** | OpenAI speech-to-text. Used by the **OpenAI Whisper API** voice provider and by hands-free voice. |
| **wf_setup** | The Joget settings table. AI Central Config stores its settings there. A value holds about 64 KB. |
| **wflow** | The Joget base directory. |
| **Workflow variable** | A process-level variable. Agents can write to it with **Store to Process Variable**. |

## Console pages, builders and elements the plugins add

URLs are relative to the Joget base address (for example `http://localhost:8080/jw`). A space in a plugin name is written as `%20` in a URL. A console page plugin is reached at `/web/console/plugin/<plugin name>`.

| Name in the UI | Where | Plugin name in the URL | Who can open it |
|---|---|---|---|
| **AI Central Config** | **Settings > AI Central Config** | `AI Central Config` | Administrator, app creator |
| **AI Session Monitor** | **Monitor > AI Session Monitor** | `AI Session Monitor` | Administrator, app creator |
| **AI Usage Dashboard** | **Monitor > AI Usage Dashboard** | `AI Usage Dashboard` | Administrator, app creator |
| **Agent Execution Audit Trail** | **Monitor > Agent Execution Audit Trail** | `Agent Execution Audit Trail` | Administrator, app creator |
| **Agent Eval Dashboard** | **Monitor > Agent Eval Dashboard** | `Agent Eval Runs` | Administrator, app creator |
| **AI Agent Builder** | App Composer, next to the Form, List, UI, Process and Theme builders | - | App designers |
| **Agent Eval Builder** | App Composer | - | App designers |
| **AI Agent** (process tool) | Process Builder, tool activity, plugin list | - | App designers |
| **AI Decision (Multiple Choice)** (process tool, Preview) | Process Builder, tool activity | - | App designers |
| **Run AI Agent** (datalist action) | List Builder, row action | - | App designers |
| **Joget AI Assistant** (userview menu) | Userview Builder, category **Marketplace** | - | App designers |
| Form Composer pill | Form builder, page `/web/console/app/<app>/<version>/form/builder/<id>` | - | Users of the builder |
| List Composer pill | List builder, `/web/console/app/<app>/<version>/datalist/builder/<id>` | - | Users of the builder |
| UI Composer pill | Userview builder, `/web/console/app/<app>/<version>/userview/builder/<id>` | - | Users of the builder |
| Process Composer pill | Process builder, `/web/console/app/<app>/<version>/process/builder...` | - | Users of the builder |
| Theme Composer pill | Theme builder, `/web/console/app/<app>/<version>/cbuilder/theme/design/<id>` | - | Users of the builder |
| Orchestrator pill | App Composer page `/web/console/app/<app>/<version>/builders...` | - | Users of the builder |
| Hands-free voice | Loaded into the whole console when enabled on the **Voice** tab | - | Administrator, app creator |

AI Central Config tabs: **AI Credentials**, **AI Services**, **AI Plugins**, **Skills**, **Tool Templates**, **Voice**.

AI Session Monitor sections: **Total Active Sessions**, **Heap Used / Max**, **GC Activity (Collection Count)**, **Active Sessions**, **AI Composer Availability**, and the limits **Limit max concurrent sessions**, **Limit max session duration (minutes)**, **Idle timeout (minutes)**, with the buttons **Save Limits**, **Refresh Now**, **Kill All Sessions**.

## Endpoints

All paths are relative to the Joget base address. Plugin web services run as `/web/json/plugin/<class name>/service`. An app-scoped form is `/web/json/app/<app id>/<version>/plugin/<class name>/service`.

| Endpoint | Method | Purpose | Access |
|---|---|---|---|
| `/web/socket/plugin/<composer class name>` | WebSocket | Chat connection of a composer panel. Class names: `org.joget.ai.agent.lib.composer.FormComposerChat`, `DatalistComposerChat`, `UiComposerChat`, `ProcessComposerChat`, `ThemeBuilderComposerChat`, `OrchestratorChat`. Opened when you first click the pill. | Logged-in builder user |
| `/web/json/plugin/org.joget.ai.agent.AgentBuilder/service?action=<action>` | GET, POST | Agent Builder web service. Actions: `config`, `selector`, `options`, `elements`, `runAgent`, `submitUserInput`, `ajaxRendering`, `listColumnsOptions`, `generatePrompt`, `llmOptions`, `llmConfig`, `taskOptions`, `skillOptions`, `listVersions`, `getVersion`, `checkoutVersion`, `setActiveVersion`, `deleteVersion`, `createNewVersion`, `renameVersion`. | Administrator or app creator |
| `/web/json/plugin/org.joget.ai.agent.lib.AgentEvalBuilder/service?action=<action>` | POST | Evaluation service. Actions: `run`, `getResult`, `listRuns`, `saveCase`, `deleteCase`, `uploadFile`. | Administrator or app creator |
| `/web/json/plugin/org.joget.ai.config.CentralConfig/service?_a=<action>` | POST | AI Central Config service (credentials, services, skills, central instructions, tool templates, voice, TypeSafe test). | Administrator or app creator |
| `/web/json/plugin/org.joget.ai.config.VoiceTranscriptionService/service` | POST | Server-side speech-to-text for the Whisper provider. | Administrator or app creator |
| `/web/json/plugin/org.joget.ai.voice.VoiceCommandService/service?_a=<action>` | POST | Hands-free voice. Actions: `status`, `destinations`, `command`, `realtimeSession`. | Administrator or app creator |
| `/web/console/plugin/<plugin name>/list`, `/abort`, `/data`, `/kill`, `/killAll`, `/saveSettings`, `/saveComposerTypes` | various | Data and actions of the monitoring pages (list and abort for Agent Execution Audit Trail, data, kill, limits and availability for AI Session Monitor, list for AI Usage Dashboard). | Console users with the role |
| `/jw/mcp/composer/mcp` | POST | Composer MCP endpoint (Streamable HTTP, protocol 2025-03-26). Session header `Mcp-Session-Id`. | Key or administrator session for `builder_*` tools |
| `/jw/mcp/composer/register` | POST | A builder tab registers a slot. | Not behind the key |
| `/jw/mcp/composer/slot-status?slotId=<id>&sinceVersion=<n>` | GET | The builder tab polls for canvas updates (about every 1.2 seconds). | Not behind the key |
| `/jw/mcp/composer/inject.js` | GET | Script injected into builder pages. | Public |
| `/jw/api/*` | various | Builder REST API used by the `builder_*` tools. | Key or administrator session |
| `/jw/plugin/<plugin class name>/...` | GET | Bundled JavaScript, CSS and templates of the plugin. | Public static files |

The Agent Builder web service trusts the definition that the caller sends. It is meant for the builders, not as an integration route for other systems. Use the process tool, the datalist action or the assistant to call an agent from an app (see [Using Agents Inside Joget Apps](08-using-agents-in-apps.md)).

## Configuration keys, system properties and environment variables

### Agent Builder: system properties and environment variables

System properties are set on the Java command line, for example in `setenv.sh` or `setenv.bat`: `-Dname=value`. They are read when Joget starts, so a restart is needed.

| Name | Type | Default | What it does |
|---|---|---|---|
| `agent.async.pollInterval` | system property | `5` (seconds) | How often the executor looks for `PENDING` async jobs. |
| `agent.async.workerThreads` | system property | `4` | Number of threads that run async jobs. |
| `aiab.orchestrator.review` | system property | `true` | Set `false` to switch off the review pass of the Orchestrator. |
| `aiab.orchestrator.review.rounds` | system property | `2` (range 0 to 3) | Maximum refinement rounds for each artifact. A further round runs only while the previous round made progress. |
| `ai.central.config.hidden.providers` | system property | empty | Comma-separated provider names to hide in AI Central Config (for example `Custom`). |
| `debug` | environment variable | not set | When `true`, the composer panel writes debug messages to the browser console. |

### Agent Builder: settings stored by AI Central Config and the monitors

These are rows in the Joget settings table `wf_setup` (property name in the first column). Do not edit them by hand. Use the console pages.

| Key | Holds | Written by |
|---|---|---|
| `AI_CONFIGS` | AI credentials (provider, key, base URL, other fields) | **AI Credentials** tab |
| `MY_AI_SERVICES` | AI services (name, model class, model name, tunable settings) | **AI Services** tab |
| `CURRENT_AI_SERVICE` | The id of the Active AI Service | **AI Services** tab |
| `AI_CENTRAL_INSTRUCTIONS` | Global and per-target central instructions | **Skills** tab |
| `AI_TOOL_TEMPLATES` | Tool templates | **Tool Templates** tab |
| `VOICE_PROVIDER_CONFIG` | Primary voice provider, provider fields, hands-free settings | **Voice** tab |
| `aiComposerSessionLimits` | Session limits: `maxSessionsEnabled`, `maxSessions`, `maxDurationEnabled`, `maxDurationMinutes`, `idleTimeoutMinutes` | **AI Session Monitor** |
| `aiComposerEnablement` | Which composer types are switched off | **AI Session Monitor** |
| `API_SERVER_CONFIG` | API server address and key | Older setting; no tab in this build |
| `MY_PII_MASKING_RULES_CONFIG` | PII rules | Older setting; no tab in this build |
| `AI_SKILLS` | Skills in older versions | Retired. Skills are files now. Composer MCP reads it only as a fallback. |

Hands-free voice settings in `VOICE_PROVIDER_CONFIG` use these names: `handsFreeEnabled`, `handsFreeSttMode`, `handsFreeSttModel`, `handsFreeRealtimeModel`, `handsFreeRealtimeDelay`, `handsFreeAddressThreshold`, `handsFreeActionThreshold`, `handsFreeAutoApplyThreshold`, `handsFreeNavigationMode`.

### Composer MCP: system properties and environment variables

Each setting is a system property or an environment variable. The system property wins. A blank value counts as unset. The environment variable name is the property name in upper case with dots replaced by underscores.

| System property | Environment variable | Default | Purpose |
|---|---|---|---|
| `joget.builder.api.key` | `JOGET_BUILDER_API_KEY` | not set | Shared secret that clients send as `X-Builder-Api-Key` or `Authorization: Bearer <key>`. |
| `joget.builder.api.runAs` | `JOGET_BUILDER_API_RUNAS` | `admin` | Joget user that key and anonymous callers write as. |
| `joget.builder.api.enable` | `JOGET_BUILDER_API_ENABLE` | none | Comma list of opt-in features: `plugin-upload`, `app-create`, `all`. |
| `joget.builder.api.allowAnonymous` | `JOGET_BUILDER_API_ALLOWANONYMOUS` | `false` | `true` removes the key check. Development only. |
| `joget.mcp.knowledge.dir` | none | not set | A folder of skills served live instead of the bundled skills (system property only). |

The helper command line tool in the Composer MCP repository reads `JOGET_API_KEY` (key), or `JOGET_USER` and `JOGET_PASSWORD` (sign-in), and `JOGET_URL` (address) from the environment of the person who runs it. These are client-side variables, not server settings.

Build-time settings of Composer MCP (Maven): `joget.version` (default `9.1.1`), `spring.version` (default `6.2.19`) and `agent.builder.api.version` (default `8.2-SNAPSHOT`).

## Database tables and file locations

The plugins create their tables when they start (`hbm2ddl.auto=update`). The database user needs the right to create and alter tables. No table has an automatic clean-up.

| Table | Holds | Notes |
|---|---|---|
| `ai_agent_execution` | One row per run: `run_id`, `agent_id`, `agent_name`, `app_id`, `user_id`, `trigger_source`, `status`, token counts (input, output, cache), `created_at` | Keep it longest: it holds the token totals. |
| `ai_agent_trace_event` | One row per step: `run_id`, `node_id`, `type`, `label`, `status`, `request`, `response` | Contains full text. A field is cut at 1,000,000 characters. |
| `ai_llm_calls` | One row per model call that succeeded: `session_id` (= `run_id`), `llm_model`, `model_class`, `prompt`, `response`, token counts, `user_id` | Contains full prompts and replies. Failed attempts are not stored. |
| `ai_agent_async_job` | Async jobs: `agent_id`, `agent_properties_json` (input values, plain text), `app_id`, `app_version`, `username`, process and activity ids, `status`, `error_message`, `created_at`, `modified_at` | Statuses `PENDING`, `RUNNING`, `DONE`, `FAILED`. |
| `ai_agent_abort_request` | Cooperative stop requests: `run_id`, `date_created` | Works across nodes. |
| `ai_agent_input_response` | Answers to **User Input** questions | |
| `ai_agent_version` | Saved versions of agents | |
| `ai_eval_run` | Evaluation runs: `suite_id`, `agent_id`, `scorer_type`, `total_cases`, `passed_cases`, `overall_score`, `results_json` | |
| `ai_system_agent` | Registry of system agents | |
| `ai_assistant_conversation`, `ai_assistant_message`, `ai_assistant_run` | Joget AI Assistant conversations in Agent Mode | |
| `ai_chat_session`, `ai_chat_message` | Mapped by the plugin | |
| `mcp_artifact_slot`, `mcp_session` | Composer MCP slots and sessions | Created by Composer MCP. Written about every 5 seconds. |
| `wf_setup` | Joget settings, including the AI Central Config keys above | Value size about 64 KB. |
| `app_fd_<table>`, columns `c_<field>` | Form data of Joget forms | The Orchestrator names tables `tbl_<form id>`, so the table is `app_fd_tbl_<form id>`. |

| Location | Holds |
|---|---|
| `<JOGET_HOME>/wflow/app_ai_skills/<skill folder>/SKILL.md` | One file per skill. Create, edit and delete skills in AI Central Config or place files here. A cluster needs shared storage for this folder. |
| `<Tomcat>/logs/joget.log` | Joget server log, including all plugin log lines. |
| Browser developer console | Messages of the composer panels. With the environment variable `debug=true`, more detail. |

## Defaults: agent, task and runtime settings

The agent settings are on the **Settings** tab of the AI Agent Builder. The task settings are in the task's **Timeout Settings** section.

| Setting | Default | Notes |
|---|---|---|
| **Enable Token Streaming** | Off | |
| **Debug Mode** | Off | Writes full requests and replies to the log. |
| **Max Iterations** | 50 | A value of 0 or less is replaced by 50. |
| **Enable Parallel Tool Calls** | Off | |
| **Parallel Tool Call Threads** | 4 | |
| **Max Tool Call Depth** | 100 | |
| **LLM Socket Timeout (seconds)** | 120 | A task can override it. |
| **LLM Connect Timeout (seconds)** | 30 | A task can override it. |
| **LLM Retry Max Attempts** | 3 | Total attempts, including the first. 1 turns retries off. |
| **LLM Retry Initial Delay (ms)** | 1000 | Doubles on each retry. |
| **LLM Retry Max Delay (ms)** | 30000 | A random 0 to 999 ms is added to each wait. |
| **Enable Context Window Management** | On | |
| **Trimming Strategy** | Truncate | The other value is Sliding Window. |
| **Max Characters** | 400000 | About 100,000 tokens. 0 turns the check off. |
| **Max Messages** | 0 (off) | Needed for Sliding Window. |
| **Preserve System Messages** | On | |
| **Preserve First User Message** | On | |
| **Warn Threshold (%)** | 80 | 0 turns the warning off. |
| **Task Timeout (seconds)** | Blank (no timeout) | Per task. A task with a timeout runs on its own worker thread. |
| **Include Task Metadata in Next Task** | Off | Passes the task result to the next task with its number and name. |
| **Run Mode** (Agent Process Tool) | Synchronous | |
| **On Agent Failure** (Agent Process Tool) | Continue (log only) | Only in Synchronous mode. |
| **Failure Marker Value** | `failed` | For **Set Status & Continue**. |
| **Mode** (Run AI Agent datalist action) | direct | The other value is chat. |
| **Timeout (seconds)** (User Input tools) | 300 | |
| Evaluation **Pass Threshold** | 0.7 | Range 0 to 1. |
| Evaluation **Strict Matching** | False | Exact Match only. |
| Joget AI Assistant **Messages Sent To Agent** | 10 | Agent Mode. |
| Joget AI Assistant **Keep Conversation History** | True | Agent Mode, logged-in users. |
| Joget AI Assistant **Maximum Upload Size (MB)** | 10 | |
| Joget AI Assistant **Allowed Upload File Types** | `pdf,png,jpg,jpeg,gif,webp,bmp,txt,csv,json,md,xml,html,log` | |
| Composer **Limit max concurrent sessions** | Off (value 10 when switched on) | Per node. |
| Composer **Limit max session duration (minutes)** | Off (value 30 when switched on) | |
| Composer **Idle timeout (minutes)** | 10 | Always on. Must be smaller than the maximum duration. |
| Composer create mode | **Confirm** | Choose **Create** and then **Confirm**, **Autopilot** or **Plan**. |
| Voice **Primary Voice Provider** | OpenAI Whisper API | Falls back to Google Web Speech API when no key is saved. |
| Voice **Speech-to-text mode** | Realtime | |
| Voice **Realtime model** | `gpt-live-transcribe` | |
| Voice **Live transcript delay** | Low | |
| Voice **Per-utterance transcription model** | `gpt-4o-mini-transcribe` | Hands-free voice. |
| Whisper provider **Model** | `gpt-4o-transcribe` | Field **Model** of the provider. |
| Hands-free **Addressed-to-Composer threshold** | 0.85 | |
| Hands-free **Action threshold** | 0.70 | |
| Hands-free **Auto-apply threshold** | 0.90 | |

## Limits: sizes, counts and timeouts

| Limit | Value | Where it applies |
|---|---|---|
| Composer WebSocket message buffer | 2 MB | Text and binary messages of a composer session. |
| Composer ping | about every 30 seconds | Also the tick for session duration and idle checks. |
| Image attached to a composer message | 5 MB | Per image. |
| Turns of chat sent with each composer request | last 6 | Repeat ids in each request. |
| Composer model **Max Tokens** | 16000 | Used by the Form, List, Process, Theme composers and the Orchestrator. |
| Sub-composer run inside the Orchestrator | 300 seconds | Then "<composer name> timed out after 300 seconds". |
| Orchestrator approval wait (Confirm mode) | 30 minutes | |
| Orchestrator review rounds | 0 to 3, default 2 | `aiab.orchestrator.review.rounds`. |
| Orchestrator artifact summary for the review | 3,500 characters | |
| Orchestrator ids | lower case, underscores, at most 20 characters | Tables are `tbl_<form id>`. |
| Value in `wf_setup` | about 64 KB (65,535 bytes) | Central instructions, tool templates and other settings. Skills are files and have no such limit. |
| Async job poll fetch | 8 jobs per poll | |
| Async job workers | 4 | `agent.async.workerThreads`. |
| Trace field sent live to the browser | 200,000 characters | |
| Trace field saved in the database | 1,000,000 characters | |
| Decision options (AI Decision) | at most 255; 2 to 10 in Score mode | |
| Model call time | connect 30 s, read 120 s, 3 attempts | Defaults. A call that never answers can take about 6 minutes plus waits. |
| Joget AI Assistant HTTP timeout | 120 seconds; streaming 5 minutes | Chat Mode. |
| Composer MCP skill response | 40,000 characters | Continue with `offset`. |
| Composer MCP `builder_data` `file` | 50 MB | |
| Composer MCP session / slot idle removal | 2 hours / 4 hours | Clean-up every 30 minutes. |
| Composer MCP tab counts as open | polled within 15 seconds | |
| Composer MCP state write to the database | about every 5 seconds | The last few seconds can be lost in a crash. |

## Supported providers and default models

| Provider | Used by | Default model | Default API URL | Default **Max Tokens** |
|---|---|---|---|---|
| OpenAI | Agent LLM node, AI Service (model class **OpenAI**); also the default model name of the assistant Chat Mode | `gpt-5.4` | `https://api.openai.com/v1/chat/completions` | 4096 |
| Anthropic (Claude) | Agent LLM node, AI Service (**Anthropic**) | `claude-sonnet-5` | `https://api.anthropic.com/v1/messages` (header `anthropic-version` `2023-06-01`) | 32000 |
| Google Gemini | Agent LLM node, AI Service (**Google Gemini**) | `gemini-3.5-flash` | `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` | 8192 |
| IBM watsonx | Agent LLM node only (needs a **Project ID**) | `meta-llama/llama-3-2-11b-vision-instruct` | `https://us-south.ml.cloud.ibm.com/ml/v1/chat/completions` (API version `2024-05-31`) | 4096 |
| OpenRouter | Agent LLM node only | `google/gemma-3-27b-it:free` | `https://openrouter.ai/api/v1/chat/completions` | 4096 |
| Custom (OpenAI-compatible) | Credential type and model class; works like OpenAI with your base URL | your model name (**Custom Model Name**) | your **Custom LLM URL** | as OpenAI |
| TypeSafe (System One) | Credential for hands-free voice and decision providers; not a chat model | `jev-latest` | `https://api.typesafe.ai` | timeout 4000 ms |
| Pinecone | Credential for the Pinecone tool (vector search) | - | your index URL | - |

Other provider facts:

- The AI Composers and the Orchestrator can use only the model classes that appear in the **Add AI Service** popup: OpenAI, Anthropic, Google Gemini and Custom. IBM watsonx and OpenRouter work in agent LLM nodes.
- A credential for AWS Bedrock is not offered in this build.
- You can type any model ID. The suggestion list is only a help.
- The model must support tool (function) calling for the composers and for agents that use tools.
- Voice providers: **OpenAI Whisper API** (needs an OpenAI key) and **Google Web Speech API** (Chromium browsers only, no key).
- Embedding providers for vector tools: OpenAI Embedding and Gemini Embedding.

## Bundled skills

The jar seeds these skills in `app_ai_skills` when the plugin starts: `joget-form-builder`, `joget-form-validators-binders`, `joget-datalist-builder`, `joget-process-builder`, `joget-userview-builder`, `joget-form-field-ui`, `joget-theme-builder`, `joget-orchestrator`, `joget-app-design`, `joget-known-solutions` and `joget-hash-variables`. See [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md).

## Where to go next

- [Troubleshooting](30-troubleshooting.md) for symptoms, causes, fixes and the index of messages.
- [Frequently Asked Questions](31-faq.md) for short answers.
- [AI Central Config Reference](03-ai-central-config.md) for every field of the console page.
- [Runtime Settings, Monitoring and Cost](09-runtime-and-monitoring.md) for the agent settings in detail.
- [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md) for the Composer MCP settings.
