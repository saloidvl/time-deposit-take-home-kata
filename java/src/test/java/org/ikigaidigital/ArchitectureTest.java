package org.ikigaidigital;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.or;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.ikigaidigital", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // The domain is domain.. plus the two classes that must stay in the root package.
    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndItself = classes()
            .that(or(resideInAPackage("org.ikigaidigital.domain.."), equivalentTo(TimeDeposit.class), equivalentTo(TimeDepositCalculator.class)))
            .should().onlyDependOnClassesThat(
                    or(resideInAPackage("java.."), resideInAPackage("org.ikigaidigital.domain.."),
                            equivalentTo(TimeDeposit.class), equivalentTo(TimeDepositCalculator.class)));

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdaptersWebOrJdbc = noClasses()
            .that().resideInAPackage("org.ikigaidigital.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.ikigaidigital.adapter..", "org.springframework.web..", "org.springframework.jdbc..");

    @ArchTest
    static final ArchRule controllerDoesNotDependOnPersistence = noClasses()
            .that().resideInAPackage("org.ikigaidigital.adapter.controller..")
            .should().dependOnClassesThat().resideInAPackage("org.ikigaidigital.adapter.persistence..");

    @ArchTest
    static final ArchRule persistenceDoesNotDependOnController = noClasses()
            .that().resideInAPackage("org.ikigaidigital.adapter.persistence..")
            .should().dependOnClassesThat().resideInAPackage("org.ikigaidigital.adapter.controller..");
}
