#!/usr/bin/env python
import requests
import json
import sys

url = "http://127.0.0.1:5000/predict"
payload = {"history": [1.0, 2.0, 3.0, 4.0, 5.0]}
headers = {"Content-Type": "application/json"}

print(f"Sending request to {url}")
print(f"Payload: {json.dumps(payload)}")
print()

try:
    resp = requests.post(url, json=payload, headers=headers, timeout=120)
    print(f"Status Code: {resp.status_code}")
    print(f"Response Headers: {dict(resp.headers)}")
    print(f"Response Body: {resp.text}")
    print()
    
    if resp.status_code == 200:
        try:
            json_resp = resp.json()
            print("[SUCCESS] Prediction received!")
            print(json.dumps(json_resp, indent=2))
        except:
            print(f"[INFO] Response is not JSON: {resp.text}")
    else:
        print(f"[ERROR] Server returned {resp.status_code}")
except requests.RequestException as e:
    print(f"[ERROR] Request failed: {e}")
    sys.exit(1)

