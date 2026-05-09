"""
Generates a synthetic leuven_trash.csv that mirrors the DB schema:
  (id, latitude, longitude, time, likelihood)

Trash likelihood is modelled from real Leuven geography:
  - Oude Markt bar district: high on Fri/Sat nights
  - Leuven train station:    moderate throughout day
  - Bondgenotenlaan:         moderate daytime
  - KU Leuven campus:        weekday peaks
  - Stadspark:               lower, rises on weekends
  - Residential blocks:      baseline low
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta

RNG = np.random.default_rng(42)

# (name, lat, lng, base_likelihood, weekend_boost, night_boost, radius_km)
HOTSPOTS = [
    ("Oude Markt",       50.8793, 4.7005, 0.70, 0.20, 0.25, 0.15),
    ("Train station",    50.8820, 4.7163, 0.45, 0.05, 0.05, 0.20),
    ("Bondgenotenlaan",  50.8790, 4.7020, 0.40, 0.00, 0.00, 0.20),
    ("KU Leuven campus", 50.8751, 4.6980, 0.35, -0.10, 0.05, 0.30),
    ("Stadspark",        50.8761, 4.6956, 0.20, 0.10, 0.00, 0.25),
    ("Naamsestraat",     50.8764, 4.7009, 0.55, 0.15, 0.20, 0.18),
    ("Diestsestraat",    50.8830, 4.7050, 0.38, 0.05, 0.05, 0.22),
    ("Ladeuzeplein",     50.8778, 4.7046, 0.50, 0.10, 0.10, 0.15),
]

LEUVEN_CENTER = (50.8798, 4.7005)
LAT_SPREAD, LNG_SPREAD = 0.04, 0.06   # ~4 km radius

N_SAMPLES = 8_000
START = datetime(2025, 1, 1)
END   = datetime(2026, 5, 1)
SPAN_SECONDS = int((END - START).total_seconds())


def haversine_km(lat1, lng1, lat2, lng2):
    R = 6371.0
    dlat = np.radians(lat2 - lat1)
    dlng = np.radians(lng2 - lng1)
    a = np.sin(dlat / 2) ** 2 + np.cos(np.radians(lat1)) * np.cos(np.radians(lat2)) * np.sin(dlng / 2) ** 2
    return R * 2 * np.arcsin(np.sqrt(a))


def point_likelihood(lat, lng, hour, dow):
    is_weekend = dow >= 5
    is_night   = hour >= 22 or hour <= 3
    is_day     = 9 <= hour <= 18

    base = 0.10
    for _, hlat, hlng, bl, wb, nb, radius in HOTSPOTS:
        dist = haversine_km(lat, lng, hlat, hlng)
        if dist < radius:
            weight = max(0, 1 - dist / radius)
            contrib = bl
            if is_weekend:
                contrib += wb
            if is_night:
                contrib += nb
            if is_day and wb < 0:
                contrib += wb * 0.5
            base += contrib * weight

    base += RNG.normal(0, 0.05)
    return float(np.clip(base, 0.0, 1.0))


def main():
    lats = RNG.normal(LEUVEN_CENTER[0], LAT_SPREAD / 3, N_SAMPLES)
    lngs = RNG.normal(LEUVEN_CENTER[1], LNG_SPREAD / 3, N_SAMPLES)
    lats = np.clip(lats, LEUVEN_CENTER[0] - LAT_SPREAD, LEUVEN_CENTER[0] + LAT_SPREAD)
    lngs = np.clip(lngs, LEUVEN_CENTER[1] - LNG_SPREAD, LEUVEN_CENTER[1] + LNG_SPREAD)

    offsets = RNG.integers(0, SPAN_SECONDS, N_SAMPLES)
    timestamps = [START + timedelta(seconds=int(s)) for s in offsets]

    rows = []
    for i, (lat, lng, ts) in enumerate(zip(lats, lngs, timestamps)):
        hour = ts.hour
        dow  = ts.weekday()
        likelihood = point_likelihood(lat, lng, hour, dow)
        rows.append({
            "id": i + 1,
            "latitude":  round(lat, 6),
            "longitude": round(lng, 6),
            "time":      ts.strftime("%Y-%m-%dT%H:%M:%S"),
            "likelihood": round(likelihood, 4),
        })

    df = pd.DataFrame(rows)
    out = "data/leuven_trash.csv"
    df.to_csv(out, index=False)
    print(f"Wrote {len(df)} rows → {out}")
    print(df.describe())


if __name__ == "__main__":
    main()
