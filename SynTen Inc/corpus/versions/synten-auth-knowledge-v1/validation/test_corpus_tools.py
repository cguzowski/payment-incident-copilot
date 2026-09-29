from __future__ import annotations

import sys
import tempfile
import unittest
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
TEMP_ROOT = TENANT_ROOT / "corpus" / "validation" / ".tmp"
TEMP_ROOT.mkdir(exist_ok=True)


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
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as directory:
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
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as directory:
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
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as directory:
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
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as directory:
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
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as directory:
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
