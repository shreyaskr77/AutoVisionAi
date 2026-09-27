import cv2
import numpy as np
from typing import Tuple, Dict

COLOR_PALETTE = {
    "Black": "#111827",
    "White": "#F8FAFC",
    "Silver": "#94A3B8",
    "Grey": "#4B5563",
    "Red": "#DC2626",
    "Maroon": "#881337",
    "Orange": "#EA580C",
    "Gold": "#D97706",
    "Yellow": "#EAB308",
    "Green": "#16A34A",
    "Electric Blue": "#2563EB",
    "Navy Blue": "#1E3A8A",
    "Teal": "#0D9488",
    "Purple": "#7C3AED",
    "Brown": "#78350F",
}

class ColorExtractor:
    """
    Estimates dominant exterior vehicle colour from cropped vehicle imagery
    using OpenCV HSV and CIELAB color spaces.
    Excludes windshield/roof, tyres, road shadows, and specular glare.
    """

    @classmethod
    def estimate_color(cls, vehicle_bgr: np.ndarray) -> Dict[str, str]:
        if vehicle_bgr is None or vehicle_bgr.size == 0:
            return {"colour": "Unknown", "colour_hex": "#64748B"}

        h, w = vehicle_bgr.shape[:2]
        if h < 20 or w < 20:
            return {"colour": "Unknown", "colour_hex": "#64748B"}

        # Step 1: Crop vehicle body center
        # Exclude top 18% (windshield, roofline, sky reflections)
        # Exclude bottom 20% (undercarriage, wheels, tyres, road shadows)
        # Exclude left & right 8% (mirrors, background bleeds)
        top = int(h * 0.18)
        bottom = int(h * 0.80)
        left = int(w * 0.08)
        right = int(w * 0.92)

        body_crop = vehicle_bgr[top:bottom, left:right]
        if body_crop.size == 0:
            body_crop = vehicle_bgr

        # Resize for consistent sampling
        crop_small = cv2.resize(body_crop, (120, 80), interpolation=cv2.INTER_AREA)

        # Convert to HSV
        hsv = cv2.cvtColor(crop_small, cv2.COLOR_BGR2HSV)
        h_channel = hsv[:, :, 0]
        s_channel = hsv[:, :, 1]
        v_channel = hsv[:, :, 2]

        # Filter out extreme glare (V > 250 and S < 20) and extreme dark shadows (V < 22)
        valid_mask = (v_channel >= 22) & ~((v_channel > 250) & (s_channel < 20))

        if np.sum(valid_mask) < 50:
            # Fall back to entire crop if mask is too aggressive
            valid_mask = np.ones((crop_small.shape[0], crop_small.shape[1]), dtype=bool)

        h_valid = h_channel[valid_mask]
        s_valid = s_channel[valid_mask]
        v_valid = v_channel[valid_mask]

        # Calculate median statistics of the valid vehicle body pixels
        median_h = float(np.median(h_valid))
        median_s = float(np.median(s_valid))
        median_v = float(np.median(v_valid))

        # Sample RGB values for accurate representative hex
        rgb_crop = cv2.cvtColor(crop_small, cv2.COLOR_BGR2RGB)
        r_valid = rgb_crop[:, :, 0][valid_mask]
        g_valid = rgb_crop[:, :, 1][valid_mask]
        b_valid = rgb_crop[:, :, 2][valid_mask]

        med_r = int(np.median(r_valid))
        med_g = int(np.median(g_valid))
        med_b = int(np.median(b_valid))
        sampled_hex = f"#{med_r:02X}{med_g:02X}{med_b:02X}"

        # Color classification logic
        # Achromatic: Low saturation (< 36)
        if median_s < 36:
            if median_v < 45:
                color_name = "Black"
                palette_hex = COLOR_PALETTE["Black"]
            elif median_v > 195:
                color_name = "White"
                palette_hex = COLOR_PALETTE["White"]
            elif median_v >= 135:
                color_name = "Silver"
                palette_hex = COLOR_PALETTE["Silver"]
            else:
                color_name = "Grey"
                palette_hex = COLOR_PALETTE["Grey"]
        else:
            # Chromatic colors based on Hue (0-179 in OpenCV HSV)
            if (median_h <= 10) or (median_h >= 170):
                if median_v < 85 and median_s > 60:
                    color_name = "Maroon"
                    palette_hex = COLOR_PALETTE["Maroon"]
                elif median_s < 70 and median_v < 110:
                    color_name = "Brown"
                    palette_hex = COLOR_PALETTE["Brown"]
                else:
                    color_name = "Red"
                    palette_hex = COLOR_PALETTE["Red"]
            elif 11 <= median_h <= 22:
                if median_v < 90:
                    color_name = "Brown"
                    palette_hex = COLOR_PALETTE["Brown"]
                else:
                    color_name = "Orange"
                    palette_hex = COLOR_PALETTE["Orange"]
            elif 23 <= median_h <= 35:
                if median_s < 90 or median_v < 160:
                    color_name = "Gold"
                    palette_hex = COLOR_PALETTE["Gold"]
                else:
                    color_name = "Yellow"
                    palette_hex = COLOR_PALETTE["Yellow"]
            elif 36 <= median_h <= 85:
                color_name = "Green"
                palette_hex = COLOR_PALETTE["Green"]
            elif 86 <= median_h <= 98:
                color_name = "Teal"
                palette_hex = COLOR_PALETTE["Teal"]
            elif 99 <= median_h <= 130:
                if median_v < 80:
                    color_name = "Navy Blue"
                    palette_hex = COLOR_PALETTE["Navy Blue"]
                else:
                    color_name = "Electric Blue"
                    palette_hex = COLOR_PALETTE["Electric Blue"]
            elif 131 <= median_h <= 155:
                color_name = "Purple"
                palette_hex = COLOR_PALETTE["Purple"]
            elif 156 <= median_h < 170:
                color_name = "Maroon"
                palette_hex = COLOR_PALETTE["Maroon"]
            else:
                color_name = "Grey"
                palette_hex = COLOR_PALETTE["Grey"]

        return {
            "colour": color_name,
            "colour_hex": palette_hex,
            "sampled_hex": sampled_hex
        }
