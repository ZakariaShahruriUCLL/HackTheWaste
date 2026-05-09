"""
Trains a Random Forest regressor on leuven_trash.csv and saves the model to model/.

Features engineered from the raw schema (latitude, longitude, time, likelihood):
  - lat, lng
  - hour_of_day, day_of_week, month, is_weekend, is_night, is_rush_hour
  - dist_to_city_center_km
  - dist_to_* (each known hotspot)

Output:
  model/rf_trash.joblib   – trained RandomForestRegressor
  model/feature_names.json – ordered feature list (required by api.py)
  model/metrics.json       – R², MAE on held-out test set
"""

import json
import joblib
import numpy as np
import pandas as pd
from pathlib import Path
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score

MODEL_DIR = Path("model")
MODEL_DIR.mkdir(exist_ok=True)

LEUVEN_CENTER = (50.8798, 4.7005)

HOTSPOTS = {
    "oude_markt":      (50.8793, 4.7005),
    "station":         (50.8820, 4.7163),
    "bondgenotenlaan": (50.8790, 4.7020),
    "ku_leuven":       (50.8751, 4.6980),
    "stadspark":       (50.8761, 4.6956),
    "naamsestraat":    (50.8764, 4.7009),
    "diestsestraat":   (50.8830, 4.7050),
    "ladeuzeplein":    (50.8778, 4.7046),
}


def haversine_km(lat1, lng1, lat2, lng2):
    R = 6371.0
    dlat = np.radians(lat2 - lat1)
    dlng = np.radians(lng2 - lng1)
    a = (np.sin(dlat / 2) ** 2
         + np.cos(np.radians(lat1)) * np.cos(np.radians(lat2)) * np.sin(dlng / 2) ** 2)
    return R * 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))


def engineer_features(df: pd.DataFrame) -> pd.DataFrame:
    ts = pd.to_datetime(df["time"], utc=True).dt.tz_convert("Europe/Brussels")

    df = df.copy()
    df["hour"]       = ts.dt.hour
    df["dow"]        = ts.dt.dayofweek   # 0=Mon … 6=Sun
    df["month"]      = ts.dt.month
    df["is_weekend"] = (df["dow"] >= 5).astype(int)
    df["is_night"]   = ((df["hour"] >= 22) | (df["hour"] <= 3)).astype(int)
    df["is_rush"]    = df["hour"].apply(lambda h: 1 if (7 <= h <= 9 or 17 <= h <= 19) else 0)

    lats = df["latitude"].values
    lngs = df["longitude"].values

    df["dist_center"] = haversine_km(lats, lngs, *LEUVEN_CENTER)

    for name, (hlat, hlng) in HOTSPOTS.items():
        df[f"dist_{name}"] = haversine_km(lats, lngs, hlat, hlng)

    return df


FEATURE_COLS = (
    ["latitude", "longitude", "hour", "dow", "month",
     "is_weekend", "is_night", "is_rush", "dist_center"]
    + [f"dist_{n}" for n in HOTSPOTS]
)


def main():
    for candidate in ["data/leuven_trash_likelihood.csv", "data/leuven_trash.csv"]:
        if Path(candidate).exists():
            csv_path = Path(candidate)
            break
    else:
        raise FileNotFoundError(
            "No CSV found in data/ — expected leuven_trash_likelihood.csv or leuven_trash.csv"
        )

    df = pd.read_csv(csv_path)
    df.columns = [c.lower() for c in df.columns]
    print(f"Loaded {len(df)} rows from {csv_path}")

    df = engineer_features(df)
    X = df[FEATURE_COLS].values
    y = df["likelihood"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    print("Training Random Forest …")
    rf = RandomForestRegressor(
        n_estimators=200,
        max_depth=12,
        min_samples_leaf=4,
        n_jobs=-1,
        random_state=42,
    )
    rf.fit(X_train, y_train)

    y_pred = rf.predict(X_test)
    r2  = r2_score(y_test, y_pred)
    mae = mean_absolute_error(y_test, y_pred)
    print(f"Test R²={r2:.4f}  MAE={mae:.4f}")

    joblib.dump(rf, MODEL_DIR / "rf_trash.joblib")
    (MODEL_DIR / "feature_names.json").write_text(json.dumps(FEATURE_COLS, indent=2))
    (MODEL_DIR / "metrics.json").write_text(
        json.dumps({"r2": round(r2, 4), "mae": round(mae, 4), "n_train": len(X_train)}, indent=2)
    )
    print(f"Saved model → {MODEL_DIR}/rf_trash.joblib")

    importances = sorted(
        zip(FEATURE_COLS, rf.feature_importances_), key=lambda x: -x[1]
    )
    print("\nTop-10 feature importances:")
    for feat, imp in importances[:10]:
        print(f"  {feat:<22} {imp:.4f}")


if __name__ == "__main__":
    main()
