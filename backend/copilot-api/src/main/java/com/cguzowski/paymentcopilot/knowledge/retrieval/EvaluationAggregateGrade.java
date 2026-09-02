package com.cguzowski.paymentcopilot.knowledge.retrieval;

record EvaluationAggregateGrade(
        int expectedCaseCount,
        int expectedVariantCount,
        int actualVariantCount,
        int ineligibleCandidateCount,
        int primaryRunbookCasesPassed,
        int primaryRunbookCasesRequired,
        int supportingPolicyCasesPassed,
        int supportingPolicyCasesRequired,
        int primaryOutranksWeakCasesPassed,
        int primaryOutranksWeakApplicableCases,
        int primaryOutranksWeakCasesRequired,
        boolean partialSemanticsPassed,
        boolean unavailableSemanticsPassed,
        boolean supersededExclusionPassed,
        boolean structurePassed,
        boolean expectedVariantsPassed) {}
