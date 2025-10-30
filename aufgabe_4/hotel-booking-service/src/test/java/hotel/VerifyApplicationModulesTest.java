package hotel;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.modulith.core.ApplicationModules;


@AnalyzeClasses(packages = "hotel")
public class VerifyApplicationModulesTest {

    @ArchTest
    void verifyModularStructure(JavaClasses importedClasses) {
        ApplicationModules modules = ApplicationModules.of(HotelApplication.class);
        modules.verify();
    }
}

