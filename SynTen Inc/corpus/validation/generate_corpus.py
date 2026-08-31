from __future__ import annotations

import argparse
import html
import json
import re
from functools import partial
from pathlib import Path

from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.pdfgen.canvas import Canvas

from corpus_tools import InventoryDocument, parse_inventory


GENERATOR_VERSION = "synten-pdf-generator/v1"
TENANT_ID = "8b860d80-d17f-4e6b-8c48-af35f26a4d61"
APPROVER_ID = "7b636625-53d1-46f7-92a9-9c8c27a243d1"
INCIDENT_FAMILY = "AUTHORIZATION_DECLINE_RATE_SPIKE"
CLASSIFICATION = "Internal - Synthetic Demo"
SYNTHETIC_NOTICE = "Synthetic demonstration data only. No real payments, customers, merchants, credentials, or company records."

ROOT = Path(__file__).resolve().parents[3]
TENANT_ROOT = ROOT / "SynTen Inc"
CORPUS_ROOT = TENANT_ROOT / "corpus"
INVENTORY_PATH = CORPUS_ROOT / "inventory.md"
SOURCES = CORPUS_ROOT / "sources"
PDFS = CORPUS_ROOT / "pdfs"
TEMP = CORPUS_ROOT / "validation" / ".tmp"
SCENARIOS_PATH = ROOT / "syntheticIncidentGenerator" / "src" / "main" / "resources" / "scenarios" / "catalog.json"

REPLACEMENTS = {
    "RB-022": "RB-002 and RB-006",
    "PL-007": "PL-006",
    "PL-008": "PL-003",
}

OWNERS = {
    "RB-001": "Payment Operations",
    "RB-002": "Gateway Integration",
    "RB-003": "Gateway Capacity Management",
    "RB-004": "Authorization Service",
    "RB-005": "Issuer Integration",
    "RB-006": "Authorization Resilience",
    "RB-007": "Authorization Service",
    "RB-008": "Payment Routing",
    "RB-009": "Merchant Configuration",
    "RB-010": "Fraud Decision Services",
    "RB-011": "Network Operations",
    "RB-012": "Platform Security",
    "RB-013": "Authorization Service",
    "RB-014": "Platform Security",
    "RB-015": "Platform Security",
    "RB-016": "Cryptographic Services",
    "RB-017": "Gateway Integration",
    "RB-018": "Site Reliability Engineering",
    "RB-019": "Issuer Reference Data",
    "RB-020": "Token Services",
    "RB-021": "Authorization Product Operations",
    "RB-022": "Gateway Integration",
    "PL-001": "Incident Management",
    "PL-002": "Operational Assurance",
    "PL-003": "Operational Risk",
    "PL-004": "Knowledge Governance",
    "PL-005": "Data Protection",
    "PL-006": "Incident Communications",
    "PL-007": "Incident Communications",
    "PL-008": "Operational Risk",
}

RELATED = {
    "RB-001": "PL-001, PL-002, PL-003",
    "RB-002": "RB-006, RB-011, PL-006",
    "RB-003": "RB-002, PL-006",
    "RB-004": "RB-007, RB-013, PL-002",
    "RB-005": "RB-017, RB-019, PL-006",
    "RB-006": "RB-002, RB-007, PL-003",
    "RB-007": "RB-004, RB-006, PL-003",
    "RB-008": "RB-018, RB-019, PL-004",
    "RB-009": "PL-005, PL-002",
    "RB-010": "PL-003, PL-005",
    "RB-011": "RB-002, RB-014, PL-006",
    "RB-012": "RB-014, PL-005",
    "RB-013": "RB-004, RB-007, PL-002",
    "RB-014": "RB-011, RB-012, PL-005",
    "RB-015": "RB-016, PL-005",
    "RB-016": "RB-015, PL-005",
    "RB-017": "RB-005, PL-005",
    "RB-018": "RB-008, RB-011, PL-003",
    "RB-019": "RB-005, RB-008, PL-004",
    "RB-020": "PL-002, PL-005",
    "RB-021": "PL-003, PL-005",
    "RB-022": "RB-002, RB-006",
    "PL-001": "PL-002, PL-003, RB-001",
    "PL-002": "PL-001, PL-004, RB-001",
    "PL-003": "PL-001, PL-005",
    "PL-004": "PL-002, corpus/inventory.md",
    "PL-005": "PL-002, PL-003",
    "PL-006": "PL-001, PL-003, RB-002",
    "PL-007": "PL-006",
    "PL-008": "PL-003",
}

