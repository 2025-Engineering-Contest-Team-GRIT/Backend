package grit.guidance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = "grit.guidance.domain")
public class GuidanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuidanceApplication.class, args);
	}

}
