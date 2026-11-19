package edu.cit.alvarado.phishmenot;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PhishMeNotApplication {

    public static void main(String[] args) {
        // All timestamps (created_at, last_login_at) are stored and returned in UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PhishMeNotApplication.class, args);
    }
}
