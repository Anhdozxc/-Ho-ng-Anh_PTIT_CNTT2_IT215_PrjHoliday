package vn.edu.ptit.holidayplanner.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DemoDataProperties.class)
public class DemoDataConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock demoDataClock() {
        return Clock.systemDefaultZone();
    }
}
