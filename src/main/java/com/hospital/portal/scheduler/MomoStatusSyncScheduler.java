package com.hospital.portal.scheduler;

import com.hospital.portal.domain.model.MomoPayment;
import com.hospital.portal.repository.jdbc.MomoJdbcRepository;
import com.hospital.portal.service.MomoClientService;
import com.hospital.portal.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically polls PawaPay for PENDING deposits and syncs their final status.
 *
 * This is the resilience layer: even if the frontend never polls /status,
 * or if the PawaPay callback never fires, every transaction will eventually
 * be resolved to SUCCESSFUL or FAILED — and OC_PATIENTCREDITS will be written.
 */
@Component
public class MomoStatusSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MomoStatusSyncScheduler.class);

    private static final int PENDING_TIMEOUT_MINUTES = 30;

    private final MomoJdbcRepository momoRepo;
    private final MomoClientService pawaPayClient;
    private final PaymentService paymentService;

    public MomoStatusSyncScheduler(MomoJdbcRepository momoRepo,
                                   MomoClientService pawaPayClient,
                                   PaymentService paymentService) {
        this.momoRepo       = momoRepo;
        this.pawaPayClient  = pawaPayClient;
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelayString = "${app.sync.retry-interval-ms:60000}")
    public void syncPendingTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES);
        // Skip deposits less than 1 minute old — give PawaPay time to process
        List<MomoPayment> pending = momoRepo.findPendingOlderThan(
                LocalDateTime.now().minusMinutes(1));

        if (pending.isEmpty()) {
            log.debug("PawaPay sync: no PENDING deposits to process");
            return;
        }

        log.info("PawaPay sync: checking {} PENDING deposit(s)", pending.size());

        for (MomoPayment payment : pending) {
            try {
                if (payment.getCreatedAt() != null && payment.getCreatedAt().isBefore(cutoff)) {
                    momoRepo.updateStatus(payment.getTransactionId(), "FAILED", null);
                    log.warn("PawaPay sync: timed out depositId={} after {} min",
                            payment.getTransactionId(), PENDING_TIMEOUT_MINUTES);
                    continue;
                }

                String apiStatus = pawaPayClient.getPaymentStatus(payment.getTransactionId());

                if (!apiStatus.equalsIgnoreCase(payment.getStatus())) {
                    momoRepo.updateStatus(payment.getTransactionId(), apiStatus, null);
                    log.info("PawaPay sync: depositId={} {} → {}",
                            payment.getTransactionId(), payment.getStatus(), apiStatus);

                    if ("SUCCESSFUL".equalsIgnoreCase(apiStatus)) {
                        paymentService.writePatientCredit(payment);
                    }
                }

            } catch (Exception ex) {
                log.error("PawaPay sync error for depositId={}: {}",
                        payment.getTransactionId(), ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanExpiredOtps() {
        log.debug("OTP cleanup tick");
    }
}
