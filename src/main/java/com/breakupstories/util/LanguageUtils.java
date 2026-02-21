package com.breakupstories.util;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class LanguageUtils {
    private static final Map<String, String> NAME_TO_CODE = new HashMap<>();

    static {
        // South Indian
        NAME_TO_CODE.put("telugu", "te");
        NAME_TO_CODE.put("tamil", "ta");
        NAME_TO_CODE.put("kannada", "kn");
        NAME_TO_CODE.put("malayalam", "ml");
        // North / West Indian
        NAME_TO_CODE.put("hindi", "hi");
        NAME_TO_CODE.put("marathi", "mr");
        NAME_TO_CODE.put("gujarati", "gu");
        NAME_TO_CODE.put("punjabi", "pa");
        NAME_TO_CODE.put("bengali", "bn");
        NAME_TO_CODE.put("urdu", "ur");
        // East Indian
        NAME_TO_CODE.put("odia", "or");
        NAME_TO_CODE.put("odiya", "or");   // common alternate spelling
        NAME_TO_CODE.put("oriya", "or");   // legacy spelling still in use
        // International
        NAME_TO_CODE.put("english", "en");
    }

    public static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }

        // Handle locales like "te-IN" or "telugu_IN" by taking the part before - or _
        String cleanLang = language.trim().toLowerCase().split("[-_]")[0];

        // If it's already a 2-char code, return it
        if (cleanLang.length() <= 2) {
            return cleanLang;
        }

        // Try to map from name to code
        return NAME_TO_CODE.getOrDefault(cleanLang, cleanLang);
    }

    public static List<String> getLanguageVariants(String language) {
        String normalized = normalizeLanguage(language);
        if (normalized == null) {
            return Collections.emptyList();
        }

        List<String> variants = new ArrayList<>();
        variants.add(normalized);

        // Add corresponding full name if exists
        for (Map.Entry<String, String> entry : NAME_TO_CODE.entrySet()) {
            if (entry.getValue().equals(normalized)) {
                variants.add(entry.getKey());
            }
        }
        return variants;
    }
}
