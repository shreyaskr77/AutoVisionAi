import cv2
import numpy as np
import pytest
from app.services.color_extractor import ColorExtractor

def test_red_car_crop():
    # Synthetic BGR image: pure Red body (B: 30, G: 30, R: 220)
    img = np.full((100, 100, 3), (30, 30, 220), dtype=np.uint8)
    res = ColorExtractor.estimate_color(img)
    assert res["colour"] == "Red"
    assert res["colour_hex"] == "#DC2626"

def test_blue_car_crop():
    # Synthetic BGR image: pure Electric Blue body (B: 230, G: 100, R: 30)
    img = np.full((100, 100, 3), (230, 100, 30), dtype=np.uint8)
    res = ColorExtractor.estimate_color(img)
    assert res["colour"] in ["Electric Blue", "Navy Blue"]

def test_white_car_crop():
    # Synthetic BGR image: bright white car body (B: 240, G: 240, R: 240)
    img = np.full((100, 100, 3), (240, 240, 240), dtype=np.uint8)
    res = ColorExtractor.estimate_color(img)
    assert res["colour"] == "White"

def test_black_car_crop():
    # Synthetic BGR image: black car body (B: 30, G: 30, R: 30)
    img = np.full((100, 100, 3), (30, 30, 30), dtype=np.uint8)
    res = ColorExtractor.estimate_color(img)
    assert res["colour"] == "Black"

def test_empty_or_small_image():
    empty = np.zeros((5, 5, 3), dtype=np.uint8)
    res = ColorExtractor.estimate_color(empty)
    assert res["colour"] == "Unknown"
