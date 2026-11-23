# 06-custom-llm-provider: a provider for a private model gateway

One small plugin jar (`private-gateway-llm`, about 20 KB) that connects Agent Builder to a language model that is
not a built-in provider: a model you host yourself, or one behind a company gateway. The example gateway speaks the
OpenAI chat completions protocol, needs a tenant header and may need no key. The provider name is `PrivateGateway`.

Use the built-in **Custom** Model Class first: it needs no code. Write a provider like this one only when the
gateway needs an extra HTTP header, rejects a field of the standard request, answers in a slightly different shape,
or when you want your own form fields, your own tuning parameter or your own name in the AI Usage Dashboard.

## What the sample shows

| Class | Extension point | What it adds |
|---|---|---|
| `GatewayProvider` | `LLMProviderAbstract` (provider) | `PrivateGateway` in **Model Class** of **Settings > AI Central Config > AI Services**, with model suggestions and five credential fields |
| `GatewayLLMConfig` | `AbstractLLMConfig` | The view of one saved service: turns the saved fields into the URL, the key and the model of a call |
| `GatewayAgentLLM` | `AgentLLMAbstract` (LLM plugin) | Builds the request, sends it and reads the reply. It is also the element **Private Gateway LLM** in the **LLM Services** section of the Agent Builder palette |
| `GatewayCapabilityProvider` | `LLMModelCapabilityProvider` | States which parameters each model accepts and declares one extra parameter, **Repetition Penalty**. It is registered with `LLMModelCapabilityRegistry.register`, not as a plugin |
| `GatewayNames` | none | The names that must be identical in all of the above |
| `Activator`, `Registrar` | none | Start the bundle and register the provider, the LLM plugin and the parameter rules |

What you learn from the code:

1. A provider that composers, the Orchestrator and system agents can use needs two plugins with the same name: the
   provider (the form and the saved service) and the LLM plugin (the call). The sample registers the LLM plugin in
   the `AgentComponent` registry so that the right class is found.
2. Do not name a credential field `apiKey`. The AI Service form has a field of its own with that name. The sample
   uses `gatewayKey` and `gatewayUrl`, and `GatewayLLMConfig` passes them on as the key and the URL of the call.
3. The LLM plugin sends the request with the HTTP client of the JDK. It does not override `prepareRequestHeader` or
   `parseResponse` of `AgentLLMAbstract`: those methods name Apache HttpClient classes, Agent Builder does not export
   that library, and a class in another bundle that names them fails with `NoClassDefFoundError` at the first call.
4. Composers always ask for 16000 output tokens. The plugin limits the request to **Max Output Tokens Allowed**.
5. One `LLMParameter` gives a typed field in **Advanced Configuration**, shown only for the models that accept it.

## Files

| Path | Content |
|---|---|
| `pom.xml` | Build file. `joget.version` (default `9.1.2`) and `agent-builder.version` (`8.2.5-ALPHA`) are properties. |
| `src/main/java/org/joget/samples/llm/Activator.java` | Bundle entry point. Checks that Agent Builder is present, then calls `Registrar`. |
| `.../Registrar.java` | Registers the provider and the LLM plugin as OSGi services under their class names, the LLM class in `AgentComponent`, and the parameter rules in the capability registry. |
| `.../GatewayNames.java` | Provider name, default URL and model, field names. |
| `.../GatewayProvider.java` | The provider. |
| `.../GatewayLLMConfig.java` | The view of a saved service. |
| `.../GatewayCapabilityProvider.java` | The parameter rules. |
| `.../GatewayAgentLLM.java` | The LLM plugin: identity, request, transport, reply. |
| `src/test/java/.../GatewayAgentLLMTest.java` | 2 unit tests. A small HTTP server inside the test stands in for the gateway. They run without Joget and without a model. |

## Prerequisites

- [00-setup](../00-setup/README.md) is done (API jar and Joget platform jars in your local Maven repository).
- Joget DX Enterprise with Agent Builder installed.
- To send a real message: a server that implements `POST <base URL>/chat/completions` in the OpenAI format and that
  the Joget server can reach. For the AI Composers the model must support tool calling.

## Build

```bash
mvn clean package "-Djoget.version=9.1.2"
```

