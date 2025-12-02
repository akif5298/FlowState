#!/usr/bin/env python3
"""
Alternative model fetcher using direct requests - bypasses xet/git-lfs issues
"""
import os
import sys
import urllib.request
import urllib.error
from pathlib import Path
from tqdm import tqdm

# Disable HF environment variables that cause issues
os.environ["HF_HUB_DISABLE_SYMLINKS"] = "1"
os.environ["HF_HUB_ENABLE_XET_SYMLINK"] = "0"

# Use raw GitHub/HuggingFace CDN URLs
print("🔍 Attempting to fetch TimesFM model files...\n")

# The model file is available at HuggingFace CDN
repo_id = "google/timesfm-1.0-200m"
snapshot = "8775f7531211ac864b739fe776b0b255c277e2be"

cache_dir = Path.home() / ".cache" / "huggingface" / "hub" / f"models--{repo_id.replace('/', '--')}" / "snapshots" / snapshot
cache_dir.mkdir(parents=True, exist_ok=True)

checkpoint_dir = cache_dir / "checkpoints" / "checkpoint_1100000" / "state"
checkpoint_dir.mkdir(parents=True, exist_ok=True)

# Files to download from HuggingFace
files_to_download = [
    ("checkpoints/checkpoint_1100000/state/checkpoint", "https://huggingface.co/google/timesfm-1.0-200m/resolve/main/checkpoints/checkpoint_1100000/state/checkpoint"),
    ("checkpoints/checkpoint_1100000/descriptor/descriptor.pbtxt", "https://huggingface.co/google/timesfm-1.0-200m/resolve/main/checkpoints/checkpoint_1100000/descriptor/descriptor.pbtxt"),
]

print(f"📁 Cache directory: {cache_dir}\n")

for local_path, url in files_to_download:
    local_file = cache_dir / local_path
    local_file.parent.mkdir(parents=True, exist_ok=True)
    
    if local_file.exists() and local_file.stat().st_size > 0:
        print(f"✅ Already exists: {local_path}")
        continue
    
    print(f"⬇️  Downloading: {local_path}")
    print(f"   From: {url[:70]}...")
    
    try:
        def download_with_progress(url, filepath):
            def reporthook(blocknum, blocksize, totalsize):
                readsofar = blocknum * blocksize
                if totalsize > 0:
                    percent = readsofar * 1e2 / totalsize
                    s = f"\r   Progress: {percent:5.1f}%  ({readsofar}/{totalsize} bytes)"
                    sys.stderr.write(s)
                    sys.stderr.flush()
            
            urllib.request.urlretrieve(url, filepath, reporthook)
            sys.stderr.write("\n")
        
        download_with_progress(url, str(local_file))
        print(f"   ✅ Saved")
        
    except Exception as e:
        print(f"   ❌ Failed: {e}")

print("\n" + "="*60)
print("NOTE: The TimesFM model includes large binary files hosted on")
print("HuggingFace that require special handling on Windows.")
print("\nBest approach for Windows:")
print("1. Use a Linux/Mac system or WSL2 to download")
print("2. Use the web UI to manually download from HuggingFace")  
print("3. Or use: git clone with proper LFS configuration")
print("="*60)
