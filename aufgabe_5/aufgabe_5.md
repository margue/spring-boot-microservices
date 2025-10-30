```
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesDddRules;

@AnalyzeClasses(packages = "hotel")
public class ArchUnitTest {

     @ArchTest
     ArchRule dddRules = JMoleculesDddRules.all();
}
```