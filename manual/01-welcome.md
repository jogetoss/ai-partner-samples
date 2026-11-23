# Welcome and What Is Included

This manual explains how to set up, use and extend the Joget AI tooling: **Agent Builder**, the **AI Composers** and **Composer MCP**. It is written for implementers and plugin developers who already know Joget DX well. Start here to see what is included, what each part is for, and which pages to read for your job.

## What is included

| Item | What it is | Where it is used |
|---|---|---|
| This manual | 38 pages: setup, user guides, reference, developer guides | Read it in the order below, or jump to the page you need |
| `agent-builder-plugin-8.2.5-ALPHA.jar` | The Agent Builder plugin. It also contains AI Central Config, the AI Composers, the Orchestrator, voice input and the Joget AI Assistant | Upload it to your Joget DX Enterprise server. See [Installing and Setting Up Your Joget Instance](02-installation-and-setup.md) |
| `composer-mcp-9.1.0-ALPHA.jar` | The Composer MCP plugin. It lets an AI coding client on your own machine (for example Claude Code) build Joget apps through the Model Context Protocol (MCP) | Upload it to a Joget DX 9.1.x Enterprise server. See [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md) |
| The examples repository (`ai-partner-samples`) | Seven sample folders: custom tools, a system agent, a custom composer tool, sample skills, Composer MCP prompts and scripts, a custom LLM provider and a custom composer. All sample plugins build with one command | See [The Examples Repository](29-examples-repository.md) |

You do not receive, and do not need, the source code of Agent Builder or Composer MCP. Everything in this manual works with the four items above and a normal Joget DX Enterprise installation.

> **Note:** Both plugins are early (alpha) builds. Agent Builder 8.2.5-ALPHA is an early build, two releases ahead of the current stable release. Its features are subject to change and can be unstable. Use both plugins on development and test servers only, not in production. Features marked **Preview** in this manual are switched off by default or are the least mature parts.

## The three products in one minute

**Agent Builder** is a visual builder inside the Joget App Composer. You draw an agent as a flow of tasks. Each task has a prompt, a large language model (LLM), and optional tools, enhancers, decisions and guards. You then run the agent from a process, a form, a list, a chat page or your own plugin. **AI Central Config** is the one place where an administrator stores the LLM credentials, the AI services, skills, tool templates and voice settings that agents and composers use.

**AI Composers** are chat panels inside the Joget builders. You describe what you want in plain language and the composer changes the form, list, userview, process or theme for you. The **Orchestrator** does the same for a whole app: it plans the app and asks each composer to build its part. The composers run inside Joget and use the AI service that you configured in AI Central Config.

**Composer MCP** turns your Joget server into an MCP server. The model is not inside Joget: it is the AI client on the developer's machine. Joget only supplies tools (create an app, create a form, lint, run a process and so on) and a library of skills that teach the client how to build good Joget apps.

```text
                       +------------------------------------------+
  Browser (builders) --|  Joget DX Enterprise                     |
                       |                                          |
                       |  Agent Builder plugin                    |--> LLM provider
                       |    AI Central Config                     |    (your AI service)
                       |    Agents, AI Composers, Orchestrator    |
                       |                                          |
  AI client (MCP) -----|  Composer MCP plugin                     |
  on your machine      |    builder tools + skills library        |
                       +------------------------------------------+
```

## How to read this manual

