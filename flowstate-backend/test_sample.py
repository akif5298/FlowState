#!/usr/bin/env python
"""Test the /sample endpoint for a quick sample prediction."""
import requests
import json

url = "http://127.0.0.1:5000/sample"

print("=" * 70)
print("Testing /sample endpoint (no input required)")
print("=" * 70)
print(f"\nURL: GET {url}\n")

try:
    print("Sending request...")
    resp = requests.get(url, timeout=10)
    
    print(f"Status Code: {resp.status_code}\n")
    
    if resp.status_code == 200:
        result = resp.json()
        print("[SUCCESS] Sample prediction received!\n")
        
        forecast = result.get('output', {}).get('forecast', [])
        history = result.get('input', {}).get('history', [])
        
        print(f"Input history ({len(history)} values):")
        print(f"  {history}")
        
        print(f"\nForecast ({len(forecast)} values):")
        print(f"  {[round(v, 2) for v in forecast]}")
        
        print(f"\nStatistics:")
        print(f"  History - Min: {min(history)}, Max: {max(history)}, Mean: {sum(history)/len(history):.2f}")
        print(f"  Forecast - Min: {min(forecast):.2f}, Max: {max(forecast):.2f}, Mean: {sum(forecast)/len(forecast):.2f}")
        
        print(f"\nFull response:")
        print(json.dumps(result, indent=2))
    else:
        print(f"[ERROR] Server returned {resp.status_code}")
        print(f"Response: {resp.text}")

except requests.RequestException as e:
    print(f"[ERROR] Request failed: {e}")
    print("\nMake sure the server is running:")
    print("  cd flowstate-backend")
    print("  python app.py")
