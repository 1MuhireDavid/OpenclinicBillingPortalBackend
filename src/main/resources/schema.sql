-- Portal tables added to the openclinic database.
-- OC_PORTAL_* tables are created automatically by Hibernate (ddl-auto=update).
-- OC_MOMO must be created manually — run this file once against the openclinic DB.

CREATE TABLE IF NOT EXISTS OC_MOMO (
    OC_MOMO_TRANSACTIONID          VARCHAR(100)  NOT NULL PRIMARY KEY,
    OC_MOMO_FINANCIALTRANSACTIONID VARCHAR(100)  DEFAULT NULL,
    OC_MOMO_INVOICEUID             VARCHAR(50)   NOT NULL,
    OC_MOMO_PATIENTUID             VARCHAR(20)   NOT NULL,
    OC_MOMO_UPDATEUID              VARCHAR(20)   DEFAULT NULL,
    OC_MOMO_AMOUNT                 DECIMAL(15,2) NOT NULL,
    OC_MOMO_CURRENCY               VARCHAR(10)   NOT NULL DEFAULT 'RWF',
    OC_MOMO_PAYERPHONE             VARCHAR(20)   NOT NULL,
    OC_MOMO_STATUS                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    OC_MOMO_OPERATOR               VARCHAR(50)   DEFAULT NULL,
    OC_MOMO_PAYERMESSAGE           VARCHAR(255)  DEFAULT NULL,
    OC_MOMO_PAYEEMESSAGE           VARCHAR(255)  DEFAULT NULL,
    OC_MOMO_PATIENTCREDITUID       VARCHAR(50)   DEFAULT NULL,
    OC_MOMO_WICKETCREDITUID        VARCHAR(50)   DEFAULT NULL,
    OC_MOMO_CREATEDATETIME         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    OC_MOMO_UPDATETIME             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_momo_invoice (OC_MOMO_INVOICEUID),
    INDEX idx_momo_patient (OC_MOMO_PATIENTUID),
    INDEX idx_momo_status  (OC_MOMO_STATUS),
    INDEX idx_momo_phone   (OC_MOMO_PAYERPHONE)
);

CREATE TABLE IF NOT EXISTS OC_PORTAL_USERS (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_uid   VARCHAR(20)  NOT NULL UNIQUE COMMENT 'OpenClinic composite UID e.g. 1.1234',
    person_id     INT          NOT NULL UNIQUE COMMENT 'Admin.personid from ocadmin DB',
    phone_number  VARCHAR(20)  NOT NULL UNIQUE,
    full_name     VARCHAR(255),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login    DATETIME
);

CREATE TABLE IF NOT EXISTS OC_PORTAL_OTP (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number  VARCHAR(20)  NOT NULL,
    otp_code      VARCHAR(10)  NOT NULL,
    expires_at    DATETIME     NOT NULL,
    is_used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_phone (phone_number),
    INDEX idx_otp_expires (expires_at)
);
