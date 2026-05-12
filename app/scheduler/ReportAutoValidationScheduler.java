package app.scheduler;

import app.service.ReportValidationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportAutoValidationScheduler {

    private final ReportValidationService validationService;

    public ReportAutoValidationScheduler(ReportValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Cherche chaque jour à 6h les rapports DRAFT du mois précédent.
     * Si la date courante >= 5 du mois, ils sont auto-validés.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void autoValidateExpiredReports() {
        int count = validationService.autoValidateReports();
        if (count > 0) {
            System.out.printf("[Scheduler] %d rapport(s) auto-validé(s)%n", count);
        }
    }
}
