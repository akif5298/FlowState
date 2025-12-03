#!/usr/bin/env python3
"""
Direct model downloader - bypasses xet issues on Windows
"""
import os
import sys
from pathlib import Path

os.environ["HF_HUB_DISABLE_SYMLINKS"] = "1"
os.environ["HF_HUB_ENABLE_XET_SYMLINK"] = "0"

from huggingface_hub import hf_hub_download, list_repo_files

repo_id = "google/timesfm-1.0-200m"
cache_dir = Path.home() / ".cache" / "huggingface" / "hub"

print(f"🔍 Listing files in {repo_id}...")

try:
    files = list_repo_files(repo_id=repo_id)
    print(f"📋 Found {len(files)} files")
    
    # Download each file individually
    for i, file_path in enumerate(files, 1):
        if file_path.endswith(('.pt', '.ckpt', '.bin', '.safetensors', '.pth')):
            print(f"\n[{i}/{len(files)}] Downloading: {file_path}")
            try:
                local_path = hf_hub_download(
                    repo_id=repo_id,
                    filename=file_path,
                    cache_dir=str(cache_dir),
                )
                print(f"   ✅ Downloaded to: {local_path}")
            except Exception as e:
                print(f"   ⚠️  Could not download: {e}")
    
    print("\n✅ Download attempt complete!")
    
except Exception as e:
    print(f"❌ Error: {e}")
    sys.exit(1)
