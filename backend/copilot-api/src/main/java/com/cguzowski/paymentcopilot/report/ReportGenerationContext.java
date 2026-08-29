package com.cguzowski.paymentcopilot.report;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshot;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshot;

public record ReportGenerationContext(
        ReportInvestigationSnapshot investigation,
        ReportEvidenceSnapshot evidence,
        ReportKnowledgeSnapshot knowledge) {}
