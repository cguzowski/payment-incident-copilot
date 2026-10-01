"""Offline risk-derived PDF builder/validator. No scenario or runtime inputs."""
import argparse
import hashlib
import json
import re
import sys
import uuid
from datetime import date, datetime, timezone
from pathlib import Path
from xml.sax.saxutils import escape

import pdfplumber
import reportlab
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle

VERSION = 'synten-independent-pdf/v1'
FONTS = Path(reportlab.__file__).parent / 'fonts'
for name, file in [('Vera', 'Vera.ttf'), ('VeraBold', 'VeraBd.ttf')]:
    if name not in pdfmetrics.getRegisteredFontNames():
        pdfmetrics.registerFont(TTFont(name, str(FONTS / file)))


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_inventory(root):
    inventory = json.loads((root / 'inventory.json').read_text(encoding='utf-8'))
    if inventory.get('corpusVersion') != 'synten-payment-knowledge/v1':
        raise ValueError('corpus version')
    rows = inventory['documents']
    if len(rows) != 16 or sum(r['type'] == 'RUNBOOK' for r in rows) != 11 or sum(r['type'] == 'POLICY' for r in rows) != 5:
        raise ValueError('inventory membership/type count')
    for field in ('key', 'documentId', 'filename'):
        if len({r[field] for r in rows}) != len(rows):
            raise ValueError('duplicate identity: ' + field)
    keys = {r['key'] for r in rows}
    covered = set()
    valid_families = {'AUTHORIZATION_DECLINE_RATE_SPIKE', 'AUTHORIZATION_TIMEOUT_SPIKE',
                     'CAPTURE_FAILURE_SPIKE', 'REFUND_FAILURE_SPIKE', 'SETTLEMENT_DELAY',
                     'WEBHOOK_DELIVERY_FAILURE', 'RECONCILIATION_MISMATCH'}
    valid_stages = {'authorization', 'capture', 'release', 'refund', 'settlement',
                    'delivery', 'reconciliation', 'cross-stage'}
    for row in rows:
        if not re.fullmatch(r'(RB|PL)-1\d{2}', row['key']) or (row['type'] == 'RUNBOOK') != row['key'].startswith('RB-'):
            raise ValueError('key/type')
        if uuid.UUID(row['documentId']).version != 4:
            raise ValueError('document identity')
        if not re.fullmatch(r'[a-z0-9-]+\.pdf', row['filename']):
            raise ValueError('unsafe filename')
        if row['status'] != 'APPROVED' or row['version'] != '1.0.0' or date.fromisoformat(row['effectiveDate']) != date(2026, 10, 1):
            raise ValueError('approval/version metadata')
        if row['tenantId'] != '8b860d80-d17f-4e6b-8c48-af35f26a4d61' or row['classification'] != 'Internal - Synthetic Demo':
            raise ValueError('tenant/classification metadata')
        if not row['stages'] or not set(row['stages']) <= valid_stages or not row['families'] or not set(row['families']) <= valid_families:
            raise ValueError('stage/family metadata')
        if not row['risks'] or not set(row['risks']) <= {f'R{i:02}' for i in range(1, 25)}:
            raise ValueError('risk reference')
        if not row['sources'] or not set(row['sources']) <= {f'E{i:02}' for i in range(1, 12)}:
            raise ValueError('source reference')
        if not set(row['related']) <= keys or row['key'] in row['related']:
            raise ValueError('related reference')
        for field in ('title', 'owner'):
            if not isinstance(row[field], str) or not row[field].strip():
                raise ValueError('metadata ' + field)
        covered.update(row['risks'])
    if covered != {f'R{i:02}' for i in range(1, 25)}:
        raise ValueError('missing risk coverage')
    return inventory


def membership(directory, expected, suffix):
    actual = {p.name for p in directory.glob('*' + suffix)}
    if actual != set(expected):
        raise ValueError(f'{directory.name} membership mismatch: {actual ^ set(expected)}')


