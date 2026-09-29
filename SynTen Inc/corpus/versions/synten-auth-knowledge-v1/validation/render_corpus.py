from __future__ import annotations

import argparse
import json
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from corpus_tools import parse_inventory
from generate_corpus import CORPUS_ROOT, INVENTORY_PATH, PDFS


RENDERS = CORPUS_ROOT / "validation" / ".renders"


def render_document(key: str, pdf: Path, page_count: int, pdftoppm: str) -> Path:
    document_dir = RENDERS / key
    document_dir.mkdir(parents=True, exist_ok=True)
    for existing in document_dir.glob("page-*.png"):
        existing.unlink()
    prefix = document_dir / "page"
    subprocess.run(
        [pdftoppm, "-png", "-r", "110", str(pdf), str(prefix)],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        text=True,
    )
    pages = sorted(document_dir.glob("page-*.png"))
    if len(pages) != page_count:
        raise RuntimeError(f"{key}: expected {page_count} renders, found {len(pages)}")
    return contact_sheet(key, pages)


def contact_sheet(key: str, pages: list[Path]) -> Path:
    columns = 2
    thumb_width = 420
    gap = 18
    label_height = 34
    loaded = [Image.open(path).convert("RGB") for path in pages]
    thumbnails = []
    for image in loaded:
        height = round(image.height * thumb_width / image.width)
        thumbnails.append(image.resize((thumb_width, height), Image.Resampling.LANCZOS))
    thumb_height = max(image.height for image in thumbnails)
    rows = (len(thumbnails) + columns - 1) // columns
    canvas = Image.new(
        "RGB",
        (columns * thumb_width + (columns + 1) * gap, rows * (thumb_height + label_height) + (rows + 1) * gap),
        "#dfe7eb",
    )
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default(size=18)
    for index, image in enumerate(thumbnails):
        row, column = divmod(index, columns)
        x = gap + column * (thumb_width + gap)
        y = gap + row * (thumb_height + label_height + gap)
        draw.text((x, y), f"{key} - page {index + 1}", fill="#17324d", font=font)
        canvas.paste(image, (x, y + label_height))
    output = RENDERS / f"{key}-contact.png"
    canvas.save(output)
    for image in loaded:
        image.close()
    return output


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pdftoppm", default=shutil.which("pdftoppm"))
    args = parser.parse_args()
    if not args.pdftoppm:
        raise SystemExit("pdftoppm was not found")
    RENDERS.mkdir(parents=True, exist_ok=True)
    manifest = json.loads((CORPUS_ROOT / "validation-manifest.json").read_text(encoding="utf-8"))
    page_counts = {record["key"]: record["pageCount"] for record in manifest["documents"]}
    contacts = []
    for document in parse_inventory(INVENTORY_PATH):
        contact = render_document(document.key, PDFS / document.filename, page_counts[document.key], args.pdftoppm)
        contacts.append(str(contact))
        print(f"rendered {document.key}: {page_counts[document.key]} pages")
    print(f"contact sheets: {len(contacts)} in {RENDERS}")


if __name__ == "__main__":
    main()
