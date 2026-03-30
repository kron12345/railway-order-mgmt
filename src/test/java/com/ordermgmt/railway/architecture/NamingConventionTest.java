package com.ordermgmt.railway.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import jakarta.persistence.Entity;

import org.springframework.stereotype.Service;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.ordermgmt.railway",
        importOptions = ImportOption.DoNotIncludeTests.class)
class NamingConventionTest {

    @ArchTest
    static final ArchRule entities_should_reside_in_model_package =
            classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage("..model..");

    @ArchTest
    static final ArchRule services_should_be_annotated =
            classes()
                    .that()
                    .resideInAPackage("..domain..service..")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule dtos_should_reside_in_dto_package =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Dto")
                    .or()
                    .haveSimpleNameEndingWith("Request")
                    .or()
                    .haveSimpleNameEndingWith("Response")
                    .should()
                    .resideInAnyPackage("..dto..", "..model..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule mappers_should_reside_in_mapper_package =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Mapper")
                    .should()
                    .resideInAPackage("..mapper..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule configs_should_be_suffixed =
            classes()
                    .that()
                    .resideInAPackage("..config..")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .haveSimpleNameEndingWith("Config");
}
