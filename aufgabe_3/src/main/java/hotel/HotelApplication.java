package hotel;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootApplication
public class HotelApplication {

    Logger logger = LoggerFactory.getLogger(HotelApplication.class);

    public static void main(final String[] args) {
        SpringApplication.run(HotelApplication.class, args);
    }

    @PostConstruct
    public void init() {
        logger.info(ApplicationModules.of(getClass()).toString());
    }
}