def read_source(root, row):
    path = root / 'sources' / (Path(row['filename']).stem + '.md')
    parts = path.read_text(encoding='utf-8').split('---\n', 2)
    if len(parts) != 3 or parts[0] or json.loads(parts[1]) != row:
        raise ValueError('source metadata mismatch: ' + row['key'])
    pages = parts[2].strip().split('\n<!-- page -->\n')
    if len(pages) != 3 or any(not page.strip() for page in pages):
        raise ValueError('source must contain three nonempty editorial pages')
    return pages


class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        kwargs['invariant'] = 1
        super().__init__(*args, **kwargs)
        self.saved_states = []

    def showPage(self):
        self.saved_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        total = len(self.saved_states)
        for state in self.saved_states:
            self.__dict__.update(state)
            row = self._doc_metadata
            self.setFillColor(colors.HexColor('#536577'))
            self.setFont('Vera', 8)
            self.drawString(48, 810, f"SYNTEN INC  /  {row['key']}  /  {row['type']}  /  v{row['version']}")
            self.setStrokeColor(colors.HexColor('#c9d5df'))
            self.line(48, 798, 547, 798)
            self.line(48, 50, 547, 50)
            self.drawString(48, 36, 'SynTen Inc - Internal - Synthetic Demo')
            self.drawRightString(547, 36, f'Page {self._pageNumber} of {total}')
            self.setFont('Vera', 7)
            self.drawString(48, 23, f"{row['documentId']} / {row['version']}")
            super().showPage()
        super().save()


STYLE = {
    'body': ParagraphStyle('body', fontName='Vera', fontSize=10, leading=14.4, spaceAfter=8,
                           textColor=colors.HexColor('#253545')),
    'title': ParagraphStyle('title', fontName='VeraBold', fontSize=25, leading=30, spaceAfter=16,
                            textColor=colors.HexColor('#173b53')),
    'h2': ParagraphStyle('h2', fontName='VeraBold', fontSize=14, leading=18, spaceBefore=8,
                         spaceAfter=8, keepWithNext=True, textColor=colors.HexColor('#14677a')),
    'cell': ParagraphStyle('cell', fontName='Vera', fontSize=9, leading=12, spaceAfter=0),
}


def paragraph(text, style='body'):
    text = escape(text)
    text = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', text)
    text = re.sub(r'`(.+?)`', r'\1', text)
    return Paragraph(text, STYLE[style])


def markdown_flow(text):
    flow = []
    lines = text.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1; continue
        if line.startswith('|'):
            cells = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                values = [v.strip() for v in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', v) for v in values):
                    cells.append([paragraph(v, 'cell') for v in values])
                i += 1
            width = 499 / len(cells[0])
            table = Table(cells, colWidths=[width] * len(cells[0]), repeatRows=1, hAlign='LEFT')
            table.setStyle(TableStyle([('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#e2edf2')),
                ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('BOX', (0, 0), (-1, -1), .5, colors.HexColor('#bbcbd5')),
                ('INNERGRID', (0, 0), (-1, -1), .3, colors.HexColor('#c9d5df')),
                ('LEFTPADDING', (0, 0), (-1, -1), 9), ('RIGHTPADDING', (0, 0), (-1, -1), 9),
                ('TOPPADDING', (0, 0), (-1, -1), 8), ('BOTTOMPADDING', (0, 0), (-1, -1), 8)]))
            flow.extend([table, Spacer(1, 12)]); continue
        if line.startswith('# '):
            flow.append(paragraph(line[2:], 'title'))
        elif line.startswith('## '):
            flow.append(paragraph(line[3:], 'h2'))
        elif line.startswith('- '):
            flow.append(paragraph('• ' + line[2:]))
        elif re.match(r'^\d+\. ', line):
            combined = [line]
            while i + 1 < len(lines) and lines[i+1].strip() and not re.match(r'^(#|\||- |\d+\. )', lines[i+1]):
                i += 1; combined.append(lines[i].strip())
            flow.append(paragraph(' '.join(combined)))
        else:
            combined = [line]
            while i + 1 < len(lines) and lines[i+1].strip() and not re.match(r'^(#|\||- |\d+\. )', lines[i+1]):
                i += 1; combined.append(lines[i].strip())
            flow.append(paragraph(' '.join(combined)))
        i += 1
    return flow