| If you want to | Read |
|---|---|
| Install the plugins and make the first agent run | [Installing and Setting Up Your Joget Instance](02-installation-and-setup.md), then [AI Central Config Reference](03-ai-central-config.md) |
| Understand and build agents | [Agent Builder Concepts and How an Agent Runs](04-agent-builder-concepts.md), [Building Agents Step by Step](05-building-agents-tutorials.md) |
| Look up a block and its properties | [Building Block Reference: Tools](06-reference-tools.md), [Building Block Reference: Tools (continued)](06b-reference-tools-continued.md), [Building Block Reference: Prompts, LLMs, Enhancers, Decisions and Guards](07-reference-other-blocks.md) |
| Use an agent in a process, form, list or chat page | [Using Agents Inside Joget Apps](08-using-agents-in-apps.md) |
| Run agents in production | [Runtime Settings, Monitoring and Cost](09-runtime-and-monitoring.md), [Evaluating Agents](10-evaluating-agents.md), [Security and Governance](11-security-and-governance.md) |
| Build forms, lists, userviews, processes and themes by chat | [AI Composers User Guide](12-composers-user-guide.md), [Writing Good Composer Prompts](14-composer-prompt-guide.md) |
| Build a whole app from one request | [The Orchestrator: Building Whole Apps](13-orchestrator.md) |
| Know what happens behind a composer message | [How the Composers Work](15-how-composers-work.md) |
| Use dictation or add the assistant to a userview | [Voice Input and the Joget AI Assistant](16-voice-and-assistant.md) |
| Build Joget apps from Claude Code or another MCP client | [Composer MCP: Overview and How It Works](17-composer-mcp-overview.md), [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md), [Composer MCP: Tool Reference](19-composer-mcp-tools.md), [Composer MCP: Tool Reference (continued)](19b-composer-mcp-tools-continued.md), [Composer MCP: Skills, Prompts and the App Generator](20-composer-mcp-skills-and-prompts.md), [Composer MCP: Security, Clustering and Troubleshooting](21-composer-mcp-security-and-operations.md) |
| Teach the AI your company standards without code | [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md) |
| Write your own plugins | [Extending Agent Builder: Overview and Development Setup](23-extending-overview.md), [Writing Custom Tools](24-custom-tools.md), [Writing Custom Guards, Enhancers, Decisions and Prompts](25-custom-guards-enhancers-decisions-prompts.md), [Adding a Custom LLM Provider](26-custom-llm-providers.md), [Adding a Custom LLM Provider (continued)](26b-custom-llm-providers-continued.md), [System Agents](27-system-agents.md), [System Agents (continued)](27b-system-agents-continued.md), [Custom Composer Tools and Building Your Own Composer](28-custom-composer-tools-and-composers.md), [Custom Composer Tools and Building Your Own Composer (continued)](28b-custom-composer-tools-and-composers-continued.md) |
| Build and run the samples | [The Examples Repository](29-examples-repository.md) |
| Fix a problem or find a setting | [Troubleshooting](30-troubleshooting.md), [Frequently Asked Questions](31-faq.md), [Glossary and Reference Tables](32-glossary-and-reference.md) |

## Suggested first day

1. Install the Agent Builder plugin and add one AI credential and one AI service. Follow [Installing and Setting Up Your Joget Instance](02-installation-and-setup.md).
2. Build the first agent in [Building Agents Step by Step](05-building-agents-tutorials.md) and run it.
3. Open a form in the Form Builder and send one message to the composer. Follow [AI Composers User Guide](12-composers-user-guide.md).
4. Install Composer MCP, connect your AI client and run the smoke test. Follow [Composer MCP: Install, Configure and Connect](18-composer-mcp-setup.md).
5. Install the Agent Builder API jar from the examples repository and build the first sample. Follow [The Examples Repository](29-examples-repository.md).

## Conventions used in this manual

- **Bold** text is something you see on the screen: a menu, a button, a field label.
- `Monospace` text is something you type or a name in code: an id, a file name, a property key.
- Menu paths are written with `>`, for example **Settings > Manage Plugins**.
- `<JOGET_HOME>` is the folder where Joget is installed. `http://localhost:8080/jw` stands for the address of your Joget server.
- A **Note** gives extra information. A **Limitations and things to know** section lists behaviour that you must plan for.
- **Preview** marks a feature that is not yet mature.

## Where to go next

- [Installing and Setting Up Your Joget Instance](02-installation-and-setup.md)
- [AI Central Config Reference](03-ai-central-config.md)
- [Agent Builder Concepts and How an Agent Runs](04-agent-builder-concepts.md)
- [Composer MCP: Overview and How It Works](17-composer-mcp-overview.md)
