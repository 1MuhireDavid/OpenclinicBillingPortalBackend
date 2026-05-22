package com.hospital.portal.repository.jdbc;

import com.hospital.portal.domain.model.InvoiceLineItem;
import com.hospital.portal.domain.model.PatientBillSummary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
 * Reads from openclinic.OC_PATIENTINVOICES.
 */
@Repository
public class InvoiceJdbcRepository {

    private final JdbcTemplate jdbc;

    @Value("${app.openclinic.server-id:1}")
    private int serverId;

    public InvoiceJdbcRepository(@Qualifier("openclinicJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** HMS stores plain personId; portal uses {serverId}.{personId}. Strip prefix for queries. */
    private String toHmsUid(String portalUid) {
        return portalUid != null && portalUid.contains(".") ? portalUid.split("\\.")[1] : portalUid;
    }

    private static final String BASE_SELECT =
        "SELECT OC_PATIENTINVOICE_SERVERID, OC_PATIENTINVOICE_OBJECTID, " +
        "       OC_PATIENTINVOICE_ID, OC_PATIENTINVOICE_PATIENTUID, " +
        "       OC_PATIENTINVOICE_STATUS, OC_PATIENTINVOICE_BALANCE, " +
        "       OC_PATIENTINVOICE_NUMBER, OC_PATIENTINVOICE_DATE, " +
        "       OC_PATIENTINVOICE_CREATETIME, OC_PATIENTINVOICE_UPDATETIME, " +
        "       OC_PATIENTINVOICE_COMMENT " +
        "FROM OC_PATIENTINVOICES ";

    public List<PatientBillSummary> findByPatientUid(String patientUid) {
        return jdbc.query(
            BASE_SELECT + "WHERE OC_PATIENTINVOICE_PATIENTUID = ? ORDER BY OC_PATIENTINVOICE_DATE DESC",
            rowMapper(), toHmsUid(patientUid));
    }

    public List<PatientBillSummary> findOpenByPatientUid(String patientUid) {
        return jdbc.query(
            BASE_SELECT + "WHERE OC_PATIENTINVOICE_PATIENTUID = ? AND OC_PATIENTINVOICE_STATUS = 'open' " +
                         "ORDER BY OC_PATIENTINVOICE_DATE DESC",
            rowMapper(), toHmsUid(patientUid));
    }

    public Optional<PatientBillSummary> findLatestOpenByPatientUid(String patientUid) {
        try {
            PatientBillSummary result = jdbc.queryForObject(
                BASE_SELECT + "WHERE OC_PATIENTINVOICE_PATIENTUID = ? AND OC_PATIENTINVOICE_STATUS = 'open' " +
                             "ORDER BY OC_PATIENTINVOICE_DATE DESC LIMIT 1",
                rowMapper(), toHmsUid(patientUid));
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<PatientBillSummary> findByUid(String invoiceUid) {
        // UID format: "{serverId}.{objectId}"
        String[] parts = invoiceUid.split("\\.");
        if (parts.length < 2) return Optional.empty();
        try {
            PatientBillSummary result = jdbc.queryForObject(
                BASE_SELECT + "WHERE OC_PATIENTINVOICE_SERVERID = ? AND OC_PATIENTINVOICE_OBJECTID = ?",
                rowMapper(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns itemised charge lines (OC_DEBETS) for an invoice, joined with
     * OC_PRESTATIONS for a human-readable service name.
     */
    public List<InvoiceLineItem> findLineItemsByInvoiceUid(String invoiceUid) {
        return jdbc.query(
            "SELECT d.OC_DEBET_SERVERID, d.OC_DEBET_OBJECTID, " +
            "       d.OC_DEBET_PATIENTINVOICEUID, d.OC_DEBET_PRESTATIONUID, " +
            "       d.OC_DEBET_AMOUNT, d.OC_DEBET_INSURARAMOUNT, " +
            "       d.OC_DEBET_QUANTITY, d.OC_DEBET_DATE, d.OC_DEBET_CREDITED, " +
            "       COALESCE(p.OC_PRESTATION_DESCRIPTION, p.OC_PRESTATION_CODE, d.OC_DEBET_PRESTATIONUID) AS service_name " +
            "FROM OC_DEBETS d " +
            "LEFT JOIN OC_PRESTATIONS p " +
            "     ON p.OC_PRESTATION_SERVERID = CAST(SUBSTRING_INDEX(d.OC_DEBET_PRESTATIONUID,'.',1) AS UNSIGNED) " +
            "     AND p.OC_PRESTATION_OBJECTID = CAST(SUBSTRING_INDEX(d.OC_DEBET_PRESTATIONUID,'.',-1) AS UNSIGNED) " +
            "WHERE d.OC_DEBET_PATIENTINVOICEUID = ? " +
            "ORDER BY d.OC_DEBET_DATE ASC",
            lineItemRowMapper(), toHmsUid(invoiceUid));
    }

    private RowMapper<InvoiceLineItem> lineItemRowMapper() {
        return (rs, rowNum) -> {
            InvoiceLineItem item = new InvoiceLineItem();
            int sid = rs.getInt("OC_DEBET_SERVERID");
            int oid = rs.getInt("OC_DEBET_OBJECTID");
            item.setDebetUid(sid + "." + oid);
            item.setInvoiceUid(rs.getString("OC_DEBET_PATIENTINVOICEUID"));
            item.setPrestationUid(rs.getString("OC_DEBET_PRESTATIONUID"));
            item.setServiceName(rs.getString("service_name"));
            item.setQuantity(rs.getInt("OC_DEBET_QUANTITY"));
            BigDecimal total     = BigDecimal.valueOf(rs.getDouble("OC_DEBET_AMOUNT"));
            BigDecimal insurance = BigDecimal.valueOf(rs.getDouble("OC_DEBET_INSURARAMOUNT"));
            item.setTotalAmount(total);
            item.setInsuranceAmount(insurance);
            item.setPatientAmount(total.subtract(insurance));
            item.setServiceDate(toLocalDateTime(rs.getTimestamp("OC_DEBET_DATE")));
            item.setCredited(rs.getInt("OC_DEBET_CREDITED") == 1);
            return item;
        };
    }

    /**
     * Returns the sum of all successful PawaPay payments made against this invoice.
     */
    public BigDecimal getTotalPaidByInvoiceUid(String invoiceUid) {
        BigDecimal result = jdbc.queryForObject(
            "SELECT COALESCE(SUM(OC_MOMO_AMOUNT), 0) FROM OC_MOMO " +
            "WHERE OC_MOMO_INVOICEUID = ? AND OC_MOMO_STATUS = 'SUCCESSFUL'",
            BigDecimal.class, invoiceUid);
        return result != null ? result : BigDecimal.ZERO;
    }

    private RowMapper<PatientBillSummary> rowMapper() {
        return (rs, rowNum) -> {
            PatientBillSummary b = new PatientBillSummary();
            int sid = rs.getInt("OC_PATIENTINVOICE_SERVERID");
            int oid = rs.getInt("OC_PATIENTINVOICE_OBJECTID");
            b.setServerId(sid);
            b.setObjectId(oid);
            b.setInvoiceUid(sid + "." + oid);
            b.setInvoiceNumber(rs.getInt("OC_PATIENTINVOICE_ID"));
            String rawPid = rs.getString("OC_PATIENTINVOICE_PATIENTUID");
            b.setPatientUid(rawPid != null && rawPid.contains(".") ? rawPid : serverId + "." + rawPid);
            b.setStatus(rs.getString("OC_PATIENTINVOICE_STATUS"));
            b.setPatientBalance(BigDecimal.valueOf(rs.getDouble("OC_PATIENTINVOICE_BALANCE")));
            b.setInvoiceNumber2(rs.getString("OC_PATIENTINVOICE_NUMBER"));
            b.setComment(rs.getString("OC_PATIENTINVOICE_COMMENT"));
            b.setInvoiceDate(toLocalDateTime(rs.getTimestamp("OC_PATIENTINVOICE_DATE")));
            b.setCreateTime(toLocalDateTime(rs.getTimestamp("OC_PATIENTINVOICE_CREATETIME")));
            b.setUpdateTime(toLocalDateTime(rs.getTimestamp("OC_PATIENTINVOICE_UPDATETIME")));
            return b;
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
