package dev.leocamacho.demo.tests.structure;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static dev.leocamacho.demo.tests.structure.ArchUnitFacadeConstrains.*;
import static dev.leocamacho.demo.tests.structure.StructureValidationTests.BASE;

@AnalyzeClasses(packages = BASE)
public class StructureValidationTests {
    public static final String BASE = "dev.leocamacho";

    @ArchTest
    static final ArchRule validateControllerClasses =
            classes().that().haveNameMatching(".*Controller")
                    .and().resideInAPackage("..api.rests..")
                    .should(haveSpecifiedAnnotations(RestController.class, RequestMapping.class))
                    .andShould(haveMaxPublicMethods(7))
                    .andShould(haveSpecifiedReturnTypes("api.types"))
                    .andShould(haveSpecifiedParameterTypes("api.types"))
                    .andShould(haveSpecifiedMethodAnnotations(GetMapping.class, PutMapping.class, DeleteMapping.class, PatchMapping.class, RequestMapping.class))
                    .as("Controller classes should have the specified annotations, methods and parameters");

    @ArchTest
    static final ArchRule validateHandlersClasses =
            classes().that().haveNameMatching(".*HandlerImpl")
                    .and().resideInAPackage("..commands.impl..")
                    .should(haveAnyAnnotations(Component.class, Service.class))
                    .andShould(haveMaxPublicMethods(1))
                    .andShould(haveSpecifiedReturnTypes("commands", "java"))
                    .andShould(haveSpecifiedParameterTypes("jpa.entities", "commands"))
                    .andShould(haveImplementedFrom("commands"))
                    .as("Handlers classes should have the specified annotations, methods and parameters");

    @ArchTest
    static final ArchRule validateRepositoriesClasses =
            classes().that().haveNameMatching(".*Repository")
                    .and().resideInAPackage("..jpa.repositories..")
                    .should(haveAnyAnnotations(Repository.class))
                    .as("Repositories classes should have the specified annotations");


    @Test
    public void givenApplicationClasses_thenNoLayerViolationsShouldExist() {

        JavaClasses jc = new ClassFileImporter().importPackages(BASE);

        Architectures.LayeredArchitecture arch = layeredArchitecture().consideringAllDependencies()
                .layer("api").definedBy("..api.rests..", "..api.types..", "..api.exceptions..")
                .layer("handlers").definedBy("..handlers.commands..", "..handlers.queries..")
                .layer("security").definedBy("..security..")
                .layer("repositories").definedBy("..repositories..")
                .layer("test").definedBy("..tests..")

                .whereLayer("api").mayOnlyBeAccessedByLayers("security", "test")
                .whereLayer("handlers").mayOnlyBeAccessedByLayers("api",  "test")
                .whereLayer("repositories").mayOnlyBeAccessedByLayers("handlers", "security", "test");

        arch.check(jc);
    }
}