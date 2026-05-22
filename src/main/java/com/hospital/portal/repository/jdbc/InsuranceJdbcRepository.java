package com.hospital.portal.repository.jdbc;

import com.hospital.portal.domain.model.InsuranceCoverage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Reads from openclinic.OC_INSURANCES joined with OC_INSURARS for insurer name.
 */
@Repository
public class InsuranceJdbcRepository {

    private final JdbcTemplate jdbc;

    @Value("${app.openclinic.server-id:1}")
    private int serverId;

    public InsuranceJdbcRepository(@Qualifier("openclinicJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String toHmsUid(String portalUid) {
        return portalUid != null && portalUid.contains(".") ? portalUid.split("\\.")[1] : portalUid;
    }

    private static final String BASE_SELECT =
        "SELECT i.OC_INSURANCE_SERVERID, i.OC_INSURANCE_OBJECTID, " +
        "       i.OC_INSURANCE_NR, i.OC_INSURANCE_INSURARUID, " +
        "       i.OC_INSURANCE_TYPE, i.OC_INSURANCE_MEMBER, " +
        "       i.OC_INSURANCE_MEMBER_EMPLOYER, i.OC_INSURANCE_MEMBERCATEGORY, " +
        "       i.OC_INSURANCE_INSURANCECATEGORYLETTER, i.OC_INSURANCE_STATUS, " +
        "       i.OC_INSURANCE_START, i.OC_INSURANCE_STOP, " +
        "       i.OC_INSURANCE_PATIENTUID, " +
        "       ins.OC_INSURAR_NAME AS insurar_name, " +
        "       ic.OC_INSURANCECATEGORY_PATIENTSHARE AS db_patient_share " +
        "FROM OC_INSURANCES i " +
        "LEFT JOIN OC_INSURARS ins ON ins.OC_INSURAR_SERVERID = " +
        "     CAST(SUBSTRING_INDEX(i.OC_INSURANCE_INSURARUID,'.',1) AS UNSIGNED) " +
        "     AND ins.OC_INSURAR_OBJECTID = " +
        "     CAST(SUBSTRING_INDEX(i.OC_INSURANCE_INSURARUID,'.',-1) AS UNSIGNED) " +
        "LEFT JOIN OC_INSURANCECATEGORIES ic " +
        "     ON ic.OC_INSURANCECATEGORY_INSURARUID = i.OC_INSURANCE_INSURARUID " +
        "     AND ic.OC_INSURANCECATEGORY_CATEGORY = i.OC_INSURANCE_INSURANCECATEGORYLETTER ";

    public List<InsuranceCoverage> findByPatientUid(String patientUid) {
        return jdbc.query(
            BASE_SELECT + "WHERE i.OC_INSURANCE_PATIENTUID = ? ORDER BY i.OC_INSURANCE_DEFAULT DESC, i.OC_INSURANCE_START DESC",
            rowMapper(), toHmsUid(patientUid));
    }

    public Optional<InsuranceCoverage> findActiveByPatientUid(String patientUid) {
        List<InsuranceCoverage> list = jdbc.query(
            BASE_SELECT +
            "WHERE i.OC_INSURANCE_PATIENTUID = ? " +
            "  AND (i.OC_INSURANCE_STOP IS NULL OR i.OC_INSURANCE_STOP >= CURDATE()) " +
            "ORDER BY i.OC_INSURANCE_DEFAULT DESC, i.OC_INSURANCE_START DESC LIMIT 1",
            rowMapper(), toHmsUid(patientUid));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private RowMapper<InsuranceCoverage> rowMapper() {
        return (rs, rowNum) -> {
            InsuranceCoverage c = new InsuranceCoverage();
            int sid = rs.getInt("OC_INSURANCE_SERVERID");
            int oid = rs.getInt("OC_INSURANCE_OBJECTID");
            c.setInsuranceUid(sid + "." + oid);
            String rawPid = rs.getString("OC_INSURANCE_PATIENTUID");
            c.setPatientUid(rawPid != null && rawPid.contains(".") ? rawPid : serverId + "." + rawPid);
            c.setInsurarUid(rs.getString("OC_INSURANCE_INSURARUID"));
            c.setInsurarName(rs.getString("insurar_name"));
            c.setInsuranceNr(rs.getString("OC_INSURANCE_NR"));
            c.setType(rs.getString("OC_INSURANCE_TYPE"));
            c.setMember(rs.getString("OC_INSURANCE_MEMBER"));
            c.setMemberEmployer(rs.getString("OC_INSURANCE_MEMBER_EMPLOYER"));
            c.setMembercategory(rs.getString("OC_INSURANCE_MEMBERCATEGORY"));
            c.setInsuranceCategoryLetter(rs.getString("OC_INSURANCE_INSURANCECATEGORYLETTER"));
            c.setStatus(rs.getString("OC_INSURANCE_STATUS"));
            c.setStartDate(toLocalDate(rs.getDate("OC_INSURANCE_START")));
            c.setEndDate(toLocalDate(rs.getDate("OC_INSURANCE_STOP")));
            // Live rate from OC_INSURANCECATEGORIES (null if the category row doesn't exist)
            double dbShare = rs.getDouble("db_patient_share");
            if (!rs.wasNull()) c.setDbPatientSharePercent(dbShare);
            return c;
        };
    }

    private LocalDate toLocalDate(Date d) {
        return d != null ? d.toLocalDate() : null;
    }
}
