"""Render all frozen-library pages for human visual inspection with Poppler."""
import argparse
import shutil
import subprocess
from pathlib import Path

from library import load_inventory, validate


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path)
    parser.add_argument('--pdftoppm', default=shutil.which('pdftoppm'))
    args = parser.parse_args()
    if not args.pdftoppm:
        raise ValueError('Poppler pdftoppm is required')
    result = validate(args.root)
    output = args.output or args.root.parents[2] / 'tmp/e2-pdf-qa'
    counts = {d['key']: d['pageCount'] for d in result['documents']}
    for row in load_inventory(args.root)['documents']:
        target = output / row['key']
        target.mkdir(parents=True, exist_ok=True)
        subprocess.run([args.pdftoppm, '-png', '-r', '96',
                        str(args.root / 'pdfs' / row['filename']), str(target / 'page')], check=True)
        if len(list(target.glob('page-*.png'))) != counts[row['key']]:
            raise ValueError('Rendered page count mismatch: ' + row['key'])
        print(f"{row['key']}: {counts[row['key']]} pages rendered")


if __name__ == '__main__':
    main()
