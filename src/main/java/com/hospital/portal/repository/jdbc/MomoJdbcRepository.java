package com.hospital.portal.repository.jdbc;

import com.hospital.portal.domain.model.MomoPayment;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes openclinic.OC_MOMO — reusing the exact schema OpenClinic already maintains.
 */
@Repository
public class MomoJdbcRepository {

    private final JdbcTemplate jdbc;

    public MomoJdbcRepository(@Qualifier("openclinicJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ─── Insert ──────────────────────────────────────────────────────────────

    public void insertPaymentRequest(String transactionId, String invoiceUid, String patientUid,
                                     double amount, String currency, String payerPhone,
                                     String payerMessage, String payeeMessage,
                                     String userId, String operator) {
        jdbc.update(
            "INSERT INTO OC_MOMO (" +
            "  OC_MOMO_TRANSACTIONID, OC_MOMO_CREATEDATETIME, OC_MOMO_INVOICEUID, " +
            "  OC_MOMO_PATIENTUID, OC_MOMO_UPDATEUID, OC_MOMO_AMOUNT, OC_MOMO_CURRENCY, " +
            "  OC_MOMO_PAYERPHONE, OC_MOMO_STATUS, OC_MOMO_OPERATOR, " +
            "  OC_MOMO_PAYERMESSAGE, OC_MOMO_PAYEEMESSAGE, OC_MOMO_UPDATETIME" +
            ") VALUES (?,NOW(),?,?,?,?,?,?,'PENDING',?,?,?,NOW())",
            transactionId, invoiceUid, patientUid, userId,
            amount, currency, payerPhone, operator, payerMessage, payeeMessage);
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    public void updateStatus(String transactionId, String status, String financialTransactionId) {
        jdbc.update(
            "UPDATE OC_MOMO SET OC_MOMO_STATUS = ?, OC_MOMO_FINANCIALTRANSACTIONID = ?, " +
            "OC_MOMO_UPDATETIME = NOW() WHERE OC_MOMO_TRANSACTIONID = ?",
            status, financialTransactionId, transactionId);
    }

    public void updateCreditLinks(String financialTransactionId,
                                  String patientCreditUid, String wicketCreditUid) {
        jdbc.update(
            "UPDATE OC_MOMO SET OC_MOMO_PATIENTCREDITUID = ?, OC_MOMO_WICKETCREDITUID = ?, " +
            "OC_MOMO_UPDATETIME = NOW() WHERE OC_MOMO_FINANCIALTRANSACTIONID = ?",
            patientCreditUid, wicketCreditUid, financialTransactionId);
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    public Optional<MomoPayment> findByTransactionId(String transactionId) {
        try {
            MomoPayment p = jdbc.queryForObject(
                "SELECT * FROM OC_MOMO WHERE OC_MOMO_TRANSACTIONID = ?", rowMapper(), transactionId);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<MomoPayment> findByPatientUid(String patientUid) {
        return jdbc.query(
            "SELECT * FROM OC_MOMO WHERE OC_MOMO_PATIENTUID = ? ORDER BY OC_MOMO_CREATEDATETIME DESC",
            rowMapper(), patientUid);
    }

    public List<MomoPayment> findByInvoiceUid(String invoiceUid) {
        return jdbc.query(
            "SELECT * FROM OC_MOMO WHERE OC_MOMO_INVOICEUID = ? ORDER BY OC_MOMO_CREATEDATETIME DESC",
            rowMapper(), invoiceUid);
    }

    public List<MomoPayment> findPendingOlderThan(LocalDateTime cutoff) {
        return jdbc.query(
            "SELECT * FROM OC_MOMO WHERE OC_MOMO_STATUS = 'PENDING' AND OC_MOMO_CREATEDATETIME < ?",
            rowMapper(), Timestamp.valueOf(cutoff));
    }

    /**
     * Idempotency guard: was a PENDING or SUBMITTED payment already initiated for this
     * invoice in the last {@code windowMinutes} minutes from this phone?
     */
    public Optional<MomoPayment> findRecentPendingForInvoice(String invoiceUid,
                                                              String payerPhone,
                                                              int windowMinutes) {
        String sql =
            "SELECT * FROM OC_MOMO " +
            "WHERE OC_MOMO_INVOICEUID = ? " +
            "  AND OC_MOMO_PAYERPHONE = ? " +
            "  AND OC_MOMO_STATUS IN ('PENDING','SUBMITTED') " +
            "  AND OC_MOMO_PATIENTCREDITUID IS NULL " +
            "  AND OC_MOMO_CREATEDATETIME >= DATE_SUB(NOW(), INTERVAL ? MINUTE) " +
            "LIMIT 1";
        List<MomoPayment> results = jdbc.query(sql, rowMapper(), invoiceUid, payerPhone, windowMinutes);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public boolean existsByTransactionId(String transactionId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM OC_MOMO WHERE OC_MOMO_TRANSACTIONID = ?",
            Integer.class, transactionId);
        return count != null && count > 0;
    }

    // ─── RowMapper ───────────────────────────────────────────────────────────

    private RowMapper<MomoPayment> rowMapper() {
        return (rs, rowNum) -> {
            MomoPayment m = new MomoPayment();
            m.setTransactionId(rs.getString("OC_MOMO_TRANSACTIONID"));
            m.setFinancialTransactionId(rs.getString("OC_MOMO_FINANCIALTRANSACTIONID"));
            m.setInvoiceUid(rs.getString("OC_MOMO_INVOICEUID"));
            m.setPatientUid(rs.getString("OC_MOMO_PATIENTUID"));
            m.setAmount(BigDecimal.valueOf(rs.getDouble("OC_MOMO_AMOUNT")));
            m.setCurrency(rs.getString("OC_MOMO_CURRENCY"));
            m.setPayerPhone(rs.getString("OC_MOMO_PAYERPHONE"));
            m.setStatus(rs.getString("OC_MOMO_STATUS"));
            m.setOperator(rs.getString("OC_MOMO_OPERATOR"));
            m.setPayerMessage(rs.getString("OC_MOMO_PAYERMESSAGE"));
            m.setPayeeMessage(rs.getString("OC_MOMO_PAYEEMESSAGE"));
            m.setPatientCreditUid(rs.getString("OC_MOMO_PATIENTCREDITUID"));
            m.setWicketCreditUid(rs.getString("OC_MOMO_WICKETCREDITUID"));
            m.setCreatedAt(toLocalDateTime(rs.getTimestamp("OC_MOMO_CREATEDATETIME")));
            m.setUpdatedAt(toLocalDateTime(rs.getTimestamp("OC_MOMO_UPDATETIME")));
            return m;
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
