package app.service;

import app.model.MonthlyReport;
import app.model.StudentProfile;
import app.model.User;
import app.repository.StudentProfileRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final JavaMailSender mailSender;
    private final StudentProfileRepository studentProfileRepository;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender,
                        StudentProfileRepository studentProfileRepository) {
        this.mailSender = mailSender;
        this.studentProfileRepository = studentProfileRepository;
    }

    /** Notifie le tuteur qu'un rapport est en attente de sa validation. */
    public void sendTutorValidationRequest(MonthlyReport report) {
        StudentProfile profile = studentProfileRepository
                .findByStudentId(report.getStudent().getId()).orElse(null);
        if (profile == null) {
            log.warn("Pas de profil étudiant pour le rapport {} — email tuteur non envoyé", report.getId());
            return;
        }
        if (profile.getTutor() == null) {
            log.warn("Aucun tuteur assigné à l'étudiant {} — email tuteur non envoyé", report.getStudent().getId());
            return;
        }

        User tutor = profile.getTutor();
        String studentName = fullName(report.getStudent());
        String period = formatPeriod(report);

        send(
            tutor.getEmail(),
            "Rapport de " + studentName + " (" + period + ") – validation requise",
            buildTutorBody(tutor.getFirstName(), studentName, period)
        );
    }

    /** Notifie le formateur qu'un rapport est en attente de sa validation (après le tuteur). */
    public void sendTrainerValidationRequest(MonthlyReport report) {
        StudentProfile profile = studentProfileRepository
                .findByStudentId(report.getStudent().getId()).orElse(null);
        if (profile == null) {
            log.warn("Pas de profil étudiant pour le rapport {} — email formateur non envoyé", report.getId());
            return;
        }
        if (profile.getTrainer() == null) {
            log.warn("Aucun formateur assigné à l'étudiant {} — email formateur non envoyé", report.getStudent().getId());
            return;
        }

        User trainer = profile.getTrainer();
        String studentName = fullName(report.getStudent());
        String period = formatPeriod(report);

        send(
            trainer.getEmail(),
            "Rapport de " + studentName + " (" + period + ") – validation requise",
            buildTrainerBody(trainer.getFirstName(), studentName, period)
        );
    }

    /** Notifie un tuteur qu'une visite est à planifier. */
    public void sendVisitNotification(User tutor, User trainer) {
        send(
            tutor.getEmail(),
            "Une visite est à planifier — " + fullName(trainer),
            buildVisitBody(tutor.getFirstName(), fullName(trainer))
        );
    }

    private String buildVisitBody(String tutorFirstName, String trainerName) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto">
                  <h2 style="color:#4A90D9">Visite à planifier</h2>
                  <p>Bonjour %s,</p>
                  <p>Le formateur <strong>%s</strong> souhaite organiser une visite et a proposé des créneaux.
                     Connectez-vous à la plateforme pour sélectionner celui qui vous convient.</p>
                </body></html>
                """.formatted(tutorFirstName, trainerName);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email envoyé à {}", to);
        } catch (MessagingException e) {
            log.error("Échec de l'envoi d'email à {} : {}", to, e.getMessage());
        }
    }

    private String buildTutorBody(String firstName, String studentName, String period) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto">
                  <h2 style="color:#4A90D9">Validation de rapport d'alternance</h2>
                  <p>Bonjour %s,</p>
                  <p>Le rapport mensuel de <strong>%s</strong> pour la période de <strong>%s</strong>
                     est disponible et attend votre validation. Connectez-vous à la plateforme pour le consulter.</p>
                </body></html>
                """.formatted(firstName, studentName, period);
    }

    private String buildTrainerBody(String firstName, String studentName, String period) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto">
                  <h2 style="color:#4A90D9">Validation de rapport d'alternance</h2>
                  <p>Bonjour %s,</p>
                  <p>Le tuteur de <strong>%s</strong> a validé son rapport mensuel de <strong>%s</strong>.
                     Il attend maintenant votre validation en tant que formateur. Connectez-vous à la plateforme pour le consulter.</p>
                </body></html>
                """.formatted(firstName, studentName, period);
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private String formatPeriod(MonthlyReport report) {
        return YearMonth.of(report.getYear(), report.getMonth()).format(MONTH_FORMATTER);
    }
}
