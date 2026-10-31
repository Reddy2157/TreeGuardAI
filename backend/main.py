"""
TREEGUARD AI — FastAPI Backend Service
Longitudinal Urban Forest Intelligence & Maintenance Optimization
Endpoints:
  GET  /health
  POST /trees
  GET  /trees
  GET  /trees/{tree_id}
  POST /trees/{tree_id}/observations
  POST /trees/{tree_id}/compare
  POST /trees/{tree_id}/risk
  GET  /weather
  POST /stress/calculate
  GET  /trees/{tree_id}/stress-history
  POST /maintenance/optimize
  GET  /dashboard
"""
import math
import os
import time
import uuid
from datetime import datetime
from typing import Dict, List, Optional
import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")
GEMINI_TTS_MODEL = os.getenv("GEMINI_TTS_MODEL", "gemini-3.8-flash-tts")
VIJAYAWADA_LAT = 16.5062
VIJAYAWADA_LON = 80.6480
WEATHER_CACHE_TTL_SEC = 900  # 15 minutes

app = FastAPI(
    title="TREEGUARD AI API",
    description="Longitudinal Urban Forest Intelligence & Maintenance Optimization Backend",
    version="1.0.0",
)


class TreeCreateRequest(BaseModel):
    species: str = Field(..., min_length=1, max_length=120)
    latitude: float = Field(default=VIJAYAWADA_LAT, ge=-90.0, le=90.0)
    longitude: float = Field(default=VIJAYAWADA_LON, ge=-180.0, le=180.0)
    zone: str = Field(..., min_length=1, max_length=200)
    registrationType: str = Field(default="LIVE_GPS")
    notes: str = Field(default="")


class ObservationCreateRequest(BaseModel):
    visualCondition: str = Field(default="Monitor")
    visibleSymptoms: str = Field(default="")
    possibleConcerns: str = Field(default="")
    supportingEvidence: str = Field(default="")
    recommendedAction: str = Field(default="")
    limitations: str = Field(default="Visual screening only.")
    professionalInspection: bool = Field(default=False)
    source: str = Field(default="FIELD_INSPECTION")


class StressCalculateRequest(BaseModel):
    treeId: str
    previousStressDebt: float = Field(default=30.0, ge=0.0)
    deltaDays: float = Field(default=1.0, ge=0.0)
    heatIndicator: float = Field(..., ge=0.0, le=100.0)
    drynessIndicator: float = Field(..., ge=0.0, le=100.0)
    observationIndicator: float = Field(..., ge=0.0, le=100.0)
    halfLifeDays: float = Field(default=30.0, ge=1.0)
    dataSource: str = Field(default="OPEN_METEO_REGIONAL")


class RiskEvaluateRequest(BaseModel):
    structuralScore: Optional[float] = Field(default=None, ge=0.0, le=100.0)
    exposureScore: Optional[float] = Field(default=None, ge=0.0, le=100.0)
    declineScore: Optional[float] = Field(default=None, ge=0.0, le=100.0)
    weatherScore: Optional[float] = Field(default=None, ge=0.0, le=100.0)
    historyScore: Optional[float] = Field(default=None, ge=0.0, le=100.0)
    visualCondition: str = Field(default="Monitor")


class MaintenanceActionItem(BaseModel):
    actionId: str
    treeId: str
    treeCode: str
    action: str
    estimatedCost: float = Field(..., gt=0.0)
    expectedBenefit: float = Field(..., ge=0.0, le=100.0)
    priorityScore: float = Field(default=50.0, ge=0.0, le=100.0)
    status: str = Field(default="PROPOSED")


class OptimizeBudgetRequest(BaseModel):
    budgetInr: float = Field(default=10000.0, gt=0.0)
    actions: List[MaintenanceActionItem]


