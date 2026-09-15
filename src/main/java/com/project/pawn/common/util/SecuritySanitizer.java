package com.project.pawn.common.util;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SecuritySanitizer {

    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        String cleanValue = input.replaceAll("\\p{C}", "");
        String sanitized = StringEscapeUtils.escapeHtml4(cleanValue);
        sanitized = StringEscapeUtils.escapeEcmaScript(sanitized);
        return sanitized;
    }

    /**
     * Sanitize all String values in a Map (keys are left unchanged).
     */
    public static Map<String, String> sanitizeMap(Map<String, String> map) {
        if (map == null) return null;
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.put(entry.getKey(), sanitizeInput(entry.getValue()));
        }
        return result;
    }
}
