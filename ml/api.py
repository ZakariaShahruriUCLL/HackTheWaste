"""
FastAPI prediction server.

Endpoints:
  GET  /health              → {"status": "ok", "model_metrics": {...}}
  POST /predict             → single prediction
  POST /predict/batch       → batch predictions for heatmap grid
  POST /predict/grid        → auto-generate Leuven grid at given time

Run:
  uvicorn api:app --host 0.0.0.0 --port 8000 --reload
"""

import json
import joblib
import numpy as np
from datetime import datetime
from pathlib import Path
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

MODEL_DIR = Path("model")

# ---------------------------------------------------------------------------
# Load model at startup
# ---------------------------------------------------------------------------

def _load():
    model_path   = MODEL_DIR / "rf_trash.joblib"
    feature_path = MODEL_DIR / "feature_names.json"
    metrics_path = MODEL_DIR / "metrics.json"
    if not model_path.exists():
        raise RuntimeError("Model not found — run train.py first.")
    rf       = joblib.load(model_path)
    features = json.loads(feature_path.read_text())
    metrics  = json.loads(metrics_path.read_text()) if metrics_path.exists() else {}
    return rf, features, metrics

rf, FEATURE_NAMES, MODEL_METRICS = _load()

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

# ---------------------------------------------------------------------------
# Feature engineering (mirrors train.py)
# ---------------------------------------------------------------------------

def haversine_km(lat1, lng1, lat2, lng2):
    R = 6371.0
    dlat = np.radians(lat2 - lat1)
    dlng = np.radians(lng2 - lng1)
    a = (np.sin(dlat / 2) ** 2
         + np.cos(np.radians(lat1)) * np.cos(np.radians(lat2)) * np.sin(dlng / 2) ** 2)
    return R * 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))


def build_row(lat: float, lng: float, ts: datetime) -> list:
    hour  = ts.hour
    dow   = ts.weekday()
    month = ts.month
    is_weekend = int(dow >= 5)
    is_night   = int(hour >= 22 or hour <= 3)
    is_rush    = int((7 <= hour <= 9) or (17 <= hour <= 19))

    dist_center = haversine_km(lat, lng, *LEUVEN_CENTER)
    hotspot_dists = [haversine_km(lat, lng, hlat, hlng) for hlat, hlng in HOTSPOTS.values()]

    return [lat, lng, hour, dow, month, is_weekend, is_night, is_rush, dist_center] + hotspot_dists


def predict_one(lat: float, lng: float, ts: datetime) -> float:
    row = np.array(build_row(lat, lng, ts)).reshape(1, -1)
    score = float(rf.predict(row)[0])
    return round(float(np.clip(score, 0.0, 1.0)), 4)

# ---------------------------------------------------------------------------
# FastAPI app
# ---------------------------------------------------------------------------

app = FastAPI(title="Leuven Trash Likelihood API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class PredictRequest(BaseModel):
    latitude:  float = Field(..., ge=50.7, le=51.0)
    longitude: float = Field(..., ge=4.5,  le=4.9)
    time: str = Field(
        ..., description="ISO-8601 timestamp, e.g. 2026-05-09T22:00:00"
    )


class PredictResponse(BaseModel):
    latitude:   float
    longitude:  float
    time:       str
    likelihood: float


class BatchRequest(BaseModel):
    points: list[PredictRequest]


class GridRequest(BaseModel):
    time: str = Field(..., description="ISO-8601 timestamp")
    lat_min:  float = Field(50.855, ge=50.7, le=51.0)
    lat_max:  float = Field(50.905, ge=50.7, le=51.0)
    lng_min:  float = Field(4.670,  ge=4.5,  le=4.9)
    lng_max:  float = Field(4.730,  ge=4.5,  le=4.9)
    lat_steps: int  = Field(30, ge=5, le=100)
    lng_steps: int  = Field(30, ge=5, le=100)


@app.get("/health")
def health():
    return {"status": "ok", "model_metrics": MODEL_METRICS}


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    try:
        ts = datetime.fromisoformat(req.time)
    except ValueError:
        raise HTTPException(400, "Invalid ISO-8601 time format")
    score = predict_one(req.latitude, req.longitude, ts)
    return PredictResponse(
        latitude=req.latitude, longitude=req.longitude,
        time=req.time, likelihood=score
    )


@app.post("/predict/batch", response_model=list[PredictResponse])
def predict_batch(req: BatchRequest):
    if len(req.points) > 5_000:
        raise HTTPException(400, "Max 5000 points per batch")
    results = []
    for p in req.points:
        try:
            ts = datetime.fromisoformat(p.time)
        except ValueError:
            raise HTTPException(400, f"Invalid time: {p.time}")
        score = predict_one(p.latitude, p.longitude, ts)
        results.append(PredictResponse(
            latitude=p.latitude, longitude=p.longitude,
            time=p.time, likelihood=score
        ))
    return results


@app.post("/predict/grid")
def predict_grid(req: GridRequest):
    """Returns a flat list of {lat, lng, likelihood} for a regular grid — used to render the predictive heatmap."""
    try:
        ts = datetime.fromisoformat(req.time)
    except ValueError:
        raise HTTPException(400, "Invalid ISO-8601 time format")

    lats = np.linspace(req.lat_min, req.lat_max, req.lat_steps)
    lngs = np.linspace(req.lng_min, req.lng_max, req.lng_steps)

    rows = []
    X = []
    coords = []
    for lat in lats:
        for lng in lngs:
            X.append(build_row(lat, lng, ts))
            coords.append((round(float(lat), 6), round(float(lng), 6)))

    preds = rf.predict(np.array(X))
    preds = np.clip(preds, 0.0, 1.0)

    for (lat, lng), score in zip(coords, preds):
        rows.append({"lat": lat, "lng": lng, "likelihood": round(float(score), 4)})

    return {"time": req.time, "count": len(rows), "points": rows}
