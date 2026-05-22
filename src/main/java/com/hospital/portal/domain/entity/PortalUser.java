package com.hospital.portal.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps a patient's phone number to their OpenClinic identity.
 * Stored in openclinic.OC_PORTAL_USERS — a new table we add.
 */
@Entity
@Table(name = "OC_PORTAL_USERS")
public class PortalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_uid", nullable = false, unique = true, length = 20)
    private String patientUid;

    @Column(name = "person_id", nullable = false, unique = true)
    private int personId;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    public PortalUser() {}

    public PortalUser(String patientUid, int personId, String phoneNumber, String fullName) {
        this.patientUid  = patientUid;
        this.personId    = personId;
        this.phoneNumber = phoneNumber;
        this.fullName    = fullName;
        this.active      = true;
        this.createdAt   = LocalDateTime.now();
    }

    public Long getId()            { return id; }
    public String getPatientUid()  { return patientUid; }
    public int getPersonId()       { return personId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getFullName()    { return fullName; }
    public boolean isActive()      { return active; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getLastLogin()  { return lastLogin; }

    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public void setActive(boolean active)             { this.active = active; }
    public void setFullName(String fullName)          { this.fullName = fullName; }
}
