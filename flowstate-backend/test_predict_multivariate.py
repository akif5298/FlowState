#!/usr/bin/env python
"""Test the /predict endpoint with multivariate energy data."""
import requests
import json
from datetime import datetime, timedelta

# Sample biometric and activity data
url = "http://127.0.0.1:5000/predict"

# Example payload with realistic energy-related metrics
payload = {
    "past_values": {
        "heart_rate": [72, 76, 80, 74, 78, 82, 88, 75, 79, 84, 81, 77],
        "hrv": [55, 53, 52, 60, 58, 56, 54, 62, 59, 57, 55, 60],
        "sleep_hours": [7.2, 6.1, 8.4, 5.9, 7.0, 6.8, 7.5, 6.2, 8.1, 7.3, 6.9, 7.4],
        "typing_speed": [45, 50, 62, 54, 58, 65, 70, 52, 60, 68, 61, 55],
        "reaction_time_ms": [210, 240, 200, 190, 210, 195, 185, 220, 205, 190, 200, 215],
        "steps": [6000, 8500, 4000, 12000, 7000, 9000, 11000, 5500, 8000, 10000, 7500, 6800]
    },
    "timestamps": [
        (datetime.now() - timedelta(hours=11-i)).isoformat() + "Z" for i in range(12)
    ],
    "forecast_horizon": 12
}

print("=" * 70)
print("Testing /predict endpoint with multivariate energy data")
print("=" * 70)
print(f"\nRequest payload:")
print(json.dumps({
    "past_values": {k: f"[{len(v)} values]" for k, v in payload["past_values"].items()},
    "timestamps": f"[{len(payload['timestamps'])} timestamps]",
    "forecast_horizon": payload["forecast_horizon"]
}, indent=2))

try:
    print(f"\nSending request to {url}...")
    resp = requests.post(url, json=payload, timeout=120)
    
    print(f"Status Code: {resp.status_code}")
    
    if resp.status_code == 200:
        result = resp.json()
        print(f"\n[SUCCESS] Prediction received!")
        print(f"\nMode: {result.get('mode', 'unknown')}")
        if 'message' in result:
            print(f"Message: {result['message']}")
        
        forecast = result.get('forecast', [])
        print(f"\nForecast ({len(forecast)} values):")
        print(f"  Values: {[round(v, 2) for v in forecast[:6]]}... (showing first 6)")
        print(f"  Min: {min(forecast):.2f}")
        print(f"  Max: {max(forecast):.2f}")
        print(f"  Mean: {sum(forecast)/len(forecast):.2f}")
        
        print(f"\nFull response:")
        print(json.dumps(result, indent=2))
    else:
        print(f"\n[ERROR] Server returned {resp.status_code}")
        print(f"Response: {resp.text}")

except requests.RequestException as e:
    print(f"\n[ERROR] Request failed: {e}")
    print("\nMake sure the server is running:")
    print("  cd flowstate-backend")
    print("  python app.py")
