package com.unibook.publisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

public class ModularityTests {
    ApplicationModules modules = ApplicationModules.of(PublisherApplication.class);

    @Test
    @DisplayName("Перевірка spring modulith та створення документації")
    void verifyModularArchitecture() {
        modules.forEach(System.out::println);
        modules.verify();
        generateDocumentation();
    }

    private void generateDocumentation() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
