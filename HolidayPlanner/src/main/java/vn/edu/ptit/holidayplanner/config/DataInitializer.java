package vn.edu.ptit.holidayplanner.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final DemoDataProperties properties;
    private final DemoDataService demoDataService;

    public DataInitializer(DemoDataProperties properties, DemoDataService demoDataService) {
        this.properties = properties;
        this.demoDataService = demoDataService;
    }

    @Override
    public void run(String... args) {
        if (properties.isSeedDemoData()) {
            demoDataService.seed();
        }
    }
}
