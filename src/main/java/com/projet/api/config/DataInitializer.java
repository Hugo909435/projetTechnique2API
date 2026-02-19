package com.projet.api.config;

import com.projet.api.model.User;
import com.projet.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("test@example.com")) {
            User testUser = User.builder()
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Test")
                    .lastName("User")
                    .build();

            userRepository.save(testUser);
            log.info("Test user created: test@example.com / password123");
        }
    }
}
