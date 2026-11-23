package org.joget.samples.agenttools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.joget.apps.app.service.AppUtil;

/** Shared constants and helpers. Touches no Agent Builder class. */
final class Ext {

    static final String MESSAGES = "messages/idTools";
    static final String VERSION = readVersion();

    private Ext() {
    }

    /** Loads /properties/NAME.json from this jar and resolves its @@key@@ labels from messages/idTools.properties. */
    static String propertyOptions(String className, String name) {
        return AppUtil.readPluginResource(className, "/properties/" + name + ".json", null, true, MESSAGES);
    }

    private static String readVersion() {
        try (InputStream in = Ext.class.getResourceAsStream("/" + MESSAGES + ".properties")) {
            Properties p = new Properties();
            if (in != null) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            String v = p.getProperty("ext.version", "").trim();
            return v.isEmpty() || v.startsWith("${") ? "unknown" : v;
        } catch (IOException e) {
            return "unknown";
        }
    }
}
