# Frequently Asked Questions

This page gives short, direct answers to the questions that customers and colleagues ask most often about Agent Builder, the AI Composers, the Orchestrator and Composer MCP. The answers say plainly what the product does and does not do. It is written for implementers who must answer these questions in front of a customer. Each answer points to the page of this manual that has the detail.

The questions are in groups: general and licensing, models and providers, cost, Agent Builder behaviour, composers and the Orchestrator, Composer MCP, extending, operations and security.

> **Note:** This manual does not state prices, licence terms, build times or future features. Where a question depends on them, the answer says so. Ask Joget for those items in writing.

## General, licensing and edition

**Q1. What is in the package?**
Four things: this manual, the Agent Builder plugin jar (`agent-builder-plugin-8.2.5-ALPHA.jar`, an alpha build), the Composer MCP plugin jar (`composer-mcp`, version 9.1.0-ALPHA) and the examples repository `ai-partner-samples` with buildable sample plugins, skills and prompts. You also need a Joget DX Enterprise installation. See [Welcome and What Is Included](01-welcome.md).

**Q2. What is the difference between Agent Builder, the AI Composers, the Orchestrator and Composer MCP?**
- **Agent Builder** is a builder in the App Composer. You design **agents**: a model, a persona and an ordered list of tasks with prompts, tools, enhancers, decisions and guards. Agents run inside your apps.
- The **AI Composers** are chat panels inside the Form, List, UI, Process and Theme builders. You describe a change in words and the composer edits the artifact for you.
- The **Orchestrator** is a chat panel that plans and drives the other composers to build several artifacts of an app from one request.
- **Composer MCP** is a separate plugin that lets an outside AI client (for example Claude Code) build Joget apps with the client's own model.

**Q3. Does it run on Joget Community?**
No. The plugin registers its blocks only on Joget DX Enterprise. On another edition it writes `This plugin jar does not support community version` to the log and nothing is registered.

**Q4. Which Joget versions are supported?**
Use Joget DX 9.1 or later, Enterprise edition, for both plugins. Composer MCP requires Joget DX 9.1 or later and does not load on Joget 8.x. This manual was checked on Joget DX 9.1.2. Check [Installing and Setting Up Your Joget Instance](02-installation-and-setup.md) for the versions to use on your server.

**Q5. Does the plugin include a language model?**
No. You bring your own model and key (or your own endpoint) and register it in **Settings > AI Central Config**. The plugin calls that model over HTTP. See [AI Central Config Reference](03-ai-central-config.md).

**Q6. How much does the plugin cost?**
This manual does not state prices or licence terms. They come from your agreement with Joget. The running cost of the model is separate and is paid to your model provider (see the cost questions).

**Q7. What does "ALPHA" or "beta" mean here?**
The jars are pre-release builds for partner evaluation. Expect rough edges, test on a non-production server first, and report problems to Joget. Some features are labelled **Preview** or **beta** in the manual, for example hands-free voice. A preview feature may change.

**Q8. Can it run without internet access?**
The plugin needs a model it can reach over the network. If the model is a private or self-hosted endpoint inside your network, no internet access is needed for the model. Some optional features call outside services: voice input (OpenAI, Google), hands-free voice (TypeSafe) and Chat Mode of the Joget AI Assistant (the server in its **Domain** field). Skills and the bundled references are inside the jar.

## Models and providers

**Q9. Which model providers are supported?**
Agent nodes can use OpenAI, Anthropic (Claude), Google Gemini, IBM watsonx and OpenRouter. The AI Composers use the **Active AI Service**, which you can create with the model classes OpenAI, Anthropic, Google Gemini and **Custom**. **Custom** is any endpoint that speaks the OpenAI chat format. See [Glossary and Reference Tables](32-glossary-and-reference.md) for default models.

**Q10. Which model should I choose?**
Choose a model that supports tool (function) calling. The composers work by letting the model call functions, so a model without that support answers in text and changes nothing. Stronger models give clearly better results on large requests. Test your model with one composer request and one agent before you promise results to a customer.