POLICY_CONTROLS = {
    "PL-001": [
        "Every alert must retain detected and received timestamps and enter the tenant-scoped work queue before investigation.",
        "Evidence collection, knowledge retrieval, report generation, and the human decision must remain separate auditable attempts.",
        "Only a schema-valid report may enter AWAITING_REVIEW; schema validity does not establish factual correctness.",
        "Approval or rejection must bind to the exact report under review and record an attributable synthetic operator and reason.",
    ],
    "PL-002": [
        "Every observation must identify its persisted evidence attempt, source, bounded window, collection status, and retrieval timestamp.",
        "PARTIAL, UNAVAILABLE, TIMED_OUT, malformed, empty, stale, and contradictory outcomes must remain visible and lower confidence.",
        "Retrieved guidance must be cited by immutable document version and chunk identity but must never be presented as incident proof.",
        "An evidence gap must not be filled with an assumed value, a fabricated event, or a result from another tenant or investigation.",
    ],
    "PL-003": [
        "The copilot may propose observations, inferences, probable cause, confidence, and a recommendation; it may not approve any of them.",
        "No model output may retry a payment, reroute traffic, change configuration, rotate credentials, restart a service, or contact a third party.",
        "A human reviewer must compare citations with the displayed evidence and approved knowledge before approval or rejection.",
        "Rejected reports remain immutable and auditable; rejection must not be rewritten as model failure or silent regeneration.",
    ],
    "PL-004": [
        "Only APPROVED, effective, tenant-matching, incident-family-matching versions may enter lexical or vector ranking.",
        "A source, metadata, extraction, or embedding-input change creates a new version and requires new hashes and embeddings.",
        "SUPERSEDED material must remain auditable but must be excluded before ranking, even on an exact lexical match.",
        "Every document must have an owner, review cadence, revision history, source hash, and approved replacement where applicable.",
    ],
    "PL-005": [
        "Only opaque synthetic identifiers and bounded aggregates may appear in sources, prompts, logs, reports, or escalation packages.",
        "Credentials, private endpoints, raw gateway payloads, card data, personal data, and plausible merchant data are prohibited.",
        "Examples must be labeled synthetic and must not reproduce confidential or proprietary material from a real organization.",
        "Security validation must never be bypassed to improve availability or make a demonstration succeed.",
    ],
    "PL-006": [
        "Escalation packages must state the incident, route or cohort, UTC window, exact error categories, aggregate counts, and evidence limitations.",
        "The receiving owner must independently confirm the condition before any technical or external communication is treated as authoritative.",
        "Third-party contact must exclude credentials, raw payloads, private endpoints, and unreviewed model conclusions.",
        "Status updates must distinguish observed facts, working hypotheses, decisions, actions, owners, and next checkpoints.",
    ],
    "PL-007": [
        "Legacy emergency routing guidance required immediate coordination but is no longer the current communication authority.",
        "Historical routing terminology is retained only for audit and retrieval-exclusion testing.",
        "Current escalation and routing communication requirements are defined by PL-006.",
    ],
    "PL-008": [
        "Legacy automation language is retained only as a superseded governance record.",
        "Current policy prohibits model approval and automatic operational action.",
        "Human review and operational authority are now governed by PL-003.",
    ],
}


def scenarios_by_code() -> dict[str, dict]:
    payload = json.loads(SCENARIOS_PATH.read_text(encoding="utf-8"))
    return {scenario["code"]: scenario for scenario in payload["scenarios"]}


def scenario_codes(document: InventoryDocument, scenarios: dict[str, dict]) -> list[str]:
    if document.key == "RB-001" or document.type == "POLICY":
        return []
    return [code for code in re.findall(r"S\d{3}", document.scenario_coverage) if code in scenarios]


def error_codes(document: InventoryDocument, scenarios: dict[str, dict]) -> list[str]:
    values = {
        error["errorCode"]
        for code in scenario_codes(document, scenarios)
        for error in scenarios[code].get("evidence", {}).get("errors", [])
    }
    return sorted(values)


