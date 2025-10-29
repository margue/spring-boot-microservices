Hilfreiche Links und Snippets für Aufgabe 2

# Module in der Logausgabe

Link zur Dokumentation: https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.application-modules

```java
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
(...)
import org.springframework.modulith.core.ApplicationModules;

@SpringBootApplication
public class HotelApplication {
    Logger logger = LoggerFactory.getLogger(HotelApplication.class);

    (...)
    
    @PostConstruct
    public void init() {
        logger.info(ApplicationModules.of(getClass()).toString());
    }
}
```

# Testen der Modulabhängigkeiten

Link zur Dokumentation: https://docs.spring.io/spring-modulith/reference/verification.html

```java
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.modulith.core.ApplicationModules;

@AnalyzeClasses(packages = "hotel")
public class VerifyApplicationModulesTest {
    @ArchTest
    void verifyModularStructure(JavaClasses importedClasses) {
        ApplicationModules.of(HotelApplication.class).verify();
    }
}
```

# Kompilieren und Testen des Projekts

Das Projekt kann mit dem Maven Wrapper kompiliert und getestet werden:

```
./mvnw clean install
```