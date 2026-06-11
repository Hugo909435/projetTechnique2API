package app.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Envoi SMTP asynchrone : la requête HTTP n'attend jamais le serveur mail.
 * Tout échec (adresse invalide, timeout Brevo…) est simplement journalisé.
 */
@Component
public class MailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MailDispatcher.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public MailDispatcher(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void dispatch(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email envoyé à {}", to);
        } catch (Exception e) {
            log.error("Échec de l'envoi d'email à {} : {}", to, e.getMessage());
        }
    }
}
