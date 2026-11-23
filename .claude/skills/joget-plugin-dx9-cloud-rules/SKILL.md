---
name: joget-plugin-dx9-cloud-rules
description: Hard rules for writing Joget plugins (including Agent Builder extensions) that must run on Joget DX 9, in a cluster and on Joget Cloud. Use before writing or reviewing plugin code that touches the database, threads, files or state.
---

# Plugin rules: DX 9, cluster, Cloud

## Build against the right API jar
Agent Builder extensions compile against the `agent-builder-api` jar in your local Maven repository
(`00-setup/install-api-jar`). When you receive a newer Agent Builder, install its API jar before you package your
plugin. A jar built against an older API shows up at load time as `NoSuchMethodError` or a missing property, not at
compile time.

## javax and jakarta
- Joget DX 9 uses `jakarta.servlet`; older plugin code uses `javax.servlet`.
- A plugin compiled for `javax.servlet` runs on DX 9 when its manifest names `javax.servlet` in `Import-Package`
  (with `version="0"`): DX 9 then converts the jar to jakarta at install.

## Hibernate
- Do not call `Session.save`, `persist` and similar directly, and do not import `org.hibernate.Query`.
- Use Joget's `AbstractSpringDao` helpers (for example `saveOrUpdate(entityName, obj)`) and
  `org.hibernate.query.Query`, as the Joget DAOs do.
- One bad import can stop the whole plugin's Spring context, which looks like "the plugin did not load".

## Clustered Joget and Joget Cloud
Assume both.
- No state in local files or in static fields that another node cannot see. Keep conversations, runs and answers in
  database tables so any node can serve the next request.
- The Joget Cloud security manager refuses: `setAccessible` (so Gson or Jackson binding to your own classes),
  `new Thread(...)`, files outside `SetupManager.getBaseDirectory()`, `ProcessBuilder`, `System.setProperty`. Use
  `org.json.JSONObject` and the platform's executors instead.
- Use your own package root; never put classes in `org.joget.ai` packages.
- Use the class literal in `getClassName()`; a proxy class name breaks the plugin lookup.

## Plugin manifest (OSGi)
- Compile Joget and Agent Builder dependencies with scope `provided`. Never embed the API jar: a second copy of a
  base class makes Agent Builder reject your plugin as the wrong type.
- Keep the Activator free of Agent Builder types; check that Agent Builder is present with
  `Class.forName(marker, false, loader)` and catch `LinkageError`, so your bundle still starts without it.

## Edition
Agent Builder is a Joget DX Enterprise feature.
