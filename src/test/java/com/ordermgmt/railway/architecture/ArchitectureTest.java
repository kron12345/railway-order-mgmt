package com.ordermgmt.railway.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.ordermgmt.railway",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // === Layer Rules ===

    @ArchTest
    static final ArchRule layer_dependencies_are_respected =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Domain")
                    .definedBy("com.ordermgmt.railway.domain..")
                    .layer("UI")
                    .definedBy("com.ordermgmt.railway.ui..")
                    .layer("Infrastructure")
                    .definedBy("com.ordermgmt.railway.infrastructure..")
                    .optionalLayer("Api")
                    .definedBy("com.ordermgmt.railway.api..")
                    .optionalLayer("DTO")
                    .definedBy("com.ordermgmt.railway.dto..")
                    .optionalLayer("Mapper")
                    .definedBy("com.ordermgmt.railway.mapper..")
                    .whereLayer("UI")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Api")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Domain")
                    .mayOnlyBeAccessedByLayers("UI", "Infrastructure", "Api", "DTO", "Mapper")
                    .whereLayer("Infrastructure")
                    .mayOnlyBeAccessedByLayers("UI", "Api");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_ui =
            noClasses()
                    .that()
                    .resideInAPackage("com.ordermgmt.railway.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.ordermgmt.railway.ui..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("com.ordermgmt.railway.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.ordermgmt.railway.infrastructure..");

    // === Naming Conventions ===

    @ArchTest
    static final ArchRule repositories_should_be_suffixed =
            classes()
                    .that()
                    .resideInAPackage("..repository..")
                    .should()
                    .haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule services_should_be_suffixed =
            classes()
                    .that()
                    .resideInAPackage("..service..")
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .haveSimpleNameEndingWith("Service")
                    .orShould()
                    .haveSimpleNameEndingWith("Engine")
                    .orShould()
                    .haveSimpleNameEndingWith("Generator")
                    .orShould()
                    .haveSimpleNameEndingWith("Resolver");

    @ArchTest
    static final ArchRule views_should_be_suffixed =
            classes()
                    .that()
                    .resideInAPackage("..view..")
                    .and()
                    .areNotInterfaces()
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .haveSimpleNameEndingWith("View")
                    .orShould()
                    .haveSimpleNameEndingWith("Tab");

    // === Annotation Rules ===

    @ArchTest
    static final ArchRule repositories_should_be_interfaces =
            classes().that().resideInAPackage("..repository..").should().beInterfaces();

    @ArchTest
    static final ArchRule domain_models_must_not_use_field_injection =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..model..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("org.springframework.beans.factory.annotation..");
}
