package com.hospital.portal.repository.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Writes payment credits back into openclinic.OC_PATIENTCREDITS so that
 * OpenClinic's own cashier and finance screens reflect PawaPay payments.
 *
 * Called once per payment, only when PawaPay status transitions to SUCCESSFUL.
 */
@Repository
public class PatientCreditJdbcRepository {

    private static final Logger log = LoggerFactory.getLogger(PatientCreditJdbcRepository.class);

    private final JdbcTemplate jdbc;

    @Value("${app.openclinic.server-id:1}")
    private int serverId;

    public PatientCreditJdbcRepository(@Qualifier("openclinicJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Inserts a credit row into OC_PATIENTCREDITS.
     * Uses SELECT MAX(objectId)+1 within the same server to assign the next objectId.
     *
     * @return the new composite UID  "serverId.objectId", or null if the insert failed
     */
    public String insertCredit(String invoiceUid, BigDecimal amount, String currency,
                               String comment, LocalDateTime paymentDate) {
        try {
            // Derive next objectId for this serverId — safe enough for the portal's write rate
            Integer maxObjectId = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(OC_PATIENTCREDIT_OBJECTID), 0) FROM OC_PATIENTCREDITS " +
                    "WHERE OC_PATIENTCREDIT_SERVERID = ?",
                    Integer.class, serverId);
            int nextObjectId = (maxObjectId != null ? maxObjectId : 0) + 1;

            jdbc.update(
                "INSERT INTO OC_PATIENTCREDITS (" +
                "  OC_PATIENTCREDIT_SERVERID, OC_PATIENTCREDIT_OBJECTID, " +
                "  OC_PATIENTCREDIT_DATE, OC_PATIENTCREDIT_INVOICEUID, " +
                "  OC_PATIENTCREDIT_AMOUNT, OC_PATIENTCREDIT_TYPE, " +
                "  OC_PATIENTCREDIT_CATEGORY, OC_PATIENTCREDIT_COMMENT, " +
                "  OC_PATIENTCREDIT_CURRENCY" +
                ") VALUES (?, ?, ?, ?, ?, 'PAYMENT', 'PAWAPAY', ?, ?)",
                serverId, nextObjectId,
                paymentDate != null ? paymentDate : LocalDateTime.now(),
                invoiceUid,
                amount,
                comment,
                currency != null ? currency : "RWF");

            String uid = serverId + "." + nextObjectId;
            log.info("OC_PATIENTCREDITS written: uid={} invoiceUid={} amount={}", uid, invoiceUid, amount);
            return uid;

        } catch (Exception ex) {
            log.error("Failed to write OC_PATIENTCREDITS for invoiceUid={}: {}", invoiceUid, ex.getMessage(), ex);
            return null;
        }
    }

    /** Returns true if a credit already exists linked to this portal transaction (via comment). */
    public boolean existsForTransaction(String transactionId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM OC_PATIENTCREDITS WHERE OC_PATIENTCREDIT_COMMENT LIKE ?",
                Integer.class, "%" + transactionId + "%");
        return count != null && count > 0;
    }

    /**
     * Reduces OC_PATIENTINVOICE_BALANCE by the credit amount, and closes the invoice
     * when the balance reaches zero. Called after a successful PawaPay payment.
     */
    public boolean applyCreditToInvoice(String invoiceUid, BigDecimal amount) {
        if (invoiceUid == null || !invoiceUid.contains(".")) {
            log.error("Invalid invoiceUid format: {}", invoiceUid);
            return false;
        }
        String[] parts = invoiceUid.split("\\.");
        int invServerId = Integer.parseInt(parts[0]);
        int invObjectId = Integer.parseInt(parts[1]);

        int updated = jdbc.update(
            "UPDATE OC_PATIENTINVOICES " +
            "SET OC_PATIENTINVOICE_BALANCE = GREATEST(0, OC_PATIENTINVOICE_BALANCE - ?), " +
            "    OC_PATIENTINVOICE_STATUS = CASE " +
            "        WHEN OC_PATIENTINVOICE_BALANCE - ? <= 0 THEN 'closed' " +
            "        ELSE OC_PATIENTINVOICE_STATUS END, " +
            "    OC_PATIENTINVOICE_UPDATETIME = NOW() " +
            "WHERE OC_PATIENTINVOICE_SERVERID = ? AND OC_PATIENTINVOICE_OBJECTID = ?",
            amount, amount, invServerId, invObjectId);
        log.info("OC_PATIENTINVOICES updated: invoiceUid={} amount={} rows={}",
                invoiceUid, amount, updated);
        return updated > 0;
    }
}