def build(root):
    if (root / 'freeze-manifest.json').exists():
        verify_freeze(root)
    rows = load_inventory(root)['documents']
    membership(root / 'sources', [Path(r['filename']).stem + '.md' for r in rows], '.md')
    prepared = [(row, read_source(root, row)) for row in rows]
    (root / 'pdfs').mkdir(exist_ok=True)
    for row, pages in prepared:
        flow = []
        for i, page in enumerate(pages):
            if i:
                flow.append(PageBreak())
            flow.extend(markdown_flow(page))
            if i == 0:
                info = [
                    ['Document control', 'Value'], ['Key / version', row['key'] + ' / ' + row['version']],
                    ['Document ID', row['documentId']], ['Owner', row['owner']],
                    ['Tenant ID', row['tenantId']],
                    ['Status / effective', row['status'] + ' / ' + row['effectiveDate']],
                    ['Approver', 'Knowledge Approver (fictional role)'],
                    ['Scope', ', '.join(row['stages'])], ['Risks / sources', ', '.join(row['risks']) + ' / ' + ', '.join(row['sources'])],
                ]
                control = '\n'.join('| ' + ' | '.join(pair) + ' |' for pair in info)
                flow.extend(markdown_flow(control))
                flow.append(paragraph('Synthetic notice: All systems, records, roles and amounts are fictional. '
                                      'Guidance is advisory and never executes an action.'))
        doc = SimpleDocTemplate(str(root / 'pdfs' / row['filename']), pagesize=A4,
            leftMargin=48, rightMargin=48, topMargin=61, bottomMargin=64,
            title=row['title'], author='SynTen Inc - Synthetic Demo', pageCompression=1)
        def canvas_factory(*args, metadata=row, **kwargs):
            result = NumberedCanvas(*args, **kwargs); result._doc_metadata = metadata
            return result
        doc.build(flow, canvasmaker=canvas_factory)


def validate_pdf(path, row):
    try:
        reader = PdfReader(path)
        if reader.is_encrypted:
            raise ValueError('encrypted PDF')
        count = len(reader.pages)
        if not 1 <= count <= 15:
            raise ValueError('page count outside 1-15')
        with pdfplumber.open(path) as pdf:
            texts = [p.extract_text() or '' for p in pdf.pages]
            for page in pdf.pages:
                if any(c['x0'] < 16 or c['x1'] > page.width - 16 or c['top'] < 12 or c['bottom'] > page.height - 12 for c in page.chars):
                    raise ValueError('text outside page bounds')
        if any(len(text.strip()) < 100 for text in texts):
            raise ValueError('empty/scanned-only text page')
        all_text = re.sub(r'\s+', ' ', '\n'.join(texts))
        for value in [row['title'], row['key'], row['documentId'], row['version'],
                      row['status'], row['owner'], row['tenantId'], row['effectiveDate'], 'Synthetic notice',
                      'Internal - Synthetic Demo']:
            if value not in all_text:
                raise ValueError('PDF metadata missing: ' + value)
        for i, text in enumerate(texts, 1):
            if f'Page {i} of {count}' not in text:
                raise ValueError('page numbering')
        if count != 3:
            raise ValueError('editorial page overflow: expected 3 pages')
        return dict(pageCount=count, extractedCharacters=sum(map(len, texts)), pdfSha256=sha(path))
    except ValueError:
        raise
    except Exception as exc:
        raise ValueError('malformed PDF') from exc


