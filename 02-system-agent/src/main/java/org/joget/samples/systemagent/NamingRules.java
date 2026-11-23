package org.joget.samples.systemagent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The deterministic part of the audit: what counts as a good form / list id. The model never decides this, it only
 * explains the findings. The rule below is a stand-in for a customer convention: replace it with the real standard.
 *
 * Rule: lower snake_case, starts with a letter, at most 40 characters, no double underscore.
 */
final class NamingRules {

    static final int MAX_LENGTH = 40;
    private static final Pattern SNAKE = Pattern.compile("^[a-z][a-z0-9_]*$");

    private NamingRules() {
    }

    /** Problems found in one id; empty when the id follows the convention. */
    static List<String> problems(String id) {
        List<String> out = new ArrayList<>();
        if (id == null || id.isEmpty()) {
            out.add("id is empty");
            return out;
        }
        if (!SNAKE.matcher(id).matches()) {
            out.add("not lower snake_case (letters a-z, digits and underscores, starting with a letter)");
        }
        if (id.length() > MAX_LENGTH) {
            out.add("longer than " + MAX_LENGTH + " characters");
        }
        if (id.contains("__")) {
            out.add("contains a double underscore");
        }
        return out;
    }

    /** employeeLeaveRequest, Employee-Leave Request and employee_leave_request all become employee_leave_request. */
    static String suggest(String id) {
        if (id == null) {
            return "";
        }
        String s = id.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (s.isEmpty() || !Character.isLetter(s.charAt(0))) {
            s = "f_" + s;
        }
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH).replaceAll("_+$", "") : s;
    }
}
