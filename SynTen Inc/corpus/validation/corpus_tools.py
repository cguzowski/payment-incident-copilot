from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass
from pathlib import Path

import pdfplumber
from pypdf import PdfReader


class ValidationError(ValueError):
    pass


@dataclass(frozen=True)
class InventoryDocument:
    key: str
    document_id: str
    filename: str
    title: str
    version: str
    status: str
    applies_to: str
    target_pages: str
    retrieval_role: str
    scenario_coverage: str
    type: str

    @property
    def source_filename(self) -> str:
        return f"{Path(self.filename).stem}.md"


@dataclass(frozen=True)
class PdfValidationResult:
    page_count: int
    extracted_characters: int
    sha256: str


def parse_inventory(path: Path) -> list[InventoryDocument]:
    documents: list[InventoryDocument] = []
    document_type: str | None = None
    for line in path.read_text(encoding="utf-8").splitlines():
        if line == "## Runbooks":
            document_type = "RUNBOOK"
            continue
        if line == "## Policies":
            document_type = "POLICY"
            continue
        if not re.match(r"^\| (?:RB|PL)-\d{3} \|", line):
            continue
        if document_type is None:
            raise ValidationError("Inventory document row appears outside a type section.")
        cells = [cell.strip() for cell in line.split("|")]
        if len(cells) != 12:
            raise ValidationError(f"Inventory row is malformed: {line}")
        documents.append(
            InventoryDocument(
                key=cells[1],
                document_id=cells[2].strip("`"),
                filename=cells[3].strip("`"),
                title=cells[4],
                version=cells[5].strip("`"),
                status=cells[6].strip("`"),
                applies_to=cells[7],
                target_pages=cells[8],
                retrieval_role=cells[9],
                scenario_coverage=cells[10],
                type=document_type,
            )
        )
    _validate_inventory(documents)
    return documents


def _validate_inventory(documents: list[InventoryDocument]) -> None:
    if len(documents) != 30:
        raise ValidationError(f"Inventory must contain exactly 30 rows, found {len(documents)}.")
    if sum(document.type == "RUNBOOK" for document in documents) != 22:
        raise ValidationError("Inventory must contain exactly 22 runbooks.")
    if sum(document.type == "POLICY" for document in documents) != 8:
        raise ValidationError("Inventory must contain exactly 8 policies.")
    if sum(document.status == "APPROVED" for document in documents) != 27:
        raise ValidationError("Inventory must contain exactly 27 approved versions.")
    if sum(document.status == "SUPERSEDED" for document in documents) != 3:
        raise ValidationError("Inventory must contain exactly 3 superseded versions.")
    _require_unique("document key", [document.key for document in documents])
    _require_unique("PDF filename", [document.filename for document in documents])
    _require_unique(
        "document ID/version",
        [(document.document_id, document.version) for document in documents],
    )
    for document in documents:
        if not re.fullmatch(r"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}", document.document_id):
            raise ValidationError(f"{document.key} has an invalid document ID.")
        if not re.fullmatch(r"[a-z0-9.-]+\.pdf", document.filename):
            raise ValidationError(f"{document.key} has an invalid PDF filename.")
        match = re.fullmatch(r"(\d+)-(\d+)", document.target_pages)
        if not match or int(match.group(1)) < 1 or int(match.group(2)) > 15:
            raise ValidationError(f"{document.key} has a page target outside 1-15.")


def _require_unique(label: str, values: list[object]) -> None:
    if len(values) != len(set(values)):
        raise ValidationError(f"Inventory contains a duplicate {label}.")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_pdf(
    path: Path,
    document: InventoryDocument,
    required_codes: tuple[str, ...] | list[str],
    replacement: str | None,
) -> PdfValidationResult:
    if not path.is_file():
        raise ValidationError(f"PDF is missing: {path}")
    try:
        reader = PdfReader(path)
    except Exception as exception:
        raise ValidationError(f"PDF is malformed: {path.name}") from exception
    if reader.is_encrypted:
        raise ValidationError(f"PDF must not be encrypted: {path.name}")
    page_count = len(reader.pages)
    if page_count < 1 or page_count > 15:
        raise ValidationError(f"PDF page count must be within 1-15: {path.name} has {page_count}.")

    try:
        with pdfplumber.open(path) as pdf:
            page_texts = [(page.extract_text() or "").strip() for page in pdf.pages]
    except Exception as exception:
        raise ValidationError(f"PDF text extraction failed: {path.name}") from exception

    combined = "\n".join(page_texts)
    if len(re.sub(r"\s+", "", combined)) < 50:
        raise ValidationError(f"PDF must contain meaningful extractable text: {path.name}")
    for index, page_text in enumerate(page_texts, start=1):
        if len(re.sub(r"\s+", "", page_text)) < 50:
            raise ValidationError(f"PDF page {index} has insufficient extractable text: {path.name}")

    normalized = _normalize(combined)
    required_metadata = {
        "title": document.title,
        "document ID": document.document_id,
        "version": document.version,
        "status": document.status,
        "classification": "Internal - Synthetic Demo",
        "synthetic notice": "Synthetic demonstration data only",
    }
    for label, value in required_metadata.items():
        if _normalize(value) not in normalized:
            raise ValidationError(f"PDF is missing required {label}: {path.name}")
    for code in required_codes:
        if code not in combined:
            raise ValidationError(f"PDF is missing required error code {code}: {path.name}")

    for index, page_text in enumerate(page_texts, start=1):
        if f"Page {index} of {page_count}" not in page_text:
            raise ValidationError(f"PDF page numbering is missing or invalid on page {index}: {path.name}")

    if document.status == "SUPERSEDED":
        banner = "SUPERSEDED - NOT RETRIEVAL ELIGIBLE"
        for index, page_text in enumerate(page_texts, start=1):
            if banner not in _normalize(page_text):
                raise ValidationError(f"PDF requires {banner} on page {index}: {path.name}")
            if not replacement or replacement not in page_text:
                raise ValidationError(f"PDF is missing approved replacement {replacement} on page {index}: {path.name}")

    return PdfValidationResult(
        page_count=page_count,
        extracted_characters=len(combined),
        sha256=sha256(path),
    )


def _normalize(value: str) -> str:
    return re.sub(r"\s+", " ", value.replace("—", "-").replace("–", "-")).strip()