Use the Joget version that `00-setup/install-joget-jars` printed. Expected result: `Tests run: 2, Failures: 0,
Errors: 0, Skipped: 0`, `BUILD SUCCESS`, and the file `target/private-gateway-llm-1.0-SNAPSHOT.jar`. The test log
contains the warning `AgentLLMRegistry: no DAO wired, skipping token tracking`. It is expected: the test has no
database.

## Install

1. Open **Settings > Manage Plugins**, click **Upload**, choose `target/private-gateway-llm-1.0-SNAPSHOT.jar` in the **Upload Plugin** dialog and
   click **Upload**.
2. The list now contains **Private Gateway LLM Provider** (file `private-gateway-llm-1.0-SNAPSHOT.jar`). It is also
   listed when you select **LLM Provider Plugin** or **AI Agent LLM** in **Filter by Type**. The Joget log shows
   `Private Gateway LLM provider registered: 2 plugins` and `Bundle org.joget.samples.private-gateway-llm started`.

Optional script: `../00-setup/deploy-plugin.ps1 -Jar target\private-gateway-llm-1.0-SNAPSHOT.jar` (or `deploy-plugin.sh`).

## Configure

### An AI Service (for composers, the Orchestrator and system agents)

1. Open **Settings > AI Central Config**, tab **AI Services**, and click **Add AI Service**. Reload the page first if
   it was open while you installed the jar.
2. Step 1: keep **Use Saved Credential (Optional)** at `-- None (Manual Setup) --`, select **Model Class**
   **PrivateGateway**, click **Next**.
3. Step 2, section **Credential Configuration**:

   | Field | What it does | Default |
   |---|---|---|
   | **Model Name** | The model id sent in the request. Pick a suggestion (`private-chat`, `private-chat-large`, `private-lite-8b`) or type any id. | `private-chat` |
   | **Gateway Base URL** | The address of the gateway, for example `https://llm.internal.example/v1`. `/chat/completions` is appended unless the URL already ends with `/completions`. | empty: `http://localhost:8000/v1` is used |
   | **Gateway API Key** | Sent as `Authorization: Bearer <key>`. Leave it empty when the gateway needs no key: then no `Authorization` header is sent. | empty |
   | **Tenant ID** | Sent as the header `X-Tenant-Id`. Empty: no header. | empty |
   | **Max Output Tokens Allowed** | The largest `max_tokens` the plugin sends. Composers ask for 16000; a smaller model needs a smaller number, for example `4096`. | empty: no limit |
   | **Read Timeout (seconds)** | How long to wait for the reply. | empty: 120 |

   Open **Advanced Configuration** for **Temperature** (default `0.2`), **Max Tokens** (default `16000`), **Top P**
   and **Repetition Penalty**. For a model whose id contains `lite` or `8b`, **Top P** and **Repetition Penalty**
   are not shown.
4. Click **Next**, enter an **AI Service Name**, click **Add**.
5. To use it by default, select it in **Active AI Service** and click **Save**. To use it for one chat, select it in
   the AI service list at the bottom of the composer chat panel.

### The palette element (for agents on the canvas)

Open an agent in the Agent Builder. In the palette section **LLM Services**, drag **Private Gateway LLM** into the
box **Define Agent** (onto **Add a LLM service to here**, or onto the **LLM SERVICE** card when the agent already
has one). Its properties are **API Key**, **Tenant ID (X-Tenant-Id header)**, **Model**, **API URL**,
**Max Tokens**, **Temperature**, **Top P**, **Repetition Penalty**, **Read Timeout (seconds)** and
**Gateway supports response_format**. A canvas agent does not use the AI Service; it uses these properties.

## Try it

1. In a composer: open a form in the Form Builder, open the composer chat panel (Ctrl+J), select your service in
   the AI service list, and send a small request, for example `Create a form with a name field and a date field.`
2. On the canvas: create an agent with one task, a **Text Prompt** and the **Private Gateway LLM** element, and send
   a message in the **Preview** tab.
3. Open **Monitor > Agent Execution Audit Trail**, open the run and its **LLM** node. It shows the request body that was
   sent.
4. Open **Monitor > AI Usage Dashboard**. The tab **By Model Class** is shown first.

## Expected result

- The gateway receives `POST <base URL>/chat/completions` with the headers `Authorization: Bearer <key>` (only when a
  key is set) and `X-Tenant-Id: <tenant>` (only when a tenant is set).
