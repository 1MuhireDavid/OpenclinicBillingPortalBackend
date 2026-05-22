package com.hospital.portal.repository.jdbc;

import com.hospital.portal.domain.model.PatientProfile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Reads patient demographics from ocadmin.Admin and ocadmin.AdminPrivate.
 */
@Repository
public class PatientJdbcRepository {

    private final JdbcTemplate ocadminJdbc;

    @Value("${app.openclinic.server-id:1}")
    private int serverId;

    public PatientJdbcRepository(@Qualifier("ocadminJdbcTemplate") JdbcTemplate ocadminJdbc) {
        this.ocadminJdbc = ocadminJdbc;
    }

    private static final String FIND_BY_PERSON_ID =
        "SELECT a.personid, a.lastname, a.firstname, a.middlename, a.gender, " +
        "       DATE_FORMAT(a.dateofbirth, '%Y-%m-%d') AS dateofbirth, a.natreg, " +
        "       ap.mobile, ap.telephone, ap.email " +
        "FROM Admin a " +
        "LEFT JOIN AdminPrivate ap ON ap.personid = a.personid AND ap.stop IS NULL " +
        "WHERE a.personid = ? " +
        "ORDER BY ap.privateid DESC LIMIT 1";

    private static final String FIND_BY_PHONE =
        "SELECT a.personid, a.lastname, a.firstname, a.middlename, a.gender, " +
        "       DATE_FORMAT(a.dateofbirth, '%Y-%m-%d') AS dateofbirth, a.natreg, " +
        "       ap.mobile, ap.telephone, ap.email " +
        "FROM Admin a " +
        "INNER JOIN AdminPrivate ap ON ap.personid = a.personid AND ap.stop IS NULL " +
        "WHERE ap.mobile = ? OR ap.telephone = ? " +
        "LIMIT 1";

    public Optional<PatientProfile> findByPersonId(int personId) {
        try {
            PatientProfile p = ocadminJdbc.queryForObject(FIND_BY_PERSON_ID, rowMapper(), personId);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<PatientProfile> findByPhoneNumber(String phone) {
        String normalised = normalisePhone(phone);
        try {
            PatientProfile p = ocadminJdbc.queryForObject(FIND_BY_PHONE, rowMapper(), normalised, normalised);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<PatientProfile> searchByName(String lastName, String firstName) {
        String sql =
            "SELECT a.personid, a.lastname, a.firstname, a.middlename, a.gender, " +
            "       DATE_FORMAT(a.dateofbirth,'%Y-%m-%d') AS dateofbirth, a.natreg, " +
            "       ap.mobile, ap.telephone, ap.email " +
            "FROM Admin a " +
            "LEFT JOIN AdminPrivate ap ON ap.personid = a.personid AND ap.stop IS NULL " +
            "WHERE a.searchname LIKE ? AND (? IS NULL OR a.firstname LIKE ?) " +
            "LIMIT 20";
        String lastPattern  = "%" + lastName.toUpperCase() + "%";
        String firstPattern = firstName == null ? null : "%" + firstName + "%";
        return ocadminJdbc.query(sql, rowMapper(), lastPattern, firstName, firstPattern);
    }

    private RowMapper<PatientProfile> rowMapper() {
        return (rs, rowNum) -> {
            PatientProfile p = new PatientProfile();
            int pid = rs.getInt("personid");
            p.setPersonId(pid);
            p.setPatientUid(serverId + "." + pid);
            p.setLastName(nullSafe(rs, "lastname"));
            p.setFirstName(nullSafe(rs, "firstname"));
            String middle  = nullSafe(rs, "middlename");
            String full    = buildFullName(
                    p.getFirstName(), middle, p.getLastName());
            p.setFullName(full);
            p.setGender(nullSafe(rs, "gender"));
            p.setDateOfBirth(nullSafe(rs, "dateofbirth"));
            p.setNationalId(nullSafe(rs, "natreg"));

            // AdminPrivate columns may be null when no contact row exists
            String mobile = nullSafe(rs, "mobile");
            String tel    = nullSafe(rs, "telephone");
            p.setPhoneNumber(mobile != null ? mobile : tel);
            p.setEmail(nullSafe(rs, "email"));
            return p;
        };
    }

    private String nullSafe(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    private String buildFullName(String first, String middle, String last) {
        StringBuilder sb = new StringBuilder();
        if (first  != null && !first.isBlank())  sb.append(first).append(" ");
        if (middle != null && !middle.isBlank()) sb.append(middle).append(" ");
        if (last   != null && !last.isBlank())   sb.append(last);
        return sb.toString().trim();
    }

    /**
     * Normalises Rwandan phone numbers to the 250XXXXXXXXX format stored in OpenClinic.
     */
    private String normalisePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0") && digits.length() == 10)  return "250" + digits.substring(1);
        if (digits.startsWith("250") && digits.length() == 12) return digits;
        return digits;
    }
}
