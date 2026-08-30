package com.cguzowski.paymentcopilot;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.cguzowski.paymentcopilot", importOptions = ImportOption.DoNotIncludeTests.class)
class FeatureArchitectureTest {

    @ArchTest
    static final ArchRule knowledgeHasExplicitCatalogOrRetrievalOwnership = classes()
            .that()
            .resideInAPackage("..knowledge..")
            .should()
            .resideInAnyPackage("..knowledge.catalog..", "..knowledge.retrieval..");

    @ArchTest
    static final ArchRule incidentDoesNotDependOnDownstreamFeatures = noClasses()
            .that()
            .resideInAPackage("..incident..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..evidence..", "..knowledge.catalog..", "..knowledge.retrieval..");

    @ArchTest
    static final ArchRule evidenceDoesNotDependOnKnowledge = noClasses()
            .that()
            .resideInAPackage("..evidence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..knowledge.catalog..", "..knowledge.retrieval..");

    @ArchTest
    static final ArchRule catalogDoesNotDependOnWorkflowFeatures = noClasses()
            .that()
            .resideInAPackage("..knowledge.catalog..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..incident..", "..evidence..", "..knowledge.retrieval..");

    @ArchTest
    static final ArchRule onlyTheRetrievalAssemblerDependsOnIncidentOrEvidence = noClasses()
            .that()
            .resideInAPackage("..knowledge.retrieval..")
            .and()
            .doNotHaveSimpleName("KnowledgeRetrievalContextAssembler")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..incident..", "..evidence..");

    @ArchTest
    static final ArchRule evidencePersistenceAdaptersDoNotCrossFeatureBoundaries = noClasses()
            .that()
            .haveSimpleNameStartingWith("Postgres")
            .and()
            .resideInAPackage("..evidence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..incident..", "..knowledge.catalog..", "..knowledge.retrieval..")
            .because("the evidence persistence adapter must own only evidence storage and types");

    @ArchTest
    static final ArchRule catalogPersistenceAdaptersDoNotCrossFeatureBoundaries = noClasses()
            .that()
            .haveSimpleNameStartingWith("Postgres")
            .and()
            .resideInAPackage("..knowledge.catalog..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..incident..", "..evidence..", "..knowledge.retrieval..")
            .because("the catalog persistence adapter must own only catalog storage and types");

    @ArchTest
    static final ArchRule retrievalPersistenceAdaptersDoNotCrossWorkflowBoundaries = noClasses()
            .that()
            .haveSimpleNameStartingWith("Postgres")
            .and()
            .resideInAPackage("..knowledge.retrieval..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..incident..", "..evidence..")
            .because("a persistence adapter must own one feature's tables and domain types");

    @ArchTest
    static final ArchRule upstreamFeaturesDoNotDependOnReports = noClasses()
            .that()
            .resideInAnyPackage("..incident..", "..evidence..", "..knowledge.catalog..", "..knowledge.retrieval..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..report..");

    @ArchTest
    static final ArchRule reportPersistenceDoesNotDependOnUpstreamFeatureTypes = noClasses()
            .that()
            .haveSimpleNameStartingWith("Postgres")
            .and()
            .resideInAPackage("..report..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..incident..", "..evidence..", "..knowledge.catalog..", "..knowledge.retrieval..")
            .because("the report persistence adapter must own and query only report storage");
}