# Clearly labelled in-memory Demo Store fallback when Firebase Admin credentials are not configured
DEMO_TREES: Dict[str, dict] = {
    "TG-VJA-0001": {
        "treeId": "TG-VJA-0001",
        "species": "Azadirachta indica (Neem)",
        "latitude": 16.5062,
        "longitude": 80.6480,
        "zone": "MG Road, Benz Circle [DEMO DATA]",
        "registrationType": "DEMO_SEED",
        "registrationDate": "2026-05-14",
        "originalPhotoTag": "BASELINE_NEEM",
        "visualCondition": "Needs inspection",
        "currentPriority": "Critical",
        "currentRiskScore": 81.5,
        "stressDebt": 74.6,
        "createdAt": datetime.utcnow().isoformat(),
        "updatedAt": datetime.utcnow().isoformat(),
    }
}
DEMO_OBSERVATIONS: Dict[str, List[dict]] = {"TG-VJA-0001": []}
DEMO_STRESS_HISTORY: Dict[str, List[dict]] = {"TG-VJA-0001": []}
DEMO_PRIORITY_HISTORY: Dict[str, List[dict]] = {"TG-VJA-0001": []}
DEMO_MAINTENANCE_HISTORY: Dict[str, List[dict]] = {"TG-VJA-0001": []}
WEATHER_CACHE: Dict[str, dict] = {}


def derive_weather_engine_inputs(
    temp_c: float,
    humidity: float,
    precip_mm: float,
    wind_speed: float,
    wind_gusts: float,
    soil_moisture: Optional[float],
) -> dict:
    heat_ht = max(5.0, min(100.0, ((temp_c - 20.0) / 24.0) * 100.0))
    hum_dryness = max(0.0, min(100.0, 100.0 - humidity))
    rain_relief = max(0.0, min(40.0, precip_mm * 4.0))
    if soil_moisture is not None and soil_moisture >= 0.0:
        norm_soil = max(0.0, min(100.0, (soil_moisture / 0.45) * 100.0 if soil_moisture <= 1.0 else soil_moisture))
        soil_dry = 100.0 - norm_soil
        dryness_dt = max(5.0, min(100.0, 0.55 * soil_dry + 0.45 * hum_dryness - rain_relief))
    else:
        dryness_dt = max(10.0, min(100.0, hum_dryness - rain_relief))
    wind_hazard = max(5.0, min(100.0, ((0.4 * wind_speed + 0.6 * wind_gusts) / 60.0) * 100.0))
    weather_w = max(5.0, min(100.0, 0.40 * wind_hazard + 0.35 * heat_ht + 0.25 * dryness_dt))
    return {
        "heatIndicatorHt": round(heat_ht, 1),
        "drynessIndicatorDt": round(dryness_dt, 1),
        "weatherPriorityScoreW": round(weather_w, 1),
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "TREEGUARD AI Backend",
        "configuredGeminiModel": GEMINI_MODEL,
        "configuredTtsModel": GEMINI_TTS_MODEL,
        "defaultRegion": "Vijayawada, Andhra Pradesh, India",
    }


@app.get("/trees")
def list_trees():
    return {"trees": list(DEMO_TREES.values()), "isDemoFallback": True}


@app.post("/trees", status_code=201)
def create_tree(req: TreeCreateRequest):
    tree_id = f"TG-VJA-{len(DEMO_TREES) + 1:04d}"
    now = datetime.utcnow().isoformat()
    record = {
        "treeId": tree_id,
        "species": req.species,
        "latitude": req.latitude,
        "longitude": req.longitude,
        "zone": req.zone,
        "registrationType": req.registrationType,
        "registrationDate": datetime.utcnow().strftime("%Y-%m-%d"),
        "originalPhotoTag": "BASELINE_REGISTRATION",
        "visualCondition": "Healthy appearance",
        "currentPriority": "Low",
        "currentRiskScore": 20.0,
        "stressDebt": 18.0,
        "notes": req.notes,
        "createdAt": now,
        "updatedAt": now,
    }
    DEMO_TREES[tree_id] = record
    return record


@app.get("/trees/{tree_id}")
def get_tree_passport(tree_id: str):
    if tree_id not in DEMO_TREES:
        raise HTTPException(status_code=404, detail=f"Tree {tree_id} not found")
    return {
        "passport": DEMO_TREES[tree_id],
        "observations": DEMO_OBSERVATIONS.get(tree_id, []),
        "stressDebtHistory": DEMO_STRESS_HISTORY.get(tree_id, []),
        "inspectionPriorityHistory": DEMO_PRIORITY_HISTORY.get(tree_id, []),
        "maintenanceHistory": DEMO_MAINTENANCE_HISTORY.get(tree_id, []),
    }