def metadata(document: InventoryDocument, codes: list[str]) -> dict[str, str]:
    superseded = document.status == "SUPERSEDED"
    return {
        "documentKey": document.key,
        "documentId": document.document_id,
        "tenantId": TENANT_ID,
        "type": document.type,
        "title": document.title,
        "version": document.version,
        "incidentFamily": INCIDENT_FAMILY,
        "appliesTo": document.applies_to,
        "approvalStatus": document.status,
        "approvedBy": APPROVER_ID,
        "approvedAt": "2026-06-01T08:00:00Z" if superseded else "2026-08-30T08:00:00Z",
        "effectiveAt": "2026-06-02T09:00:00Z" if superseded else "2026-08-30T09:00:00Z",
        "ownerRole": OWNERS[document.key],
        "classification": CLASSIFICATION,
        "replacement": REPLACEMENTS.get(document.key, "None"),
        "requiredCodes": ",".join(codes) if codes else "None",
        "relatedDocuments": RELATED[document.key],
        "generatorVersion": GENERATOR_VERSION,
    }


def seed_source(document: InventoryDocument, scenarios: dict[str, dict]) -> str:
    codes = error_codes(document, scenarios)
    control = metadata(document, codes)
    lines = ["---"] + [f"{key}: {value}" for key, value in control.items()] + ["---", f"# {document.title}"]
    if document.type == "RUNBOOK":
        lines.extend(runbook_body(document, scenarios, codes))
    else:
        lines.extend(policy_body(document))
    return "\n".join(lines).rstrip() + "\n"


