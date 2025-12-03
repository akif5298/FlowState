#!/usr/bin/env python3
"""
Pre-download Chronos model during Railway build phase
"""
import os
import sys

print("=" * 60)
print("Pre-downloading Chronos model for Railway deployment...")
print("=" * 60)

# Set environment variables for download
os.environ["HF_HUB_DISABLE_SYMLINKS"] = "1"
os.environ["HF_HUB_DOWNLOAD_TIMEOUT"] = "300"

try:
    from chronos import ChronosPipeline
    import torch
    
    print("⬇️  Downloading amazon/chronos-t5-tiny (~200MB)...")
    print("⏱️  This may take 2-5 minutes...")
    print()
    
    # Download and cache the model
    pipeline = ChronosPipeline.from_pretrained(
        "amazon/chronos-t5-tiny",
        device_map="cpu",
        torch_dtype=torch.float32,
    )
    
    print()
    print("✅ Chronos model downloaded and cached successfully!")
    print(f"📦 Model cache: {os.environ.get('HF_HOME', '~/.cache/huggingface')}")
    print("=" * 60)
    
except Exception as e:
    print()
    print(f"⚠️  Model download failed: {e}")
    print("Server will fall back to sample prediction mode")
    print("=" * 60)
    # Don't exit - let the server start anyway with sample mode
    sys.exit(0)