@app.post("/trees/{tree_id}/observations", status_code=201)
def add_tree_observation(tree_id: str, req: ObservationCreateRequest):
    if tree_id not in DEMO_TREES:
        raise HTTPException(status_code=404, detail=f"Tree {tree_id} not found")
    obs = {
        "observationId": f"obs_{uuid.uuid4().hex[:8]}",
        "treeId": tree_id,
        "visualCondition": req.visualCondition,
        "visibleSymptoms": req.visibleSymptoms,
        "possibleConcerns": req.possibleConcerns,
        "supportingEvidence": req.supportingEvidence,
        "recommendedAction": req.recommendedAction,
        "limitations": req.limitations,
        "professionalInspection": req.professionalInspection,
        "source": req.source,
        "observedAt": datetime.utcnow().isoformat(),
    }
    DEMO_OBSERVATIONS.setdefault(tree_id, []).insert(0, obs)
    DEMO_TREES[tree_id]["visualCondition"] = req.visualCondition
    return obs


@app.post("/stress/calculate")
def calculate_stress(req: StressCalculateRequest):
    daily_stress = round(
        0.45 * req.heatIndicator + 0.35 * req.drynessIndicator + 0.20 * req.observationIndicator,
        2,
    )
    decay_lambda = math.log(2.0) / req.halfLifeDays
    decayed_prev = req.previousStressDebt * math.exp(-decay_lambda * req.deltaDays)
    accumulated = round(decayed_prev + daily_stress, 2)
    entry = {
        "stressRecordId": f"stress_{uuid.uuid4().hex[:8]}",
        "treeId": req.treeId,
        "date": datetime.utcnow().strftime("%Y-%m-%d"),
        "heatIndicator": req.heatIndicator,
        "drynessIndicator": req.drynessIndicator,
        "observationIndicator": req.observationIndicator,
        "dailyStress": daily_stress,
        "stressDebt": accumulated,
        "dataSource": req.dataSource,
        "createdAt": datetime.utcnow().isoformat(),
    }
    DEMO_STRESS_HISTORY.setdefault(req.treeId, []).append(entry)
    return entry


@app.get("/trees/{tree_id}/stress-history")
def get_stress_history(tree_id: str):
    return {"treeId": tree_id, "history": DEMO_STRESS_HISTORY.get(tree_id, [])}


@app.post("/trees/{tree_id}/risk")
def evaluate_tree_risk(tree_id: str, req: RiskEvaluateRequest):
    """
    Explainable Tree Inspection-Priority Engine
    Formula: R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T
    Each factor is normalized to a 0-100 scale.
    """
    weights = [
        ("S", "Structural Concerns", req.structuralScore, 0.35),
        ("E", "Public / Property Exposure", req.exposureScore, 0.25),
        ("D", "Visible Canopy / Trunk Decline", req.declineScore, 0.20),
        ("W", "Regional Weather Stress", req.weatherScore, 0.10),
        ("T", "Inspection Lag / History", req.historyScore, 0.10),
    ]
    available_weight = sum(w for _, _, val, w in weights if val is not None)
    if available_weight <= 0:
        raise HTTPException(status_code=400, detail="At least one risk factor must be provided")

    factor_breakdown = []
    reasons = []
    for code, label, val, base_w in weights:
        if val is not None:
            norm_val = max(0.0, min(100.0, float(val)))
            eff_w = base_w / available_weight
            weighted_pts = round(norm_val * eff_w, 1)
            factor_breakdown.append(
                {
                    "code": code,
                    "label": label,
                    "normalizedScore": round(norm_val, 1),
                    "baseWeight": base_w,
                    "effectiveWeight": round(eff_w, 2),
                    "weightedContribution": weighted_pts,
                }
            )
            reasons.append(f"{code} ({label}): {norm_val:.0f}/100 * {eff_w:.2f} = +{weighted_pts} pts")
        else:
            factor_breakdown.append(
                {
                    "code": code,
                    "label": label,
                    "normalizedScore": None,
                    "baseWeight": base_w,
                    "effectiveWeight": 0.0,
                    "weightedContribution": 0.0,
                }
            )

    score = round(
        sum(f["normalizedScore"] * f["effectiveWeight"] for f in factor_breakdown if f["normalizedScore"] is not None),
        1,
    )
    category = (
        "Critical"
        if score >= 75
        else "High"
        if score >= 50
        else "Moderate"
        if score >= 25
        else "Low"
    )
    missing = [code for code, _, val, _ in weights if val is None]
    completeness = int(((5 - len(missing)) / 5.0) * 100)
    limitations = [
        "Index R is an explainable inspection-priority ranking, not a biomechanical probability of failure.",
        "Weather factor W uses regional Open-Meteo telemetry, not a root-zone sensor at the tree base.",
    ]
    if missing:
        limitations.append(f"Missing factors ({', '.join(missing)}) excluded via provisional weight normalization.")

    result = {
        "treeId": tree_id,
        "finalPriorityScore": score,
        "category": category,
        "factors": factor_breakdown,
        "reasons": reasons,
        "missingFactors": missing,
        "dataCompletenessPercent": completeness,
        "limitations": limitations,
    }
    DEMO_PRIORITY_HISTORY.setdefault(tree_id, []).insert(0, result)
    return result


