package com.ordermgmt.railway.domain.pathmanager.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Generates TTT PlannedTransportIdentifiers (TRID, PAID, PRID, ROID).
 *
 * <p>Format: company (4 chars, SBB = "0085"), core (8-char short hash), variant ("01"+).
 */
@Service
public class IdentifierGenerator {

    private static final String SBB_COMPANY_CODE = "0085";
    private static final int CORE_LENGTH = 8;

    public String company() {
        return SBB_COMPANY_CODE;
    }

    /** Generates a unique core value derived from a UUID. */
    public String generateCore() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return raw.substring(0, CORE_LENGTH).toUpperCase();
    }

    /** Returns the initial variant code. */
    public String initialVariant() {
        return "01";
    }

    /**
     * Increments a two-digit variant code (e.g., "01" -> "02").
     *
     * @param currentVariant the current variant code
     * @return the next variant code
     */
    public String nextVariant(String currentVariant) {
        int current = Integer.parseInt(currentVariant);
        return String.format("%02d", current + 1);
    }
}
