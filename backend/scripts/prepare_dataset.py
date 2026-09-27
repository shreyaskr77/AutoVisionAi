"""
AutoVision AI - Dataset Preparation Script
Downloads and structures the Stanford Cars Dataset for fine-tuning.

Dataset details:
- Stanford Cars 196: 16,185 images of 196 classes of cars.
- Classes: Make, Model, Body type, Year (e.g., 2012 Tesla Model S Sedan)
- Format: PyTorch ImageFolder layout:
    data/
      train/
        Audi_A5_Coupe_2012/
          001.jpg
          ...
      val/
        Audi_A5_Coupe_2012/
          ...
"""

import os
import sys
import tarfile
import urllib.request
import scipy.io
import shutil
from pathlib import Path

DATASET_DIR = Path("data")
MODELS_DIR = Path("models")

URLS = {
    "cars_train": "https://ai.stanford.edu/~jkrause/car196/cars_train.tgz",
    "cars_test": "https://ai.stanford.edu/~jkrause/car196/cars_test.tgz",
    "cars_devkit": "https://ai.stanford.edu/~jkrause/car196/car_devkit.tgz"
}

def download_file(url: str, dest: Path):
    if dest.exists():
        print(f"[+] Already downloaded: {dest}")
        return
    print(f"[-] Downloading {url} -> {dest} ...")
    urllib.request.urlretrieve(url, dest)
    print(f"[+] Download complete: {dest}")

def extract_archive(archive_path: Path, extract_to: Path):
    print(f"[-] Extracting {archive_path} ...")
    with tarfile.open(archive_path, "r:gz") as tar:
        tar.extractall(extract_to)
    print(f"[+] Extracted to {extract_to}")

def main():
    DATASET_DIR.mkdir(parents=True, exist_ok=True)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    print("=========================================")
    print(" AutoVision AI: Dataset Preparation")
    print("=========================================")
    print("1. To download and train on the Stanford Cars Dataset:")
    print("   Run: python scripts/prepare_dataset.py --download")
    print("2. For custom vehicle images:")
    print("   Place folders named '<Make> <Model>' inside data/train/ and data/val/")
    print("=========================================")

    if "--download" in sys.argv:
        for name, url in URLS.items():
            archive = DATASET_DIR / f"{name}.tgz"
            download_file(url, archive)
            extract_archive(archive, DATASET_DIR)
        print("[+] Dataset archives extracted. Run scripts/train_classifier.py to start training.")

if __name__ == "__main__":
    main()