def runbook_body(document: InventoryDocument, scenarios: dict[str, dict], codes: list[str]) -> list[str]:
    selected = [scenarios[code] for code in scenario_codes(document, scenarios)]
    lines = [
        "",
        "[[PAGEBREAK]]",
        "## Document control",
        "",
        "| Control | Value |",
        "|---|---|",
        f"| Owner | {OWNERS[document.key]} |",
        "| Audience | Payment Operations Analyst, Incident Commander, and named technical owner |",
        "| Review cadence | Every 180 days and after a material synthetic scenario change |",
        f"| Related documents | {RELATED[document.key]} |",
        f"| Retrieval role | {document.retrieval_role} |",
        "| Classification | Internal - Synthetic Demo |",
        "",
        "### Revision history",
        "",
        "| Version | Status | Date | Summary |",
        "|---|---|---|---|",
        f"| {document.version} | {document.status} | 2026-08-30 | Controlled corpus version for authorization-decline investigation. |",
        "",
        "## 1. Purpose and scope",
        "",
        f"This runbook guides a human analyst investigating {document.applies_to.lower()}. Its operational purpose is: {document.retrieval_role} It applies only to persisted SynTen Inc synthetic evidence for the current tenant and investigation.",
        "",
        "The runbook does not establish that a described failure occurred. An alert is a signal; approved knowledge is guidance; probable cause remains an inference that must be supported by cited evidence.",
        "",
        "## 2. Entry conditions and authority",
        "",
        "- Confirm the incident is INVESTIGATING and the tenant and investigation identifiers match the evidence request.",
        "- Record the alert window, evidence observation window, collection status, source, and retrieval timestamp before interpretation.",
        "- Stop and preserve the gap when evidence is unavailable, malformed, stale, cross-tenant, or outside the incident window.",
        "- Use this document to diagnose and prepare an escalation. The copilot may not execute recovery, approve a report, or contact an owner.",
        "",
        "## 3. Evidence prerequisites",
        "",
        "| Evidence | Minimum check | Why it matters |",
        "|---|---|---|",
        "| Service-error observation | Persisted status, service name, bounded UTC window, exact codes, and aggregate counts | Separates observed facts from a generic runbook match. |",
        "| Alert context | Detected time, received time, duration, affected opaque route or cohort, and severity | Establishes the comparison window and operational scope. |",
        "| Knowledge context | Approved document version, chunk identity, and retrieval status | Makes later recommendations traceable without treating guidance as proof. |",
        "| Independent confirmation | Named source or owner when the runbook calls for it | Prevents a single synthetic signal from becoming an unsupported conclusion. |",
        "",
    ]
    if document.key == "RB-001":
        lines.extend(generic_triage_sections())
    else:
        lines.extend(signal_sections(selected, codes))
    lines.extend(
        [
            "",
            "## 7. Failure and uncertainty handling",
            "",
            "- AVAILABLE with an empty error list is valid negative evidence for that exact source and window; it is not an unavailable result.",
            "- PARTIAL means missing partitions may contain confirming or contradictory observations. State the missing scope and use LOW confidence unless independent evidence closes it.",
            "- UNAVAILABLE or TIMED_OUT means no technical cause can be concluded from this source. Preserve the attempt and seek an approved independent source.",
            "- Contradictory timestamps, routes, instance groups, or error ordering must appear in the report as contradictions, not be averaged away.",
            "- A retrieved weak match may suggest a check, but it cannot override stronger observed evidence or an approval-status filter.",
            "",
            "## 8. Escalation package",
            "",
            f"Escalate to {OWNERS[document.key]} with the incident and investigation identifiers, bounded UTC window, service name, exact error codes and counts, affected opaque route or cohort, evidence status, selected knowledge references, contradictions, and the next requested human confirmation.",
            "",
            "Do not include credentials, raw provider payloads, private endpoints, plausible merchant data, or an unreviewed model conclusion. Record the receiving owner and next checkpoint; sending the package remains a human action outside the copilot.",
            "",
            "## 9. Validation and closure checks",
            "",
            "1. Recollect the same approved evidence source only through a separately authorized operator action and compare equivalent windows.",
            "2. Confirm whether the named error categories and decline-rate signal have returned toward the synthetic baseline.",
            "3. Record any separately authorized change, its owner, validation result, and rollback decision without attributing it to the model.",
            "4. Generate a report only from persisted evidence and retrieval snapshots; review every citation before approval or rejection.",
            "5. Retain failed attempts, negative evidence, and superseded hypotheses in the audit timeline.",
            "",
            "## 10. Related documents",
            "",
            f"Use {RELATED[document.key]} for adjacent diagnostic or governance requirements. Only approved and effective versions are retrieval eligible.",
            "",
            "## 11. Required investigation record",
            "",
            "Complete the following record in the incident workspace before requesting review. Use explicit `Not observed`, `Unavailable`, or `Not applicable` values instead of leaving fields blank.",
            "",
            "| Record group | Required entry | Reviewer checkpoint |",
            "|---|---|---|",
            "| Correlation | Tenant ID, incident ID, investigation ID, alert ID, and UTC observation window | All identifiers resolve to the same tenant-scoped investigation. |",
            "| Evidence | Source, collection status, retrieval time, exact codes, aggregate counts, and missing partitions | Observations are distinguishable from unavailable or partial evidence. |",
            "| Knowledge | Document key, version, approval status, retrieval query, and selected chunk references | Every cited item was approved and effective at retrieval time. |",
            "| Assessment | Observed facts, candidate inference, confidence, alternatives, contradictions, and limitations | The inference does not exceed the cited evidence. |",
            "| Handoff | Receiving owner, requested confirmation, next checkpoint, and escalation time | The request is bounded and no operational action is attributed to the copilot. |",
            "| Review | Handoff state (`READY FOR REVIEW`, `INSUFFICIENT EVIDENCE`, or `ESCALATED FOR CONFIRMATION`), reviewer identity and time, accepted or rejected citations, disposition, and follow-up owner | Approval and operational action remain human decisions outside this runbook. |",
        ]
    )
    return lines


def generic_triage_sections() -> list[str]:
    return [
        "## 4. Triage decision flow",
        "",
        "1. Verify tenant, incident, and investigation correlation before reading any technical signal.",
        "2. Compare alert detection and duration with the evidence observation window; mark non-overlap as a limitation.",
        "3. Classify evidence as positive observation, valid negative evidence, partial coverage, unavailable, timed out, or malformed.",
        "4. Group exact error codes by dependency family: gateway, service state, routing, network, security, cryptography, response mapping, or cohort configuration.",
        "5. Retrieve approved guidance for the strongest observed family and retain weak or conflicting candidates for reviewer context.",
        "6. Stop with INSUFFICIENT_EVIDENCE when no approved source supports a probable cause and recommendation.",
        "",
        "## 5. Evidence status matrix",
        "",
        "| Status | Analyst treatment | Report effect |",
        "|---|---|---|",
        "| AVAILABLE with errors | Cite exact codes, counts, source, and time window. | May support an observation and bounded inference. |",
        "| AVAILABLE and empty | Record successful negative evidence. | Lowers support for causes expected in that window. |",
        "| PARTIAL | State returned and missing partitions. | Preserve gap; normally lower confidence. |",
        "| UNAVAILABLE or TIMED_OUT | Retain the failed attempt and status detail. | No cause from that source; seek independent evidence. |",
        "| Malformed | Reject payload as evidence and retain validation failure. | Never repair or infer missing values. |",
        "",
        "## 6. Handoff checkpoint",
        "",
        "The analyst hands off a fact/inference split: observed source results, candidate dependency family, contradictory or missing evidence, approved runbook/policy references, and the next named owner confirmation. No handoff may claim remediation has occurred.",
    ]


