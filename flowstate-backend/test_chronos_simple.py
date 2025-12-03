#!/usr/bin/env python
"""Simple test to verify Chronos model loads and works."""
import torch
from chronos import ChronosPipeline

print("=" * 70)
print("Testing Amazon Chronos Model")
print("=" * 70)

print("\n[1/3] Loading Chronos pipeline...")
try:
    pipeline = ChronosPipeline.from_pretrained(
        "amazon/chronos-t5-tiny",
        device_map="cpu",
        torch_dtype=torch.float32,
    )
    print("✅ Chronos loaded successfully\n")
except Exception as e:
    print(f"❌ Failed to load Chronos: {e}\n")
    exit(1)

print("[2/3] Testing prediction with sample data...")
try:
    # Sample energy history data
    history = [72, 76, 80, 74, 78, 82, 88, 75, 79, 84, 81, 77]
    context = torch.tensor([history], dtype=torch.float32)
    
    # Predict next 12 values
    forecast = pipeline.predict(
        context=context,
        prediction_length=12,
        num_samples=1
    )
    
    prediction_list = forecast[0].mean(axis=0).tolist()
    print(f"Input history: {history}")
    print(f"Forecast: {[round(v, 2) for v in prediction_list]}")
    print("✅ Prediction successful\n")
except Exception as e:
    print(f"❌ Prediction failed: {e}\n")
    exit(1)

print("[3/3] Testing with different horizon...")
try:
    forecast = pipeline.predict(
        context=context,
        prediction_length=8,
        num_samples=1
    )
    prediction_list = forecast[0].mean(axis=0).tolist()
    print(f"Forecast (8 steps): {[round(v, 2) for v in prediction_list]}")
    print("✅ Variable horizon works\n")
except Exception as e:
    print(f"❌ Variable horizon failed: {e}\n")
    exit(1)

print("=" * 70)
print("✅ All Chronos tests passed!")
print("=" * 70)
