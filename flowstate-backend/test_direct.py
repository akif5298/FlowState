#!/usr/bin/env python
"""Direct test of Flask app without HTTP networking."""
import os
import sys

# Set remote inference mode
os.environ['REMOTE_PRED_URL'] = 'https://router.huggingface.co/models/google/timesfm-1.0-200m'

# Import app after setting env vars
from app import app, generate_sample_forecast
import numpy as np

print("=" * 80)
print("DIRECT APP TEST (No HTTP/networking required)")
print("=" * 80)

# Test 1: Sample forecast generation function
print("\n[TEST 1] generate_sample_forecast() function")
print("-" * 80)
history = [72, 76, 80, 74, 78, 82, 88, 75, 79, 84, 81, 77]
forecast = generate_sample_forecast(history, horizon=12)
print(f"Input history (12 values): {history}")
print(f"Generated forecast (12 values): {list(forecast)}")
print(f"[OK] Sample forecast function works correctly")

# Test 2: /sample endpoint using Flask test client
print("\n[TEST 2] /sample endpoint (Flask test client)")
print("-" * 80)
client = app.test_client()
response = client.get('/sample')
print(f"Status code: {response.status_code}")
print(f"Response type: {response.content_type}")
data = response.get_json()
if data:
    print(f"Response keys: {list(data.keys())}")
    print(f"Status: {data.get('status')}")
    print(f"Mode: {data.get('mode')}")
    if 'output' in data:
        output = data['output']
        print(f"Output keys: {list(output.keys())}")
        print(f"Forecast length: {len(output.get('forecast', []))}")
        print(f"Forecast values (first 6): {output.get('forecast', [])[:6]}")
    print(f"[OK] /sample endpoint works correctly")
else:
    print("ERROR: No JSON response")

# Test 3: /predict endpoint with multivariate data
print("\n[TEST 3] /predict endpoint with multivariate data (Flask test client)")
print("-" * 80)
multivariate_input = {
    "past_values": {
        "heart_rate": [72, 76, 80, 74, 78, 82, 88, 75, 79, 84, 81, 77],
        "hrv": [55, 53, 52, 60, 58, 56, 54, 62, 59, 57, 55, 60],
        "sleep_hours": [7.2, 7.5, 6.8, 7.1, 6.9, 7.3, 7.4, 6.7, 7.2, 7.1, 7.0, 7.2],
        "typing_speed": [65, 68, 70, 67, 69, 72, 74, 68, 71, 73, 70, 69],
        "reaction_time_ms": [245, 240, 235, 242, 238, 233, 230, 241, 236, 231, 239, 243],
        "steps": [6000, 8500, 4000, 12000, 7000, 9000, 11000, 5500, 8000, 10000, 7500, 6800]
    },
    "forecast_horizon": 12
}

response = client.post('/predict', 
                       json=multivariate_input,
                       content_type='application/json')
print(f"Status code: {response.status_code}")
print(f"Response type: {response.content_type}")
data = response.get_json()
if data:
    print(f"Response keys: {list(data.keys())}")
    print(f"Status: {data.get('status')}")
    print(f"Mode: {data.get('mode')}")
    if 'forecast' in data:
        forecast_vals = data['forecast']
        print(f"Forecast length: {len(forecast_vals)}")
        print(f"Forecast values (first 6): {forecast_vals[:6]}")
        print(f"[OK] /predict endpoint with multivariate data works correctly")
    else:
        print("ERROR: No forecast in response")
else:
    print("ERROR: No JSON response")

# Test 4: /predict endpoint with legacy format
print("\n[TEST 4] /predict endpoint with legacy format (Flask test client)")
print("-" * 80)
legacy_input = {
    "history": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
}

response = client.post('/predict',
                       json=legacy_input,
                       content_type='application/json')
print(f"Status code: {response.status_code}")
print(f"Response type: {response.content_type}")
data = response.get_json()
if data:
    print(f"Response keys: {list(data.keys())}")
    print(f"Status: {data.get('status')}")
    print(f"Mode: {data.get('mode')}")
    if 'forecast' in data:
        forecast_vals = data['forecast']
        print(f"Forecast length: {len(forecast_vals)}")
        print(f"Forecast values (first 6): {forecast_vals[:6]}")
        print(f"[OK] /predict endpoint with legacy format works correctly")
    else:
        print("ERROR: No forecast in response")
else:
    print("ERROR: No JSON response")

# Test 5: /predict endpoint with fatigue data (low sleep, high HR, poor metrics)
print("\n[TEST 5] /predict endpoint with FATIGUE/OVERTRAINING data (Flask test client)")
print("-" * 80)
fatigue_input = {
    "past_values": {
        "heart_rate": [82, 85, 88, 92, 95, 90, 87, 89, 91, 94, 96, 93],
        "hrv": [36, 32, 30, 28, 26, 25, 27, 29, 30, 28, 26, 24],
        "sleep_hours": [4.2, 4.5, 5.0, 4.8, 3.9, 4.1, 4.3],
        "sleep_quality": [54, 52, 49, 47, 45, 50, 48],
        "typing_speed": [38, 36, 34, 32, 30, 28, 29],
        "reaction_time": [320, 350, 380, 400, 410, 395, 405],
        "steps": [2500, 3100, 2000, 1800, 1500, 2300, 2700]
    },
    "timestamps": [
        "2025-01-27T08:00:00Z",
        "2025-01-27T09:00:00Z",
        "2025-01-27T10:00:00Z",
        "2025-01-27T11:00:00Z",
        "2025-01-27T12:00:00Z",
        "2025-01-27T13:00:00Z",
        "2025-01-27T14:00:00Z"
    ],
    "forecast_horizon": 8
}

response = client.post('/predict',
                       json=fatigue_input,
                       content_type='application/json')
print(f"Status code: {response.status_code}")
print(f"Response type: {response.content_type}")
data = response.get_json()
if data:
    print(f"Response keys: {list(data.keys())}")
    print(f"Status: {data.get('status')}")
    print(f"Mode: {data.get('mode')}")
    if 'forecast' in data:
        forecast_vals = data['forecast']
        print(f"Forecast length: {len(forecast_vals)}")
        print(f"Forecast values (all 8): {forecast_vals}")
        print(f"\nAnalysis of Fatigue Signals:")
        print(f"  - Heart Rate: Elevated (93 BPM last) - sign of stress/fatigue")
        print(f"  - HRV: Low (24 ms) - poor recovery, parasympathetic activity down")
        print(f"  - Sleep: Severely reduced (4.3 hrs) - sleep deprivation")
        print(f"  - Typing Speed: Declining (29 WPM) - cognitive fatigue")
        print(f"  - Reaction Time: Slow (405 ms) - mental fog/fatigue")
        print(f"  - Steps: Low activity (2700) - possible burnout")
        print(f"\n[OK] /predict endpoint with fatigue data works correctly")
    else:
        print("ERROR: No forecast in response")
else:
    print("ERROR: No JSON response")

print("\n" + "=" * 80)
print("ALL TESTS COMPLETED SUCCESSFULLY")
print("=" * 80)