- The request body of a composer call contains `model`, `messages`, `tools`, `tool_choice`, `parallel_tool_calls`,
  `temperature` and `max_tokens`. With **Max Output Tokens Allowed** `4096`, `max_tokens` is `4096`, not `16000`.
- The request body of a canvas agent without tools contains no `parallel_tool_calls`. With **Top P** `0.9` and
  **Repetition Penalty** `1.1` on a model that is not `lite` or `8b`, it contains `top_p` and `repetition_penalty`.
- No key of the body starts with an underscore.
- The answer of the model appears in the chat. Text between `<think>` and `</think>` is removed.
- The AI Usage Dashboard shows the prompt tokens and the completion tokens of the Model Class `PrivateGateway` (in
  capital letters), and the list **LLM Calls** below has one row per call. The tokens are above zero when the gateway
  returns a `usage` object.

## How to adapt it

- **Your own name and defaults:** change `GatewayNames`. Keep one constant for the provider name: the provider, the
  capability provider, the `AgentComponent` entry and `getCapabilityProviderName()` must all return the same text.
- **Your own headers or authorization:** change `GatewayAgentLLM.addHeaders`. Add a field to
  `GatewayProvider.getPropertyOptions()` for each value an administrator must enter; the plugin reads it with
  `getPropertyString("<field name>")`.
- **A field the gateway rejects:** remove it in `presetConfig` or in `preparePayload`.
- **A reply in another shape:** change `GatewayAgentLLM.readReply`.
- **An API that is not shaped like the OpenAI API:** also override `convertMessages`, `convertFunctions` and
  `extractTokenUsage`.
- **Your own models and parameters:** change `GatewayProvider.getModelNames()` and `GatewayCapabilityProvider`.
- **Rename:** change `artifactId`, `Bundle-SymbolicName`, `Private-Package`, `Bundle-Activator`, `Joget-Name` in
  `pom.xml`, and the Java package. Saved services store the provider name and saved agents store the class name of
  the LLM element, so do not change them after they are in use.

## Limitations and things to know

- The sample does not stream tokens. A streaming run receives the whole answer in one piece.
- The transport is the HTTP client of the JDK (`java.net.http`), with the retry rules of the built-in providers:
  HTTP 429 and 5xx and network errors are retried up to the number of attempts of the run (3 for composers and
  system agents), a clear error answer of the gateway is not.
- There is no credential type in the sample, so **Use Saved Credential (Optional)** offers no `PrivateGateway`
  credential. Enter the URL and the key in the service.
- `AgentComponent` has no way to remove an entry. After you remove the jar, the entry stays until Joget restarts.
- The **Gateway Base URL** is entered by an administrator and is called from the Joget server. Limit who has the
  Admin role.
- No list of models that work with the AI Composers exists. A composer needs reliable tool calling, long
  instructions and long answers. Test each model.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `PrivateGateway` is not in **Model Class** | Agent Builder was not running when this bundle started (the log shows `Agent Builder is not available, nothing registered`), or the page was open during the install | Install Agent Builder, upload this jar again, reload the page |
| The form shows two fields **API Key** | A provider field is named `apiKey` | Use another field name, as the sample does |
| `NoClassDefFoundError: org/apache/http/client/methods/HttpRequestBase` in the Joget log at the first call | Your LLM plugin overrides `prepareRequestHeader` or `parseResponse`, or names another Apache HttpClient class | Send the request yourself, as `GatewayAgentLLM.getResponse` does |
| `No plugin found for provider: PrivateGateway` in a composer | The LLM plugin is not registered, or the `AgentComponent` entry is missing | Check `Registrar` |
| `API Error: the gateway call failed after 3 attempt(s): ...` | The Joget server cannot reach the gateway, or the read timeout is too short | Check **Gateway Base URL** from the Joget server; set **Read Timeout (seconds)** |
| HTTP 400 "unknown field" from the gateway | The gateway rejects a field of the body | Open the **LLM** node of the run, read the body, remove the field in `presetConfig` |
| **Repetition Penalty** is not shown | The model id contains `lite` or `8b`, or the list of parameter rules is still cached (30 seconds) | Check `GatewayCapabilityProvider.getCapabilities`; wait and reload |
| Tokens are zero in the AI Usage Dashboard | The gateway returns no `usage` object | Return `usage.prompt_tokens` and `usage.completion_tokens` |
| A composer answers in text and builds nothing | The model does not call tools | Use a model with tool calling |