def validate(root):
    inventory = load_inventory(root)
    rows = inventory['documents']
    membership(root / 'sources', [Path(r['filename']).stem + '.md' for r in rows], '.md')
    membership(root / 'pdfs', [r['filename'] for r in rows], '.pdf')
    result = dict(corpusVersion=inventory['corpusVersion'], generatorVersion=VERSION,
                  reportlabVersion=reportlab.Version, inventorySha256=sha(root / 'inventory.json'),
                  generatorSha256=sha(Path(__file__)),
                  fontSha256={f: sha(FONTS / f) for f in ('Vera.ttf', 'VeraBd.ttf')}, documents=[])
    result['packageFilesSha256'] = {name: sha(root / name) for name in
        ['README.md', 'inventory.md', 'authoring-standard.md', 'review.md', 'input-provenance.json',
         'tools/render_library.py', 'tools/test_library.py'] if (root / name).exists()}
    if 'inputProvenance' in inventory:
        if inventory['inputProvenance'] != 'input-provenance.json':
            raise ValueError('input provenance filename')
        provenance = json.loads((root / 'input-provenance.json').read_text(encoding='utf-8'))
        allowed = {'SynTen Inc/domain/v2/' + name for name in
                   ('README.md', 'lifecycle.md', 'risk-inventory.md', 'evidence-requirements.md')}
        allowed.add('SynTen Inc/corpus/validation-manifest.json')
        if {item['path'] for item in provenance['inputs']} != allowed or len(provenance['inputs']) != 5:
            raise ValueError('authoring input allowlist')
        for item in provenance['inputs']:
            if sha(root.parents[2] / item['path']) != item['sha256']:
                raise ValueError('authoring input hash mismatch')
        result['inputProvenance'] = provenance
    for row in rows:
        pages = read_source(root, row)
        source = root / 'sources' / (Path(row['filename']).stem + '.md')
        pdf = root / 'pdfs' / row['filename']
        facts = validate_pdf(pdf, row)
        extracted = ' '.join(PdfReader(pdf).pages[i].extract_text() or '' for i in range(facts['pageCount']))
        # Each source line must survive extraction, normalized only for wrapping/style markers.
        normalize = lambda s: re.sub(r'\s+', '', s.replace('**', '').replace('`', ''))
        for page in pages:
            for line in page.splitlines():
                if not line.strip() or line.startswith('|') and all(re.fullmatch(r':?-+:?', c.strip()) for c in line.strip('|').split('|')):
                    continue
                value = line.lstrip('# ').removeprefix('- ').replace('|', '')
                if normalize(value) not in normalize(extracted):
                    raise ValueError('source text missing from PDF: ' + row['key'] + ': ' + value[:70])
        result['documents'].append(dict(key=row['key'], documentId=row['documentId'],
            version=row['version'], source='sources/' + source.name, pdf='pdfs/' + pdf.name,
            sourceSha256=sha(source), risks=row['risks'], **facts))
    return result


def verify_freeze(root):
    recorded = json.loads((root / 'freeze-manifest.json').read_text(encoding='utf-8'))
    if 'frozenAt' in recorded:
        frozen_at = datetime.fromisoformat(recorded.pop('frozenAt'))
        if frozen_at.utcoffset() != timezone.utc.utcoffset(frozen_at):
            raise ValueError('freeze timestamp must be UTC')
    if recorded != validate(root):
        raise ValueError('freeze manifest hash/provenance mismatch')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['build', 'validate', 'freeze', 'verify'])
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    if args.command == 'build':
        build(args.root)
    elif args.command == 'validate':
        result = validate(args.root)
        print(f"Validated {len(result['documents'])} PDFs / {sum(d['pageCount'] for d in result['documents'])} pages")
    elif args.command == 'freeze':
        path = args.root / 'freeze-manifest.json'
        if path.exists():
            raise ValueError('Refusing to overwrite a frozen manifest; create a successor version')
        result = validate(args.root)
        required = {'README.md', 'inventory.md', 'authoring-standard.md', 'review.md',
                    'input-provenance.json', 'tools/render_library.py', 'tools/test_library.py'}
        if set(result['packageFilesSha256']) != required or 'inputProvenance' not in result:
            raise ValueError('freeze requires complete review, control and input provenance files')
        result['frozenAt'] = datetime.now(timezone.utc).isoformat()
        path.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8', newline='\n')
    else:
        verify_freeze(args.root)
        print('Frozen hashes and generation provenance match')


if __name__ == '__main__':
    main()
