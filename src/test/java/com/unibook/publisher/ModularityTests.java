package com.unibook.publisher;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

public class ModularityTests {
    ApplicationModules modules = ApplicationModules.of(PublisherApplication.class);

    @Test
    void verifyModularArchitecture() {
        modules.forEach(System.out::println);
        modules.verify();
    }

    @Test
    void generateDocumentation() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
