package com.hospital.portal.service;

import com.hospital.portal.domain.model.InsuranceCoverage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Applies Rwanda healthcare insurance split rules to a bill amount.
 *
 * Rules (statutory):
 *   RSSB          → 85 % insurance / 15 % patient
 *   Old Mutual    → 90 % insurance / 10 % patient
 *   Full coverage → 100 % insurance /  0 % patient
 *   Private/Cash  →  0 % insurance / 100 % patient
 *   Others        → 80 % insurance / 20 % patient (conservative default)
 */
@Service
public class InsuranceCalculationService {

    public enum InsuranceType {
        RSSB(85.0, 15.0),
        OLD_MUTUAL(90.0, 10.0),
        FULL_COVERAGE(100.0, 0.0),
        PRIVATE(0.0, 100.0),
        RAMA(85.0, 15.0),
        MFP(85.0, 15.0),
        CCBRT(80.0, 20.0),
        DEFAULT(80.0, 20.0);

        final double insuranceRate;
        final double patientRate;

        InsuranceType(double insuranceRate, double patientRate) {
            this.insuranceRate = insuranceRate;
            this.patientRate   = patientRate;
        }
    }

    /**
     * Resolves which insurance type applies based on the insurer's display name.
     */
    public InsuranceType resolveType(String insurarName) {
        if (insurarName == null || insurarName.isBlank()) return InsuranceType.PRIVATE;
        String upper = insurarName.toUpperCase();
        if (upper.contains("RSSB"))                                  return InsuranceType.RSSB;
        if (upper.contains("OLD MUTUAL") || upper.contains("OLDMUTUAL")) return InsuranceType.OLD_MUTUAL;
        if (upper.contains("RAMA"))                                  return InsuranceType.RAMA;
        if (upper.contains("MFP"))                                   return InsuranceType.MFP;
        if (upper.contains("CCBRT"))                                 return InsuranceType.CCBRT;
        if (upper.contains("100") || upper.contains("EMPLOYER")
                || upper.contains("FULL") || upper.contains("CPLR")) return InsuranceType.FULL_COVERAGE;
        return InsuranceType.DEFAULT;
    }

    /**
     * Enriches the InsuranceCoverage model with the computed split rates.
     * Prefers the live rate from OC_INSURANCECATEGORIES; falls back to name-based matching.
     */
    public void applyRates(InsuranceCoverage coverage) {
        if (coverage.getDbPatientSharePercent() != null) {
            double patientRate   = coverage.getDbPatientSharePercent();
            double insuranceRate = 100.0 - patientRate;
            coverage.setInsuranceRatePercent(insuranceRate);
            coverage.setPatientRatePercent(patientRate);
            return;
        }
        InsuranceType type = resolveType(coverage.getInsurarName());
        coverage.setInsuranceRatePercent(type.insuranceRate);
        coverage.setPatientRatePercent(type.patientRate);
    }

    /**
     * Returns the amount the insurance covers for a given total bill amount.
     * Uses HALF_UP rounding to avoid under/over-charging by fractions.
     */
    public BigDecimal insuranceShare(BigDecimal totalAmount, InsuranceCoverage coverage) {
        BigDecimal rate = BigDecimal.valueOf(coverage.getInsuranceRatePercent())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return totalAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the amount the patient owes for a given total bill amount.
     * Derived by subtraction to guarantee: insuranceShare + patientShare == totalAmount exactly.
     */
    public BigDecimal patientShare(BigDecimal totalAmount, InsuranceCoverage coverage) {
        return totalAmount.subtract(insuranceShare(totalAmount, coverage))
                          .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Derives the original total bill from the patient's balance and their coverage rate.
     * Used when OpenClinic only stores the patient's share (balance field).
     *
     * Formula: total = patientBalance / (patientRate / 100)
     * For private patients (100 % patient), total == patientBalance.
     */
    public BigDecimal deriveTotalFromPatientBalance(BigDecimal patientBalance,
                                                    InsuranceCoverage coverage) {
        double patientRate = coverage.getPatientRatePercent();
        if (patientRate == 0.0) {
            // Full coverage — patient pays nothing; total is unknown from balance alone
            return BigDecimal.ZERO;
        }
        BigDecimal rate = BigDecimal.valueOf(patientRate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return patientBalance.divide(rate, 2, RoundingMode.HALF_UP);
    }

    /**
     * Builds a human-readable coverage label for the UI.
     */
    public String coverageLabel(InsuranceCoverage coverage) {
        String name = coverage.getInsurarName() != null ? coverage.getInsurarName() : "Insurance";
        double ins  = coverage.getInsuranceRatePercent();
        if (ins == 100.0) return name + " — 100 % covered (you pay nothing)";
        if (ins == 0.0)   return "Private / Cash — no insurance";
        return name + " — " + (int) ins + " % covered / " + (int) coverage.getPatientRatePercent() + " % your share";
    }
}
