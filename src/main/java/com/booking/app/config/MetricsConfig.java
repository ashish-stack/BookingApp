package com.booking.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration(proxyBeanMethods = false)
public class MetricsConfig {

    @Bean
    public ApplicationMetrics applicationMetrics(
            MeterRegistry meterRegistry
    ) {
        return new ApplicationMetrics(meterRegistry);
    }

    public static class ApplicationMetrics {

        public ApplicationMetrics(MeterRegistry meterRegistry) {
            meterRegistry.gauge(
                    "seat_reservation.application.status",
                    1
            );
        }
    }
}