def signal_sections(selected: list[dict], codes: list[str]) -> list[str]:
    lines = ["## 4. Signal interpretation", "", "Exact machine codes in scope:", ""]
    for code in codes:
        lines.append(f"- `{code}`")
    lines.extend(["", "| Exact signal | Bounded interpretation | Required caution |", "|---|---|---|"])
    for code in codes:
        matching = [item for item in selected if code in {error["errorCode"] for error in item.get("evidence", {}).get("errors", [])}]
        cause = matching[0]["truth"]["rootCause"] if matching else "The signal belongs to this dependency family."
        lines.append(f"| `{code}` | {cause} | Treat as observed only when the persisted window contains the code; the cause remains an inference. |")
    lines.extend(["", "## 5. Diagnostic procedure", ""])
    steps = [
        "Anchor the analysis to the alert and observation windows. Reject a causal ordering that the timestamps do not support.",
        "Confirm the service is `payment-authorization` and group counts by exact code, route or cohort, and observation time.",
        "Compare co-occurring signals before preferring a single-component explanation; preserve absent expected signals as negative evidence.",
        "Check whether the evidence status is AVAILABLE or PARTIAL and identify any missing partition, region, route, or instance group.",
        "Request the named independent owner or approved source needed to confirm the candidate cause; do not manufacture unavailable context.",
        "Record alternative explanations whose expected signals are missing, weak, or contradictory.",
    ]
    for index, step in enumerate(steps, start=1):
        lines.append(f"{index}. {step}")
    lines.extend(["", "## 6. Scenario decision matrix", "", "| Scenario | Severity | Observed signal | Candidate explanation and reviewer checkpoint |", "|---|---|---|---|"])
    for scenario in selected:
        scenario_codes_text = ", ".join(error["errorCode"] for error in scenario.get("evidence", {}).get("errors", [])) or "No returned error observations"
        lines.append(
            f"| {scenario['code']} | {scenario['severity']} | {scenario_codes_text} | {scenario['truth']['rootCause']} Confirm the stated required evidence before using this as probable cause. |"
        )
    return lines


