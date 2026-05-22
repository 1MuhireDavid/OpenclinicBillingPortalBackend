package com.hospital.portal.security;

/**
 * Immutable token stored as the Spring Security principal after JWT validation.
 * Controllers retrieve it via SecurityContextHolder to get the caller's identity.
 */
public class PortalPrincipal {

    private final String phoneNumber;
    private final String patientUid;   // e.g. "1.1234" — used in OC_ table lookups
    private final int personId;        // Admin.personid in ocadmin DB

    public PortalPrincipal(String phoneNumber, String patientUid, int personId) {
        this.phoneNumber = phoneNumber;
        this.patientUid  = patientUid;
        this.personId    = personId;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getPatientUid()  { return patientUid; }
    public int    getPersonId()    { return personId; }

    @Override
    public String toString() {
        return "PortalPrincipal{phone=" + phoneNumber + ", uid=" + patientUid + "}";
    }
}
