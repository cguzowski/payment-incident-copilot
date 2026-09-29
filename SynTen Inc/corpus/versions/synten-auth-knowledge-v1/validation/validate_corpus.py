from __future__ import annotations

import json
import re
from datetime import datetime, timezone
from pathlib import Path

import pdfplumber

from corpus_tools import ValidationError, parse_inventory, sha256, validate_pdf
from generate_corpus import (
    APPROVER_ID,
    CLASSIFICATION,
    CORPUS_ROOT,
    GENERATOR_VERSION,
    INCIDENT_FAMILY,
    INVENTORY_PATH,
    PDFS,
    REPLACEMENTS,
    SOURCES,
    SYNTHETIC_NOTICE,
    TENANT_ID,
    error_codes,
    parse_source,
    scenarios_by_code,
)


MANIFEST = CORPUS_ROOT / "validation-manifest.json"
SENSITIVE_PATTERNS = {
    "private key": re.compile(r"BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY", re.IGNORECASE),
    "password assignment": re.compile(r"password\s*[:=]", re.IGNORECASE),
    "secret assignment": re.compile(r"secret\s*[:=]", re.IGNORECASE),
    "API key assignment": re.compile(r"api[_-]?key\s*[:=]", re.IGNORECASE),
    "PAN-like number": re.compile(r"\b(?:\d[ -]*?){13,19}\b"),
    "IP address": re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b"),
    "URL": re.compile(r"https?://", re.IGNORECASE),
}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def validate_source(document, source: Path, required_codes: list[str], replacement: str | None) -> dict[str, str]:
    values, body = parse_source(source)
    expected = {
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
        "ownerRole": values.get("ownerRole", ""),
        "classification": CLASSIFICATION,
        "replacement": replacement or "None",
        "requiredCodes": ",".join(required_codes) if required_codes else "None",
        "generatorVersion": GENERATOR_VERSION,
    }
    for key, expected_value in expected.items():
        require(values.get(key) == expected_value, f"{document.key} source metadata mismatch for {key}.")
    require(values.get("approvedAt", "").endswith("Z"), f"{document.key} has invalid approvedAt.")
    require(values.get("effectiveAt", "").endswith("Z"), f"{document.key} has invalid effectiveAt.")
    text = "\n".join(body)
    require(len(re.sub(r"\s+", "", text)) >= 2500, f"{document.key} source is too thin for a real-life controlled document.")
    require(SYNTHETIC_NOTICE in source.read_text(encoding="utf-8") or "synthetic" in text.lower(), f"{document.key} lacks a synthetic-data notice.")
    for code in required_codes:
        require(code in text, f"{document.key} source is missing required error code {code}.")
    if document.status == "SUPERSEDED":
        require(replacement is not None and replacement in values.get("replacement", ""), f"{document.key} lacks its replacement.")
    scan_sensitive(source.read_text(encoding="utf-8"), source.name)
    return values


def scan_sensitive(text: str, label: str) -> None:
    for name, pattern in SENSITIVE_PATTERNS.items():
        if pattern.search(text):
            raise ValidationError(f"{label} contains a prohibited {name} pattern.")


def main() -> None:
    documents = parse_inventory(INVENTORY_PATH)
    scenarios = scenarios_by_code()
    expected_sources = {document.source_filename for document in documents}
    expected_pdfs = {document.filename for document in documents}
    actual_sources = {path.name for path in SOURCES.glob("*.md")}
    actual_pdfs = {path.name for path in PDFS.glob("*.pdf")}
    require(actual_sources == expected_sources, f"Source membership mismatch. Missing={sorted(expected_sources - actual_sources)} Extra={sorted(actual_sources - expected_sources)}")
    require(actual_pdfs == expected_pdfs, f"PDF membership mismatch. Missing={sorted(expected_pdfs - actual_pdfs)} Extra={sorted(actual_pdfs - expected_pdfs)}")

    records = []
    for document in documents:
        source = SOURCES / document.source_filename
        pdf = PDFS / document.filename
        codes = error_codes(document, scenarios)
        replacement = REPLACEMENTS.get(document.key)
        values = validate_source(document, source, codes, replacement)
        result = validate_pdf(pdf, document, codes, replacement)
        with pdfplumber.open(pdf) as opened:
            extracted = "\n".join((page.extract_text() or "") for page in opened.pages)
        scan_sensitive(extracted, pdf.name)
        if document.status == "APPROVED":
            require("SUPERSEDED - NOT RETRIEVAL ELIGIBLE" not in extracted, f"{document.key} approved PDF carries a superseded banner.")
        records.append(
            {
                "key": document.key,
                "documentId": document.document_id,
                "version": document.version,
                "type": document.type,
                "approvalStatus": document.status,
                "incidentFamily": INCIDENT_FAMILY,
                "source": str(source.relative_to(CORPUS_ROOT)).replace("\\", "/"),
                "pdf": str(pdf.relative_to(CORPUS_ROOT)).replace("\\", "/"),
                "sourceSha256": sha256(source),
                "pdfSha256": result.sha256,
                "pageCount": result.page_count,
                "extractedCharacters": result.extracted_characters,
                "requiredErrorCodes": codes,
                "replacement": replacement,
                "checks": {
                    "inventoryMetadata": "PASS",
                    "pageRange1To15": "PASS",
                    "unencrypted": "PASS",
                    "textExtraction": "PASS",
                    "pageNumbering": "PASS",
                    "requiredVocabulary": "PASS",
                    "supersession": "PASS",
                    "sensitivePatterns": "PASS",
                },
            }
        )

    page_counts = sorted(record["pageCount"] for record in records)
    manifest = {
        "corpusVersion": "synten-auth-knowledge/v1",
        "authoringStandardVersion": "synten-pdf-authoring/v1",
        "generatorVersion": GENERATOR_VERSION,
        "validatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "documentCount": len(records),
        "totalPages": sum(page_counts),
        "minimumPages": min(page_counts),
        "maximumPages": max(page_counts),
        "medianPages": (page_counts[14] + page_counts[15]) / 2,
        "documents": records,
    }
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(
        f"validated {len(records)} PDFs, {manifest['totalPages']} pages; "
        f"min={manifest['minimumPages']} max={manifest['maximumPages']} median={manifest['medianPages']}"
    )
    print(f"manifest: {MANIFEST}")


if __name__ == "__main__":
    main()
