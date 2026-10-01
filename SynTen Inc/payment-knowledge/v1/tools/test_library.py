import copy
import json
import tempfile
import unittest
import uuid
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.pdfgen import canvas

from library import build, load_inventory, validate, validate_pdf, verify_freeze


class LibraryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / 'sources').mkdir()
        self.inventory = {'corpusVersion': 'synten-payment-knowledge/v1', 'documents': []}
        for i in range(16):
            key = f'RB-{101+i}' if i < 11 else f'PL-{101+i-11}'
            row = dict(key=key, documentId=str(uuid.uuid4()), version='1.0.0',
                       type='RUNBOOK' if i < 11 else 'POLICY', title=f'Test guidance {i}',
                       owner='Synthetic Owner', status='APPROVED', effectiveDate='2026-10-01',
                       tenantId='8b860d80-d17f-4e6b-8c48-af35f26a4d61',
                       classification='Internal - Synthetic Demo',
                       stages=['authorization'], families=['AUTHORIZATION_DECLINE_RATE_SPIKE'],
                       risks=[f'R{j:02}' for j in range(1, 25)], sources=['E01'],
                       related=[], filename=f'{key.lower()}.pdf')
            self.inventory['documents'].append(row)
            source = '---\n' + json.dumps(row, sort_keys=True) + '\n---\n'
            source += '\n<!-- page -->\n'.join(
                f'# {row["title"]}\n\n## Section {p}\n\n' +
                'Synthetic data only. Guidance is advisory, not observed fact. ' * 8
                for p in range(3))
            (self.root / 'sources' / f'{key.lower()}.md').write_text(source, encoding='utf-8')
        self.save()

    def tearDown(self):
        self.temp.cleanup()

    def save(self):
        (self.root / 'inventory.json').write_text(json.dumps(self.inventory), encoding='utf-8')

    def test_inventory_rejects_duplicate_identity_and_missing_risk(self):
        self.inventory['documents'][1]['documentId'] = self.inventory['documents'][0]['documentId']
        self.save()
        with self.assertRaisesRegex(ValueError, 'identity'):
            load_inventory(self.root)
        self.inventory['documents'][1]['documentId'] = str(uuid.uuid4())
        for row in self.inventory['documents']:
            row['risks'].remove('R24')
        self.save()
        with self.assertRaisesRegex(ValueError, 'risk coverage'):
            load_inventory(self.root)

    def test_inventory_rejects_paths_unknown_sources_and_foreign_related(self):
        original = copy.deepcopy(self.inventory)
        for field, value, error in [('filename', '../escape.pdf', 'filename'),
                                    ('sources', ['E99'], 'source'),
                                    ('related', ['PL-999'], 'related')]:
            self.inventory = copy.deepcopy(original)
            self.inventory['documents'][0][field] = value
            self.save()
            with self.assertRaisesRegex(ValueError, error):
                load_inventory(self.root)

    def test_build_rejects_source_metadata_mismatch(self):
        path = self.root / 'sources/rb-101.md'
        path.write_text(path.read_text().replace('Synthetic Owner', 'Other Owner'), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'metadata'):
            build(self.root)

    def test_missing_and_extra_membership_fail_closed(self):
        path = self.root / 'sources/rb-101.md'
        path.unlink()
        with self.assertRaisesRegex(ValueError, 'membership'):
            build(self.root)
        (self.root / 'sources/extra.md').write_text('extra')
        with self.assertRaisesRegex(ValueError, 'membership'):
            build(self.root)

    def test_build_is_byte_deterministic_and_validates_all_documents(self):
        build(self.root)
        before = {p.name: p.read_bytes() for p in (self.root / 'pdfs').glob('*.pdf')}
        build(self.root)
        self.assertEqual(before, {p.name: p.read_bytes() for p in (self.root / 'pdfs').glob('*.pdf')})
        result = validate(self.root)
        self.assertEqual(len(result['documents']), 16)
        self.assertTrue(all(d['pageCount'] == 3 for d in result['documents']))
        (self.root / 'pdfs/extra.pdf').write_bytes(b'bad')
        with self.assertRaisesRegex(ValueError, 'membership'):
            validate(self.root)

    def test_pdf_rejects_over_limit_empty_encrypted_and_malformed(self):
        path = self.root / 'bad.pdf'
        c = canvas.Canvas(str(path))
        for _ in range(16):
            c.drawString(50, 700, 'Synthetic')
            c.showPage()
        c.save()
        with self.assertRaisesRegex(ValueError, 'page count'):
            validate_pdf(path, self.inventory['documents'][0])
        c = canvas.Canvas(str(path)); c.showPage(); c.save()
        with self.assertRaisesRegex(ValueError, 'text'):
            validate_pdf(path, self.inventory['documents'][0])
        writer = PdfWriter(); writer.add_page(PdfReader(path).pages[0]); writer.encrypt('synthetic')
        with path.open('wb') as stream:
            writer.write(stream)
        with self.assertRaisesRegex(ValueError, 'encrypted'):
            validate_pdf(path, self.inventory['documents'][0])
        path.write_bytes(b'malformed')
        with self.assertRaises(ValueError):
            validate_pdf(path, self.inventory['documents'][0])

    def test_pdf_detects_content_or_metadata_missing_from_extract(self):
        build(self.root)
        row = copy.deepcopy(self.inventory['documents'][0]); row['title'] = 'Foreign title'
        with self.assertRaisesRegex(ValueError, 'metadata'):
            validate_pdf(self.root / 'pdfs/rb-101.pdf', row)

    def test_freeze_detects_source_inventory_pdf_and_tool_tampering(self):
        build(self.root)
        result = validate(self.root)
        (self.root / 'freeze-manifest.json').write_text(json.dumps(result), encoding='utf-8')
        verify_freeze(self.root)
        altered = copy.deepcopy(result); altered['generatorSha256'] = '0' * 64
        (self.root / 'freeze-manifest.json').write_text(json.dumps(altered), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'provenance'):
            verify_freeze(self.root)
        (self.root / 'freeze-manifest.json').write_text(json.dumps(result), encoding='utf-8')
        for path in [self.root / 'sources/rb-101.md', self.root / 'inventory.json',
                     self.root / 'pdfs/rb-101.pdf']:
            original = path.read_bytes(); path.write_bytes(original + b' ')
            with self.assertRaises(ValueError):
                verify_freeze(self.root)
            path.write_bytes(original)


if __name__ == '__main__':
    unittest.main()
