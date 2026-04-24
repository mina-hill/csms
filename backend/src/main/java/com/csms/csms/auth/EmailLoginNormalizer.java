package com.csms.csms.auth;

import org.springframework.util.StringUtils;

import java.text.Normalizer;

/**
 * Normalizes the email string before DB lookup: Unicode NFKC, trim, strip NBSP.
 * Does not change meaning of a valid single email address; avoids mismatch when DB
 * was seeded with different invisible Unicode or casing.
 */
public final class EmailLoginNormalizer {

    private EmailLoginNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        // NFKC so fullwidth "＠" and similar become ASCII where applicable
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim();
        // non-breaking space often pasted from web/docs
        s = s.replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ');
        s = s.trim();
        if (!StringUtils.hasText(s)) {
            return "";
        }
        return s;
    }
}