def policy_body(document: InventoryDocument) -> list[str]:
    controls = POLICY_CONTROLS[document.key]
    lines = [
        "",
        "[[PAGEBREAK]]",
        "## Document control",
        "",
        "| Control | Value |",
        "|---|---|",
        f"| Owner | {OWNERS[document.key]} |",
        "| Audience | Payment Operations, Incident Management, technical owners, and reviewers |",
        "| Review cadence | Every 180 days and after a material control or corpus change |",
        f"| Related documents | {RELATED[document.key]} |",
        f"| Retrieval role | {document.retrieval_role} |",
        "| Classification | Internal - Synthetic Demo |",
        "",
        "### Revision history",
        "",
        "| Version | Status | Date | Summary |",
        "|---|---|---|---|",
        f"| {document.version} | {document.status} | 2026-08-30 | Controlled corpus version for authorization-decline governance. |",
        "",
        "## 1. Purpose",
        "",
        f"This policy establishes mandatory SynTen Inc controls for {document.applies_to.lower()}. It supports auditable investigation of the synthetic authorization-decline incident family while preserving evidence integrity and human authority.",
        "",
        "## 2. Scope",
        "",
        "The policy applies to synthetic alerts, evidence attempts, approved knowledge, retrieval context, AI-assisted reports, human decisions, escalation records, and audit projections associated with the tenant ID shown on the cover.",
        "",
        "It does not authorize real payment processing, access to real customer or merchant data, production remediation, external communication, or model approval of an operational decision.",
        "",
        "## 3. Governing principles",
        "",
        "- Observed facts, inference, approved guidance, recommendation, and human decision must remain distinguishable.",
        "- Tenant, source, version, time, status, model, retrieval, and decision metadata must remain reviewable.",
        "- Missing or contradictory information must reduce confidence rather than invite fabrication.",
        "- No recommendation executes automatically; human authority remains outside the model boundary.",
        "",
        "## 4. Mandatory controls",
        "",
    ]
    for index, control in enumerate(controls, start=1):
        lines.append(f"{index}. {control}")
    lines.extend(
        [
            "",
            "## 5. Roles and responsibilities",
            "",
            "| Role | Responsible | Accountable | Consulted or informed |",
            "|---|---|---|---|",
            f"| {OWNERS[document.key]} | Maintain this policy and control evidence. | Approve content through the synthetic role account. | Incident Management and affected technical owners. |",
            "| Payment Operations Analyst | Apply the policy during investigation and record limitations. | Accountable for the submitted human review decision. | Incident Commander and evidence owners. |",
            "| Technical owner | Confirm component-specific facts and authorize separate actions. | Accountable for changes within that owner's controlled system. | Payment Operations and security where applicable. |",
            "| Knowledge Approver | Verify version, status, classification, and retrieval eligibility. | Accountable for approved/effective publication. | Document owner and Operational Assurance. |",
            "",
            "## 6. Evidence and records",
            "",
            "Retain the authoritative incident, evidence attempt, retrieval attempt, selected chunk references, report attempt, model and prompt metadata, human decision, and chronological audit projection. Do not replace a failed or superseded record with a cleaner narrative.",
            "",
            "Control evidence must use opaque synthetic identifiers and UTC timestamps. Raw credentials, private endpoints, plausible personal or payment data, and unbounded provider payloads are prohibited.",
            "",
            "## 7. Exceptions and non-compliance",
            "",
            "There is no exception allowing a model to approve a report, execute a recommendation, use cross-tenant data, or conceal missing evidence. A human may document an operational exception outside the copilot only through the separately governed process and must preserve its owner, rationale, scope, and expiry.",
            "",
            "A detected policy breach must be recorded as a limitation, escalated to the owner, and excluded from trusted report support until corrected. Do not silently relabel an ineligible document or malformed source as approved evidence.",
            "",
            "## 8. Monitoring and review",
            "",
            "Review retrieval eligibility, source/version hashes, unexplained ranking changes, missing citations, rejected reports, evidence gaps, and attempted automatic actions. Material findings require policy review and a new version rather than an in-place rewrite.",
            "",
            "## 9. Related documents",
            "",
            f"Apply this policy with {RELATED[document.key]}. Where guidance conflicts, use only the approved and effective version and preserve the conflict for audit.",
        ]
    )
    return lines


