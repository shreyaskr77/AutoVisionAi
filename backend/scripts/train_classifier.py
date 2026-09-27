"""
AutoVision AI - Vehicle Make & Model Training Script
Fine-tunes a pretrained ResNet-50 / EfficientNet backbone on vehicle classes.
Saves PyTorch weights to models/car_classifier_resnet50.pth.
"""

import os
import sys
import time
import argparse
from pathlib import Path
import torch
import torch.nn as nn
import torch.optim as optim
from torchvision import datasets, models, transforms
from torch.utils.data import DataLoader

def parse_args():
    parser = argparse.ArgumentParser(description="Train Vehicle Make & Model Classifier")
    parser.add_argument("--data-dir", type=str, default="data", help="Root data folder containing train/ and val/")
    parser.add_argument("--models-dir", type=str, default="models", help="Folder to save trained weights")
    parser.add_argument("--epochs", type=int, default=15, help="Number of training epochs")
    parser.add_argument("--batch-size", type=int, default=32, help="Batch size")
    parser.add_argument("--lr", type=float, default=1e-4, help="Learning rate")
    return parser.parse_args()

def main():
    args = parse_args()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"[*] Training AutoVision AI classifier on device: {device}")

    data_dir = Path(args.data_dir)
    train_dir = data_dir / "train"
    val_dir = data_dir / "val"

    if not train_dir.exists():
        print(f"[!] Directory '{train_dir}' not found.")
        print("[!] Please run 'python scripts/prepare_dataset.py --download' or arrange folders:")
        print("    data/train/<class_name>/*.jpg")
        print("    data/val/<class_name>/*.jpg")
        return

    data_transforms = {
        "train": transforms.Compose([
            transforms.RandomResizedCrop(224, scale=(0.8, 1.0)),
            transforms.RandomHorizontalFlip(),
            transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
            transforms.ToTensor(),
            transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
        ]),
        "val": transforms.Compose([
            transforms.Resize(256),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
        ]),
    }

    train_dataset = datasets.ImageFolder(train_dir, data_transforms["train"])
    train_loader = DataLoader(train_dataset, batch_size=args.batch_size, shuffle=True, num_workers=4)

    num_classes = len(train_dataset.classes)
    print(f"[+] Loaded {len(train_dataset)} training samples across {num_classes} vehicle classes.")

    val_loader = None
    if val_dir.exists():
        val_dataset = datasets.ImageFolder(val_dir, data_transforms["val"])
        val_loader = DataLoader(val_dataset, batch_size=args.batch_size, shuffle=False, num_workers=4)
        print(f"[+] Loaded {len(val_dataset)} validation samples.")

    # Initialize pretrained ResNet-50
    model = models.resnet50(weights=models.ResNet50_Weights.DEFAULT)
    in_features = model.fc.in_features
    model.fc = nn.Sequential(
        nn.Dropout(0.3),
        nn.Linear(in_features, 512),
        nn.ReLU(inplace=True),
        nn.Dropout(0.2),
        nn.Linear(512, num_classes)
    )
    model = model.to(device)

    criterion = nn.CrossEntropyLoss()
    optimizer = optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-3)
    scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=args.epochs)

    best_acc = 0.0
    models_dir = Path(args.models_dir)
    models_dir.mkdir(parents=True, exist_ok=True)
    out_path = models_dir / "car_classifier_resnet50.pth"

    for epoch in range(args.epochs):
        model.train()
        running_loss = 0.0
        corrects = 0

        for inputs, labels in train_loader:
            inputs, labels = inputs.to(device), labels.to(device)
            optimizer.zero_grad()

            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()

            _, preds = torch.max(outputs, 1)
            running_loss += loss.item() * inputs.size(0)
            corrects += torch.sum(preds == labels.data)

        scheduler.step()
        epoch_loss = running_loss / len(train_dataset)
        epoch_acc = corrects.double() / len(train_dataset)
        print(f"Epoch {epoch+1}/{args.epochs} - Train Loss: {epoch_loss:.4f} Acc: {epoch_acc:.4f}")

    # Save final model state dict
    torch.save(model.state_dict(), out_path)
    print(f"[+] Successfully saved fine-tuned model weights to: {out_path}")

if __name__ == "__main__":
    main()