**Q11. Can I use a private, self-hosted or sovereign model?**
Yes, if the model is served through an endpoint that accepts the OpenAI chat-completions format and supports tool calling. Create an **AI Credentials** entry of type **Custom** with your base URL and key, and then an **AI Service** with model class **Custom**. Enter only the base address, not a request path. The same works for agents through the OpenAI service with its **API URL** changed, for example for Azure OpenAI, an AI gateway, LM Studio, LocalAI or LiteLLM. The quality of the result depends on the model. Small models often fail at tool calling.

**Q12. Can we keep data inside our country?**
The plugin itself keeps its own data in your Joget database. What leaves your server depends on the model endpoint you choose. If the endpoint runs in your country or in your own data centre, prompts stay there. If you use a cloud provider, check which region serves the request and the provider's terms. IBM watsonx uses a regional **API URL**. The plugin has no data-residency switch of its own. Voice features are different (see Q13).

**Q13. What data leaves my server?**
- **Composers and the Orchestrator** send your request, the current artifact definition (field ids, labels, options, binder settings, SQL inside JDBC binders, e-mail templates), the app structure (forms, lists, tables, columns), the text of skills and the central instructions to the model provider. They do not read record data.
- **Agents** send whatever their prompts and tools put into the conversation. This can include form records, list rows, file contents, database query results and API responses, because the **Form Data**, **List Data**, **File**, **Database Query** and **JSON API** blocks read them for the model.
- **Voice** sends audio to OpenAI (Whisper and the realtime mode) or to Google (the browser's Web Speech API). Hands-free voice also sends the transcripts to TypeSafe.
- **Composer MCP** sends what your outside AI client sends to its own provider. The plugin does not control that.

**Q14. Does the model provider keep or train on our data?**
That depends on the provider's terms and your contract, not on the plugin. Read the provider's data-use terms, and use an enterprise or zero-retention agreement if your policy needs it.

**Q15. Can different composers use different models?**
The composers use the one **Active AI Service**. You can change it at any time in **AI Central Config**. Agents are different: each agent LLM node selects its own credential, so one app can use several models.

**Q16. Can I route traffic through a proxy or an AI gateway?**
Yes. Set the **API URL** (base address) of the credential or the node to the gateway address. The HTTP client uses the Java proxy settings of the Joget server.

**Q17. Why does the provider return "unsupported parameter"?**
Some models reject parameters such as **Temperature**, **Top P**, **Top K**, a fixed thinking budget or `max_tokens`. The plugin removes parameters that a known model does not accept, and assumes the newest behaviour for an unknown model ID. Clear the parameters you do not need. See [Troubleshooting](30-troubleshooting.md).

## Cost

**Q18. How much will the model cost me to run?**
There is no fixed number. The cost depends on the model, the size of your requests, the size of your apps and how often people use the AI features. The plugin counts tokens, not money. Run a realistic test on your own model, read the token totals on the **AI Usage Dashboard**, and multiply by your provider's price.

**Q19. How can I see what the AI features use?**
**Monitor > AI Usage Dashboard** shows token use. **Monitor > Agent Execution Audit Trail** lists every run with its token counts. The table `ai_llm_calls` has one row for each model call that succeeded. See [Runtime Settings, Monitoring and Cost](09-runtime-and-monitoring.md).

**Q20. Can I set a spending limit?**
Not in the plugin. There is no money value, no price table and no budget. Set spend limits at the model provider. Inside Joget you can limit session count and duration of the composers, switch composers off, and bound agent runs with **Max Iterations**, **Max Tool Call Depth** and **Task Timeout (seconds)**.

**Q21. Are the token totals exact?**
They are a minimum. Only calls that succeeded are recorded. A failed or timed-out attempt may still be billed by the provider but is not counted by Joget.

**Q22. Why does one request use so many tokens?**
A composer or agent run is a loop: the model calls a function, reads the result, and calls again. Each call sends the conversation again. Large artifacts, many skills and long central instructions add to every call. The Orchestrator also writes a design brief and reviews each artifact, so it uses more calls than a single composer.

**Q23. How can I reduce the cost?**
Use a cheaper model for simple work, ask for one change at a time, keep central instructions short and put long reference text in skills (a skill is read only when needed), switch off composers you do not use, lower **Max Characters** for agents, and keep prompts and tool results small. For the Orchestrator, `-Daiab.orchestrator.review.rounds` (0 to 3, default 2) and `-Daiab.orchestrator.review=false` reduce calls but can lower quality.

## Agent Builder behaviour

**Q24. Is the output of an agent always the same?**
No. A language model can give a different answer to the same input. Use a low **Temperature** where the model allows it, give clear output formats (the **Output Format** prompt and **JSON Extractor** enhancer), and test with an evaluation suite. See [Evaluating Agents](10-evaluating-agents.md).

**Q25. Can an agent act without a person checking?**
Yes, if you build it that way. An agent in a process step or an async job runs without anyone watching. For actions that matter, design a person-in-the-loop step: the agent prepares, a person decides. See [Using Agents Inside Joget Apps](08-using-agents-in-apps.md).

**Q26. What stops an agent from looping or running too long?**
**Max Iterations** (default 50) limits the tasks of a run. **Max Tool Call Depth** (default 100) limits tool-call rounds inside a task. **Task Timeout (seconds)** limits one task. There is no time limit or token limit for a whole run, and no spending cap. Set limits at the provider as well.

**Q27. Whose permissions does an agent run with?**
A run started by a user (Preview, a datalist action, a synchronous process step) uses the current Joget user. An async job restores the user stored when the job was queued. Do not treat the run-as user as a security boundary: the agent's tools can read what the tool is configured to read. Give each tool only the access it needs.

**Q28. Can an agent call other systems?**
Yes, with tools: **JSON API Tool**, **Database Query**, **E-mail**, **MCP Client**, **GitLab**, **Google Web Search**, **Pinecone**, **Mem0 Memory** and your own tools. Header values and keys in tool properties are stored in the agent definition, so they are exported with the app. Keep secrets out of definitions you share. See [Building Block Reference: Tools](06-reference-tools.md).

**Q29. What is the difference between synchronous and asynchronous runs?**
In **Synchronous** mode the process waits for the agent. In **Asynchronous** mode the process continues at once and a background worker runs the agent later and stores the result. Asynchronous jobs are not retried and not recovered after a restart. See [Runtime Settings, Monitoring and Cost](09-runtime-and-monitoring.md).

**Q30. How do I test an agent before I use it?**
Use the **Preview** tab to run it by hand, read the trace in the node viewer, and build an evaluation suite with the **Agent Eval Builder**. The scorers are **Exact Match**, **Contains**, **LLM Judge** and **Decision**. The default pass threshold is 0.7.

**Q31. Can I keep versions of an agent?**
Yes. The **Agent Versions** dialog lets you create, rename, check out, activate and delete versions. A run row in the history does not record which version ran, so note the date of every change in your own change log.

**Q32. Do guards make an agent safe?**
Guards reduce risk. They do not remove it. The **Rule-Based Guard** blocks, redirects or replaces text that matches your patterns, on input and output. The **LLM-Based Guard** and the **Webhook Guard** call another service and let the content pass when that call fails. The Webhook Guard blocks when the endpoint answers with a status other than 200. Combine guards, and test them. See [Security and Governance](11-security-and-governance.md).

## Composers and the Orchestrator

**Q33. What can the composers build?**
- **Form Composer**: forms, sections, fields, validators, binders, options, visibility rules.
- **List Composer**: lists with columns, filters, sorting, formatters, actions and binders.
- **UI Composer**: userviews with categories, menus, pages and dashboards.
- **Process Composer**: processes with activities, routes, tools and rules.
- **Theme Composer**: theme properties such as colours and layout.
- **Orchestrator**: several of these in one request, in one app.

See [AI Composers User Guide](12-composers-user-guide.md) and [The Orchestrator: Building Whole Apps](13-orchestrator.md).

**Q34. Is the result always correct?**
No. The model can make mistakes: use a class your server does not have, misread a requirement, or build something that looks right and is wrong. Always check the saved result in the builder and run it in a published app. The panel offers **Revert** and `undo`. Standalone composers only change the canvas until you press **Save** in the builder.

**Q35. How long does a build take?**
Joget gives no fixed time. A single change in one composer is usually a short wait, and a request that builds several artifacts takes much longer. The time depends on the model, the provider load, the size of the request and of the app, and the Orchestrator's review passes. Do not promise a build time to your customer. The panel shows a timeline while it works and you can press **Stop**.

**Q36. Why is the Orchestrator slower or more expensive than one composer?**
It plans first, then builds each artifact with the matching composer, reviews each result and may refine it, and finally checks for gaps. Each step is a model call. It is built for quality and coverage over a whole app, not for speed.

**Q37. Which languages can I use? Does it work in Bahasa Indonesia?**
The panel text and the Joget buttons are in English. You can write your request in any language that your model understands, including Bahasa Indonesia, and the model can create labels, descriptions and messages in that language. Results depend on the model. To keep terms the same across an app, write a skill with your Bahasa terminology and scope it to the composers, and keep ids in English (lower case, underscores). For voice input in Bahasa Indonesia, enter the code `id` in **Language (optional)** on the **Voice** tab, because automatic detection can fail on short clips. See [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md).

**Q38. What can the composers not do?**
Some examples (the full list per composer is in [AI Composers User Guide](12-composers-user-guide.md)): the Form Composer cannot check that a named sub-form or master form exists; the List Composer does not build inbox, calendar, tree or datatables templates and handles only one join per table; the UI Composer cannot set per-menu permission objects; the Process Composer cannot map lanes to people (you do it in **Participants**); the Theme Composer needs hex colours such as `#1a73e8`. No composer deploys, tests in a running app, or undoes a **Save** that you already made. Use the app version history or an app export for that.

**Q39. Can the composers edit an app that already exists?**
Yes. Open the artifact in its builder and ask for a change, or ask the Orchestrator to update an artifact. Give the exact ids and say "update the existing ...". If you say "build the form" again, the Orchestrator can create a second copy.

**Q40. Do the composers read my data records?**
The composer tools work on definitions and app structure (forms, lists, tables, columns). They are not built to read the rows in your form tables. Agents are different: they read records when you give them blocks such as **Form Data**, **List Data** or **Database Query**.

**Q41. Can I make the composers follow our house standards?**
Yes. Write skills with your naming, layout, terminology and validation rules, scope them to the composers, and add short rules to the central instructions. Check the effect by running the same request with the skill active and as draft. See [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md) and [Writing Good Composer Prompts](14-composer-prompt-guide.md).

**Q42. Can I restrict a composer to some roles?**
Not as a feature. Anyone who can open a builder sees the composer pill. An administrator can switch a composer type off for everyone in **AI Session Monitor > AI Composer Availability**, and can limit the number of sessions and their duration. Save and publish rights stay with Joget's own permissions.

**Q43. Why did the Orchestrator create duplicate artifacts?**
A new request such as "build the form" creates a new artifact. To change an existing one, name its id and say "update". The Orchestrator uses ids in lower case with underscores, at most 20 characters, and tables named `tbl_<form id>`. It remembers the last 6 messages of the chat, so repeat the exact ids in each request.

**Q44. What is the difference between Ask, Confirm, Autopilot and Plan?**
**Ask** converses and previews and saves nothing. **Confirm** (the default when you choose **Create**) saves after you approve each change. **Autopilot** builds and saves without stopping to ask. **Plan** answers with a plan and builds nothing. Use **Confirm** for customer work.

## Composer MCP

**Q45. How is Composer MCP different from the composers?**
The composers are chat panels inside Joget that use the model you registered in AI Central Config. Composer MCP is a server inside Joget that your own AI client connects to. The client uses its own model and can also edit your source code in the same conversation. By default it works headlessly: it creates and changes apps, forms, lists, userviews, themes, processes and seed data in the database without any builder page open. As an option it can also drive an open builder tab live.

**Q46. What do I need to use it?**
Joget DX 9.1 or later (Enterprise), the Composer MCP jar, a shared key set as `JOGET_BUILDER_API_KEY` (or the system property `joget.builder.api.key`), and an MCP client that supports Streamable HTTP and custom headers, for example Claude Code. For the optional browser-connected composers you also need the Agent Builder plugin. See [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md).

**Q47. Is Composer MCP secure?**
It is deny-by-default for the builder tools: a call needs the shared key (or an administrator session). The key is one shared secret with administrator power, there are no per-user keys, and the browser-connected composer endpoints, the skill tools and the tool list are not behind the key. The CORS header allows any origin. Use TLS, a long random key, and limit network access to the Joget host. Do not expose it to the internet. Keep `plugin-upload` switched off. See [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md).

**Q48. Does the confirm flag protect against mistakes?**
Only as a prompt to the assistant. `confirm: true` is required for deletes, deploys, data writes and similar calls, but it is not an access control. Whoever has the key can set it.

**Q49. Is Composer MCP ready for production?**
No. It is version `9.1.0-ALPHA`, a pre-release. Use it on development and test servers.

**Q50. Can Composer MCP build a very large app?**
There is an app generator in the repository that turns a compact JSON spec into a large app and pushes it through the MCP endpoint, with lint checks before each write. It needs Node 18 or newer. See [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md). Give no build time or size promise to a customer.

## Extending

**Q51. What can we extend?**
Tools, guards, enhancers, decisions and prompts for agents; custom LLM providers; system agents; custom composer tools; and whole composers (which the Orchestrator discovers by itself). You write Java plugins as OSGi bundles and upload them like other Joget plugins. Skills and tool templates need no code. See [Extending Agent Builder: Overview and Development Setup](23-extending-overview.md).

**Q52. What do I need to build a plugin?**
A JDK, Maven, the Agent Builder API (the jar's exported packages) and the Joget plugin libraries. Start from the samples in the examples repository: `01-agent-builder-tools`, `02-system-agent`, `03-composer-custom-tool`, `04-custom-skills` and `05-composer-mcp`. See [The Examples Repository](29-examples-repository.md).

**Q53. Can we add our own model provider?**
Yes, with a provider plugin and a model-capability provider that agree on one name. For many private models the **Custom** credential (OpenAI-compatible endpoint) is enough and needs no code. See [Adding a Custom LLM Provider](26-custom-llm-providers.md).

**Q54. Will our custom plugin keep working after an upgrade of Agent Builder?**
Rebuild and retest it against the installed version. The API classes are exported by the plugin, and there is no formal compatibility promise in this manual. Keep custom plugins in your own package, not in a package of Agent Builder.

**Q55. Can a tool be limited to the Orchestrator?**
A function can be flagged `orchestratorOnly`. This is a convention that outside bridges such as Composer MCP honour. The Agent Builder runtime does not enforce it, so do not use it as a security control.

## Operations

**Q56. Does it work in a cluster?**
Yes, with these points. Runs, traces, usage rows, async job claims, abort requests, saved session limits and composer on/off settings live in the database and work across nodes. The list of composer sessions, the session counter, the streaming event bus and a run that waits for user input live in one node's memory. Use sticky sessions for browsers. The `app_ai_skills` folder must be on storage that every node sees. See [Runtime Settings, Monitoring and Cost](09-runtime-and-monitoring.md).

**Q57. Does it work on Joget Cloud?**
The plugin has handling for multi-tenant servers (for example the async executor stops itself for a removed tenant), but Joget Cloud has its own rules for plugins. Test on your own cloud tenant before you promise it to a customer, and do not assume that every feature behaves the same as on your own server.

**Q58. How do I upgrade?**
Upload the new jar in **Settings > Manage Plugins** and remove the older one so that only one version is active. Hard refresh the browser. Rebuild custom plugins against the new version. Bundled skills are refreshed when the new jar carries a higher `builtinVersion`: copy a bundled skill under a new name before you customise it, because your edits to the original can be overwritten.

**Q59. What do I need to back up?**
The Joget database, including the tables of the plugin (`ai_agent_execution`, `ai_agent_trace_event`, `ai_llm_calls`, `ai_agent_async_job`, `ai_agent_abort_request`, `ai_eval_run`, `ai_system_agent`, `ai_agent_version`, `ai_agent_input_response`, `ai_assistant_*`) and the settings table `wf_setup` (AI credentials, services, central instructions, tool templates). Also back up the `app_ai_skills` folder in the Joget base directory (`<JOGET_HOME>/wflow`). Agent definitions are part of the app, so an app export contains them. Composer MCP keeps `mcp_artifact_slot` and `mcp_session`.

**Q60. How long are prompts and logs kept?**
Until you delete them. There is no retention period, no automatic clean-up and no switch to turn off the saving of prompts and replies. Agree a retention period with the customer and run a purge job. See [Runtime Settings, Monitoring and Cost](09-runtime-and-monitoring.md).

**Q61. How do I find out why a run failed?**
Open **Monitor > Agent Execution Audit Trail**, open the run and open the red node. Then read the Joget log. See [Troubleshooting](30-troubleshooting.md).

## Security

**Q62. Who can configure the AI features?**
Administrators and app creators can open AI Central Config, the monitoring pages and the voice services. Settings are for the whole Joget platform, not per app: skills, central instructions, credentials and services are shared by all apps.

**Q63. Are API keys stored safely?**
Credentials are saved in the settings table (`wf_setup`) and secret values are meant to be stored as encrypted text. Check this on your own server: read the row for `AI_CONFIGS` and confirm that keys are not plain text. Limit who can open AI Central Config and the database, create a separate key per environment, set spend limits at the provider, and rotate keys when people leave. Tool properties and tool templates keep keys in the definition as plain properties.

**Q64. Does it mask personal data?**
There is no PII masking feature that you can configure in this build. Use the **Rule-Based Guard** to replace patterns on input and output, send only the fields the agent needs, and review what your tools read.

**Q65. What about prompt injection?**
Any text that an agent reads can contain instructions: a form field, a file, a web page, an API reply. Treat that text as untrusted. Give tools only the access they need, put guards on input and output, require a person to approve actions with side effects, and test with hostile input.

**Q66. Are the AI actions logged?**
Yes. Every run, every step (with request and reply) and every successful model call is stored, and the monitoring pages show them. Composer runs are in the history too. These rows contain full prompts and replies, so protect them like the data they came from. Debug Mode copies the same content into the server log, so keep it off in production.

**Q67. Could a skill be used to steer the composers in a bad way?**
Yes. Skills and central instructions are platform-wide, and administrators and app creators can edit them. Treat a change to a skill like a release: review it, test it and note it in your change log.

**Q68. Do the composers respect the user's permissions?**
The standalone composers change the canvas of a builder that the user already has open, and the user must press **Save**. The Orchestrator in **Confirm** or **Autopilot** mode saves the artifacts itself, after your approval in **Confirm** mode. Composer MCP writes as the configured run-as user (default `admin`) when a key is used.

**Q69. Where do I report a problem or ask for help?**
Collect the items listed in [Troubleshooting](30-troubleshooting.md) under "What to collect before you ask Joget for support" and send them to Joget.

## Where to go next

- [Troubleshooting](30-troubleshooting.md) for tables of symptoms, causes and fixes.
- [Glossary and Reference Tables](32-glossary-and-reference.md) for terms, settings, defaults and limits.
- [Security and Governance](11-security-and-governance.md) for roles, keys, guards and data handling.
- [Runtime Settings, Monitoring and Cost](09-runtime-and-monitoring.md) for runtime settings, monitoring and the purge statements.