def parse_source(path: Path) -> tuple[dict[str, str], list[str]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    if not lines or lines[0] != "---":
        raise ValueError(f"Source has no front matter: {path}")
    end = lines.index("---", 1)
    metadata_values: dict[str, str] = {}
    for line in lines[1:end]:
        key, value = line.split(":", 1)
        metadata_values[key.strip()] = value.strip()
    return metadata_values, lines[end + 1 :]


def styles() -> dict[str, ParagraphStyle]:
    base = getSampleStyleSheet()
    return {
        "cover_company": ParagraphStyle("cover_company", parent=base["Title"], fontName="Helvetica-Bold", fontSize=16, leading=20, textColor=colors.HexColor("#16697A"), alignment=TA_CENTER, spaceAfter=12),
        "cover_title": ParagraphStyle("cover_title", parent=base["Title"], fontName="Helvetica-Bold", fontSize=24, leading=29, textColor=colors.HexColor("#17324D"), alignment=TA_CENTER, spaceAfter=20),
        "h1": ParagraphStyle("h1", parent=base["Heading1"], fontName="Helvetica-Bold", fontSize=16, leading=20, textColor=colors.HexColor("#17324D"), spaceBefore=10, spaceAfter=7),
        "h2": ParagraphStyle("h2", parent=base["Heading2"], fontName="Helvetica-Bold", fontSize=12.5, leading=16, textColor=colors.HexColor("#16697A"), spaceBefore=9, spaceAfter=5, keepWithNext=True),
        "h3": ParagraphStyle("h3", parent=base["Heading3"], fontName="Helvetica-Bold", fontSize=10.5, leading=13, textColor=colors.HexColor("#34495E"), spaceBefore=7, spaceAfter=4, keepWithNext=True),
        "body": ParagraphStyle("body", parent=base["BodyText"], fontName="Helvetica", fontSize=9.5, leading=13, textColor=colors.HexColor("#243746"), spaceAfter=6),
        "bullet": ParagraphStyle("bullet", parent=base["BodyText"], fontName="Helvetica", fontSize=9.2, leading=12.5, leftIndent=12, firstLineIndent=-7, bulletIndent=2, spaceAfter=4),
        "small": ParagraphStyle("small", parent=base["BodyText"], fontName="Helvetica", fontSize=7.4, leading=9.2, textColor=colors.HexColor("#405261")),
        "notice": ParagraphStyle("notice", parent=base["BodyText"], fontName="Helvetica-Bold", fontSize=9, leading=12, alignment=TA_CENTER, textColor=colors.HexColor("#8A3B12"), borderColor=colors.HexColor("#E8A87C"), borderWidth=0.8, borderPadding=8, backColor=colors.HexColor("#FFF7ED")),
    }


def rich(value: str) -> str:
    escaped = html.escape(value)
    return re.sub(r"`([^`]+)`", r'<font name="Courier">\1</font>', escaped)


def markdown_story(lines: list[str], style: dict[str, ParagraphStyle]) -> list:
    story: list = []
    index = 0
    paragraph: list[str] = []

    def flush() -> None:
        if paragraph:
            story.append(Paragraph(rich(" ".join(paragraph)), style["body"]))
            paragraph.clear()

    while index < len(lines):
        line = lines[index].rstrip()
        if line == "[[PAGEBREAK]]":
            flush()
            story.append(PageBreak())
        elif not line:
            flush()
        elif line.startswith("# "):
            flush()
        elif line.startswith("## "):
            flush()
            story.append(Paragraph(rich(line[3:]), style["h2"]))
        elif line.startswith("### "):
            flush()
            story.append(Paragraph(rich(line[4:]), style["h3"]))
        elif line.startswith("|"):
            flush()
            table_lines: list[str] = []
            while index < len(lines) and lines[index].startswith("|"):
                table_lines.append(lines[index])
                index += 1
            index -= 1
            rows = [[cell.strip() for cell in table_line.strip("|").split("|")] for table_line in table_lines]
            if len(rows) > 1 and all(re.fullmatch(r"[-:]+", cell) for cell in rows[1]):
                rows.pop(1)
            rendered = [[Paragraph(rich(cell), style["small"]) for cell in row] for row in rows]
            available = A4[0] - 34 * mm
            weights = [1.0] * len(rendered[0])
            if len(weights) == 2:
                weights = [0.32, 0.68]
            elif len(weights) == 3:
                weights = [0.30, 0.35, 0.35]
            elif len(weights) == 4:
                weights = [0.12, 0.12, 0.32, 0.44]
            widths = [available * weight / sum(weights) for weight in weights]
            table = Table(rendered, colWidths=widths, repeatRows=1, hAlign="LEFT")
            table.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#DDEFF2")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#17324D")),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#AFC4CC")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F7FAFB")]),
            ]))
            story.extend([table, Spacer(1, 6)])
        elif re.match(r"^\d+\. ", line):
            flush()
            number, text = line.split(". ", 1)
            story.append(Paragraph(rich(text), style["bullet"], bulletText=f"{number}."))
        elif line.startswith("- "):
            flush()
            story.append(Paragraph(rich(line[2:]), style["bullet"], bulletText="-"))
        else:
            paragraph.append(line)
        index += 1
    flush()
    return story


