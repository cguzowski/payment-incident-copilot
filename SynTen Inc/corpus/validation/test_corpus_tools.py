from __future__ import annotations

import hashlib
import json
import shutil
import sys
import tempfile
import unittest
import uuid
from contextlib import contextmanager
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen.canvas import Canvas
from reportlab.platypus import Paragraph

sys.path.insert(0, str(Path(__file__).resolve().parent))

from corpus_tools import (  # noqa: E402
    ValidationError,
    parse_inventory,
    validate_pdf,
)
from generate_corpus import build_document, styles  # noqa: E402


TENANT_ROOT = Path(__file__).resolve().parents[2]
INVENTORY = TENANT_ROOT / "corpus" / "inventory.md"
TEMP_ROOT = Path(tempfile.gettempdir()) / "synten-corpus-tests"
TEMP_ROOT.mkdir(exist_ok=True)
ARCHIVE = TENANT_ROOT / "corpus" / "versions" / "synten-auth-knowledge-v1"
ORACLE = TENANT_ROOT.parent / "syntheticIncidentGenerator" / "src" / "main" / "resources" / "scenarios" / "oracle.json"


@contextmanager
def temporary_directory():
    path = TEMP_ROOT / f"case-{uuid.uuid4()}"
    path.mkdir(parents=True)
    try:
        yield str(path)
    finally:
        shutil.rmtree(path)


def write_pdf(path: Path, page_count: int, lines: list[str] | None = None) -> None:
    canvas = Canvas(str(path), pagesize=A4)
    for page in range(1, page_count + 1):
        y = 800
        for line in lines or []:
            canvas.drawString(50, y, line)
            y -= 18
        canvas.drawString(50, 40, f"Page {page} of {page_count}")
        canvas.showPage()
    canvas.save()


