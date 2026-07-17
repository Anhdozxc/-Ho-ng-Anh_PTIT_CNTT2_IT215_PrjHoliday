package vn.edu.ptit.holidayplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class DemoDataProperties {
    private boolean seedDemoData = true;

    public boolean isSeedDemoData() {
        return seedDemoData;
    }

    public void setSeedDemoData(boolean seedDemoData) {
        this.seedDemoData = seedDemoData;
    }
}
