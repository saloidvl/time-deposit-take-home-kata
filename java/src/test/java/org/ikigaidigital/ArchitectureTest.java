package org.ikigaidigital;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.ikigaidigital.domain.policy.InterestPolicy;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.ikigaidigital", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayersOrSpring = noClasses()
            .that().resideInAPackage("org.ikigaidigital.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.ikigaidigital.application..", "org.ikigaidigital.adapter..", "org.springframework..");

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

    // A policy that the calculator does not create would silently earn nothing for its plan (D19).
    @ArchTest
    static final ArchRule everyInterestPolicyIsCreatedInTheCalculator = classes()
            .that().implement(InterestPolicy.class)
            .should(beCreatedIn(TimeDepositCalculator.class));

    private static ArchCondition<JavaClass> beCreatedIn(Class<?> owner) {
        return new ArchCondition<>("be created in " + owner.getSimpleName()) {
            @Override
            public void check(JavaClass policy, ConditionEvents events) {
                boolean created = policy.getConstructorCallsToSelf().stream()
                        .anyMatch(call -> call.getOriginOwner().isEquivalentTo(owner));
                events.add(new SimpleConditionEvent(policy, created,
                        policy.getSimpleName() + " is not created in " + owner.getSimpleName()));
            }
        };
    }
}