class CorpusToolsTest(unittest.TestCase):
    def test_v1_archive_preserves_every_manifest_hash(self) -> None:
        manifest = json.loads((ARCHIVE / "validation-manifest.json").read_text(encoding="utf-8"))

        self.assertEqual("synten-auth-knowledge/v1", manifest["corpusVersion"])
        for document in manifest["documents"]:
            source = ARCHIVE / document["source"]
            pdf = ARCHIVE / document["pdf"]
            self.assertEqual(document["sourceSha256"], hashlib.sha256(source.read_bytes()).hexdigest())
            self.assertEqual(document["pdfSha256"], hashlib.sha256(pdf.read_bytes()).hexdigest())

    def test_v2_corpus_is_version_bumped_and_contains_no_oracle_answers(self) -> None:
        manifest = json.loads((TENANT_ROOT / "corpus" / "validation-manifest.json").read_text(encoding="utf-8"))
        inventory = parse_inventory(INVENTORY)
        archived_inventory = parse_inventory(ARCHIVE / "inventory.md")
        oracle = json.loads(ORACLE.read_text(encoding="utf-8"))

        self.assertEqual("synten-auth-knowledge/v2", manifest["corpusVersion"])
        self.assertEqual("synten-pdf-authoring/v2", manifest["authoringStandardVersion"])
        self.assertEqual(30, len(inventory))
        self.assertEqual(
            {item.document_id for item in archived_inventory},
            {item.document_id for item in inventory},
        )
        old_versions = {item.key: item.version for item in archived_inventory}
        self.assertTrue(all(item.version != old_versions[item.key] for item in inventory))

        source_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted((TENANT_ROOT / "corpus" / "sources").glob("*.md"))
        )
        pdf_text = "\n".join(
            "\n".join((page.extract_text() or "") for page in PdfReader(path).pages)
            for path in sorted((TENANT_ROOT / "corpus" / "pdfs").glob("*.pdf"))
        )
        corpus_text = "\n".join((source_text, pdf_text, json.dumps(manifest)))
        for scenario in oracle["scenarios"]:
            truth = scenario["truth"]
            forbidden = [truth["rootCause"], truth["recommendation"], *truth["requiredEvidence"]]
            for value in forbidden:
                self.assertNotIn(value, corpus_text)

        generator = (TENANT_ROOT / "corpus" / "validation" / "generate_corpus.py").read_text(encoding="utf-8")
        for field in ("truth", "rootCause", "expectedDisposition", "expectedConfidence", "requiredEvidence"):
            self.assertNotIn(f'[{field!r}]', generator)
            self.assertNotIn(f'["{field}"]', generator)
        self.assertNotIn("oracle.json", generator)

    def test_parses_the_exact_inventory_contract(self) -> None:
        documents = parse_inventory(INVENTORY)

        self.assertEqual(30, len(documents))
        self.assertEqual(22, sum(document.type == "RUNBOOK" for document in documents))
        self.assertEqual(8, sum(document.type == "POLICY" for document in documents))
        self.assertEqual(27, sum(document.status == "APPROVED" for document in documents))
        self.assertEqual(3, sum(document.status == "SUPERSEDED" for document in documents))
        self.assertEqual(30, len({document.filename for document in documents}))
        self.assertEqual(30, len({(document.document_id, document.version) for document in documents}))

    def test_accepts_one_and_fifteen_pages_but_rejects_sixteen(self) -> None:
        document = parse_inventory(INVENTORY)[0]
        metadata = [
            document.title,
            document.document_id,
            document.version,
            document.status,
            "Internal - Synthetic Demo",
            "Synthetic demonstration data only",
        ]
        with temporary_directory() as directory:
            root = Path(directory)
            for count in (1, 15):
                path = root / f"accepted-{count}.pdf"
                write_pdf(path, count, metadata)
                result = validate_pdf(path, document, required_codes=(), replacement=None)
                self.assertEqual(count, result.page_count)

            rejected = root / "rejected-16.pdf"
            write_pdf(rejected, 16, metadata)
            with self.assertRaisesRegex(ValidationError, "1-15"):
                validate_pdf(rejected, document, required_codes=(), replacement=None)

    def test_rejects_encrypted_and_scanned_only_pdfs(self) -> None:
        document = parse_inventory(INVENTORY)[0]
        with temporary_directory() as directory:
            root = Path(directory)
            readable = root / "readable.pdf"
            encrypted = root / "encrypted.pdf"
            blank = root / "blank.pdf"
            write_pdf(readable, 1, [document.title, document.document_id, document.version])
            reader = PdfReader(readable)
            writer = PdfWriter()
            writer.append_pages_from_reader(reader)
            writer.encrypt("synthetic-password")
            with encrypted.open("wb") as stream:
                writer.write(stream)
            write_pdf(blank, 1, [])

            with self.assertRaisesRegex(ValidationError, "encrypted"):
                validate_pdf(encrypted, document, required_codes=(), replacement=None)
            with self.assertRaisesRegex(ValidationError, "extractable text"):
                validate_pdf(blank, document, required_codes=(), replacement=None)

    def test_rejects_missing_metadata_and_required_error_codes(self) -> None:
        document = parse_inventory(INVENTORY)[1]
        with temporary_directory() as directory:
            path = Path(directory) / "metadata.pdf"
            write_pdf(
                path,
                1,
                [
                    document.title,
                    document.document_id,
                    document.status,
                    "Internal - Synthetic Demo",
                    "Synthetic demonstration data only",
                ],
            )

            with self.assertRaisesRegex(ValidationError, "version"):
                validate_pdf(path, document, required_codes=("GATEWAY_TIMEOUT",), replacement=None)

    def test_rejects_superseded_pdf_without_banner_and_replacement(self) -> None:
        document = next(item for item in parse_inventory(INVENTORY) if item.key == "RB-022")
        with temporary_directory() as directory:
            path = Path(directory) / "superseded.pdf"
            write_pdf(
                path,
                1,
                [
                    document.title,
                    document.document_id,
                    document.version,
                    document.status,
                    "Internal - Synthetic Demo",
                    "Synthetic demonstration data only",
                    "GATEWAY_TIMEOUT",
                ],
            )

            with self.assertRaisesRegex(ValidationError, "SUPERSEDED - NOT RETRIEVAL ELIGIBLE"):
                validate_pdf(path, document, required_codes=("GATEWAY_TIMEOUT",), replacement="RB-002")

    def test_generator_uses_invariant_pdf_metadata(self) -> None:
        document = parse_inventory(INVENTORY)[0]
        with temporary_directory() as directory:
            path = Path(directory) / "invariant.pdf"
            build_document(
                path,
                document,
                {"replacement": "None"},
                [Paragraph("Deterministic SynTen Inc PDF fixture", styles()["body"])],
                total_pages=1,
            )

            creation_date = PdfReader(path).metadata.creation_date
            self.assertIsNotNone(creation_date)
            self.assertEqual(2000, creation_date.year)


if __name__ == "__main__":
    unittest.main()
