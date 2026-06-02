package app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SeedService seedService;

    public DataInitializer(SeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(String... args) {
        seedService.seed();
    }
}
