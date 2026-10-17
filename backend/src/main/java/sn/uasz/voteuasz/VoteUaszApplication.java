package sn.uasz.voteuasz;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class VoteUaszApplication {

    @PostConstruct
    public void init() {
        // Fuseau horaire officiel du Sénégal (GMT / UTC)
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Dakar"));
    }

    public static void main(String[] args) {
        SpringApplication.run(VoteUaszApplication.class, args);
    }
}
