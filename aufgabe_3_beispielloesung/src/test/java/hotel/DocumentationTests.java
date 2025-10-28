package hotel;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class DocumentationTests {

    ApplicationModules modules = ApplicationModules.of(HotelApplication.class);

    @Test
    void writeDocumentationSnippets() {

        new Documenter(modules)
                .writeDocumentation();
    }
}
