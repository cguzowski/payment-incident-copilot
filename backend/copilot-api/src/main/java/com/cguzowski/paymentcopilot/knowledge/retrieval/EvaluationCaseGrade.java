package com.cguzowski.paymentcopilot.knowledge.retrieval;

record EvaluationCaseGrade(
        String caseId,
        int variantCount,
        boolean primaryRunbookSelected,
        boolean supportingPolicySelected,
        boolean primaryOutranksWeakMatch,
        boolean passed) {}