@app.post("/maintenance/optimize")
def optimize_maintenance(req: OptimizeBudgetRequest):
    ranked = sorted(
        req.actions,
        key=lambda a: (a.expectedBenefit / a.estimatedCost, a.priorityScore),
        reverse=True,
    )
    selected, unfunded = [], []
    spent = 0.0
    for item in ranked:
        if spent + item.estimatedCost <= req.budgetInr:
            selected.append(item)
            spent += item.estimatedCost
        else:
            unfunded.append(item)
    return {
        "budgetInr": req.budgetInr,
        "selectedActions": selected,
        "unfundedActions": unfunded,
        "totalEstimatedExpenditure": round(spent, 2),
        "remainingBudget": round(req.budgetInr - spent, 2),
        "expectedAggregateBenefit": round(sum(i.expectedBenefit for i in selected), 2),
    }


@app.get("/weather")
def get_weather(lat: float = VIJAYAWADA_LAT, lon: float = VIJAYAWADA_LON, force_refresh: bool = False):
    cache_key = f"{lat:.3f},{lon:.3f}"
    now = time.time()
    if not force_refresh and cache_key in WEATHER_CACHE:
        cached = WEATHER_CACHE[cache_key]
        if now - cached["timestamp"] <= WEATHER_CACHE_TTL_SEC:
            return {**cached["payload"], "cacheStatus": "CACHED_RECENT"}

    url = (
        f"https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}"
        "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_gusts_10m"
        "&hourly=soil_moisture_0_to_1cm"
        "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_gusts_10m_max"
        "&timezone=Asia%2FKolkata"
    )
    try:
        resp = requests.get(url, timeout=8)
        resp.raise_for_status()
        raw = resp.json()
        cur = raw.get("current", {})
        soil_arr = raw.get("hourly", {}).get("soil_moisture_0_to_1cm", [])
        soil_val = next((v for v in soil_arr if v is not None), None)
        engine_inputs = derive_weather_engine_inputs(
            temp_c=cur.get("temperature_2m", 34.0),
            humidity=cur.get("relative_humidity_2m", 60.0),
            precip_mm=cur.get("precipitation", 0.0),
            wind_speed=cur.get("wind_speed_10m", 14.0),
            wind_gusts=cur.get("wind_gusts_10m", 26.0),
            soil_moisture=soil_val,
        )
        payload = {
            "source": "Open-Meteo Regional Context",
            "cacheStatus": "LIVE_API",
            "coordinates": {"latitude": lat, "longitude": lon},
            "current": {
                "temperature": cur.get("temperature_2m"),
                "humidity": cur.get("relative_humidity_2m"),
                "precipitation": cur.get("precipitation"),
                "windSpeed": cur.get("wind_speed_10m"),
                "windGusts": cur.get("wind_gusts_10m"),
                "soilMoisture": soil_val,
            },
            "engineInputs": engine_inputs,
            "forecast7Day": raw.get("daily", {}),
        }
        WEATHER_CACHE[cache_key] = {"timestamp": now, "payload": payload}
        return payload
    except Exception as exc:
        if cache_key in WEATHER_CACHE:
            return {
                **WEATHER_CACHE[cache_key]["payload"],
                "cacheStatus": "CACHED_FALLBACK",
                "warning": str(exc),
            }
        return {
            "source": "DEMO_FALLBACK",
            "cacheStatus": "DEMO_FALLBACK",
            "error": str(exc),
            "engineInputs": derive_weather_engine_inputs(35.4, 58.0, 0.0, 16.8, 31.2, 0.21),
        }


@app.get("/dashboard")
def get_dashboard():
    trees = list(DEMO_TREES.values())
    return {
        "totalRegisteredTrees": len(trees),
        "treesRequiringInspection": sum(1 for t in trees if t["visualCondition"] == "Needs inspection"),
        "averageStressDebt": round(sum(t["stressDebt"] for t in trees) / max(len(trees), 1), 1),
        "isDemoData": True,
    }