def cover_story(document: InventoryDocument, values: dict[str, str], style: dict[str, ParagraphStyle]) -> list:
    accent = colors.HexColor("#B94A48") if document.status == "SUPERSEDED" else colors.HexColor("#16697A")
    rows = [
        ["Document key", document.key],
        ["Document ID", document.document_id],
        ["Version / type", f"{document.version} / {document.type}"],
        ["Status", document.status],
        ["Owner", values["ownerRole"]],
        ["Effective", values["effectiveAt"]],
        ["Incident family", INCIDENT_FAMILY],
        ["Classification", CLASSIFICATION],
    ]
    table = Table([[Paragraph(f"<b>{rich(left)}</b>", style["small"]), Paragraph(rich(right), style["small"])] for left, right in rows], colWidths=[42 * mm, 105 * mm])
    table.setStyle(TableStyle([
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#B7C8CE")),
        ("BACKGROUND", (0, 0), (0, -1), colors.HexColor("#EAF3F5")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story = [
        Spacer(1, 30 * mm),
        Paragraph("SYNTEN INC", style["cover_company"]),
        Paragraph(rich(document.title), style["cover_title"]),
        Spacer(1, 5 * mm),
        table,
        Spacer(1, 12 * mm),
    ]
    if document.status == "SUPERSEDED":
        story.append(Paragraph(f"SUPERSEDED - NOT RETRIEVAL ELIGIBLE<br/>Approved replacement: {rich(values['replacement'])}", ParagraphStyle("superseded_cover", parent=style["notice"], textColor=accent, borderColor=accent, backColor=colors.HexColor("#FFF1F0"))))
        story.append(Spacer(1, 5 * mm))
    story.append(Paragraph(SYNTHETIC_NOTICE, style["notice"]))
    return story


def render_pdf(document: InventoryDocument, source: Path, output: Path) -> None:
    values, body = parse_source(source)
    style = styles()

    def story() -> list:
        return cover_story(document, values, style) + markdown_story(body, style)

    TEMP.mkdir(parents=True, exist_ok=True)
    draft = TEMP / f"{output.stem}.draft.pdf"
    build_document(draft, document, values, story(), total_pages=None)
    total = len(PdfReader(draft).pages)
    build_document(output, document, values, story(), total_pages=total)
    draft.unlink(missing_ok=True)


def build_document(path: Path, document: InventoryDocument, values: dict[str, str], story: list, total_pages: int | None) -> None:
    doc = BaseDocTemplate(str(path), pagesize=A4, leftMargin=17 * mm, rightMargin=17 * mm, topMargin=23 * mm, bottomMargin=19 * mm, title=document.title, author="SynTen Inc", invariant=1)
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="body")

    def decorate(canvas, current_doc) -> None:
        canvas.saveState()
        page = current_doc.page
        width, height = A4
        canvas.setStrokeColor(colors.HexColor("#B7C8CE"))
        canvas.setLineWidth(0.5)
        canvas.line(17 * mm, height - 16 * mm, width - 17 * mm, height - 16 * mm)
        canvas.setFont("Helvetica-Bold", 7.5)
        canvas.setFillColor(colors.HexColor("#17324D"))
        canvas.drawString(17 * mm, height - 12.5 * mm, f"{document.key} | {document.title[:58]}")
        canvas.drawRightString(width - 17 * mm, height - 12.5 * mm, f"v{document.version} | {CLASSIFICATION}")
        if document.status == "SUPERSEDED":
            canvas.setFillColor(colors.HexColor("#FFF1F0"))
            canvas.rect(17 * mm, height - 22 * mm, width - 34 * mm, 5 * mm, stroke=0, fill=1)
            canvas.setFillColor(colors.HexColor("#B42318"))
            canvas.setFont("Helvetica-Bold", 8)
            canvas.drawCentredString(width / 2, height - 20.2 * mm, f"SUPERSEDED - NOT RETRIEVAL ELIGIBLE | Approved replacement: {values['replacement']}")
        canvas.setStrokeColor(colors.HexColor("#B7C8CE"))
        canvas.line(17 * mm, 13 * mm, width - 17 * mm, 13 * mm)
        canvas.setFont("Helvetica", 7.5)
        canvas.setFillColor(colors.HexColor("#405261"))
        canvas.drawString(17 * mm, 9 * mm, "SynTen Inc - Internal - Synthetic Demo")
        count = str(total_pages) if total_pages is not None else "?"
        canvas.drawRightString(width - 17 * mm, 9 * mm, f"{document.document_id} | Page {page} of {count}")
        canvas.restoreState()

    doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=decorate)])
    doc.build(story, canvasmaker=partial(Canvas, invariant=1))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-sources", action="store_true", help="Create or replace the maintained Markdown sources.")
    args = parser.parse_args()
    documents = parse_inventory(INVENTORY_PATH)
    scenarios = scenarios_by_code()
    SOURCES.mkdir(parents=True, exist_ok=True)
    PDFS.mkdir(parents=True, exist_ok=True)
    for document in documents:
        source = SOURCES / document.source_filename
        if args.seed_sources:
            source.write_text(seed_source(document, scenarios), encoding="utf-8", newline="\n")
        if not source.is_file():
            raise SystemExit(f"Missing source: {source}")
        output = PDFS / document.filename
        render_pdf(document, source, output)
        print(f"generated {document.key}: {output.name} ({len(PdfReader(output).pages)} pages)")


if __name__ == "__main__":
    main()
