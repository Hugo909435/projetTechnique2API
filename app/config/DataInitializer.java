package app.config;

import app.model.Role;
import app.model.StudentProfile;
import app.model.User;
import app.repository.StudentProfileRepository;
import app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           StudentProfileRepository studentProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@example.com")) return;

        String encoded = passwordEncoder.encode("password123");

        User admin = save(User.builder().email("admin@example.com").password(encoded)
                .firstName("Admin").lastName("System").role(Role.ADMIN).build());

        User trainer1 = save(User.builder().email("trainer1@example.com").password(encoded)
                .firstName("Jean").lastName("Dupont").phone("0601020304").role(Role.TRAINER).build());

        User trainer2 = save(User.builder().email("trainer2@example.com").password(encoded)
                .firstName("Sophie").lastName("Bernard").phone("0605060708").role(Role.TRAINER).build());

        User tutor1 = save(User.builder().email("tutor1@example.com").password(encoded)
                .firstName("Pierre").lastName("Martin").phone("0611121314").role(Role.TUTOR).build());

        User tutor2 = save(User.builder().email("tutor2@example.com").password(encoded)
                .firstName("Marie").lastName("Leblanc").phone("0615161718").role(Role.TUTOR).build());

        User student1 = save(User.builder().email("student1@example.com").password(encoded)
                .firstName("Alice").lastName("Durand").role(Role.STUDENT).build());

        User student2 = save(User.builder().email("student2@example.com").password(encoded)
                .firstName("Bob").lastName("Martin").role(Role.STUDENT).build());

        User student3 = save(User.builder().email("student3@example.com").password(encoded)
                .firstName("Clara").lastName("Petit").role(Role.STUDENT).build());

        studentProfileRepository.save(StudentProfile.builder()
                .student(student1).studentNumber("ALT-2024-001")
                .companyName("TechCorp").trainer(trainer1).tutor(tutor1).build());

        studentProfileRepository.save(StudentProfile.builder()
                .student(student2).studentNumber("ALT-2024-002")
                .companyName("TechCorp").trainer(trainer1).tutor(tutor1).build());

        studentProfileRepository.save(StudentProfile.builder()
                .student(student3).studentNumber("ALT-2024-003")
                .companyName("DataSoft").trainer(trainer2).tutor(tutor2).build());

        log.info("Données de test créées :");
        log.info("  admin@example.com        / password123  (ADMIN)");
        log.info("  trainer1@example.com     / password123  (TRAINER)");
        log.info("  trainer2@example.com     / password123  (TRAINER)");
        log.info("  tutor1@example.com       / password123  (TUTOR)");
        log.info("  tutor2@example.com       / password123  (TUTOR)");
        log.info("  student1@example.com     / password123  (STUDENT — trainer1, tutor1)");
        log.info("  student2@example.com     / password123  (STUDENT — trainer1, tutor1)");
        log.info("  student3@example.com     / password123  (STUDENT — trainer2, tutor2)");
    }

    private User save(User user) {
        return userRepository.save(user);
    }
}
