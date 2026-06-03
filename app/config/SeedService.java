package app.config;

import app.model.*;
import app.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private final UserRepository            userRepo;
    private final StudentProfileRepository  profileRepo;
    private final MonthlyReportRepository   reportRepo;
    private final ReportSectionRepository   sectionRepo;
    private final ReportStatusLogRepository logRepo;
    private final ReportCommentRepository   commentRepo;
    private final PasswordEncoder           encoder;

    public SeedService(UserRepository userRepo, StudentProfileRepository profileRepo,
                       MonthlyReportRepository reportRepo, ReportSectionRepository sectionRepo,
                       ReportStatusLogRepository logRepo, ReportCommentRepository commentRepo,
                       PasswordEncoder encoder) {
        this.userRepo = userRepo; this.profileRepo = profileRepo;
        this.reportRepo = reportRepo; this.sectionRepo = sectionRepo;
        this.logRepo = logRepo; this.commentRepo = commentRepo;
        this.encoder = encoder;
    }

    @Transactional
    public void seed() {
        if (reportRepo.count() > 0) return;

        String pwd = encoder.encode("password123");

        // ── Utilisateurs ──────────────────────────────────────────────────────
        User admin    = uoc("admin@example.com",    pwd, "Admin",   "System",  null,         Role.ADMIN);
        User trainer1 = uoc("trainer1@example.com", pwd, "Jean",    "Dupont",  "0601020304", Role.TRAINER);
        User trainer2 = uoc("trainer2@example.com", pwd, "Sophie",  "Bernard", "0605060708", Role.TRAINER);
        User tutor1   = uoc("tutor1@example.com",   pwd, "Pierre",  "Martin",  "0611121314", Role.TUTOR);
        User tutor2   = uoc("tutor2@example.com",   pwd, "Marie",   "Leblanc", "0615161718", Role.TUTOR);
        User alice    = uoc("student1@example.com", pwd, "Alice",   "Durand",  "0620304050", Role.STUDENT);
        User bob      = uoc("student2@example.com", pwd, "Bob",     "Martin",  "0621314151", Role.STUDENT);
        User clara    = uoc("student3@example.com", pwd, "Clara",   "Petit",   "0622324252", Role.STUDENT);

        // ── Profils ───────────────────────────────────────────────────────────
        if (profileRepo.findByStudentId(alice.getId()).isEmpty())
            profileRepo.save(StudentProfile.builder().student(alice).studentNumber("ALT-2025-001")
                    .companyName("TechCorp").trainer(trainer1).tutor(tutor1).build());
        if (profileRepo.findByStudentId(bob.getId()).isEmpty())
            profileRepo.save(StudentProfile.builder().student(bob).studentNumber("ALT-2025-002")
                    .companyName("TechCorp").trainer(trainer1).tutor(tutor1).build());
        if (profileRepo.findByStudentId(clara.getId()).isEmpty())
            profileRepo.save(StudentProfile.builder().student(clara).studentNumber("ALT-2025-003")
                    .companyName("DataSoft").trainer(trainer2).tutor(tutor2).build());

        // ══════════════════════════════════════════════════════════════════════
        //  ALICE DURAND — backend Java/Spring chez TechCorp
        //  Janv 2026 COMPLETED · Fev 2026 COMPLETED
        //  Mars 2026 COMPLETED · Avr 2026 REOPENED
        // ══════════════════════════════════════════════════════════════════════

        MonthlyReport a1 = completed(alice, 2026, 1, alice, trainer1, tutor1,
            "Excellent debut d'annee. Alice maitrise Spring Boot et livre des fonctionnalites complexes. " +
            "Le code est propre, teste et bien documente.",
            "Premier mois tres productif. Alice a pris en charge un module entier en autonomie. " +
            "Elle s'integre parfaitement dans l'equipe.");
        sections(a1,
            "Cours avance sur les architectures hexagonales et le Domain-Driven Design (DDD). " +
            "TP : migration d'une application Spring Boot vers une architecture ports & adapters.",
            "Developpement complet du module de gestion des contrats : CRUD, validation metier, " +
            "workflow de signature electronique. Livraison de 3 fonctionnalites en sprint. " +
            "Participation active aux ceremonies Scrum (planning, review, retrospective).",
            "Tres satisfaite du niveau de qualite atteint ce mois-ci. " +
            "La code review avec le tech lead m'a permis de progresser enormement.");
        logs(a1, alice, trainer1, tutor1);
        comment(a1, trainer1, "Alice est maintenant au niveau d'un developpeur junior confirme. Bravo.");
        comment(a1, tutor1, "Elle peut travailler en autonomie sur les fonctionnalites de complexite standard.");

        MonthlyReport a2 = completed(alice, 2026, 2, alice, trainer1, tutor1,
            "Alice monte en responsabilites. Elle a anime sa premiere session de revue de code " +
            "et forme un nouveau stagiaire. Tres bonne progression.",
            "Elle prend des initiatives et propose des ameliorations architecturales pertinentes. " +
            "Candidature a un projet interne ambitieux retenue.");
        sections(a2,
            "Cours sur les microservices : decomposition d'un monolithe, communication inter-services, " +
            "service discovery avec Eureka, API Gateway avec Spring Cloud Gateway.",
            "Migration du module de facturation vers un microservice independant. " +
            "Mise en place de la communication asynchrone via RabbitMQ. " +
            "Animation d'une session de revue de code pour l'equipe junior. " +
            "Formation du nouveau stagiaire sur Spring Boot.",
            "Former d'autres developpeurs est une experience enrichissante. " +
            "Cela m'oblige a vraiment comprendre ce que j'explique.");
        logs(a2, alice, trainer1, tutor1);

        MonthlyReport a3 = completed(alice, 2026, 3, alice, trainer1, tutor1,
            "Alice contribue a l'architecture du nouveau projet strategique de l'entreprise. " +
            "Tres haute maturite technique et comportementale.",
            "Elle est desormais une reference technique dans l'equipe. Je la recommande pour un CDI.");
        sections(a3,
            "Cours sur la securite des API REST : OWASP Top 10, authentification OAuth2/OIDC, " +
            "rate limiting, audit logging. Certification Secure Coding en preparation.",
            "Participation a la conception de l'architecture du projet CRM nouvelle generation. " +
            "Implementation du module de securite : OAuth2 avec Keycloak, audit trail complet, " +
            "chiffrement des donnees sensibles au repos et en transit.",
            "Participer a la conception d'un vrai projet d'entreprise est une chance incroyable. " +
            "Je me sens prete pour prendre plus de responsabilites.");
        logs(a3, alice, trainer1, tutor1);
        comment(a3, trainer1, "Excellent mois. Alice est prete pour un poste de developpeur senior.");

        MonthlyReport a4 = reportRepo.save(mkReport(alice, 2026, 4, ReportStatus.DRAFT,
            alice, null, null, null, null));
        sections(a4,
            "Cours sur le DevOps : CI/CD avance avec GitLab CI, deployment Kubernetes, " +
            "observabilite avec Prometheus et Grafana.",
            "Mise en place du pipeline CI/CD pour le projet CRM : build, tests, analyse statique, " +
            "deploiement automatique en staging. Configuration des alertes monitoring.",
            "Je dois completer ce rapport suite aux retours du formateur.");
        logRepo.save(mkLog(a4, null, ReportStatus.DRAFT, alice, null));
        logRepo.save(mkLog(a4, ReportStatus.DRAFT, ReportStatus.STUDENT_VALIDATED, alice, null));
        logRepo.save(mkLog(a4, ReportStatus.STUDENT_VALIDATED, ReportStatus.DRAFT, alice,
            "Rapport modifié par l'étudiant — validation réinitialisée"));

        // ══════════════════════════════════════════════════════════════════════
        //  BOB MARTIN — frontend Vue 3 / Nuxt chez TechCorp
        //  Janv 2026 COMPLETED · Fev 2026 COMPLETED
        //  Mars 2026 TRAINER_VALIDATED · Avr 2026 AUTO_VALIDATED
        // ══════════════════════════════════════════════════════════════════════

        MonthlyReport b1 = completed(bob, 2026, 1, bob, trainer1, tutor1,
            "Bob s'affirme comme le referent front-end de l'equipe. " +
            "Tres bonne qualite de code et sens de l'UX en nette progression.",
            "Il prend des initiatives sur l'architecture front-end. " +
            "Propose des solutions pertinentes lors des design reviews.");
        sections(b1,
            "Cours sur les architectures front-end modernes : micro-frontends, module federation, " +
            "monorepos avec Nx. Cours sur l'accessibilite WCAG 2.1.",
            "Refonte complete du systeme de design de l'application : creation d'une design system library " +
            "avec des composants accessibles. Integration dans Storybook et publication en npm interne. " +
            "Revue UX avec le designer et correction de 12 problemes d'accessibilite.",
            "Creer une design system est un travail de longue haleine mais tres gratifiant. " +
            "Les retours de l'equipe sont tres positifs.");
        logs(b1, bob, trainer1, tutor1);
        comment(b1, trainer1, "Bob est maintenant une reference technique front-end dans l'equipe.");

        MonthlyReport b2 = completed(bob, 2026, 2, bob, trainer1, tutor1,
            "Excellent mois. Bob a delivre une fonctionnalite temps reel complexe " +
            "et a forme l'equipe sur les nouvelles technos front.",
            "Il s'implique dans la formation des autres. Leader technique front-end confirme.");
        sections(b2,
            "Cours sur le temps reel dans les applications web : WebSocket, Server-Sent Events, " +
            "WebRTC. Cours sur les Progressive Web Apps (PWA) et les Service Workers.",
            "Integration de notifications temps reel dans le CRM avec WebSocket et STOMP. " +
            "Mise en place du mode offline avec Service Workers et strategie de cache. " +
            "Formation de 3 developpeurs sur Vue 3 et la Composition API.",
            "Former les autres est vraiment enrichissant. Expliquer quelque chose clairement " +
            "montre si on le comprend vraiment ou non.");
        logs(b2, bob, trainer1, tutor1);

        MonthlyReport b3 = reportRepo.save(mkReport(bob, 2026, 3, ReportStatus.TRAINER_VALIDATED,
            bob, trainer1, null,
            "Rapport tres complet ce mois-ci. Bob a bien progresse sur les tests E2E et la performance. " +
            "Je transmets au tuteur avec confiance.",
            null));
        sections(b3,
            "Cours sur les tests front-end avances : testing-library, Cypress component testing, " +
            "Playwright pour les tests E2E cross-browser. Cours sur Web Performance (LCP, CLS, FID).",
            "Mise en place de Playwright pour les tests E2E du CRM : 45 scenarios couverts. " +
            "Optimisation des performances : passage du LCP de 4.2s a 1.8s. " +
            "Audit complet Lighthouse et correction des issues critiques.",
            "Un LCP de 1.8s sur une app aussi complexe, c'est une vraie satisfaction technique !");
        logRepo.save(mkLog(b3, null, ReportStatus.DRAFT, bob, null));
        logRepo.save(mkLog(b3, ReportStatus.DRAFT, ReportStatus.STUDENT_VALIDATED, bob, null));
        logRepo.save(mkLog(b3, ReportStatus.STUDENT_VALIDATED, ReportStatus.TRAINER_VALIDATED, trainer1, null));
        comment(b3, trainer1, "Excellent travail sur les tests E2E. A transmettre au tuteur.");

        MonthlyReport b4 = reportRepo.save(mkReport(bob, 2026, 4, ReportStatus.AUTO_VALIDATED,
            null, null, null,
            "Rapport recu via validation automatique car Bob n'a pas soumis dans les delais. " +
            "Le contenu est globalement correct mais je rappelle l'importance de respecter les echeances. " +
            "Un rappel a ete envoye.",
            null));
        b4.setAutoValidatedAt(LocalDateTime.of(2026, 5, 5, 0, 1));
        sections(b4,
            "Cours sur l'architecture des applications Vue 3 complexes : patterns avances, " +
            "state management distribue, reactive programming avec RxJS.",
            "Developpement du module d'analytics client : graphiques interactifs avec D3.js, " +
            "tableaux de bord configurables par l'utilisateur, export PDF et Excel.",
            "J'aurais du soumettre ce rapport a temps. Je dois mieux organiser mon planning.");
        logRepo.save(mkLog(b4, null, ReportStatus.DRAFT, bob, null));
        logRepo.save(mkLog(b4, ReportStatus.DRAFT, ReportStatus.AUTO_VALIDATED, null,
            "Validation automatique — delai de depot expire"));

        // ══════════════════════════════════════════════════════════════════════
        //  CLARA PETIT — data engineering Python/Spark chez DataSoft
        //  Janv 2026 COMPLETED · Fev 2026 COMPLETED
        //  Mars 2026 STUDENT_VALIDATED · Avr 2026 DRAFT
        // ══════════════════════════════════════════════════════════════════════

        MonthlyReport c1 = completed(clara, 2026, 1, clara, trainer2, tutor2,
            "Clara est une des meilleures alternantes que j'ai encadrees. " +
            "Elle contribue au niveau d'un profil senior sur les sujets data.",
            "Elle a livre un projet de bout en bout en autonomie complete. " +
            "Je lui confie des projets de plus en plus strategiques.");
        sections(c1,
            "Cours sur le Data Mesh : principes, domaines de donnees, data products. " +
            "Cours sur les architectures Lambda et Kappa en production.",
            "Conception et implementation d'un data product 'KPIs commerciaux' : " +
            "pipeline d'ingestion Kafka, transformation PySpark, exposition via API REST. " +
            "Presentation aux parties prenantes metier et validation des regles de calcul.",
            "Voir mes KPIs utilises par la direction commerciale chaque matin est une vraie satisfaction.");
        logs(c1, clara, trainer2, tutor2);
        comment(c1, trainer2, "Clara delivre au niveau d'un data engineer senior. Impressionnant.");
        comment(c1, tutor2, "Le data product est desormais reference dans les reunions de direction.");

        MonthlyReport c2 = completed(clara, 2026, 2, clara, trainer2, tutor2,
            "Clara prend en charge des sujets de fond sur la gouvernance des donnees. " +
            "Niveau d'expertise reconnu en interne et en externe.",
            "Elle represente DataSoft lors d'une conference data. Tres fiere de son parcours.");
        sections(c2,
            "Cours sur la Data Governance : data catalog, data lineage, data quality frameworks. " +
            "Cours sur le Machine Learning en production : MLOps, model monitoring, drift detection.",
            "Mise en place d'Apache Atlas pour le data catalog de DataSoft : " +
            "catalogage de 200+ datasets, lineage automatique des pipelines Airflow. " +
            "Presentation d'un talk technique a la conference DataFr sur le data mesh.",
            "Presenter a une conference a 200 personnes etait stressant mais extraordinairement formateur. " +
            "Je veux continuer a contribuer a la communaute data.");
        logs(c2, clara, trainer2, tutor2);

        MonthlyReport c3 = reportRepo.save(mkReport(clara, 2026, 3, ReportStatus.STUDENT_VALIDATED,
            clara, null, null, null, null));
        sections(c3,
            "Cours sur le streaming de donnees avance : Apache Flink stateful processing, " +
            "event time vs processing time, watermarks. Certification Databricks en preparation.",
            "Prototype d'une plateforme de detection de fraude en temps reel avec Apache Flink : " +
            "traitement de 50 000 transactions/seconde, latence < 100ms. " +
            "Redaction du document d'architecture et validation par le CTO.",
            "Ce projet de detection de fraude est le plus ambitieux que j'ai realise. " +
            "Hâte de le voir partir en production.");
        logRepo.save(mkLog(c3, null, ReportStatus.DRAFT, clara, null));
        logRepo.save(mkLog(c3, ReportStatus.DRAFT, ReportStatus.STUDENT_VALIDATED, clara, null));

        MonthlyReport c4 = reportRepo.save(mkReport(clara, 2026, 4, ReportStatus.DRAFT,
            null, null, null, null, null));
        sections(c4,
            "Cours sur le Data Engineering moderne : dbt Core avance, materializations, tests, " +
            "documentation automatique. Cours sur la gouvernance et la qualite des donnees en continu.",
            "Mise en production du prototype de detection de fraude. " +
            "Integration dans le systeme de paiement de DataSoft. " +
            "Premier retour en production : 23 fraudes detectees en 48h sans faux positif.",
            "Voir mon travail detecter de vraies fraudes en production est la meilleure recompense. " +
            "Ce projet restera un moment cle de mon alternance.");
        logRepo.save(mkLog(c4, null, ReportStatus.DRAFT, clara, null));

        log.info("=== Donnees de demonstration creees ===");
        log.info("  12 rapports (jan 2026 -> avr 2026) — tous complets");
        log.info("  admin@example.com    / password123  (ADMIN)");
        log.info("  trainer1@example.com / password123  (TRAINER — Jean Dupont)");
        log.info("  trainer2@example.com / password123  (TRAINER — Sophie Bernard)");
        log.info("  tutor1@example.com   / password123  (TUTOR   — Pierre Martin)");
        log.info("  tutor2@example.com   / password123  (TUTOR   — Marie Leblanc)");
        log.info("  student1@example.com / password123  (Alice — COMPLETED/COMPLETED/COMPLETED/DRAFT)");
        log.info("  student2@example.com / password123  (Bob   — COMPLETED/COMPLETED/TRAINER_VALIDATED/AUTO_VALIDATED)");
        log.info("  student3@example.com / password123  (Clara — COMPLETED/COMPLETED/STUDENT_VALIDATED/DRAFT)");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private MonthlyReport completed(User student, int year, int month,
                                     User byStudent, User byTrainer, User byTutor,
                                     String trainerNote, String tutorNote) {
        return reportRepo.save(mkReport(student, year, month, ReportStatus.COMPLETED,
                byStudent, byTrainer, byTutor, trainerNote, tutorNote));
    }

    private void logs(MonthlyReport r, User student, User trainer, User tutor) {
        logRepo.save(mkLog(r, null, ReportStatus.DRAFT, student, null));
        logRepo.save(mkLog(r, ReportStatus.DRAFT, ReportStatus.STUDENT_VALIDATED, student, null));
        logRepo.save(mkLog(r, ReportStatus.STUDENT_VALIDATED, ReportStatus.TRAINER_VALIDATED, trainer, null));
        logRepo.save(mkLog(r, ReportStatus.TRAINER_VALIDATED, ReportStatus.TUTOR_VALIDATED, tutor, null));
        logRepo.save(mkLog(r, ReportStatus.TUTOR_VALIDATED, ReportStatus.COMPLETED, tutor, null));
    }

    private MonthlyReport mkReport(User student, int year, int month, ReportStatus status,
                                    User byStudent, User byTrainer, User byTutor,
                                    String trainerNote, String tutorNote) {
        MonthlyReport r = new MonthlyReport();
        r.setStudent(student); r.setYear(year); r.setMonth(month); r.setStatus(status);
        r.setValidatedByStudent(byStudent);
        r.setValidatedByTrainer(byTrainer);
        r.setValidatedByTutor(byTutor);
        r.setTrainerNote(trainerNote);
        r.setTutorNote(tutorNote);
        if (byStudent != null) r.setStudentValidatedAt(LocalDateTime.now().minusDays(20));
        if (byTrainer != null) r.setTrainerValidatedAt(LocalDateTime.now().minusDays(15));
        if (byTutor   != null) r.setTutorValidatedAt(LocalDateTime.now().minusDays(10));
        if (status == ReportStatus.COMPLETED) r.setCompletedAt(LocalDateTime.now().minusDays(10));
        return r;
    }

    private ReportStatusLog mkLog(MonthlyReport r, ReportStatus from, ReportStatus to,
                                   User by, String note) {
        return ReportStatusLog.builder()
                .report(r).fromStatus(from).toStatus(to).changedBy(by).note(note).build();
    }

    private void sections(MonthlyReport r, String school, String company, String free) {
        sectionRepo.saveAll(List.of(
            sec(r, ReportSectionType.SCHOOL_ACTIVITIES,  school),
            sec(r, ReportSectionType.COMPANY_ACTIVITIES, company),
            sec(r, ReportSectionType.FREE_COMMENT,       free)
        ));
    }

    private ReportSection sec(MonthlyReport r, ReportSectionType t, String c) {
        return ReportSection.builder().report(r).sectionType(t).content(c != null ? c : "").build();
    }

    private void comment(MonthlyReport r, User author, String content) {
        commentRepo.save(ReportComment.builder()
                .report(r).author(author).authorRole(author.getRole()).content(content).build());
    }

    private User uoc(String email, String pwd, String first, String last, String phone, Role role) {
        return userRepo.findByEmail(email).orElseGet(() ->
                userRepo.save(User.builder().email(email).password(pwd)
                        .firstName(first).lastName(last).phone(phone).role(role).build()));
    }
}
